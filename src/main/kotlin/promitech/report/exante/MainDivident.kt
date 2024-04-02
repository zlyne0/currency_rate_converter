package promitech.report.exante

import promitech.currencyrateconverter.NbpPlnRateRepository

fun main() {
    val rateRepository = NbpPlnRateRepository("archiwum_tab_a_2023.xls")

    val report = DividentReport("divident.xlsx", "divident_output.xls")
    report.calculateDividents(rateRepository)
    report.save()

}