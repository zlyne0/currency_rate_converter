package promitech.currencyrateconverter

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun writableCell(index: Int, row: Row): Cell {
    val cell = row.getCell(index)
    if (cell != null) {
        return cell
    }
    return row.createCell(index, CellType.STRING)
}

fun Cell.cellAsLocalDate(): LocalDate {
    if (cellType == CellType.NUMERIC) {
        val rowDateTime = localDateTimeCellValue
        return rowDateTime.toLocalDate()
    }
    if (cellType == CellType.STRING) {
        return LocalDate.parse(stringCellValue, DateTimeFormatter.ISO_LOCAL_DATE)
    }
    throw IllegalStateException("row ${row.rowNum} cell $columnIndex can not recognize date $cellType " + this.toString())
}

fun Cell.cellAsString(): String {
    if (cellType != CellType.STRING) {
        throw IllegalStateException("row ${row.rowNum} cell ${this.columnIndex} not string type but ${cellType}")
    }
    return stringCellValue
}

fun Cell.cellAsBigDecimal(): BigDecimal {
    if (cellType != CellType.NUMERIC) {
        throw IllegalStateException("row ${row.rowNum} cell ${columnIndex} not numeric type but ${cellType}")
    }
    return BigDecimal.valueOf(numericCellValue)
}

fun cellAsInt(cell: Cell): Int {
    if (cell.cellType != CellType.NUMERIC) {
        throw IllegalStateException("row ${cell.row.rowNum} cell ${cell.columnIndex} not numeric type but ${cell.cellType}")
    }
    return cell.numericCellValue.toInt()
}
