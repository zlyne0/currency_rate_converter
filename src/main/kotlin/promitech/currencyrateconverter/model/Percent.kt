package promitech.currencyrateconverter.model

import java.math.BigDecimal

data class Percent(val value: BigDecimal) {

    operator fun compareTo(v: Percent): Int {
        return value.compareTo(v.value)
    }

    operator fun minus(v: Percent): Percent {
        return Percent(value - v.value)
    }

    fun toDouble(): Double {
        return value.toDouble()
    }

    companion object {
        fun valueOf(value: Int): Percent {
            return Percent(BigDecimal.valueOf(value.toLong()))
        }
        val ZERO: Percent = Percent(BigDecimal.ZERO)
    }
}