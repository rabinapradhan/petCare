package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
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
import com.example.petcare.ui.auth.RegisterState
import com.example.petcare.ui.auth.RegisterViewModel
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordVisibilityButton: ImageButton
    private lateinit var createAccountButton: Button

    private var isPasswordVisible = false

    private val registerViewModel: RegisterViewModel by viewModels {

        RegisterViewModel.Factory(
            UserRepository(
                PetCareDatabase
                    .getDatabase(applicationContext)
                    .userDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_register
        )

        initialiseViews()

        setupPasswordVisibility()

        setupClickListeners()

        observeRegistrationState()
    }

    private fun initialiseViews() {

        nameEditText =
            findViewById(R.id.nameEditText)

        emailEditText =
            findViewById(R.id.emailEditText)

        passwordEditText =
            findViewById(R.id.passwordEditText)

        passwordVisibilityButton =
            findViewById(R.id.passwordVisibilityButton)

        createAccountButton =
            findViewById(R.id.createAccountButton)
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

    private fun setupClickListeners() {

        val signInText =
            findViewById<TextView>(
                R.id.signInText
            )

        val backHomeText =
            findViewById<TextView>(
                R.id.backHomeText
            )

        // CREATE ACCOUNT

        createAccountButton.setOnClickListener {

            registerUser()
        }

        // SIGN IN

        signInText.setOnClickListener {

            val intent =
                Intent(
                    this,
                    LoginActivity::class.java
                )

            startActivity(intent)

            finish()
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

    private fun registerUser() {

        val name =
            nameEditText.text
                .toString()
                .trim()

        val email =
            emailEditText.text
                .toString()
                .trim()

        val password =
            passwordEditText.text
                .toString()

        registerViewModel.register(
            name = name,
            email = email,
            password = password
        )
    }

    private fun observeRegistrationState() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                registerViewModel
                    .registerState
                    .collect { state ->

                        when (state) {

                            RegisterState.Idle -> {

                                createAccountButton
                                    .isEnabled = true

                                createAccountButton.text =
                                    "Create account"
                            }

                            RegisterState.Loading -> {

                                createAccountButton
                                    .isEnabled = false

                                createAccountButton.text =
                                    "Creating account..."
                            }

                            is RegisterState.Success -> {

                                createAccountButton
                                    .isEnabled = true

                                createAccountButton.text =
                                    "Create account"

                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Account created successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Go to Login

                                val intent =
                                    Intent(
                                        this@RegisterActivity,
                                        LoginActivity::class.java
                                    )

                                intent.putExtra(
                                    "registered_email",
                                    emailEditText
                                        .text
                                        .toString()
                                        .trim()
                                )

                                startActivity(intent)

                                finish()
                            }

                            is RegisterState.Error -> {

                                createAccountButton
                                    .isEnabled = true

                                createAccountButton.text =
                                    "Create account"

                                Toast.makeText(
                                    this@RegisterActivity,
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