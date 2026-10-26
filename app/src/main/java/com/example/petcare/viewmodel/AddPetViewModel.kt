package com.example.petcare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.entity.Pet
import com.example.petcare.data.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AddPetState {

    object Idle : AddPetState()

    object Saving : AddPetState()

    data class Success(
        val petId: Long
    ) : AddPetState()

    data class Error(
        val message: String
    ) : AddPetState()
}


class AddPetViewModel(
    private val repository: PetRepository
) : ViewModel() {

    private val _state =
        MutableStateFlow<AddPetState>(
            AddPetState.Idle
        )

    val state: StateFlow<AddPetState> =
        _state.asStateFlow()


    fun savePet(pet: Pet) {

        if (pet.name.isBlank()) {

            _state.value =
                AddPetState.Error(
                    "Please enter your pet's name"
                )

            return
        }

        if (pet.species.isBlank()) {

            _state.value =
                AddPetState.Error(
                    "Please select a species"
                )

            return
        }

        viewModelScope.launch {

            try {

                _state.value =
                    AddPetState.Saving

                val petId =
                    repository.addPet(pet)

                _state.value =
                    AddPetState.Success(petId)

            } catch (exception: Exception) {

                _state.value =
                    AddPetState.Error(
                        exception.message
                            ?: "Unable to save pet"
                    )
            }
        }
    }


    class Factory(
        private val repository: PetRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {

            if (
                modelClass.isAssignableFrom(
                    AddPetViewModel::class.java
                )
            ) {

                return AddPetViewModel(
                    repository
                ) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class"
            )
        }
    }
}