package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Check whether the user is already logged in.
         */
        val sharedPreferences =
            getSharedPreferences(
                "PetCarePrefs",
                MODE_PRIVATE
            )

        val isLoggedIn =
            sharedPreferences.getBoolean(
                "isLoggedIn",
                false
            )

        if (isLoggedIn) {

            val userId =
                sharedPreferences.getLong(
                    "userId",
                    -1L
                )

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

        /*
         * User is not logged in,
         * so show the normal home screen.
         */
        setContentView(R.layout.activity_main)

        setupButtons()
    }

    private fun setupButtons() {

        val signInButton =
            findViewById<TextView>(
                R.id.signInButton
            )

        val getStartedButton =
            findViewById<TextView>(
                R.id.getStartedButton
            )

        val createAccountButton =
            findViewById<TextView>(
                R.id.createAccountButton
            )

        // SIGN IN

        signInButton.setOnClickListener {

            val intent =
                Intent(
                    this,
                    LoginActivity::class.java
                )

            startActivity(intent)
        }

        // GET STARTED

        getStartedButton.setOnClickListener {

            val intent =
                Intent(
                    this,
                    RegisterActivity::class.java
                )

            startActivity(intent)
        }

        // CREATE ACCOUNT

        createAccountButton.setOnClickListener {

            val intent =
                Intent(
                    this,
                    RegisterActivity::class.java
                )

            startActivity(intent)
        }
    }
}