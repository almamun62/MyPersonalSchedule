package com.example.domain.parser

import android.graphics.Bitmap
import android.util.Base64
import com.example.domain.model.ImportedCourse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiScheduleScanner {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    sealed class ScanResult {
        data class Success(val courses: List<ImportedCourse>, val source: String) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    suspend fun scanScheduleImage(bitmap: Bitmap, customApiKey: String? = null): ScanResult = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() } ?: getBuildConfigApiKey()

        if (apiKey.isNullOrBlank()) {
            // Intelligent demo OCR simulation for testing without configured API key
            val sampleCourses = ScheduleParser.parseCsv(
                """Code,Name,Days,Start Time,End Time,Room,Instructor,Credits
CS 180,Computer Graphics & Vision,MWF,10:00,11:15,Visual Lab 2,Dr. Sutherland,4
MATH 260,Discrete Mathematics,TR,09:30,10:45,Math 101,Prof. Boole,3
PHYS 210,Quantum Mechanics Basics,MWF,13:00,14:15,Physics 204,Dr. Planck,4
PHIL 102,Ethics & Computing,Friday,14:30,16:00,Auditorium 1,Prof. Socrates,3"""
            )
            return@withContext ScanResult.Success(
                courses = sampleCourses,
                source = "Demo Vision OCR (Add Gemini API key in Settings to use real AI Scan)"
            )
        }

        try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val prompt = """
                You are an expert student schedule reader. Analyze this image of a class schedule, syllabus, or timetable.
                Extract all courses and lectures. Return ONLY a raw JSON array of objects with the following keys:
                - "code": course code (e.g. "CS 101", "MATH 240")
                - "name": course title (e.g. "Intro to Computer Science")
                - "days": array of day names (e.g. ["Monday", "Wednesday", "Friday"])
                - "startTime": start time in 24-hour HH:mm format (e.g. "09:00", "13:30")
                - "endTime": end time in 24-hour HH:mm format (e.g. "10:15", "14:45")
                - "room": classroom location or hall (e.g. "Hall 101", "Online")
                - "instructor": instructor or professor name
                - "credits": number of credit hours (integer, default 3)
                Do not include markdown code block backticks (like ```json), return raw JSON array only.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ScanResult.Error("Gemini API error: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string() ?: return@withContext ScanResult.Error("Empty response from AI")
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            // Parse extracted JSON array
            val cleanJson = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleanJson)
            val extractedCourses = mutableListOf<ImportedCourse>()
            var colorIdx = 0

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val code = obj.optString("code", "")
                val name = obj.optString("name", code)
                val room = obj.optString("room", "")
                val instructor = obj.optString("instructor", "")
                val startTime = ScheduleParser.normalizeTime(obj.optString("startTime", "09:00"))
                val endTime = ScheduleParser.normalizeTime(obj.optString("endTime", "10:15"))
                val credits = obj.optInt("credits", 3)
                val daysArr = obj.optJSONArray("days")

                val daysList = mutableListOf<Int>()
                if (daysArr != null && daysArr.length() > 0) {
                    for (d in 0 until daysArr.length()) {
                        val dStr = daysArr.getString(d)
                        daysList.addAll(ScheduleParser.parseDays(dStr))
                    }
                } else {
                    daysList.add(1) // Monday default
                }

                val color = ScheduleParser.getColorForIndex(colorIdx++)

                for (day in daysList.distinct()) {
                    extractedCourses.add(
                        ImportedCourse(
                            name = name,
                            code = code,
                            instructor = instructor,
                            classroom = room,
                            dayOfWeek = day,
                            startTime = startTime,
                            endTime = endTime,
                            colorHex = color,
                            credits = credits,
                            isSelected = true
                        )
                    )
                }
            }

            if (extractedCourses.isEmpty()) {
                ScanResult.Error("No courses could be detected from the image. Please verify image clarity.")
            } else {
                ScanResult.Success(extractedCourses, "Gemini Vision AI")
            }
        } catch (e: Exception) {
            ScanResult.Error("Scan failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun getBuildConfigApiKey(): String? {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            field.get(null) as? String
        } catch (e: Exception) {
            null
        }
    }
}
