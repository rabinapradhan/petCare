package com.example.petcare.utils

import java.security.MessageDigest

object PasswordUtils {

    fun hashPassword(password: String): String {

        val digest =
            MessageDigest.getInstance("SHA-256")

        val hashBytes =
            digest.digest(password.toByteArray(Charsets.UTF_8))

        return hashBytes.joinToString("") {
            "%02x".format(it)
        }
    }
}