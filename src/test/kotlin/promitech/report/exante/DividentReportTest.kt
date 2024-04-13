package promitech.report.exante

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import promitech.currencyrateconverter.NbpPlnRateRepository
import promitech.currencyrateconverter.model.Currency.Companion.PLN
import promitech.currencyrateconverter.model.Money
import java.math.BigDecimal

class DividentReportTest : StringSpec({

    "should extract tax percent value from description" {
        val str = "100 shares ExD 2023-11-22 PD 2023-12-08 dividend VALE.NYSE 15.57 USD (0.155609 per share) tax -2.34 USD (-15.000%) DivCntry BR"

        val tax = DividentReport.extractTaxPercentValue(str)

        tax!!.value shouldBeEqualComparingTo BigDecimal(15)
    }

    "can load and save" {
        // given
        val rateRepository = NbpPlnRateRepository(mapOf(2023 to "archiwum_tab_a_2023.xls"))

        // when
        val report = DividentReport("classpath:/exante_divident_report_example.xlsx", "exante_divident_report_example_output.xlsx")
        val taxDeclaration = report.calculateDividents(rateRepository)
        report.save()

        // then
        taxDeclaration.sumOfDividentInPLN shouldBe Money.valueOf("928.6841", PLN)
        taxDeclaration.sumOfRealSourcePaidTaxInPLN shouldBe Money.valueOf("132.3606", PLN)
        taxDeclaration.sumOfPLTaxInPLN shouldBe Money.valueOf("176.4499", PLN)
        taxDeclaration.sumOfDeclarationSourcePaidTax shouldBe Money.valueOf("131.6979", PLN)
        taxDeclaration.difference() shouldBe Money.valueOf("44.7520", PLN)
        taxDeclaration.taxToPay() shouldBe Money.valueOf("44.7520", PLN)
    }

})
