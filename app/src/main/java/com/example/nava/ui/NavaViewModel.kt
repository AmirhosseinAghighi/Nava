package com.example.nava.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nava.domain.auth.AuthRepository
import com.example.nava.domain.auth.AuthSession
import com.example.nava.domain.preferences.AppLanguage
import com.example.nava.domain.preferences.PreferencesRepository
import com.example.nava.domain.preferences.ThemeMode
import com.example.nava.domain.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NavaUiState {
    data object Loading : NavaUiState
    data class SignedOut(
        val preferences: UserPreferences,
        val isAuthenticating: Boolean,
    ) : NavaUiState
    data class SignedIn(val session: AuthSession, val preferences: UserPreferences) : NavaUiState
}

sealed interface NavaEvent {
    data class SignIn(val email: String, val password: String) : NavaEvent
    data class SignUp(val email: String, val password: String) : NavaEvent
    data class SetTheme(val mode: ThemeMode) : NavaEvent
    data class SetLanguage(val language: AppLanguage) : NavaEvent
    data object SignOut : NavaEvent
}

sealed interface NavaEffect {
    data object InvalidCredentials : NavaEffect
    data object AuthenticationFailed : NavaEffect
    data object AccountConfirmationSent : NavaEffect
}

@HiltViewModel
class NavaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val isAuthenticating = MutableStateFlow(false)

    val uiState: StateFlow<NavaUiState> = combine(
        authRepository.session,
        preferencesRepository.preferences,
        isAuthenticating,
    ) { session, preferences, submitting ->
        if (session == null) NavaUiState.SignedOut(preferences, submitting) else NavaUiState.SignedIn(session, preferences)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), NavaUiState.Loading)

    private val effectChannel = Channel<NavaEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onEvent(event: NavaEvent) = viewModelScope.launch {
        when (event) {
            is NavaEvent.SignIn -> submitAuthentication(
                operation = { authRepository.signIn(event.email, event.password) },
            )
            is NavaEvent.SignUp -> submitAuthentication(
                operation = { authRepository.signUp(event.email, event.password) },
                successEffect = NavaEffect.AccountConfirmationSent,
            )
            is NavaEvent.SetTheme -> preferencesRepository.setThemeMode(event.mode)
            is NavaEvent.SetLanguage -> preferencesRepository.setLanguage(event.language)
            NavaEvent.SignOut -> authRepository.signOut()
        }
    }

    private suspend fun submitAuthentication(
        operation: suspend () -> Result<Unit>,
        successEffect: NavaEffect? = null,
    ) {
        isAuthenticating.value = true
        val result = operation()
        isAuthenticating.value = false
        result.onSuccess {
            successEffect?.let { effectChannel.send(it) }
        }.onFailure { exception ->
            effectChannel.send(
                if (exception is IllegalArgumentException) NavaEffect.InvalidCredentials else NavaEffect.AuthenticationFailed,
            )
        }
    }
}
