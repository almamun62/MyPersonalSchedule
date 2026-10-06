package com.example.domain.parser

import com.example.domain.model.ImportedCourse

object ExcelParser {

    /**
     * Parse Excel files (.xlsx, .xls) and Spreadsheet XML tables into ImportedCourse lists.
     * Uses lightweight stream-based string parsing to avoid heavy POI dependencies and OOM crashes.
     */
    fun parseExcelContent(content: String): List<ImportedCourse> {
        val list = mutableListOf<ImportedCourse>()

        // Check if content is XML Spreadsheet 2003 (<ss:Workbook> or <Table>)
        if (content.contains("<Table") || content.contains("<ss:Table") || content.contains("<Workbook")) {
            val rowRegex = Regex("(?i)(?s)<Row[^>]*>(.*?)</Row>")
            val cellRegex = Regex("(?i)(?s)<Data[^>]*>(.*?)</Data>")

            val rows = rowRegex.findAll(content).map { rowMatch ->
                cellRegex.findAll(rowMatch.groupValues[1]).map { cellMatch ->
                    cellMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                }.toList()
            }.toList()

            if (rows.isNotEmpty()) {
                val csvBuffer = StringBuilder()
                for (row in rows) {
                    csvBuffer.append(row.joinToString(",")).append("\n")
                }
                return ScheduleParser.parseCsvSchedule(csvBuffer.toString())
            }
        }

        // Fallback to ScheduleParser CSV/TSV engine if text-based
        return ScheduleParser.parseCsvSchedule(content)
    }
}
