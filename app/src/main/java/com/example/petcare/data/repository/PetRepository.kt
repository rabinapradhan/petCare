package com.example.petcare.data.repository

import com.example.petcare.data.dao.PetDao
import com.example.petcare.data.entity.Pet
import kotlinx.coroutines.flow.Flow

class PetRepository(
    private val petDao: PetDao
) {

    // =========================================================
    // ADD PET
    // =========================================================

    suspend fun addPet(
        pet: Pet
    ): Long {
        return petDao.insertPet(pet)
    }

    // =========================================================
    // GET PETS
    // =========================================================

    fun getPets(
        userId: Long
    ): Flow<List<Pet>> {
        return petDao.getPetsForUser(userId)
    }

    // =========================================================
    // PET COUNT
    // =========================================================

    fun getPetCount(
        userId: Long
    ): Flow<Int> {
        return petDao.getPetCount(userId)
    }

    // =========================================================
    // GET PET
    // =========================================================

    suspend fun getPetById(
        petId: Int
    ): Pet? {
        return petDao.getPetById(petId)
    }

    // =========================================================
    // GET PET FOR USER
    // =========================================================

    suspend fun getPetByIdForUser(
        petId: Int,
        userId: Long
    ): Pet? {
        return petDao.getPetByIdForUser(
            petId,
            userId
        )
    }

    // =========================================================
    // UPDATE PET
    // =========================================================

    suspend fun updatePet(
        pet: Pet
    ) {

        petDao.updatePet(
            petId = pet.id,
            userId = pet.userId,
            name = pet.name,
            species = pet.species,
            breed = pet.breed,
            age = pet.age,
            weight = pet.weight,
            dietaryPreferences = pet.dietaryPreferences,
            allergies = pet.allergies,
            vaccinationHistory = pet.vaccinationHistory,
            favouriteToys = pet.favouriteToys,
            notes = pet.notes,
            photoUri = pet.photoUri
        )
    }

    // =========================================================
    // DELETE PET
    // =========================================================

    suspend fun deletePet(
        petId: Int,
        userId: Long
    ) {
        petDao.deletePet(
            petId,
            userId
        )
    }
}