package com.example.petcare.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.petcare.data.entity.Place
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {

    @Insert
    suspend fun insertPlace(place: Place): Long

    @Query("""
        SELECT * FROM places
        WHERE userId = :userId
        ORDER BY id DESC
    """)
    fun getPlacesForUser(userId: Long): Flow<List<Place>>

    @Query("""
        SELECT * FROM places
        WHERE id = :placeId
        AND userId = :userId
        LIMIT 1
    """)
    suspend fun getPlaceById(
        placeId: Int,
        userId: Long
    ): Place?

    @Delete
    suspend fun deletePlace(place: Place)

    @Query("""
        DELETE FROM places
        WHERE id = :placeId
        AND userId = :userId
    """)
    suspend fun deletePlaceById(
        placeId: Int,
        userId: Long
    )
}