package promitech.report.exante

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import promitech.currencyrateconverter.*
import promitech.currencyrateconverter.model.Currency
import promitech.currencyrateconverter.model.MapList
import promitech.currencyrateconverter.model.Money
import promitech.report.exante.TransactionSide.buy
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

class PitZG(
    // PIT/ZG pole 6
    val countryName: String,
    // PIT/ZG pole 7
    val countryCode: String
) {

    var sumOfAllIncome = Money.ZERO_PLN
        private set

    var sumOfAllCostsOfIncome = Money.ZERO_PLN
        private set

    // PIT/ZG pole 29
    fun profit(): Money {
        val sum = sumOfAllIncome - sumOfAllCostsOfIncome
        if (sum < Money.ZERO_PLN) {
            return Money.ZERO_PLN
        }
        return sum
    }

    fun addProfitLose(plnCostOfIncome: Money, plnIncome: Money) {
        sumOfAllCostsOfIncome += plnCostOfIncome
        sumOfAllIncome += plnIncome
    }

}

class TradeYearlyDeclaration {
    // pit-38 pole 20
    var sumOfAllIncome = Money.ZERO_PLN
        private set

    // pit-38 pole 21
    var sumOfAllCostsOfIncome = Money.ZERO_PLN
        private set

    var otherCosts = Money.ZERO_PLN
        private set

    val pitgdByCountry = HashMap<String, PitZG>()

    fun profit(): Money {
        return sumOfAllIncome - (sumOfAllCostsOfIncome + otherCosts)
    }

    fun addOtherCosts(costs: Money) {
        otherCosts += costs
    }

    fun addProfitLose(profitLoss: ProfitLoss, rateRepository: NbpPlnRateRepository, symbolId: SymbolId) {
        val plnCostOfIncome = rateRepository.convertToPLN(profitLoss.openDate.toLocalDate(), profitLoss.openPrice.multiply(profitLoss.amount))
        val plnIncome = rateRepository.convertToPLN(profitLoss.closeDate.toLocalDate(), profitLoss.closePrice.multiply(profitLoss.amount))

        sumOfAllCostsOfIncome += plnCostOfIncome
        sumOfAllIncome += plnIncome

        findPitZG(symbolId.country()).addProfitLose(plnCostOfIncome, plnIncome)
    }

    fun addProfitLose(date: LocalDate, income: Money, costOfIncome: Money, rateRepository: NbpPlnRateRepository, symbolId: SymbolId) {
        val rate = rateRepository.rate(date, income.currency)
        val plnCostOfIncome = rate.convertToPLN(costOfIncome)
        val plnIncome = rate.convertToPLN(income)

        sumOfAllCostsOfIncome += plnCostOfIncome
        sumOfAllIncome += plnIncome

        findPitZG(symbolId.country()).addProfitLose(plnCostOfIncome, plnIncome)
    }

    private fun findPitZG(country: SymbolId.Country): PitZG {
        var pitZG = pitgdByCountry.get(country.code)
        if (pitZG == null) {
            pitZG = PitZG(country.name, country.code)
            pitgdByCountry.put(country.code, pitZG)
        }
        return pitZG
    }
}

