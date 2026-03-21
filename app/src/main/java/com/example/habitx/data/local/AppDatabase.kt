package com.example.habitx.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.habitx.data.local.dao.HabitDao
import com.example.habitx.data.local.dao.HabitEntryDao
import com.example.habitx.data.local.dao.UserDao
import com.example.habitx.data.local.dao.ReminderScheduleDao
import com.example.habitx.data.local.entity.Habit
import com.example.habitx.data.local.entity.HabitEntry
import com.example.habitx.data.local.entity.User
import com.example.habitx.data.local.entity.ReminderSchedule

@Database(
    entities = [User::class, Habit::class, HabitEntry::class, ReminderSchedule::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
    abstract fun habitEntryDao(): HabitEntryDao
    abstract fun reminderScheduleDao(): ReminderScheduleDao
}
