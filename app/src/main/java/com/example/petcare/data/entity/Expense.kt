package com.example.petcare.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["petId"]),
        Index(value = ["date"])
    ]
)
data class Expense(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Long,

    val petId: Int? = null,

    val amount: Double,

    val category: String,

    val date: String,

    val note: String = ""
)