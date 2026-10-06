package com.example.domain.parser

import android.util.Base64
import com.example.domain.model.Course
import com.example.domain.model.ImportedCourse
import org.json.JSONArray
import org.json.JSONObject

object ShareCodeManager {

    /**
     * Generates a compact Share Code (e.g., SCH#...) from a list of courses.
     */
    fun generateShareCode(courses: List<Course>): String {
        val array = JSONArray()
        courses.forEach { c ->
            val obj = JSONObject()
            obj.put("n", c.name)
            obj.put("r", c.classroom)
            obj.put("t", c.instructor)
            obj.put("d", c.dayOfWeek)
            obj.put("sp", c.startPeriod)
            obj.put("ep", c.endPeriod)
            obj.put("st", c.startTime)
            obj.put("et", c.endTime)
            obj.put("c", c.colorHex)
            array.put(obj)
        }
        val rawJson = array.toString()
        val base64 = Base64.encodeToString(rawJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
        return "SCH#$base64"
    }

    /**
     * Parses a Share Code (SCH#...) into a list of ImportedCourse.
     */
    fun parseShareCode(code: String): List<ImportedCourse> {
        val list = mutableListOf<ImportedCourse>()
        try {
            val cleanCode = code.trim().removePrefix("SCH#")
            val decodedBytes = Base64.decode(cleanCode, Base64.NO_WRAP or Base64.URL_SAFE)
            val jsonString = String(decodedBytes, Charsets.UTF_8)
            val array = JSONArray(jsonString)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ImportedCourse(
                        name = obj.optString("n", "Course"),
                        classroom = obj.optString("r", "Room 101"),
                        instructor = obj.optString("t", "Faculty"),
                        dayOfWeek = obj.optInt("d", 1),
                        startPeriod = obj.optInt("sp", 1),
                        endPeriod = obj.optInt("ep", 2),
                        startTime = obj.optString("st", "08:00"),
                        endTime = obj.optString("et", "09:35"),
                        colorHex = obj.optString("c", "#2563EB")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
