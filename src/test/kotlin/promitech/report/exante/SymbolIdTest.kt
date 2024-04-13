package promitech.report.exante

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class SymbolIdTest : StringSpec({

    "can parse symbolId" {
        val phys = SymbolId("PHYS.ARCA")
        phys.paper shouldBe "PHYS"
        phys.stockExchange shouldBe "ARCA"
        phys.country().code shouldBe "US"
    }

    "should throw exception when can not parse symbolId" {
        val exception = shouldThrow<IllegalArgumentException> {
            SymbolId("PHYS")
        }
        exception.message shouldStartWith "can not parse symbolId"
    }

    "should throw exception when can not find country for stock exchange" {
        val phys = SymbolId("PHYS.unknown")
        val exception = shouldThrow<IllegalArgumentException> {
            phys.country()
        }
        exception.message shouldStartWith "can not find country by stockExchange"
    }

})
