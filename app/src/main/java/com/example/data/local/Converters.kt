package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.WeekRule

class Converters {
    @TypeConverter
    fun fromWeekRule(rule: WeekRule): String = rule.name

    @TypeConverter
    fun toWeekRule(value: String): WeekRule {
        return try {
            WeekRule.valueOf(value)
        } catch (e: Exception) {
            WeekRule.ALL
        }
    }
}
