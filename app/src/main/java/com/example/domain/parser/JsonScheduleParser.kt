package com.example.domain.parser

import com.example.data.model.WeekRule
import com.example.domain.model.ImportedCourse
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust 100% offline JSON Timetable Parser.
 * Supports:
 * 1. WakeUp 课程表 (WakeUp Schedule) open JSON format
 * 2. ClassIsland timetable profile JSON
 * 3. Universal JSON course array [ { name, day, startPeriod, endPeriod, room, teacher } ]
 * 4. App native schedule backup JSON
 */
object JsonScheduleParser {

    private val COLORS = listOf(
        "#2563EB", "#10B981", "#F59E0B", "#EC4899",
        "#8B5CF6", "#EF4444", "#06B6D4", "#F97316"
    )

    fun parseJsonSchedule(jsonString: String): List<ImportedCourse> {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            if (trimmed.startsWith("[")) {
                parseJsonArray(JSONArray(trimmed))
            } else if (trimmed.startsWith("{")) {
                parseJsonObject(JSONObject(trimmed))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseJsonObject(obj: JSONObject): List<ImportedCourse> {
        // 1. WakeUp 课程表 standard format:
        // Has "courses" array and "courseItems" / "schedule" array
        if (obj.has("courses") || obj.has("course_items") || obj.has("courseItems")) {
            return parseWakeUpFormat(obj)
        }

        // 2. ClassIsland profile format: Has "Classes" or "TimeLayout"
        if (obj.has("Classes") || obj.has("classes")) {
            val classArray = obj.optJSONArray("Classes") ?: obj.optJSONArray("classes")
            if (classArray != null) {
                return parseJsonArray(classArray)
            }
        }

        // 3. Nested "data", "schedule", "timetable", "coursesList"
        val nestedKeys = listOf("data", "schedule", "timetable", "coursesList", "list", "items", "courseList")
        for (key in nestedKeys) {
            val nestedArray = obj.optJSONArray(key)
            if (nestedArray != null && nestedArray.length() > 0) {
                return parseJsonArray(nestedArray)
            }
        }

        return emptyList()
    }

    /**
     * Parse standard WakeUp 课程表 JSON schema:
     * "courses": [ { "id": 1, "name": "...", "teacher": "..." } ]
     * "courseItems": [ { "id": 1, "courseId": 1, "day": 1, "startSection": 1, "endSection": 2, "room": "..." } ]
     */
    private fun parseWakeUpFormat(root: JSONObject): List<ImportedCourse> {
        val coursesMap = mutableMapOf<Int, JSONObject>()
        val coursesArray = root.optJSONArray("courses") ?: JSONArray()
        for (i in 0 until coursesArray.length()) {
            val c = coursesArray.optJSONObject(i) ?: continue
            val id = c.optInt("id", i + 1)
            coursesMap[id] = c
        }

        val itemsArray = root.optJSONArray("courseItems")
            ?: root.optJSONArray("course_items")
            ?: root.optJSONArray("items")
            ?: JSONArray()

        val list = mutableListOf<ImportedCourse>()
        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.optJSONObject(i) ?: continue
            val courseId = item.optInt("courseId", item.optInt("course_id", -1))
            val courseObj = coursesMap[courseId] ?: item

            val name = courseObj.optString("name", item.optString("name", "Course $i"))
            val teacher = courseObj.optString("teacher", item.optString("teacher", ""))
            val room = item.optString("room", courseObj.optString("room", item.optString("location", "Classroom")))
            val day = item.optInt("day", item.optInt("dayOfWeek", 1)).coerceIn(1, 7)
            val startSec = item.optInt("startSection", item.optInt("start_section", item.optInt("startPeriod", 1))).coerceIn(1, 12)
            val endSec = item.optInt("endSection", item.optInt("end_section", item.optInt("endPeriod", startSec + 1))).coerceIn(startSec, 12)

            // Week rule: checks if odd/even weeks
            var weekRule = WeekRule.ALL
            val weeksArray = item.optJSONArray("weeks")
            if (weeksArray != null && weeksArray.length() > 0) {
                val weekNumbers = mutableListOf<Int>()
                for (w in 0 until weeksArray.length()) {
                    weekNumbers.add(weeksArray.optInt(w))
                }
                if (weekNumbers.all { it % 2 != 0 }) {
                    weekRule = WeekRule.ODD
                } else if (weekNumbers.all { it % 2 == 0 }) {
                    weekRule = WeekRule.EVEN
                }
            }

            val (sTime, eTime) = ScheduleParser.defaultTimesForPeriods(startSec, endSec)
            val color = COLORS[list.size % COLORS.size]

            list.add(
                ImportedCourse(
                    name = name,
                    classroom = room.ifBlank { "Classroom" },
                    instructor = teacher,
                    dayOfWeek = day,
                    startPeriod = startSec,
                    endPeriod = endSec,
                    startTime = sTime,
                    endTime = eTime,
                    weekRule = weekRule,
                    colorHex = color
                )
            )
        }

        // If items were directly in courses
        if (list.isEmpty() && coursesArray.length() > 0) {
            return parseJsonArray(coursesArray)
        }

        return list
    }

    private fun parseJsonArray(array: JSONArray): List<ImportedCourse> {
        val list = mutableListOf<ImportedCourse>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue

            val name = obj.optString("name",
                obj.optString("courseName",
                    obj.optString("title",
                        obj.optString("Subject", "Course ${i + 1}"))))

            val code = obj.optString("code", obj.optString("courseCode", ""))
            val room = obj.optString("classroom",
                obj.optString("room",
                    obj.optString("location",
                        obj.optString("place", "Classroom"))))

            val teacher = obj.optString("instructor",
                obj.optString("teacher",
                    obj.optString("faculty",
                        obj.optString("professor", ""))))

            // Day of week
            val rawDay = obj.opt("dayOfWeek") ?: obj.opt("day") ?: obj.opt("weekDay")
            val day = when (rawDay) {
                is Number -> rawDay.toInt().coerceIn(1, 7)
                is String -> parseDayString(rawDay) ?: 1
                else -> 1
            }

            val startP = (obj.optInt("startPeriod",
                obj.optInt("startSection",
                    obj.optInt("start", 1)))).coerceIn(1, 12)

            val endP = (obj.optInt("endPeriod",
                obj.optInt("endSection",
                    obj.optInt("end", startP + 1)))).coerceIn(startP, 12)

            val (defaultS, defaultE) = ScheduleParser.defaultTimesForPeriods(startP, endP)
            val sTime = obj.optString("startTime", defaultS)
            val eTime = obj.optString("endTime", defaultE)

            val rawWeekRule = obj.optString("weekRule", "ALL").uppercase()
            val weekRule = when {
                rawWeekRule.contains("ODD") || rawWeekRule.contains("单") -> WeekRule.ODD
                rawWeekRule.contains("EVEN") || rawWeekRule.contains("双") -> WeekRule.EVEN
                else -> WeekRule.ALL
            }

            val color = obj.optString("colorHex", COLORS[list.size % COLORS.size])

            list.add(
                ImportedCourse(
                    name = name,
                    code = code,
                    classroom = room.ifBlank { "Classroom" },
                    instructor = teacher,
                    dayOfWeek = day,
                    startPeriod = startP,
                    endPeriod = endP,
                    startTime = sTime,
                    endTime = eTime,
                    weekRule = weekRule,
                    colorHex = color
                )
            )
        }
        return list
    }

    private fun parseDayString(text: String): Int? {
        val lower = text.trim().lowercase()
        return when {
            lower.contains("mon") || lower.contains("一") -> 1
            lower.contains("tue") || lower.contains("二") -> 2
            lower.contains("wed") || lower.contains("三") -> 3
            lower.contains("thu") || lower.contains("四") -> 4
            lower.contains("fri") || lower.contains("五") -> 5
            lower.contains("sat") || lower.contains("六") -> 6
            lower.contains("sun") || lower.contains("日") || lower.contains("天") -> 7
            else -> lower.toIntOrNull()?.coerceIn(1, 7)
        }
    }
}
