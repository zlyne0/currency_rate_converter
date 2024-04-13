package promitech.currencyrateconverter.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import promitech.currencyrateconverter.model.Currency.Companion.PLN

class MoneyTest : StringSpec({

    "should be equal in scale" {
        val a = Money.valueOf("10.10111", PLN)
        val b = Money.valueOf("10.10111", PLN)
        a.isEqualInScale(b, 2) shouldBe true
    }

    "should be equal in scale 2" {
        val a = Money.valueOf("10.10111", PLN)
        val b = Money.valueOf("10.10666", PLN)
        a.isEqualInScale(b, 2) shouldBe true
    }

    "should not be equal in scale" {
        val a = Money.valueOf("10.11", PLN)
        val b = Money.valueOf("10.10", PLN)
        a.isEqualInScale(b, 2) shouldBe false
    }

})
