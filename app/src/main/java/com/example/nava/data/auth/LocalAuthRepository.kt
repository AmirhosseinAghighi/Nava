package com.example.nava.data.auth

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import com.example.nava.domain.auth.AuthRepository
import com.example.nava.domain.auth.AuthSession
import com.example.nava.domain.auth.AuthValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "nava_session")

@Singleton
class LocalAuthRepository @Inject constructor(
    private val context: Context,
) : AuthRepository {
    override val session: Flow<AuthSession?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[SessionEmail]?.let(::AuthSession) }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        if (!AuthValidator.isValid(email, password)) return Result.failure(IllegalArgumentException())
        context.sessionDataStore.edit { it[SessionEmail] = email.trim() }
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        context.sessionDataStore.edit { it.remove(SessionEmail) }
    }

    private companion object {
        val SessionEmail = stringPreferencesKey("session_email")
    }
}
