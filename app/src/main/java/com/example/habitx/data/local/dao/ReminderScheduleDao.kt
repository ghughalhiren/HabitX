package com.example.habitx.data.local.dao

import androidx.room.*
import com.example.habitx.data.local.entity.ReminderSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderScheduleDao {
    @Query("SELECT * FROM reminder_schedules WHERE habitId = :habitId LIMIT 1")
    fun getReminderByHabitId(habitId: String): Flow<ReminderSchedule?>

    @Query("SELECT * FROM reminder_schedules WHERE habitId = :habitId LIMIT 1")
    suspend fun getReminderByHabitIdSync(habitId: String): ReminderSchedule?

    @Query("SELECT * FROM reminder_schedules WHERE enabled = 1")
    suspend fun getAllEnabledRemindersSync(): List<ReminderSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderSchedule)

    @Delete
    suspend fun deleteReminder(reminder: ReminderSchedule)

    @Query("DELETE FROM reminder_schedules WHERE habitId = :habitId")
    suspend fun deleteReminderByHabitId(habitId: String)
}
