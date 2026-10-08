package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val triggerTimeMillis: Long,
    val repeatInterval: String = "NONE", // NONE, DAILY, WEEKLY
    val relatedType: String = "GENERAL", // COURSE, TASK, EXAM, GENERAL
    val relatedId: Long = 0L,
    val isEnabled: Boolean = true
)
