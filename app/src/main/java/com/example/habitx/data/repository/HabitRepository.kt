package com.example.habitx.data.repository

import com.example.habitx.data.local.dao.HabitDao
import com.example.habitx.data.local.dao.HabitEntryDao
import com.example.habitx.data.local.dao.UserDao
import com.example.habitx.data.local.dao.ReminderScheduleDao
import com.example.habitx.data.local.entity.Habit
import com.example.habitx.data.local.entity.HabitEntry
import com.example.habitx.data.local.entity.User
import com.example.habitx.data.local.entity.ReminderSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import java.util.UUID

class HabitRepository(
    private val userDao: UserDao,
    private val habitDao: HabitDao,
    private val habitEntryDao: HabitEntryDao,
    private val reminderScheduleDao: ReminderScheduleDao
) {
    fun getLoggedInUser(): Flow<User?> = userDao.getLoggedInUser()

    suspend fun authenticate(phoneNumber: String, passwordHash: String): Result<User> {
        val existingUser = userDao.getUserByPhoneNumber(phoneNumber)
        return if (existingUser != null) {
            if (existingUser.passwordHash == passwordHash) {
                Result.success(existingUser)
            } else {
                Result.failure(Exception("Incorrect password"))
            }
        } else {
            val newUser = User(
                id = UUID.randomUUID().toString(),
                phoneNumber = phoneNumber,
                passwordHash = passwordHash,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            userDao.insertUser(newUser)
            Result.success(newUser)
        }
    }

    suspend fun signOut() {
        userDao.clearUsers()
    }

    fun getHabits(userId: String): Flow<List<Habit>> = habitDao.getHabitsByUserId(userId)

    suspend fun addHabit(userId: String, name: String, color: Int, weeklyFrequency: Int) {
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            color = color,
            weeklyFrequency = weeklyFrequency,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit.copy(updatedAt = Clock.System.now()))
    }

    suspend fun deleteHabit(habit: Habit) {
        reminderScheduleDao.deleteReminderByHabitId(habit.id)
        habitEntryDao.deleteEntriesForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    fun getEntriesForHabit(habitId: String): Flow<List<HabitEntry>> =
        habitEntryDao.getEntriesForHabit(habitId)

    suspend fun toggleCompletion(habitId: String, date: LocalDate) {
        val existingEntry = habitEntryDao.getEntry(habitId, date)
        if (existingEntry != null) {
            habitEntryDao.deleteEntry(existingEntry)
        } else {
            val entry = HabitEntry(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                date = date,
                completed = true,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            habitEntryDao.insertEntry(entry)
        }
    }

    // Reminder methods
    fun getReminder(habitId: String): Flow<ReminderSchedule?> = 
        reminderScheduleDao.getReminderByHabitId(habitId)

    suspend fun saveReminder(reminder: ReminderSchedule) {
        reminderScheduleDao.insertReminder(reminder.copy(updatedAt = Clock.System.now()))
    }

    suspend fun deleteReminder(reminder: ReminderSchedule) {
        reminderScheduleDao.deleteReminder(reminder)
    }

    suspend fun getAllEnabledReminders(): List<ReminderSchedule> =
        reminderScheduleDao.getAllEnabledRemindersSync()
        
    suspend fun getHabitById(habitId: String): Habit? = habitDao.getHabitById(habitId)
}