class TradeReport(
    val inputFileName: String,
    val outputFileName: String,
    val rateRepository: NbpPlnRateRepository
) {

    val workbook: Workbook

    init {
        val inputStream = Files.newInputStream(Paths.get(inputFileName))
        workbook = WorkbookFactory.create(inputStream)
        inputStream.close()
    }

    fun save() {
        val outputStream = Files.newOutputStream(Paths.get(outputFileName))
        workbook.write(outputStream)
        workbook.close()
        outputStream.close()
    }

    fun calculateTrades(year: Int) {
        val transactionsPerIsin = loadTransactionsPerIsin()
        val profitLossByIsin = calculateTransactions(transactionsPerIsin, year)

        val yearlyDeclaration = TradeYearlyDeclaration()
        yearlyDeclaration.addOtherCosts(calculateOtherCosts(transactionsPerIsin, year, rateRepository))

        putProfitLossesInSheet(profitLossByIsin, yearlyDeclaration)

        println("")
        println("Podsumowanie PIT-38 rok $year")
        println("  Inne koszty/prowizje: " + yearlyDeclaration.otherCosts)
        println("  Przychod (Pole 22): " + yearlyDeclaration.sumOfAllIncome)
        println("  Koszt uzyskania przychodu (Pole 23): " + yearlyDeclaration.sumOfAllCostsOfIncome)
        println("  Dochod (Pole 26): " + yearlyDeclaration.profit())

        println("Podsumowanie PIZ/ZG")
        var index = 0
        for ((_, pitZG) in yearlyDeclaration.pitgdByCountry) {
            index++
            println("$index.")
            println("  Panstwo (Pole 6): " + pitZG.countryName)
            println("  Kod kraju (Pole 7): " + pitZG.countryCode)
            println("  Dochód (Pole 29): " + pitZG.profit())
        }
    }

    fun loadTransactionsPerIsin(): MapList<String, Transaction> {
        val sheet = workbook.getSheetAt(0)
        val transactionsPerIsin = MapList<String, Transaction>()

        for (row in sheet.rowIterator()) {
            if (row.rowNum == 0) {
                continue
            }
            val transactionSide = parseTransactionSide(row.getCell(2).cellAsString())
            val transactionType = row.getCell(5).cellAsString()
            if (transactionType == "STOCK") {
                val transaction = Transaction(
                    side = transactionSide,
                    date = tradeDate(row.getCell(0)),
                    symbolId = row.getCell(3).cellAsString(),
                    isin = row.getCell(4).cellAsString(),
                    price = Money(row.getCell(6).cellAsBigDecimal(), Currency(row.getCell(7).cellAsString())),
                    amount = row.getCell(8).cellAsBigDecimal(),
                    commision = Money(row.getCell(9).cellAsBigDecimal(), Currency(row.getCell(10).cellAsString())),
                    profitLoss = Money(row.getCell(11).cellAsBigDecimal(), Currency(row.getCell(7).cellAsString())),
                    orderId = row.getCell(13).cellAsString(),
                    orderState = row.getCell(14).cellAsInt()
                )
                transactionsPerIsin.put(transaction.isin, transaction)
            }
        }
        return transactionsPerIsin
    }

    fun sumAllProfitLossForOrderId(orderId: String, orderState: Int, profitLossesList: List<ProfitLoss>): Money? {
        var sum: Money? = null
        for (profitLoss in profitLossesList) {
            if (profitLoss.orderId == orderId && profitLoss.orderState == orderState) {
                if (sum == null) {
                    sum = profitLoss.calculate()
                } else {
                    sum += profitLoss.calculate()
                }
            }
        }
        return sum
    }

    fun sumAllProfitLossForOrderIdInPLN(orderId: String, orderState: Int, profitLossesList: List<ProfitLoss>): Money? {
        var sum: Money? = null
        for (profitLoss in profitLossesList) {
            if (profitLoss.orderId == orderId && profitLoss.orderState == orderState) {
                if (sum == null) {
                    sum = profitLoss.calculateInPLN(rateRepository)
                } else {
                    sum += profitLoss.calculateInPLN(rateRepository)
                }
            }
        }
        return sum
    }

    private fun putProfitLossesInSheet(profitLossByIsin: MapList<String, ProfitLoss>, tradeYearlyDeclaration: TradeYearlyDeclaration): TradeYearlyDeclaration {

        val sheet = workbook.getSheetAt(0)

        sheet.getRow(0).writableCell(19).setCellValue("Zysk/Strata PLN")
        sheet.getRow(0).writableCell(20).setCellValue("Obliczony Zysk/Strata")
        sheet.getRow(0).writableCell(21).setCellValue("Obliczony Zysk/Strata PLN")
        for (row in sheet.rowIterator()) {
            if (row.rowNum == 0) {
                continue
            }
            val date = tradeDate(row.getCell(0))
            val symbolId = SymbolId(row.getCell(3).cellAsString())
            val isin = row.getCell(4).cellAsString()
            val orderId = row.getCell(13).cellAsString()
            val orderState = row.getCell(14).cellAsInt()
            val reportProfitLoss = Money(row.getCell(11).cellAsBigDecimal(), Currency(row.getCell(7).cellAsString()))
            if (!reportProfitLoss.isZero()) {
                val reportProfitLossPLN = rateRepository.convertToPLN(date.toLocalDate(), reportProfitLoss)
                row.writableCell(19).setCellValue(reportProfitLossPLN.toDouble())
            }

            val profitLosses = profitLossByIsin.getList(isin)
            if (profitLosses != null) {
                val profitLoss = sumAllProfitLossForOrderId(orderId, orderState, profitLosses)
                var profitLossPLN = sumAllProfitLossForOrderIdInPLN(orderId, orderState, profitLosses)
                if (profitLoss != null) {
                    // Do zeznania potrzebuje cene otwarcia i tym samym musze obliczyc samemu Zysk/Strate
                    // Sa przypadki ze wartosci z raportu nie odpowiada obliczonej, np split, resplit
                    // Sa tez jakies przypadki z ulamkami. Dla takich sytuacji biore wartosc Z/S z raportu i data open/close na date transakcji.
                    if (!reportProfitLoss.isEqualInScale(profitLoss, 0)) {
                        println("Incorrect profit lose calculation isin: $isin, orderId: $orderId, report: $reportProfitLoss, calculated: $profitLoss")
                        println("  Can not use NBP rate calculation, use close position currency rate.")
                        profitLossPLN = rateRepository.convertToPLN(date.toLocalDate(), reportProfitLoss)

                        val closePrice = Money(row.getCell(6).cellAsBigDecimal(), Currency(row.getCell(7).cellAsString()))
                        val closeAmount = row.getCell(8).cellAsBigDecimal()
                        val income = closePrice.multiply(closeAmount)
                        val costOfIncome = income - reportProfitLoss
                        tradeYearlyDeclaration.addProfitLose(date.toLocalDate(), income, costOfIncome, rateRepository, symbolId)
                    } else {
                        profitLosses.asSequence()
                            .filter { pl -> pl.orderId == orderId && pl.orderState == orderState }
                            .forEach { pl -> tradeYearlyDeclaration.addProfitLose(pl, rateRepository, symbolId) }
                    }
                    row.writableCell(20).setCellValue(profitLoss.toDouble())
                }
                if (profitLossPLN != null) {
                    row.writableCell(21).setCellValue(profitLossPLN.toDouble())
                }
            }
        }

        return tradeYearlyDeclaration
    }

    private fun parseTransactionSide(sideStr: String): TransactionSide {
        if (sideStr == "sell") {
            return TransactionSide.sell
        }
        if (sideStr == "buy") {
            return buy
        }
        throw IllegalStateException("can not recognize transaction side: $sideStr")
    }

    private fun tradeDate(cell: Cell): LocalDateTime {
        if (cell.cellType != CellType.STRING) {
            throw IllegalStateException("In row ${cell.rowIndex} required trade date cell as string, but get ${cell.cellType}")
        }
        return LocalDateTime.parse(cell.stringCellValue, tradeDateFormatter)
    }

    companion object {
        val tradeDateFormatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter()
    }
}
