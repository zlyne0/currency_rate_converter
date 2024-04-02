package promitech.report.exante

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import promitech.currencyrateconverter.*
import promitech.currencyrateconverter.model.Currency
import promitech.currencyrateconverter.model.Money
import promitech.currencyrateconverter.model.Percent
import promitech.report.exante.TaxDeclaration.Companion.PL_TAX
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

class DividentTransaction(
    val transactionId: Long,
    val dividentDate: LocalDate,
    val dividentValue: Money,
    val sourceTaxPercent: Percent
)

class SingleDividentTaxDeclaration(
    rateRepository: NbpPlnRateRepository,
    dividentTransaction: DividentTransaction,
) {

    val dividentInPLN: Money
    var realSourcePaidTax: Money
    var plTaxAmount: Money
    var taxPercentToPay: Percent
    var taxToPay: Money
    var taxToPayPLN: Money
    var declarationSourcePaidTax: Money

    init {
        with(dividentTransaction) {
            val rate = rateRepository.rate(dividentDate, dividentValue.currency)
            dividentInPLN = rate.convertToPLN(dividentValue)
            realSourcePaidTax = rate.convertToPLN(dividentValue.percent(sourceTaxPercent))
            plTaxAmount = rate.convertToPLN(dividentValue.percent(PL_TAX))

            if (sourceTaxPercent < PL_TAX) {
                taxPercentToPay = PL_TAX - sourceTaxPercent
                taxToPay = dividentValue.percent(taxPercentToPay)
                taxToPayPLN = rate.convertToPLN(taxToPay)
                declarationSourcePaidTax = realSourcePaidTax
            } else {
                taxPercentToPay = Percent.ZERO
                taxToPay = Money.zero(dividentValue.currency)
                taxToPayPLN = Money.zero(Currency.PLN)
                // Komentarz na deklaracji "Kwota z pozycji 46 nie może przekraczać kwoty z pozycji 45"
                declarationSourcePaidTax = rate.convertToPLN(dividentValue.percent(PL_TAX))
            }
        }
    }
}

class TaxDeclaration() {

    var sumOfDividentInPLN = Money.zero(Currency.PLN)
    var sumOfPLTaxInPLN = Money.zero(Currency.PLN) // sum of 19%
    var sumOfRealSourcePaidTaxInPLN = Money.zero(Currency.PLN)  // suma podatkow zaplaconych u zrodla, te 15% lub 25%
    var sumOfDeclarationSourcePaidTax = Money.zero(Currency.PLN) // suma podatkow zaplaconych u zrodla ale nie wiecej niz 19%

    fun add(dec: SingleDividentTaxDeclaration) {
        sumOfDividentInPLN += dec.dividentInPLN
        sumOfPLTaxInPLN += dec.plTaxAmount
        sumOfRealSourcePaidTaxInPLN += dec.realSourcePaidTax
        sumOfDeclarationSourcePaidTax += dec.declarationSourcePaidTax
    }

    fun difference(): Money {
        return sumOfPLTaxInPLN - sumOfDeclarationSourcePaidTax
    }

    fun taxToPay(): Money {
        val toPay = sumOfPLTaxInPLN - sumOfDeclarationSourcePaidTax
        val zero = Money.zero(Currency.PLN)
        if (toPay < zero) {
            return zero
        }
        return toPay
    }

    companion object {
        val PL_TAX = Percent.valueOf(19)
    }
}

class DividentReport(inputFileName: String, val outputFileName: String) {

    val workbook: Workbook

    init {
        val inputStream = fileNameToInputStream(inputFileName)
        workbook = WorkbookFactory.create(inputStream)
        inputStream.close()
    }

    fun fileNameToInputStream(fileName: String): InputStream {
        if (fileName.startsWith("classpath:/")) {
            return DividentReport::class.java.getResourceAsStream(fileName.replace("classpath:", ""))!!
        } else {
            return Files.newInputStream(Paths.get(fileName))
        }
    }

    fun save() {
        val outputStream = Files.newOutputStream(Paths.get(outputFileName))
        workbook.write(outputStream)
        workbook.close()
        outputStream.close()
    }

