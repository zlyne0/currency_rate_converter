package promitech.report.exante

import promitech.currencyrateconverter.NbpPlnRateRepository

fun main() {
    val rateRepository = NbpPlnRateRepository(
        mapOf(
            2021 to "archiwum_tab_a_2021.xls",
            2022 to "archiwum_tab_a_2022.xls",
            2023 to "archiwum_tab_a_2023.xls",
        )
    )

    val tradeReport = TradeReport("trade.xlsx", "trade_output.xlsx", rateRepository)
    tradeReport.calculateTrades(2023)

    tradeReport.save()

}