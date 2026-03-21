package com.example.habitx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val color: Int,
    val weeklyFrequency: Int = 7, // 7 for Everyday, 3-6 for others
    val createdAt: Instant,
    val updatedAt: Instant
)
