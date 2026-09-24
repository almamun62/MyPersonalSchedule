package com.example.util

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.model.CourseEntity
import com.example.data.model.WeekRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

data class OcrResult(
    val courses: List<CourseEntity>,
    val success: Boolean,
    val errorMessage: String? = null,
    val rawText: String? = null
)

object ScheduleOcrHelper {

    private val PALETTE = listOf(
        0xFF337DFF, // Blue
        0xFF00B4D8, // Cyan
        0xFF34C759, // Green
        0xFFFF9500, // Orange
        0xFFFF2D55, // Pink
        0xFFAF52DE, // Purple
        0xFF5856D6, // Indigo
        0xFFFFCC00  // Yellow
    )

    /**
     * Sends an image of a course schedule/timetable to Gemini Vision and parses the structured response.
     */
    suspend fun recognizeScheduleFromBitmap(
        bitmap: Bitmap,
        apiKey: String,
        semesterId: Long,
        modelName: String = "gemini-1.5-flash"
    ): OcrResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext OcrResult(
                courses = emptyList(),
                success = false,
                errorMessage = "Gemini API key is required for Photo OCR. Please enter your key in AI Settings."
            )
        }

        try {
            // Resize bitmap to fit within 1280x1280 to optimize network transfer
            val scaledBitmap = scaleBitmapToMaxDimension(bitmap, 1280)
            val base64Image = bitmapToBase64Jpeg(scaledBitmap)

            val prompt = """
                You are an expert university schedule OCR assistant.
                Analyze this timetable / class schedule image and extract ALL courses.
                Output ONLY a valid JSON array of objects with NO markdown formatting (do not include ```json or ```).
                
                Each object MUST have the following schema:
                {
                  "name": "string (course name)",
                  "code": "string (course code, or '' if not available)",
                  "classroom": "string (room or building, or 'TBA')",
                  "instructor": "string (teacher name, or '')",
                  "dayOfWeek": 1 to 7 (integer: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday),
                  "startPeriod": 1 to 12 (integer: starting period/section),
                  "endPeriod": 1 to 12 (integer: ending period/section, >= startPeriod),
                  "startTime": "string (e.g. '08:00', or default for period)",
                  "endTime": "string (e.g. '09:35', or default for period)",
                  "weekRule": "ALL" | "ODD" | "EVEN" | "CUSTOM",
                  "customWeeks": "string (e.g. '1-16', '1-10,12-18', or '' if ALL)"
                }
                
                If the schedule displays clock times instead of period numbers:
                - 08:00 - 09:35 = Period 1 to 2
                - 09:50 - 11:25 = Period 3 to 4
                - 09:50 - 12:15 = Period 3 to 5
                - 14:30 - 16:05 = Period 6 to 7
                - 16:20 - 17:55 = Period 8 to 9
                - 19:00 - 21:25 = Period 10 to 12
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(
                            Part(
                                inlineData = InlineData(
                                    mimeType = "image/jpeg",
                                    data = base64Image
                                )
                            ),
                            Part(text = prompt)
                        )
                    )
                )
            )

            val response = RetrofitClient.service.generateContent(apiKey = apiKey.trim(), request = request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext OcrResult(
                    courses = emptyList(),
                    success = false,
                    errorMessage = "No response text received from Vision AI."
                )

            val cleanJson = cleanJsonResponse(candidateText)
            val courses = parseJsonArrayToCourses(cleanJson, semesterId)

            if (courses.isNotEmpty()) {
                OcrResult(courses = courses, success = true, rawText = candidateText)
            } else {
                // If JSON parsing yielded 0 courses, try fallback heuristic text parsing
                val fallbackCourses = ScheduleImportHelper.parseTextToCourses(candidateText, semesterId)
                if (fallbackCourses.isNotEmpty()) {
                    OcrResult(courses = fallbackCourses, success = true, rawText = candidateText)
                } else {
                    OcrResult(
                        courses = emptyList(),
                        success = false,
                        errorMessage = "Could not identify timetable courses in image. Raw AI output:\n${candidateText.take(200)}",
                        rawText = candidateText
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            OcrResult(
                courses = emptyList(),
                success = false,
                errorMessage = "AI OCR Error: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }

    private fun scaleBitmapToMaxDimension(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).toInt()
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(source, newWidth.coerceAtLeast(1), newHeight.coerceAtLeast(1), true)
    }

    private fun bitmapToBase64Jpeg(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun cleanJsonResponse(rawText: String): String {
        var text = rawText.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json").trim()
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```").trim()
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }

        // Find outer [ ... ] if surrounded by explanatory text
        val firstBracket = text.indexOf('[')
        val lastBracket = text.lastIndexOf(']')
        if (firstBracket != -1 && lastBracket > firstBracket) {
            text = text.substring(firstBracket, lastBracket + 1)
        }
        return text
    }

    private fun parseJsonArrayToCourses(jsonText: String, semesterId: Long): List<CourseEntity> {
        val result = mutableListOf<CourseEntity>()
        try {
            val array = JSONArray(jsonText)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                if (name.isBlank()) continue

                val code = obj.optString("code", "").trim()
                val classroom = obj.optString("classroom", "TBA").trim().ifEmpty { "TBA" }
                val instructor = obj.optString("instructor", "").trim()
                val dayOfWeek = obj.optInt("dayOfWeek", 1).coerceIn(1, 7)
                val startPeriod = obj.optInt("startPeriod", 1).coerceIn(1, 12)
                val endPeriod = obj.optInt("endPeriod", startPeriod).coerceIn(startPeriod, 12)

                val defaultStartTime = when (startPeriod) {
                    1 -> "08:00"
                    3 -> "09:50"
                    6 -> "14:30"
                    8 -> "16:20"
                    10 -> "19:00"
                    else -> String.format("%02d:00", 7 + startPeriod)
                }
                val defaultEndTime = when (endPeriod) {
                    2 -> "09:35"
                    4 -> "11:25"
                    5 -> "12:15"
                    7 -> "16:05"
                    9 -> "17:55"
                    12 -> "21:25"
                    else -> String.format("%02d:45", 7 + endPeriod)
                }

                val startTime = obj.optString("startTime", defaultStartTime).trim().ifEmpty { defaultStartTime }
                val endTime = obj.optString("endTime", defaultEndTime).trim().ifEmpty { defaultEndTime }

                val weekRuleStr = obj.optString("weekRule", "ALL").uppercase()
                val weekRule = when {
                    weekRuleStr.contains("ODD") || weekRuleStr.contains("单") -> WeekRule.ODD
                    weekRuleStr.contains("EVEN") || weekRuleStr.contains("双") -> WeekRule.EVEN
                    weekRuleStr.contains("CUSTOM") -> WeekRule.CUSTOM
                    else -> WeekRule.ALL
                }
                val customWeeks = obj.optString("customWeeks", "").trim()

                val colorHex = PALETTE[i % PALETTE.size]

                result.add(
                    CourseEntity(
                        semesterId = semesterId,
                        name = name,
                        code = code,
                        classroom = classroom,
                        instructor = instructor,
                        dayOfWeek = dayOfWeek,
                        startPeriod = startPeriod,
                        endPeriod = endPeriod,
                        startTime = startTime,
                        endTime = endTime,
                        weekRule = weekRule,
                        customWeeks = customWeeks,
                        colorHex = colorHex,
                        dndEnabled = true
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
