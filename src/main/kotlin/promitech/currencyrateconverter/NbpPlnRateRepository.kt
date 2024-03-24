package promitech.currencyrateconverter

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import promitech.currencyrateconverter.model.Currency
import java.io.Closeable
import java.io.InputStream
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

class NbpPlnRateRepository(ratesFileName: String): Closeable {

    private val workbook: Workbook

    init {
        val inputStream: InputStream = Files.newInputStream(Paths.get(ratesFileName))
        workbook = WorkbookFactory.create(inputStream)
    }

    fun rate(date: LocalDate, currency: Currency): Rate {
        val currencyColumn = findCurrencyColumn(currency)
        val dataRow = findDateRow(date)
        val rateCell = dataRow.getCell(currencyColumn)
        return Rate(rateCell.cellAsBigDecimal(), findNumberOfUnitRow(currencyColumn))
    }

    private fun findDateRow(date: LocalDate): Row {
        val sheet = workbook.getSheetAt(0)
        var previewDateRow: Row? = null
        for (row in sheet.rowIterator()) {
            val dateCell = row.getCell(0)
            if (dateCell.cellType == CellType.NUMERIC) {
                val rowDateTime = dateCell.localDateTimeCellValue

                if (rowDateTime.toLocalDate().equals(date)) {
                    return row
                }
                if (rowDateTime.toLocalDate().isAfter(date)) {
                    if (previewDateRow != null) {
                        return previewDateRow
                    }
                }
                previewDateRow = row
            }
        }
        throw IllegalStateException("can not find rate for date: " + date)
    }

    private fun findCurrencyColumn(currency: Currency): Int {
        val sheet = workbook.getSheetAt(0)

        for (row in sheet.rowIterator()) {
            val firstCell = row.getCell(0)

            if (firstCell.cellType == CellType.STRING && firstCell.stringCellValue.equals("kod ISO")) {
                for (cell in row.cellIterator()) {
                    if (cell.cellAsString() == currency.value) {
                        return cell.columnIndex
                    }
                }
            }
        }
        throw IllegalStateException("can not find row with currency ISO code")
    }

    private fun findNumberOfUnitRow(currencyColumn: Int): BigDecimal {
        val sheet = workbook.getSheetAt(0)
        for (row in sheet.rowIterator()) {
            val firstCell = row.getCell(0)
            if (firstCell.cellType == CellType.STRING && firstCell.stringCellValue.equals("liczba jednostek")) {
                return row.getCell(currencyColumn).cellAsBigDecimal()
            }
        }
        throw IllegalStateException("can not find row with 'liczba jednostek'")
    }

    override fun close() {
        workbook.close()
    }

    data class Rate(val rateValue: BigDecimal, val units: BigDecimal) {
        fun convertToPLN(amount: BigDecimal): BigDecimal {
            return rateValue.multiply(amount).divide(units, 4, RoundingMode.HALF_UP)
        }

        fun convertFromPLN(value: BigDecimal): BigDecimal {
            return value.divide(rateValue.divide(units), 4, RoundingMode.HALF_UP)
        }
    }
}
