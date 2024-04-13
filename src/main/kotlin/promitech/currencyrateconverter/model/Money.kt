package promitech.currencyrateconverter.model

import promitech.currencyrateconverter.model.Currency.Companion.PLN
import promitech.report.exante.DividentReport
import java.math.BigDecimal
import java.math.RoundingMode

data class Money(val amount: BigDecimal, val currency: Currency) {

    fun isEqualInScale(money: Money, scale: Int): Boolean {
        val a = this.amount.setScale(scale, RoundingMode.DOWN)
        val b = money.amount.setScale(scale, RoundingMode.DOWN)
        return a.compareTo(b) == 0
    }

    fun isZero(): Boolean {
        return amount.compareTo(BigDecimal.ZERO) == 0
    }

    operator fun plus(money: Money): Money {
        if (this.currency != money.currency) {
            throw IllegalArgumentException("can add money only in the same currency, actual $currency and argument ${money.currency}")
        }
        return Money(amount + money.amount, currency)
    }

    operator fun minus(money: Money): Money {
        if (this.currency != money.currency) {
            throw IllegalArgumentException("can subtraction money only in the same currency, actual $currency and argument ${money.currency}")
        }
        return Money(amount - money.amount, currency)
    }

    fun percent(percent: Percent): Money {
        return Money(
        amount * (percent.value.divide(DividentReport.ONE_HUNDRED, 2, RoundingMode.HALF_UP)),
            currency
        )
    }

    fun toDouble(): Double {
        return amount.toDouble()
    }

    operator fun compareTo(money: Money): Int {
        if (this.currency != money.currency) {
            throw IllegalArgumentException("can compare money only in the same currency, actual $currency and argument ${money.currency}")
        }
        return this.amount.compareTo(money.amount)
    }

    fun multiply(multiplicand: BigDecimal): Money {
        return Money(amount.multiply(multiplicand), currency)
    }

    override fun toString(): String {
        return "" + amount + " " + currency.value
    }

    companion object {

        val ZERO_PLN = Money.zero(PLN)

        fun valueOf(amount: Int, currency: Currency): Money {
            return Money(BigDecimal.valueOf(amount.toLong()), currency)
        }

        fun valueOf(amount: BigDecimal, currency: Currency): Money {
            return Money(amount, currency)
        }

        fun valueOf(amount: String, currency: Currency): Money {
            return Money(BigDecimal(amount), currency)
        }

        fun zero(currency: Currency): Money {
            return Money(BigDecimal.ZERO, currency)
        }
    }
}