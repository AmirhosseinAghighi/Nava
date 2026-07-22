package com.example.nava.data.auth

import com.example.nava.domain.auth.AuthRepository
import com.example.nava.domain.auth.AuthSession
import com.example.nava.domain.auth.AuthValidator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {
    override val session: Flow<AuthSession?> = supabase.auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)
            ?.session
            ?.user
            ?.let { user -> AuthSession(userId = user.id, email = user.email.orEmpty()) }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        require(AuthValidator.isValid(email, password))
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override suspend fun signUp(displayName: String, email: String, password: String): Result<Unit> = runCatching {
        require(AuthValidator.isValidRegistration(displayName, email, password))
        supabase.auth.signUpWith(Email, redirectUrl = NavaAuthRedirect.Url) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject {
                put("display_name", displayName.trim())
            }
        }
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }
}
