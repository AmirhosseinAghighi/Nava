package com.example.nava.domain.auth

import kotlinx.coroutines.flow.Flow

data class AuthSession(val email: String)

interface AuthRepository {
    val session: Flow<AuthSession?>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(displayName: String, email: String, password: String): Result<Unit>
    suspend fun signOut()
}

object AuthValidator {
    private const val MinimumPasswordLength = 6
    private const val MinimumDisplayNameLength = 2
    private const val MaximumDisplayNameLength = 60

    fun isValid(email: String, password: String): Boolean =
        email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) && password.length >= MinimumPasswordLength

    fun isValidRegistration(displayName: String, email: String, password: String): Boolean =
        displayName.trim().length in MinimumDisplayNameLength..MaximumDisplayNameLength && isValid(email, password)
}
