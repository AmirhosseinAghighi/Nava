package com.example.nava

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import com.example.nava.domain.preferences.AppLanguage
import com.example.nava.domain.preferences.ThemeMode
import com.example.nava.ui.NavaAppShell
import com.example.nava.ui.NavaEffect
import com.example.nava.ui.NavaEvent
import com.example.nava.ui.NavaUiState
import com.example.nava.ui.NavaViewModel
import com.example.nava.ui.auth.AuthScreen
import com.example.nava.ui.theme.NavaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NavaRoot() }
    }
}

@Composable
private fun NavaRoot(viewModel: NavaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val preferences = when (val current = state) {
        NavaUiState.Loading -> null
        is NavaUiState.SignedIn -> current.preferences
        is NavaUiState.SignedOut -> current.preferences
    }
    val dark = when (preferences?.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        else -> systemDark
    }

    LaunchedEffect(preferences?.language) {
        AppCompatDelegate.setApplicationLocales(
            when (preferences?.language) {
                AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
                AppLanguage.PERSIAN -> LocaleListCompat.forLanguageTags("fa")
                else -> LocaleListCompat.getEmptyLocaleList()
            },
        )
    }

    NavaTheme(darkTheme = dark) {
        when (val current = state) {
            NavaUiState.Loading -> LoadingContent()
            is NavaUiState.SignedOut -> AuthScreen(
                onSignIn = { email, password -> viewModel.onEvent(NavaEvent.SignIn(email, password)) },
                onSignUp = { email, password -> viewModel.onEvent(NavaEvent.SignUp(email, password)) },
                effects = viewModel.effects,
            )
            is NavaUiState.SignedIn -> NavaAppShell(
                session = current.session,
                preferences = current.preferences,
                onEvent = viewModel::onEvent,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
