package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.data.model.CourseEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterInputStream

object PdfScheduleHelper {

    /**
     * Attempts to extract plain text and courses from a PDF file without external heavy dependencies,
     * by parsing uncompressed and FlateDecode streams inside the PDF.
     */
    fun parsePdfText(
        context: Context,
        uri: Uri,
        semesterId: Long,
        autoTranslateToEnglish: Boolean = false,
        bilingual: Boolean = false
    ): List<CourseEntity> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return emptyList()
            val extractedText = extractTextFromPdfBytes(bytes)
            if (extractedText.isNotBlank()) {
                ScheduleImportHelper.parseTextToCourses(
                    rawText = extractedText,
                    semesterId = semesterId,
                    autoTranslateToEnglish = autoTranslateToEnglish,
                    bilingual = bilingual
                )
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Renders the first page of a PDF into a high-resolution Bitmap using native Android PdfRenderer.
     * Useful for visual preview and Photo/Vision OCR extraction.
     */
    fun renderPdfFirstPage(context: Context, uri: Uri, targetWidth: Int = 1200): Bitmap? {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            pfd.use { fd ->
                val renderer = PdfRenderer(fd)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1)
                    val outWidth = (page.width * scale).toInt().coerceIn(600, 2400)
                    val outHeight = (page.height * scale).toInt().coerceIn(600, 3200)

                    val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    bitmap
                } else {
                    renderer.close()
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Pure-Kotlin text extractor for PDF byte streams.
     * Scans for 'stream' ... 'endstream' blocks, inflates FlateDecode streams,
     * and extracts string literals from text showing operators (Tj, TJ).
     */
    fun extractTextFromPdfBytes(bytes: ByteArray): String {
        val fullTextBuilder = StringBuilder()
        var index = 0

        val streamMarker = "stream".toByteArray(Charsets.US_ASCII)
        val endStreamMarker = "endstream".toByteArray(Charsets.US_ASCII)

        while (index < bytes.size - streamMarker.size) {
            val streamStart = indexOf(bytes, streamMarker, index)
            if (streamStart == -1) break

            var dataStart = streamStart + streamMarker.size
            // Skip CRLF / LF after 'stream' keyword
            while (dataStart < bytes.size && (bytes[dataStart] == '\r'.code.toByte() || bytes[dataStart] == '\n'.code.toByte())) {
                dataStart++
            }

            val streamEnd = indexOf(bytes, endStreamMarker, dataStart)
            if (streamEnd == -1) break

            val streamBytes = bytes.copyOfRange(dataStart, streamEnd)

            // Look back up to 200 bytes for /Filter /FlateDecode
            val headerLookbackStart = (streamStart - 250).coerceAtLeast(0)
            val streamHeader = String(bytes.copyOfRange(headerLookbackStart, streamStart), Charsets.US_ASCII)
            val isFlate = streamHeader.contains("/FlateDecode")

            val decompressedBytes = if (isFlate) {
                inflateStream(streamBytes)
            } else {
                streamBytes
            }

            if (decompressedBytes != null && decompressedBytes.isNotEmpty()) {
                val streamText = extractTextFromOperatorStream(decompressedBytes)
                if (streamText.isNotBlank()) {
                    fullTextBuilder.append(streamText).append("\n")
                }
            }

            index = streamEnd + endStreamMarker.size
        }

        // Also search for uncompressed literals directly in the document body
        val literalText = extractDirectStringLiterals(bytes)
        if (literalText.isNotBlank()) {
            fullTextBuilder.append("\n").append(literalText)
        }

        return fullTextBuilder.toString()
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (fromIndex >= source.size) return -1
        outer@ for (i in fromIndex..(source.size - target.size)) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun inflateStream(data: ByteArray): ByteArray? {
        return try {
            val bis = ByteArrayInputStream(data)
            val iis = InflaterInputStream(bis)
            val bos = ByteArrayOutputStream()
            val buf = ByteArray(1024)
            var len: Int
            while (iis.read(buf).also { len = it } > 0) {
                bos.write(buf, 0, len)
            }
            bos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTextFromOperatorStream(streamBytes: ByteArray): String {
        val result = StringBuilder()
        val streamContent = try {
            String(streamBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            String(streamBytes, Charsets.ISO_8859_1)
        }

        var inBt = false
        val lines = streamContent.split("\r\n", "\n", "\r")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "BT") {
                inBt = true
                continue
            }
            if (trimmed == "ET") {
                inBt = false
                result.append("\n")
                continue
            }

            if (inBt) {
                // Look for (text) Tj
                val tjRegex = Regex("""\((.*?)\)\s*Tj""")
                tjRegex.findAll(line).forEach { match ->
                    val text = unescapePdfString(match.groupValues[1])
                    result.append(text).append(" ")
                }

                // Look for [(text) 20 (more)] TJ
                val tjArrayRegex = Regex("""\[(.*?)\]\s*TJ""")
                tjArrayRegex.findAll(line).forEach { match ->
                    val arrayContent = match.groupValues[1]
                    val innerTextRegex = Regex("""\((.*?)\)""")
                    innerTextRegex.findAll(arrayContent).forEach { item ->
                        val text = unescapePdfString(item.groupValues[1])
                        result.append(text)
                    }
                    result.append(" ")
                }

                if (trimmed.endsWith("T*") || trimmed.endsWith("TD") || trimmed.endsWith("Td")) {
                    result.append("\n")
                }
            }
        }

        return result.toString()
    }

    private fun extractDirectStringLiterals(bytes: ByteArray): String {
        val sb = StringBuilder()
        val content = try {
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            String(bytes, Charsets.ISO_8859_1)
        }

        // Match typical Chinese university timetable phrases
        val patterns = listOf(Regex("""[\u4e00-\u9fa5A-Za-z0-9\-_]{2,30}\s*\d{1,2}-\d{1,2}周"""))
        for (pattern in patterns) {
            pattern.findAll(content).forEach {
                sb.append(it.value).append("\n")
            }
        }
        return sb.toString()
    }

    private fun unescapePdfString(input: String): String {
        return input
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}
