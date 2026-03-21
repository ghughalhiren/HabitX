package com.example.habitx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val phoneNumber: String,
    val passwordHash: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
