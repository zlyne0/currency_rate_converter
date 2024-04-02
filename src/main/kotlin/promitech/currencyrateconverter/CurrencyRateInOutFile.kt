package promitech.currencyrateconverter

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.Closeable
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

class CurrencyRateInOutFile(val inputFileName: String, val outputFileName: String): Closeable {

    val moneyEntries: List<CurrencyValue>
    private val workbook: Workbook

    private val contentRowOffset = 1

    init {
        val inputStream = Files.newInputStream(Paths.get(inputFileName))
        workbook = WorkbookFactory.create(inputStream)
        //val workbook = XSSFWorkbook(Files.newInputStream(Paths.get(inputFileName)))
        moneyEntries = load()
        inputStream.close()
    }

    private fun load(): List<CurrencyValue> {
        val sheet = workbook.getSheetAt(0)

        val list = mutableListOf<CurrencyValue>()
        for (rowIndex in sheet.firstRowNum + contentRowOffset .. sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex)
            list.add(CurrencyValue(
                row.getCell(0).cellAsLocalDate(),
                row.getCell(1).cellAsString(),
                row.getCell(2).cellAsBigDecimal()
            ))
        }
        return list
    }

    fun saveConverter(converted: List<Converted>) {
        val outputStream = Files.newOutputStream(Paths.get(outputFileName))
        saveConvertedList(converted)
        workbook.write(outputStream)
        workbook.close()
        outputStream.close()
    }

    private fun saveConvertedList(convertedList: List<Converted>) {
        val sheet = workbook.getSheetAt(0)
        convertedList.forEachIndexed { index: Int, converted: Converted ->
            val row = sheet.getRow(index + contentRowOffset)

            val cell = row.writableCell(3)
            cell.setCellValue(converted.value.toDouble())

            val currencyCell = row.writableCell(4)
            currencyCell.setCellValue(converted.currency)
        }
    }

    override fun close() {
        workbook.close()
    }

    data class CurrencyValue(val date: LocalDate, val currency: String, val amount: BigDecimal)
    data class Converted(val currencyValue: CurrencyValue, var value: BigDecimal, var currency: String)
}