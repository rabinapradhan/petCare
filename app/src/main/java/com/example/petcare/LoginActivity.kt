package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.TransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.repository.UserRepository
import com.example.petcare.ui.auth.LoginState
import com.example.petcare.ui.auth.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordVisibilityButton: ImageButton
    private lateinit var signInButton: Button

    private var isPasswordVisible = false

    private val loginViewModel: LoginViewModel by viewModels {

        LoginViewModel.Factory(
            UserRepository(
                PetCareDatabase
                    .getDatabase(applicationContext)
                    .userDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Check whether the user is already logged in.
         */
        val sharedPreferences =
            getSharedPreferences("PetCarePrefs", MODE_PRIVATE)

        val isLoggedIn =
            sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {

            val userId =
                sharedPreferences.getLong("userId", -1L)

            if (userId != -1L) {

                val intent =
                    Intent(
                        this,
                        DashboardActivity::class.java
                    )

                intent.putExtra(
                    "user_id",
                    userId
                )

                startActivity(intent)

                finish()

                return
            }
        }

        setContentView(R.layout.activity_login)

        initialiseViews()

        setupPasswordVisibility()

        loadRegisteredEmail()

        setupClickListeners()

        observeLoginState()
    }

    private fun initialiseViews() {

        emailEditText =
            findViewById(R.id.emailEditText)

        passwordEditText =
            findViewById(R.id.passwordEditText)

        passwordVisibilityButton =
            findViewById(R.id.passwordVisibilityButton)

        signInButton =
            findViewById(R.id.signInButton)
    }

    private fun setupPasswordVisibility() {

        passwordVisibilityButton.setOnClickListener {

            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {

                // Show password
                passwordEditText.transformationMethod = null

                passwordVisibilityButton.setImageResource(
                    R.drawable.ic_visibility
                )

                passwordVisibilityButton.contentDescription =
                    "Hide password"

            } else {

                // Hide password
                passwordEditText.transformationMethod =
                    PasswordTransformationMethod.getInstance()

                passwordVisibilityButton.setImageResource(
                    R.drawable.ic_visibility_off
                )

                passwordVisibilityButton.contentDescription =
                    "Show password"
            }

            /*
             * Keep cursor at the end of the password.
             */
            passwordEditText.setSelection(
                passwordEditText.text.length
            )

            passwordEditText.requestFocus()
        }
    }

    private fun loadRegisteredEmail() {

        val registeredEmail =
            intent.getStringExtra(
                "registered_email"
            )

        if (!registeredEmail.isNullOrEmpty()) {

            emailEditText.setText(
                registeredEmail
            )
        }
    }

    private fun setupClickListeners() {

        val registerText =
            findViewById<TextView>(
                R.id.registerText
            )

        val backHomeText =
            findViewById<TextView>(
                R.id.backHomeText
            )

        // SIGN IN

        signInButton.setOnClickListener {

            loginUser()
        }

        // REGISTER

        registerText.setOnClickListener {

            val intent =
                Intent(
                    this,
                    RegisterActivity::class.java
                )

            startActivity(intent)
        }

        // BACK HOME

        backHomeText.setOnClickListener {

            val intent =
                Intent(
                    this,
                    MainActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP

            startActivity(intent)

            finish()
        }
    }

    private fun loginUser() {

        val email =
            emailEditText.text
                .toString()
                .trim()

        val password =
            passwordEditText.text
                .toString()

        loginViewModel.login(
            email = email,
            password = password
        )
    }

    private fun observeLoginState() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                loginViewModel
                    .loginState
                    .collect { state ->

                        when (state) {

                            LoginState.Idle -> {

                                signInButton.isEnabled = true

                                signInButton.text =
                                    "Sign in"
                            }

                            LoginState.Loading -> {

                                signInButton.isEnabled = false

                                signInButton.text =
                                    "Signing in..."
                            }

                            is LoginState.Success -> {

                                signInButton.isEnabled = true

                                signInButton.text =
                                    "Sign in"

                                val sharedPreferences =
                                    getSharedPreferences(
                                        "PetCarePrefs",
                                        MODE_PRIVATE
                                    )

                                sharedPreferences.edit()
                                    .putBoolean(
                                        "isLoggedIn",
                                        true
                                    )
                                    .putLong(
                                        "userId",
                                        state.user.id.toLong()
                                    )
                                    .apply()

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Welcome ${state.user.name}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent =
                                    Intent(
                                        this@LoginActivity,
                                        DashboardActivity::class.java
                                    )

                                intent.putExtra(
                                    "user_id",
                                    state.user.id.toLong()
                                )

                                startActivity(intent)

                                finish()
                            }

                            is LoginState.Error -> {

                                signInButton.isEnabled = true

                                signInButton.text =
                                    "Sign in"

                                Toast.makeText(
                                    this@LoginActivity,
                                    state.message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
            }
        }
    }
}