package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.HolidayOverrideType
import com.example.data.model.NoteType
import com.example.data.model.WeekRule

class Converters {
    @TypeConverter
    fun fromWeekRule(rule: WeekRule): String = rule.name

    @TypeConverter
    fun toWeekRule(value: String): WeekRule = try {
        WeekRule.valueOf(value)
    } catch (e: Exception) {
        WeekRule.ALL
    }

    @TypeConverter
    fun fromHolidayOverrideType(type: HolidayOverrideType): String = type.name

    @TypeConverter
    fun toHolidayOverrideType(value: String): HolidayOverrideType = try {
        HolidayOverrideType.valueOf(value)
    } catch (e: Exception) {
        HolidayOverrideType.HOLIDAY
    }

    @TypeConverter
    fun fromNoteType(type: NoteType): String = type.name

    @TypeConverter
    fun toNoteType(value: String): NoteType = try {
        NoteType.valueOf(value)
    } catch (e: Exception) {
        NoteType.TEXT
    }
}
