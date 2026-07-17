package com.example.nava.domain.auth

import kotlinx.coroutines.flow.Flow

data class AuthSession(val email: String)

interface AuthRepository {
    val session: Flow<AuthSession?>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
}

object AuthValidator {
    private const val MinimumPasswordLength = 6

    fun isValid(email: String, password: String): Boolean =
        email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) && password.length >= MinimumPasswordLength
}
