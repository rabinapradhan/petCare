package com.example.petcare.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.petcare.data.entity.User

@Dao
interface UserDao {

    @Insert
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query(
        "SELECT * FROM users " +
                "WHERE email = :email AND passwordHash = :passwordHash " +
                "LIMIT 1"
    )
    suspend fun loginUser(
        email: String,
        passwordHash: String
    ): User?

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun emailExists(email: String): Int

    // Get the currently logged-in user
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): User?
}