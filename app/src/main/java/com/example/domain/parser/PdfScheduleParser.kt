package com.example.domain.parser

import android.content.Context
import com.example.domain.model.ImportedCourse
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

/**
 * 100% Offline PDF Schedule Parser.
 * Uses PdfBox-Android to extract selectable text from PDF documents.
 * Feeds extracted text into MatrixTimetableParser and FreeTextScheduleParser.
 * Does not use any online or heavy OCR; if a scanned image PDF has no text, returns empty.
 */
object PdfScheduleParser {

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Extracts text from an offline PDF stream.
     * Returns a pair of:
     * - parsed courses
     * - boolean indicating whether text was found in the document (false = scanned/empty)
     */
    fun parsePdfStream(context: Context, inputStream: InputStream): Pair<List<ImportedCourse>, Boolean> {
        init(context)

        var extractedText = ""
        try {
            PDDocument.load(inputStream).use { document ->
                if (!document.isEncrypted) {
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    extractedText = stripper.getText(document).trim()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(emptyList(), false)
        }

        if (extractedText.isBlank()) {
            // Scanned image-only PDF with no text layers
            return Pair(emptyList(), false)
        }

        // Feed extracted text into parsers:
        // 1. Check if lines contain tabular structure with tabs
        if (extractedText.contains("\t")) {
            val rows = extractedText.lines().filter { it.isNotBlank() }.map { it.split("\t") }
            val matrixCourses = MatrixTimetableParser.parseMatrixGrid(rows)
            if (matrixCourses.isNotEmpty()) {
                return Pair(matrixCourses, true)
            }
        }

        // 2. Feed into free text / syllabus schedule parser
        val textCourses = FreeTextScheduleParser.parseText(extractedText)
        return Pair(textCourses, true)
    }
}
