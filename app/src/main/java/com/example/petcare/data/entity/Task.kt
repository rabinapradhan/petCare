package com.example.petcare.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",

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
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index(value = ["userId"]),
        Index(value = ["petId"])
    ]
)
data class Task(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Long,

    val petId: Int,

    val title: String,

    val category: String,

    val repeats: String,

    val time: String,

    val supplies: String,

    val notes: String,

    val completed: Boolean = false,

    val startDate: String,

    val completedDate: String? = null
)