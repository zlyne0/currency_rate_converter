package promitech.currencyrateconverter

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import promitech.currencyrateconverter.model.Currency
import promitech.currencyrateconverter.model.Money
import java.io.Closeable
import java.io.InputStream
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

class NbpPlnRateRepository(val ratesFilesNameByYear: Map<Int, String>): Closeable {

    private val workbookPerYear = HashMap<Int, Workbook>()

    fun loadWorkbook(year: Int): Workbook {
        var workbook = workbookPerYear.get(year)
        if (workbook != null) {
            return workbook
        }
        val ratesFileName = ratesFilesNameByYear.get(year)
        if (ratesFileName == null) {
            throw IllegalStateException("can not find rates file name by year $year")
        }
        val inputStream: InputStream = Files.newInputStream(Paths.get(ratesFileName))
        workbook = WorkbookFactory.create(inputStream)
        workbookPerYear.put(year, workbook)
        inputStream.close()
        return workbook
    }

    fun convertToPLN(date: LocalDate, money: Money): Money {
        return rate(date, money.currency).convertToPLN(money)
    }

    fun rate(date: LocalDate, currency: Currency): Rate {
        val workbook = loadWorkbook(date.year)
        val currencyColumn = findCurrencyColumn(currency, workbook)
        val dataRow = findDateRow(date, workbook)
        val rateCell = dataRow.getCell(currencyColumn)
        return Rate(rateCell.cellAsBigDecimal(), findNumberOfUnitRow(currencyColumn, workbook))
    }

    private fun findDateRow(date: LocalDate, workbook: Workbook): Row {
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

    private fun findCurrencyColumn(currency: Currency, workbook: Workbook): Int {
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

    private fun findNumberOfUnitRow(currencyColumn: Int, workbook: Workbook): BigDecimal {
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
        for ((_, workbook) in workbookPerYear) {
            workbook.close()
        }
    }

    data class Rate(val rateValue: BigDecimal, val units: BigDecimal) {

        fun convertToPLN(money: Money): Money {
            return Money.valueOf(convertToPLN(money.amount), Currency.PLN)
        }

        fun convertToPLN(amount: BigDecimal): BigDecimal {
            return rateValue.multiply(amount).divide(units, 4, RoundingMode.HALF_UP)
        }

        fun convertFromPLN(value: BigDecimal): BigDecimal {
            return value.divide(rateValue.divide(units), 4, RoundingMode.HALF_UP)
        }
    }
}
