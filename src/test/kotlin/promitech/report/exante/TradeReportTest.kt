package promitech.report.exante

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import promitech.currencyrateconverter.model.Currency.Companion.USD
import promitech.currencyrateconverter.model.Money
import promitech.report.exante.TransactionSide.buy
import promitech.report.exante.TransactionSide.sell
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class TradeReportTest : StringSpec({

    "buy" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2023-11-13", Money.valueOf(100, USD), 10))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 0
        temporaryState.size shouldBe 1
        temporaryState[0] shouldBe transactions[0]
    }

    "buy, buy" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2023-11-13", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(buy, "2023-11-14", Money.valueOf(100, USD), 10))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 0
        temporaryState.size shouldBe 2
        temporaryState[0] shouldBe transactions[0]
        temporaryState[1] shouldBe transactions[1]
    }

    "buy, sell" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2023-11-13", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(sell, "2023-11-14", Money.valueOf(120, USD), 10))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 1
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[1].date
        profitLosses[0].closePrice shouldBe transactions[1].price
        profitLosses[0].amount shouldBe transactions[1].amount

        temporaryState.size shouldBe 0
    }

    "buy, smaller sell" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2023-11-13", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(sell, "2023-11-14", Money.valueOf(120, USD), 5))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 1
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[1].date
        profitLosses[0].closePrice shouldBe transactions[1].price
        profitLosses[0].amount shouldBe transactions[1].amount

        temporaryState.size shouldBe 1
        temporaryState[0] shouldBe transactions[0].copy(amount = BigDecimal.valueOf(5))
    }

    "buy, buy, smaller sell" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2023-11-12", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(buy, "2023-11-13", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(sell, "2023-11-14", Money.valueOf(120, USD), 5))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 1
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[2].date
        profitLosses[0].closePrice shouldBe transactions[2].price
        profitLosses[0].amount shouldBe transactions[2].amount

        temporaryState.size shouldBe 2
        temporaryState[0] shouldBe transactions[0].copy(amount = BigDecimal.valueOf(5))
        temporaryState[1] shouldBe transactions[1]
    }

    "buy, buy, sell all" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2020-01-10", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(buy, "2020-01-11", Money.valueOf(110, USD), 10))
        transactions.add(createTransaction(sell, "2020-01-12", Money.valueOf(120, USD), 20))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 2
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[2].date
        profitLosses[0].closePrice shouldBe transactions[2].price
        profitLosses[0].amount shouldBe transactions[0].amount

        profitLosses[1].openDate shouldBe transactions[1].date
        profitLosses[1].openPrice shouldBe transactions[1].price
        profitLosses[1].closeDate shouldBe transactions[2].date
        profitLosses[1].closePrice shouldBe transactions[2].price
        profitLosses[1].amount shouldBe transactions[1].amount

        temporaryState.size shouldBe 0
    }

    "buy, smaller sell, smaller sell" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2023-11-13", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(sell, "2023-11-14", Money.valueOf(120, USD), 7))
        transactions.add(createTransaction(sell, "2023-11-15", Money.valueOf(125, USD), 3))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 2
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[1].date
        profitLosses[0].closePrice shouldBe transactions[1].price
        profitLosses[0].amount shouldBe transactions[1].amount

        profitLosses[1].openDate shouldBe transactions[0].date
        profitLosses[1].openPrice shouldBe transactions[0].price
        profitLosses[1].closeDate shouldBe transactions[2].date
        profitLosses[1].closePrice shouldBe transactions[2].price
        profitLosses[1].amount shouldBe transactions[2].amount

        temporaryState.size shouldBe 0
    }


    "buy, bigger sell" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2023-11-13", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(sell, "2023-11-14", Money.valueOf(120, USD), 15))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        profitLosses.size shouldBe 1
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[1].date
        profitLosses[0].closePrice shouldBe transactions[1].price
        profitLosses[0].amount shouldBe transactions[0].amount

        temporaryState.size shouldBe 1
        temporaryState[0] shouldBe transactions[1].copy(amount = BigDecimal.valueOf(5))
    }

    "sell, small buy" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(sell, "2023-11-13", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(buy, "2023-11-14", Money.valueOf(120, USD), 7))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 1
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[1].date
        profitLosses[0].closePrice shouldBe transactions[1].price
        profitLosses[0].amount shouldBe transactions[1].amount

        temporaryState.size shouldBe 1
        temporaryState[0] shouldBe transactions[0].copy(amount = BigDecimal.valueOf(3))
    }

    "sell, all buy" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(sell, "2023-11-13", Money.valueOf(120, USD), 15))
        transactions.add(createTransaction(buy, "2023-11-14", Money.valueOf(100, USD), 15))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 1
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[1].date
        profitLosses[0].closePrice shouldBe transactions[1].price
        profitLosses[0].amount shouldBe transactions[1].amount

        temporaryState.size shouldBe 0
    }


    "buy, buy, sell, sell" {
        //  given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2020-01-10", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(buy, "2020-01-11", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(sell, "2020-01-12", Money.valueOf(120, USD), 15))
        transactions.add(createTransaction(sell, "2020-01-13", Money.valueOf(125, USD), 5))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 3
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[2].date
        profitLosses[0].closePrice shouldBe transactions[2].price
        profitLosses[0].amount shouldBe transactions[0].amount

        profitLosses[1].openDate shouldBe transactions[1].date
        profitLosses[1].openPrice shouldBe transactions[1].price
        profitLosses[1].closeDate shouldBe transactions[2].date
        profitLosses[1].closePrice shouldBe transactions[2].price
        profitLosses[1].amount shouldBe BigDecimal.valueOf(5)

        profitLosses[2].openDate shouldBe transactions[1].date
        profitLosses[2].openPrice shouldBe transactions[1].price
        profitLosses[2].closeDate shouldBe transactions[3].date
        profitLosses[2].closePrice shouldBe transactions[3].price
        profitLosses[2].amount shouldBe BigDecimal.valueOf(5)

        temporaryState.size shouldBe 0
    }

    "buy, buy, small sell" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2020-01-10", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(buy, "2020-01-11", Money.valueOf(120, USD), 10))
        transactions.add(createTransaction(sell, "2020-01-12", Money.valueOf(100, USD), 5))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 1
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[2].date
        profitLosses[0].closePrice shouldBe transactions[2].price
        profitLosses[0].amount shouldBe transactions[2].amount

        temporaryState.size shouldBe 2
        temporaryState[0] shouldBe transactions[0].copy(amount = BigDecimal.valueOf(5))
        temporaryState[1] shouldBe transactions[1]
    }

    "buy, sell, buy, sell" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2020-01-10", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(sell, "2020-01-11", Money.valueOf(120, USD), 10))
        transactions.add(createTransaction(buy, "2020-01-12", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(sell, "2020-01-13", Money.valueOf(125, USD), 10))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 2
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[1].date
        profitLosses[0].closePrice shouldBe transactions[1].price
        profitLosses[0].amount shouldBe transactions[1].amount

        profitLosses[1].openDate shouldBe transactions[2].date
        profitLosses[1].openPrice shouldBe transactions[2].price
        profitLosses[1].closeDate shouldBe transactions[3].date
        profitLosses[1].closePrice shouldBe transactions[3].price
        profitLosses[1].amount shouldBe transactions[3].amount

        temporaryState.size shouldBe 0
    }

    "buy, buy, sell, buy" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2020-01-10", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(buy, "2020-01-11", Money.valueOf(110, USD), 10))
        transactions.add(createTransaction(sell, "2020-01-12", Money.valueOf(120, USD), 20))
        transactions.add(createTransaction(buy, "2020-01-13", Money.valueOf(100, USD), 10))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 2
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[2].date
        profitLosses[0].closePrice shouldBe transactions[2].price
        profitLosses[0].amount shouldBe transactions[0].amount

        profitLosses[1].openDate shouldBe transactions[1].date
        profitLosses[1].openPrice shouldBe transactions[1].price
        profitLosses[1].closeDate shouldBe transactions[2].date
        profitLosses[1].closePrice shouldBe transactions[2].price
        profitLosses[1].amount shouldBe transactions[1].amount

        temporaryState.size shouldBe 1
        temporaryState[0] shouldBe transactions[3]
    }

    "buy, small sell, buy, sell all" {
        // given
        val transactions = ArrayList<Transaction>()
        transactions.add(createTransaction(buy, "2020-01-10", Money.valueOf(100, USD), 10))
        transactions.add(createTransaction(sell, "2020-01-11", Money.valueOf(100, USD), 7))
        transactions.add(createTransaction(buy, "2020-01-12", Money.valueOf(120, USD), 10))
        transactions.add(createTransaction(sell, "2020-01-13", Money.valueOf(100, USD), 13))

        // when
        val (profitLosses, temporaryState) = calculate(transactions)

        // then
        profitLosses.size shouldBe 3
        profitLosses[0].openDate shouldBe transactions[0].date
        profitLosses[0].openPrice shouldBe transactions[0].price
        profitLosses[0].closeDate shouldBe transactions[1].date
        profitLosses[0].closePrice shouldBe transactions[1].price
        profitLosses[0].amount shouldBe transactions[1].amount

        profitLosses[1].openDate shouldBe transactions[0].date
        profitLosses[1].openPrice shouldBe transactions[0].price
        profitLosses[1].closeDate shouldBe transactions[3].date
        profitLosses[1].closePrice shouldBe transactions[3].price
        profitLosses[1].amount shouldBe BigDecimal.valueOf(3)

        profitLosses[2].openDate shouldBe transactions[2].date
        profitLosses[2].openPrice shouldBe transactions[2].price
        profitLosses[2].closeDate shouldBe transactions[3].date
        profitLosses[2].closePrice shouldBe transactions[3].price
        profitLosses[2].amount shouldBe BigDecimal.valueOf(10)

        temporaryState.size shouldBe 0
    }

})

fun createTransaction(
    transactionSide: TransactionSide,
    strDate: String,
    price: Money,
    amount: Int,
    commision: Money = Money(BigDecimal.valueOf(10), USD),
    profitLoss: Money = Money(BigDecimal.valueOf(10), USD)
): Transaction {
    return Transaction(
        side = transactionSide,
        date = LocalDate.parse(strDate, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(),
        symbolId = "XXX",
        isin = "isinXXX",
        price = price,
        amount = BigDecimal.valueOf(amount.toLong()),
        commision = commision,
        profitLoss = profitLoss,
        orderId = UUID.randomUUID().toString(),
        orderState = 0
    )
}
