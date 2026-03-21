package com.example.habitx.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "reminder_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId")]
)
data class ReminderSchedule(
    @PrimaryKey val habitId: String,
    val enabled: Boolean,
    val daysOfWeek: List<Int>, // 1=Mon ... 7=Sun
    val timeHour: Int,
    val timeMinute: Int,
    val updatedAt: Instant
)
