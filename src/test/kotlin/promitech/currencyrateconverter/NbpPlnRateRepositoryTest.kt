package promitech.currencyrateconverter

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import promitech.currencyrateconverter.model.Currency.Companion.JPY
import promitech.currencyrateconverter.model.Currency.Companion.USD
import promitech.currencyrateconverter.NbpPlnRateRepository.Rate
import java.math.BigDecimal
import java.time.LocalDate

class NbpPlnRateRepositoryTest : StringSpec({

    lateinit var rateRepository: NbpPlnRateRepository

    beforeSpec {
        rateRepository = NbpPlnRateRepository("archiwum_tab_a_2022.xls")
    }

    afterSpec {
        rateRepository.close()
    }

    "should load rates" {
        val rate: Rate = rateRepository.rate(LocalDate.of(2022, 12, 9), USD)
        rate shouldBe Rate(BigDecimal.valueOf(4.4351), BigDecimal.valueOf(1.0))
    }

    "should load preview working day when date on holiday" {
        val rate: Rate = rateRepository.rate(LocalDate.of(2022, 12, 11), USD)
        rate shouldBe Rate(BigDecimal.valueOf(4.4351), BigDecimal.valueOf(1.0))
    }

    "should load units" {
        val rate: Rate = rateRepository.rate(LocalDate.of(2022, 12, 11), JPY)
        rate shouldBe Rate(BigDecimal.valueOf(3.256), BigDecimal.valueOf(100.0))
    }

    "should include number of unit in USD calculation" {
        val rate: Rate = rateRepository.rate(LocalDate.of(2022, 12, 21), USD)
        // when
        val valueInPLN = rate.convertToPLN(BigDecimal.valueOf(10000))
        val valueInCurrency = rate.convertFromPLN(BigDecimal.valueOf(10000))
        // then
        valueInPLN shouldBeEqualComparingTo BigDecimal.valueOf(43947)
        valueInCurrency shouldBeEqualComparingTo BigDecimal.valueOf(2275.4682)
    }


    "should include number of unit in JPY calculation" {
        val rate: Rate = rateRepository.rate(LocalDate.of(2022, 12, 21), JPY)
        // when
        val valueInPLN = rate.convertToPLN(BigDecimal.valueOf(10000))
        val valueInCurrency = rate.convertFromPLN(BigDecimal.valueOf(10000))
        // then

        // 3.3377
        valueInPLN shouldBeEqualComparingTo BigDecimal.valueOf(333.77)
        valueInCurrency shouldBeEqualComparingTo BigDecimal.valueOf(299607.5142)
    }

})