    fun calculateDividents(rateRepository: NbpPlnRateRepository): TaxDeclaration {
        val sheet = workbook.getSheetAt(0)

        val columnsShiftSize = 3
        sheet.shiftColumns(8, 12, columnsShiftSize)

        val rollbackTransactions = HashSet<String>()

        val taxDeclaration = TaxDeclaration()

        sheet.getRow(0).writableCell(8).setCellValue("Wysokosc procentowa podatku do doplaty")
        sheet.getRow(0).writableCell(9).setCellValue("Wysokosc podatku do doplaty")
        sheet.getRow(0).writableCell(10).setCellValue("Wysokosc podatku do doplaty w PLN")

        var lastRowIndex = 0
        for (row in sheet.rowIterator()) {
            lastRowIndex = row.rowNum
            if (row.getCell(0) == null) {
                break
            }
            val operationType = row.getCell(4).cellAsString()
            val description = row.getCell(9 + columnsShiftSize).cellAsString()
            val transactionUID = row.getCell(10 + columnsShiftSize).cellAsString()
            if (operationType == DIVIDENT_TRANSACTION_TYPE) {
                if (rollbackTransactions.contains(transactionUID)) {
                    continue
                }
                if (isRollbackTransaction(description)) {
                    val parentTransactionUID = row.getCell(11 + columnsShiftSize).cellAsString()
                    rollbackTransactions.add(parentTransactionUID)
                    continue
                }
                val sourceTaxPercent = extractTaxPercentValue(description)
                if (sourceTaxPercent == null) {
                    throw IllegalStateException("can not find tax Percent value in rowNum ${row.rowNum}, " + row.getCell(0).cellAsLong())
                }

                val dividentTransaction = DividentTransaction(
                    row.getCell(0).cellAsLong(),
                    row.getCell(5).cellAsLocalDate(),
                    Money(
                        row.getCell(6).cellAsBigDecimal(),
                        Currency(row.getCell(7).cellAsString())
                    ),
                    sourceTaxPercent
                )
                val singleDeclaration = SingleDividentTaxDeclaration(rateRepository, dividentTransaction)
                taxDeclaration.add(singleDeclaration)

                row.writableCell(8).setCellValue(singleDeclaration.taxPercentToPay.toDouble())
                row.writableCell(9).setCellValue(singleDeclaration.taxToPay.toDouble())
                row.writableCell(10).setCellValue(singleDeclaration.taxToPayPLN.toDouble())
            }
        }

        writeSummary(lastRowIndex, sheet, taxDeclaration)

        fun moneyToStr(money: Money): String {
            return "" + money.amount.setScale(2, RoundingMode.HALF_UP) + " " + money.currency.value
        }

        fun moneyToIntegerStr(money: Money): String {
            return "" + money.amount.setScale(0, RoundingMode.HALF_UP) + " " + money.currency.value
        }

        println("Podsumowanie")
        println("  Suma dywidendy: " + moneyToStr(taxDeclaration.sumOfDividentInPLN))
        println("  Rzeczywisty podatek zaplacony u zrodla: " + moneyToStr(taxDeclaration.sumOfRealSourcePaidTaxInPLN) + ", (nie wpisuje w deklaracje bo pozycja 46 nie moze przekracac kwoty z pozycji 45)")
        println("Pola z deklaracji pit-38")
        println("  (Pole 45) PL 19% naleznego podatku od dywidend: " + moneyToStr(taxDeclaration.sumOfPLTaxInPLN))
        // Komentarz na deklaracji "Kwota z pozycji 46 nie może przekraczać kwoty z pozycji 45"
        println("  (Pole 46) Podatek zaplacony u zrodla: " + moneyToStr(taxDeclaration.sumOfDeclarationSourcePaidTax))
        println("  (Pole 47) Roznica: " + moneyToIntegerStr(taxDeclaration.difference()))
        println("  (Pole 49) Podatek do zaplaty: " + moneyToIntegerStr(taxDeclaration.taxToPay()))
        return taxDeclaration
    }

    fun writeSummary(lastRowIndex: Int, sheet: Sheet, taxDeclaration: TaxDeclaration) {
        sheet.createRow(lastRowIndex + 1).writableCell(0).setCellValue("Podsumowanie")
        with(sheet.createRow(lastRowIndex + 2)) {
            writableCell(0).setCellValue("Suma dywidendy PLN")
            writableCell(1).setCellValue(taxDeclaration.sumOfDividentInPLN.toDouble())
        }
        with(sheet.createRow(lastRowIndex + 3)) {
            writableCell(0).setCellValue("Rzeczywisty podatek zaplacony u zrodla PLN")
            writableCell(1).setCellValue(taxDeclaration.sumOfRealSourcePaidTaxInPLN.toDouble())
        }
        with(sheet.createRow(lastRowIndex + 4)) {
            writableCell(0).setCellValue("(Pole 45) PL 19% naleznego podatku od dywidend")
            writableCell(1).setCellValue(taxDeclaration.sumOfPLTaxInPLN.toDouble())
        }
        with(sheet.createRow(lastRowIndex + 5)) {
            writableCell(0).setCellValue("(Pole 46) Podatek zaplacony u zrodla")
            writableCell(1).setCellValue(taxDeclaration.sumOfDeclarationSourcePaidTax.toDouble())
        }
        with(sheet.createRow(lastRowIndex + 6)) {
            writableCell(0).setCellValue("(Pole 47) Roznica")
            writableCell(1).setCellValue(taxDeclaration.difference().toDouble())
        }
        with(sheet.createRow(lastRowIndex + 7)) {
            writableCell(0).setCellValue("(Pole 49) Podatek do zaplaty")
            writableCell(1).setCellValue(taxDeclaration.taxToPay().toDouble())
        }
    }

    companion object {

        val DIVIDENT_TRANSACTION_TYPE = "DIVIDEND"

        val ONE_HUNDRED = BigDecimal.valueOf(100)

        fun extractTaxPercentValue(str: String): Percent? {
            val idx = str.indexOf("tax")
            if (idx == -1) {
                return null
            }
            val leftBracketIdx = str.indexOf("(", idx)
            if (leftBracketIdx == -1) {
                return null
            }
            val rightBracketIdx = str.indexOf("%)", idx)
            if (rightBracketIdx == -1) {
                return null
            }
            // +1 - minus
            return Percent(BigDecimal(str.substring(leftBracketIdx+1+1, rightBracketIdx)))
        }

        fun isRollbackTransaction(strDescription: String): Boolean {
            return strDescription.startsWith("Rollback for transaction")
        }

    }

}
