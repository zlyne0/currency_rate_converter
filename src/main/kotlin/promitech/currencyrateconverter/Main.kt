package promitech.currencyrateconverter

import promitech.currencyrateconverter.CurrencyRateInOutFile.Converted
import promitech.currencyrateconverter.CurrencyRateInOutFile.CurrencyValue
import promitech.currencyrateconverter.model.Currency

fun main() {

    val rateRepository = NbpPlnRateRepository("archiwum_tab_a_2022.xls")
    val moneyInOutFile = CurrencyRateInOutFile("currency_rate.xlsx", "currency_rate_output.xlsx")

    for (moneyEntry in moneyInOutFile.moneyEntries) {
        println(moneyEntry)
    }

    fun convert(a: CurrencyValue): Converted {
        if (a.currency == "PLN") {
            return Converted(a, a.amount, a.currency)
        }
        val rate = rateRepository.rate(a.date, Currency(a.currency))
        return Converted(a, rate.convertToPLN(a.amount), "PLN")
    }

    val convertedList = moneyInOutFile.moneyEntries.asSequence()
        .map { currencyValue -> convert(currencyValue) }
        .toList()

    moneyInOutFile.saveConverter(convertedList)
}
