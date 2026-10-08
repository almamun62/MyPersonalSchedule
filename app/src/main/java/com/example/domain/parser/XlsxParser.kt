package com.example.domain.parser

import com.example.domain.model.ImportedCourse
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

object XlsxParser {

    /**
     * Parses real OOXML .xlsx files (which are zipped XML packages).
     * Extracts sharedStrings.xml and sheet1.xml directly using Java's built-in ZipInputStream and DOM Parser.
     * Zero external dependencies (no heavy Apache POI required).
     */
    fun parseXlsxStream(inputStream: InputStream): List<ImportedCourse> {
        val sharedStrings = mutableListOf<String>()
        var sheetXmlBytes: ByteArray? = null

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                        sharedStrings.addAll(parseSharedStrings(zip))
                    }
                    entry.name.equals("xl/worksheets/sheet1.xml", ignoreCase = true) ||
                    entry.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml", RegexOption.IGNORE_CASE)) -> {
                        if (sheetXmlBytes == null) {
                            sheetXmlBytes = zip.readBytes()
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (sheetXmlBytes == null) return emptyList()

        // Parse sheet1.xml into rows and columns
        val rows = parseSheetRows(sheetXmlBytes!!.inputStream(), sharedStrings)
        if (rows.isEmpty()) return emptyList()

        // 1. Check if the spreadsheet is a 2D Matrix Timetable (Mon-Sun columns x Period rows)
        val matrixCourses = MatrixTimetableParser.parseMatrixGrid(rows)
        if (matrixCourses.isNotEmpty()) {
            return matrixCourses
        }

        // 2. Otherwise convert parsed rows into CSV format and run through ScheduleParser record parser
        val csvBuffer = StringBuilder()
        for (row in rows) {
            val line = row.joinToString(",") { cell ->
                val escaped = cell.replace("\"", "\"\"")
                if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
                    "\"$escaped\""
                } else {
                    escaped
                }
            }
            csvBuffer.append(line).append("\n")
        }

        return ScheduleParser.parseCsvSchedule(csvBuffer.toString())
    }

    private fun parseSharedStrings(stream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            // Wrap in non-closing stream proxy because DocumentBuilder closes the stream
            val nonClosingStream = object : java.io.FilterInputStream(stream) {
                override fun close() { /* do not close zip */ }
            }
            val doc = builder.parse(nonClosingStream)
            val siNodes = doc.getElementsByTagName("si")
            for (i in 0 until siNodes.length) {
                val si = siNodes.item(i)
                val tNodes = (si as Element).getElementsByTagName("t")
                val textBuilder = StringBuilder()
                for (j in 0 until tNodes.length) {
                    textBuilder.append(tNodes.item(j).textContent)
                }
                strings.add(textBuilder.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strings
    }

    private fun parseSheetRows(stream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rowsList = mutableListOf<List<String>>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(stream)
            val rowNodes = doc.getElementsByTagName("row")

            for (i in 0 until rowNodes.length) {
                val rowElem = rowNodes.item(i) as Element
                val cNodes = rowElem.getElementsByTagName("c")
                val rowCells = mutableListOf<String>()

                for (j in 0 until cNodes.length) {
                    val cElem = cNodes.item(j) as Element
                    val cellType = cElem.getAttribute("t") // 's' for shared string
                    val vNodes = cElem.getElementsByTagName("v")
                    val valueText = if (vNodes.length > 0) vNodes.item(0).textContent else ""

                    val resolved = if (cellType == "s") {
                        val strIndex = valueText.toIntOrNull()
                        if (strIndex != null && strIndex in sharedStrings.indices) {
                            sharedStrings[strIndex]
                        } else {
                            valueText
                        }
                    } else {
                        // Might be inlineStr
                        val isNodes = cElem.getElementsByTagName("is")
                        if (isNodes.length > 0) {
                            val tNodes = (isNodes.item(0) as Element).getElementsByTagName("t")
                            if (tNodes.length > 0) tNodes.item(0).textContent else valueText
                        } else {
                            valueText
                        }
                    }
                    rowCells.add(resolved)
                }
                if (rowCells.any { it.isNotBlank() }) {
                    rowsList.add(rowCells)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rowsList
    }
}
