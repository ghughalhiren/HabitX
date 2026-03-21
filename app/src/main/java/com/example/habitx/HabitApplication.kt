package com.example.habitx

import android.app.Application
import androidx.room.Room
import com.example.habitx.data.local.AppDatabase
import com.example.habitx.data.repository.HabitRepository

class HabitApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "habit_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val repository by lazy {
        HabitRepository(
            database.userDao(),
            database.habitDao(),
            database.habitEntryDao(),
            database.reminderScheduleDao()
        )
    }
}
