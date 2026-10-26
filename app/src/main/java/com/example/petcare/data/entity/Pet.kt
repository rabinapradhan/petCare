package com.example.petcare.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pets",

    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index(value = ["userId"])
    ]
)
data class Pet(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Long,

    val name: String,

    val species: String,

    val breed: String,

    val age: String,

    val weight: String,

    val dietaryPreferences: String,

    val allergies: String,

    val vaccinationHistory: String,

    val favouriteToys: String,

    val notes: String,

    val photoUri: String? = null
)