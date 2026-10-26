package com.example.petcare.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.entity.User
import com.example.petcare.data.repository.UserRepository
import com.example.petcare.utils.PasswordUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {

    object Idle : LoginState()

    object Loading : LoginState()

    data class Success(
        val user: User
    ) : LoginState()

    data class Error(
        val message: String
    ) : LoginState()
}


class LoginViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _loginState =
        MutableStateFlow<LoginState>(
            LoginState.Idle
        )

    val loginState: StateFlow<LoginState> =
        _loginState.asStateFlow()


    fun login(
        email: String,
        password: String
    ) {

        if (email.isBlank()) {

            _loginState.value =
                LoginState.Error(
                    "Please enter your email"
                )

            return
        }


        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            _loginState.value =
                LoginState.Error(
                    "Please enter a valid email"
                )

            return
        }


        if (password.isBlank()) {

            _loginState.value =
                LoginState.Error(
                    "Please enter your password"
                )

            return
        }


        viewModelScope.launch {

            _loginState.value =
                LoginState.Loading

            val passwordHash =
                PasswordUtils.hashPassword(password)

            val result =
                repository.loginUser(
                    email = email.trim().lowercase(),
                    passwordHash = passwordHash
                )

            result.fold(

                onSuccess = { user ->

                    _loginState.value =
                        LoginState.Success(user)
                },

                onFailure = { exception ->

                    _loginState.value =
                        LoginState.Error(
                            exception.message
                                ?: "Login failed"
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
                    LoginViewModel::class.java
                )
            ) {

                return LoginViewModel(
                    repository
                ) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class"
            )
        }
    }
}