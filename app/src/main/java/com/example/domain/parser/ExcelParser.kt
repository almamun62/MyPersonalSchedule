package com.example.domain.parser

import com.example.domain.model.ImportedCourse
import java.io.ByteArrayInputStream
import java.io.InputStream

object ExcelParser {

    /**
     * Parses Excel files:
     * 1. Binary OOXML (.xlsx) packages via XlsxParser
     * 2. XML Spreadsheet 2003 (<ss:Workbook>) tables
     * 3. HTML tables disguised as .xls
     * 4. Text/CSV/TSV formatted Excel exports (both matrix grid and record list)
     */
    fun parseExcelStream(inputStream: InputStream, fileName: String = ""): List<ImportedCourse> {
        val bytes = inputStream.readBytes()

        // Check if ZIP OOXML header (PK\u0003\u0004) -> Real .xlsx file
        if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) {
            val list = XlsxParser.parseXlsxStream(ByteArrayInputStream(bytes))
            if (list.isNotEmpty()) return list
        }

        // Try text-based formats (CSV, TSV, XML Spreadsheet 2003, HTML)
        // Detect UTF-8, or fallback to GBK if contains Chinese encoding markers
        val text = try {
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            String(bytes)
        }

        return parseExcelContent(text)
    }

    /**
     * Parse text representations of spreadsheets (XML Spreadsheet 2003, HTML tables, or CSV/TSV).
     */
    fun parseExcelContent(content: String): List<ImportedCourse> {
        // Check if XML Spreadsheet 2003 (<ss:Workbook> or <Table>)
        if (content.contains("<Table", ignoreCase = true) || content.contains("<ss:Table", ignoreCase = true) || content.contains("<Workbook", ignoreCase = true)) {
            val rowRegex = Regex("(?i)(?s)<Row[^>]*>(.*?)</Row>")
            val cellRegex = Regex("(?i)(?s)<Data[^>]*>(.*?)</Data>")

            val rows = rowRegex.findAll(content).map { rowMatch ->
                cellRegex.findAll(rowMatch.groupValues[1]).map { cellMatch ->
                    cellMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                }.toList()
            }.toList()

            if (rows.isNotEmpty()) {
                val matrixCourses = MatrixTimetableParser.parseMatrixGrid(rows)
                if (matrixCourses.isNotEmpty()) return matrixCourses

                val csvBuffer = StringBuilder()
                for (row in rows) {
                    csvBuffer.append(row.joinToString(",")).append("\n")
                }
                return ScheduleParser.parseCsvSchedule(csvBuffer.toString())
            }
        }

        // Fallback to ScheduleParser CSV/TSV engine
        return ScheduleParser.parseCsvSchedule(content)
    }
}
