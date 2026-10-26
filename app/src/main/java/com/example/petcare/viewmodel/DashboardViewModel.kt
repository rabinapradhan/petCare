package com.example.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.entity.Pet
import com.example.petcare.data.repository.PetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val userId: Long,
    private val repository: PetRepository
) : ViewModel() {

    val pets: StateFlow<List<Pet>> =
        repository
            .getPets(userId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val petCount: StateFlow<Int> =
        repository
            .getPetCount(userId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )


    class Factory(
        private val userId: Long,
        private val repository: PetRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {

            if (
                modelClass.isAssignableFrom(
                    DashboardViewModel::class.java
                )
            ) {

                return DashboardViewModel(
                    userId,
                    repository
                ) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class"
            )
        }
    }
}