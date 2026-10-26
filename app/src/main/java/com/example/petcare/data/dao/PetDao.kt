package com.example.petcare.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.petcare.data.entity.Pet
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {

    @Insert
    suspend fun insertPet(pet: Pet): Long

    @Query(
        """
        SELECT * FROM pets
        WHERE userId = :userId
        ORDER BY id DESC
        """
    )
    fun getPetsForUser(
        userId: Long
    ): Flow<List<Pet>>

    @Query(
        """
        SELECT COUNT(*) FROM pets
        WHERE userId = :userId
        """
    )
    fun getPetCount(
        userId: Long
    ): Flow<Int>

    @Query(
        """
        SELECT * FROM pets
        WHERE id = :petId
        LIMIT 1
        """
    )
    suspend fun getPetById(
        petId: Int
    ): Pet?


    // GET PET FOR SPECIFIC USER


    @Query(
        """
        SELECT * FROM pets
        WHERE id = :petId
        AND userId = :userId
        LIMIT 1
        """
    )
    suspend fun getPetByIdForUser(
        petId: Int,
        userId: Long
    ): Pet?


    // UPDATE PET

    @Query(
        """
        UPDATE pets SET
            name = :name,
            species = :species,
            breed = :breed,
            age = :age,
            weight = :weight,
            dietaryPreferences = :dietaryPreferences,
            allergies = :allergies,
            vaccinationHistory = :vaccinationHistory,
            favouriteToys = :favouriteToys,
            notes = :notes,
            photoUri = :photoUri
        WHERE id = :petId
        AND userId = :userId
        """
    )
    suspend fun updatePet(
        petId: Int,
        userId: Long,
        name: String,
        species: String,
        breed: String,
        age: String,
        weight: String,
        dietaryPreferences: String,
        allergies: String,
        vaccinationHistory: String,
        favouriteToys: String,
        notes: String,
        photoUri: String?
    )

    // DELETE PET


    @Query(
        """
        DELETE FROM pets
        WHERE id = :petId
        AND userId = :userId
        """
    )
    suspend fun deletePet(
        petId: Int,
        userId: Long
    )
}