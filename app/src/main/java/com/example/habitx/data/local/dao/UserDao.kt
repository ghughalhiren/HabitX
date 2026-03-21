package com.example.habitx.data.local.dao

import androidx.room.*
import com.example.habitx.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhoneNumber(phoneNumber: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users LIMIT 1")
    fun getLoggedInUser(): Flow<User?>

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}
