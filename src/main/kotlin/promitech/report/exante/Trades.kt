package promitech.report.exante

import promitech.currencyrateconverter.NbpPlnRateRepository
import promitech.currencyrateconverter.model.MapList
import promitech.currencyrateconverter.model.Money
import promitech.report.exante.TransactionSide.buy
import java.math.BigDecimal
import java.time.LocalDateTime

class SymbolId(val symbolIdStr: String) {

    data class Country(val code: String, val name: String)

    val paper: String
    val stockExchange: String

    init {
        val pointIdx = symbolIdStr.indexOf(".")
        if (pointIdx == -1) {
            throw IllegalArgumentException("can not parse symbolId: $symbolIdStr")
        }
        paper = symbolIdStr.substring(0, pointIdx)
        stockExchange = symbolIdStr.substring(pointIdx + 1)
    }

    fun country(): Country {
        val country = countryByStockExchange.get(stockExchange)
        if (country == null) {
            throw IllegalArgumentException("can not find country by stockExchange: $stockExchange for symbol $symbolIdStr")
        }
        return country
    }

    companion object {

        // https://www.pit.pl/intrastat/kody-panstw-intrastat-922638
        val countryByStockExchange = mapOf(
            "ARCA" to Country("US", "Stany Zjedn. Ameryki"),
            "NYSE" to Country("US", "Stany Zjedn. Ameryki"),
            "NASDAQ" to Country("US", "Stany Zjedn. Ameryki"),
            "AMEX" to Country("US", "Stany Zjedn. Ameryki"),
            "SOMX" to Country("SE", "Szwecja"),
            "NOMX" to Country("SE", "Szwecja"),
            "HKEX" to Country("CN", "Chiny"),
            "SGX" to Country("SG", "Singapur"),
            "TMX" to Country("CA", "Kanada"),
            "TSE" to Country("JP", "Japonia"),
        )
    }
}

enum class TransactionSide {
    sell, buy
}

data class Transaction(
    val side: TransactionSide,
    val date: LocalDateTime,
    val symbolId: String,
    val isin: String,
    val price: Money,
    val amount: BigDecimal,
    val commision: Money,
    val profitLoss: Money,
    val orderId: String,
    val orderState: Int
)

class ProfitLoss(
    val openDate: LocalDateTime,
    val openPrice: Money,
    val closeDate: LocalDateTime,
    val closePrice: Money,
    val amount: BigDecimal,
    val direction: TransactionSide,
    val orderId: String,
    val orderState: Int
) {

    fun calculateInPLN(rateRepository: NbpPlnRateRepository): Money {
        val openInPLN = rateRepository.convertToPLN(openDate.toLocalDate(), openPrice)
        val closeInPLN = rateRepository.convertToPLN(closeDate.toLocalDate(), closePrice)
        if (direction == buy) {
            return openInPLN.multiply(amount) - closeInPLN.multiply(amount)
        }
        return closeInPLN.multiply(amount) - openInPLN.multiply(amount)
    }

    fun calculate(): Money {
        if (direction == buy) {
            return openPrice.multiply(amount) - closePrice.multiply(amount)
        }
        return closePrice.multiply(amount) - openPrice.multiply(amount)
    }
}

fun calculate(transactions: List<Transaction>): Pair<List<ProfitLoss>, List<Transaction>> {
    val profitLossList = ArrayList<ProfitLoss>()

    val rest = ArrayList<Transaction>()
    val list = ArrayList<Transaction>(transactions)

    while (list.isNotEmpty()) {
        val transaction = list.removeAt(0)

        if (rest.isEmpty()) {
            rest.add(transaction)
            continue
        }
        if (rest.isNotEmpty()) {
            if (rest.last().side == transaction.side) {
                rest.add(transaction)
                continue
            }
            reduce(rest, transaction, profitLossList)
        }
    }
    return Pair(profitLossList, rest)
}

fun reduce(rest: ArrayList<Transaction>, transaction: Transaction, profitLossList: ArrayList<ProfitLoss>) {
    var amountToReduce = transaction.amount

    while (rest.isNotEmpty() && amountToReduce.compareTo(BigDecimal.ZERO) != 0) {
        val firstToReduce = rest.removeAt(0)
        if (firstToReduce.amount == amountToReduce) {
            profitLossList += ProfitLoss(
                firstToReduce.date, firstToReduce.price, transaction.date, transaction.price, firstToReduce.amount, transaction.side,
                transaction.orderId, transaction.orderState
            )
            amountToReduce -= firstToReduce.amount
        } else
            if (firstToReduce.amount > amountToReduce) {
                profitLossList += ProfitLoss(
                    firstToReduce.date, firstToReduce.price, transaction.date, transaction.price, amountToReduce, transaction.side,
                    transaction.orderId, transaction.orderState
                )
                rest.add(0, firstToReduce.copy(amount = firstToReduce.amount - amountToReduce))
                amountToReduce -= amountToReduce
            } else
                if (firstToReduce.amount < amountToReduce) {
                    profitLossList += ProfitLoss(
                        firstToReduce.date, firstToReduce.price, transaction.date, transaction.price, firstToReduce.amount, transaction.side,
                        transaction.orderId, transaction.orderState
                    )
                    if (rest.isEmpty()) {
                        rest.add(0, transaction.copy(amount = amountToReduce - firstToReduce.amount))
                        break;
                    }
                    amountToReduce -= firstToReduce.amount
                }
    }
}

fun calculateTransactions(
    transactionsPerIsin: MapList<String, Transaction>,
    year: Int
): MapList<String, ProfitLoss> {

    val profitLossesByIsin = MapList<String, ProfitLoss>()

    println("isin.size: " + transactionsPerIsin.size())
    for ((isin, transactions) in transactionsPerIsin.entities()) {
        transactions.sortWith(
            compareBy<Transaction> { transaction -> transaction.date }
                .thenComparing { transaction -> transaction.orderState }
        )
        val (profitLosses, temporaryState) = calculate(transactions)

//            if (isin == "US49705R1059") {
//                println("isin: " + isin + ", transactions.size: " + transactions.size)
//                for (transaction in transactions) {
//                    println("  " + transaction.side + ", " + transaction.date + ", " + transaction.amount + ", order[${transaction.orderState}]")
//                }
//
//                if (temporaryState.isNotEmpty()) {
//                    println("  temporaryState.size: " + temporaryState.size)
//                    for (tmpState in temporaryState) {
//                        println("    " + tmpState.side + " " + tmpState.amount)
//                    }
//                }
//
//                if (profitLosses.isNotEmpty()) {
//                    println("  profitLosses.size: " + profitLosses.size)
//                    for (profitLoss in profitLosses) {
//                        println("    " + profitLoss.direction + " " + profitLoss.amount + " " + profitLoss.closePrice + " " + profitLoss.closeDate + " " + profitLoss.orderId)
//                        println("      calculation: " + profitLoss.calculate())
//                    }
//                }
//            }

        for (profitLoss in profitLosses) {
            if (profitLoss.closeDate.year == year) {
                profitLossesByIsin.put(isin, profitLoss)
            }
        }
    }
    return profitLossesByIsin
}

fun calculateOtherCosts(transactionsPerIsin: MapList<String, Transaction>, year: Int, rateRepository: NbpPlnRateRepository): Money {
    var costs = Money.ZERO_PLN
    for ((_, transactions) in transactionsPerIsin.entities()) {
        for (transaction in transactions) {
            if (transaction.date.year == year) {
                val cost = rateRepository.convertToPLN(transaction.date.toLocalDate(), transaction.commision)
                costs += cost
            }
        }
    }
    return costs
}
