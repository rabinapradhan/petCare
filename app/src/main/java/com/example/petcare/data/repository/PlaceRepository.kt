package com.example.petcare.data.repository

import com.example.petcare.data.dao.PlaceDao
import com.example.petcare.data.entity.Place
import kotlinx.coroutines.flow.Flow

class PlaceRepository(
    private val placeDao: PlaceDao
) {

    suspend fun addPlace(place: Place): Long {
        return placeDao.insertPlace(place)
    }

    fun getPlaces(userId: Long): Flow<List<Place>> {
        return placeDao.getPlacesForUser(userId)
    }

    suspend fun getPlaceById(
        placeId: Int,
        userId: Long
    ): Place? {
        return placeDao.getPlaceById(
            placeId,
            userId
        )
    }

    suspend fun deletePlace(
        placeId: Int,
        userId: Long
    ) {
        placeDao.deletePlaceById(
            placeId,
            userId
        )
    }
}