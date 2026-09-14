package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.*

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
}

@Database(
    entities = [
        SemesterEntity::class,
        CourseEntity::class,
        ExamEntity::class,
        TaskEntity::class,
        HolidayOverrideEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun courseDao(): CourseDao
    abstract fun examDao(): ExamDao
    abstract fun taskDao(): TaskDao
    abstract fun holidayOverrideDao(): HolidayOverrideDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myschedule.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
