package com.example.petcare.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.petcare.data.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Query("""
        SELECT * FROM expenses
        WHERE userId = :userId
        ORDER BY date DESC, id DESC
    """)
    fun getExpensesForUser(userId: Long): Flow<List<Expense>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM expenses
        WHERE userId = :userId
    """)
    fun getAllTimeTotal(userId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM expenses
        WHERE userId = :userId
        AND substr(date, 1, 7) = :yearMonth
    """)
    fun getMonthlyTotal(
        userId: Long,
        yearMonth: String
    ): Flow<Double>

    @Delete
    suspend fun deleteExpense(expense: Expense)
}