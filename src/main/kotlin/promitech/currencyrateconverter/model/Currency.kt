package promitech.currencyrateconverter.model

data class Currency(val value: String) {
    companion object {
        val USD = Currency("USD")
        val JPY = Currency("JPY")
    }
}