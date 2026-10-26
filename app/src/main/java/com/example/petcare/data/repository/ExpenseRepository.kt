package com.example.petcare.data.repository

import com.example.petcare.data.dao.ExpenseDao
import com.example.petcare.data.entity.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao
) {

    suspend fun addExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    fun getExpenses(userId: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesForUser(userId)
    }

    fun getAllTimeTotal(userId: Long): Flow<Double> {
        return expenseDao.getAllTimeTotal(userId)
    }

    fun getMonthlyTotal(
        userId: Long,
        yearMonth: String
    ): Flow<Double> {
        return expenseDao.getMonthlyTotal(
            userId,
            yearMonth
        )
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }
}