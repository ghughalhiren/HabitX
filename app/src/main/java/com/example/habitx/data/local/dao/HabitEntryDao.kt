package com.example.habitx.data.local.dao

import androidx.room.*
import com.example.habitx.data.local.entity.HabitEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface HabitEntryDao {
    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId")
    fun getEntriesForHabit(habitId: String): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getEntry(habitId: String, date: LocalDate): HabitEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HabitEntry)

    @Delete
    suspend fun deleteEntry(entry: HabitEntry)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId")
    suspend fun deleteEntriesForHabit(habitId: String)
}
