package com.example.habitx.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "habit_entries",
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
data class HabitEntry(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: LocalDate,
    val completed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
