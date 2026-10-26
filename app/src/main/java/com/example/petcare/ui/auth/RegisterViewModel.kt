package com.example.petcare.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.repository.UserRepository
import com.example.petcare.utils.PasswordUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterState {

    object Idle : RegisterState()

    object Loading : RegisterState()

    data class Success(
        val userId: Long
    ) : RegisterState()

    data class Error(
        val message: String
    ) : RegisterState()
}


class RegisterViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _registerState =
        MutableStateFlow<RegisterState>(
            RegisterState.Idle
        )

    val registerState: StateFlow<RegisterState> =
        _registerState.asStateFlow()


    fun register(
        name: String,
        email: String,
        password: String
    ) {

        if (name.isBlank()) {

            _registerState.value =
                RegisterState.Error(
                    "Please enter your name"
                )

            return
        }


        if (email.isBlank()) {

            _registerState.value =
                RegisterState.Error(
                    "Please enter your email"
                )

            return
        }


        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            _registerState.value =
                RegisterState.Error(
                    "Please enter a valid email"
                )

            return
        }


        if (password.isBlank()) {

            _registerState.value =
                RegisterState.Error(
                    "Please enter a password"
                )

            return
        }


        if (password.length < 6) {

            _registerState.value =
                RegisterState.Error(
                    "Password must contain at least 6 characters"
                )

            return
        }


        viewModelScope.launch {

            _registerState.value =
                RegisterState.Loading

            val passwordHash =
                PasswordUtils.hashPassword(password)

            val result =
                repository.registerUser(
                    name = name.trim(),
                    email = email.trim().lowercase(),
                    passwordHash = passwordHash
                )

            result.fold(

                onSuccess = { userId ->

                    _registerState.value =
                        RegisterState.Success(userId)
                },

                onFailure = { exception ->

                    _registerState.value =
                        RegisterState.Error(
                            exception.message
                                ?: "Registration failed"
                        )
                }
            )
        }
    }


    class Factory(
        private val repository: UserRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {

            if (modelClass.isAssignableFrom(
                    RegisterViewModel::class.java
                )
            ) {

                return RegisterViewModel(
                    repository
                ) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class"
            )
        }
    }
}