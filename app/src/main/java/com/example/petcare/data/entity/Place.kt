package com.example.petcare.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "places",

    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index(value = ["userId"]),
        Index(value = ["googlePlaceId"])
    ]
)
data class Place(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Long,

    val googlePlaceId: String? = null,

    val name: String,

    val address: String,

    val latitude: Double,

    val longitude: Double,

    val phone: String = "",

    val openingHours: String = "",

    val type: String
)