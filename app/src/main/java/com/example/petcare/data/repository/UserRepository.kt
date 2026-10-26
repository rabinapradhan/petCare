package com.example.petcare.data.repository

import com.example.petcare.data.dao.UserDao
import com.example.petcare.data.entity.User

class UserRepository(
    private val userDao: UserDao
) {

    suspend fun registerUser(
        name: String,
        email: String,
        passwordHash: String
    ): Result<Long> {

        return try {

            val existingUser =
                userDao.getUserByEmail(email)

            if (existingUser != null) {

                Result.failure(
                    Exception("An account with this email already exists")
                )

            } else {

                val user = User(
                    name = name,
                    email = email,
                    passwordHash = passwordHash
                )

                val userId =
                    userDao.insertUser(user)

                Result.success(userId)
            }

        } catch (exception: Exception) {

            Result.failure(exception)
        }
    }


    suspend fun loginUser(
        email: String,
        passwordHash: String
    ): Result<User> {

        return try {

            val user =
                userDao.loginUser(
                    email = email,
                    passwordHash = passwordHash
                )

            if (user != null) {

                Result.success(user)

            } else {

                Result.failure(
                    Exception("Invalid email or password")
                )
            }

        } catch (exception: Exception) {

            Result.failure(exception)
        }
    }
}