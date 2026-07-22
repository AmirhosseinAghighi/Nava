package com.example.nava

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.Intent
import android.os.Build
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.nava.domain.preferences.AppLanguage
import com.example.nava.domain.preferences.FontScale
import com.example.nava.domain.preferences.ThemeMode
import com.example.nava.ui.NavaAppShell
import com.example.nava.ui.NavaEffect
import com.example.nava.ui.NavaEvent
import com.example.nava.ui.NavaUiState
import com.example.nava.ui.NavaViewModel
import com.example.nava.ui.auth.AuthScreen
import com.example.nava.ui.theme.NavaTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var supabase: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase.handleDeeplinks(intent)
        requestNotificationsPermissionIfNeeded()
        enableEdgeToEdge()
        setContent { NavaRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        supabase.handleDeeplinks(intent)
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS_PERMISSION,
        )
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS_PERMISSION = 1001
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
    val fontScale = when (preferences?.fontScale) {
        FontScale.SMALL -> 0.9f
        FontScale.LARGE -> 1.15f
        else -> 1f
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

    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        NavaTheme(darkTheme = dark) {
            when (val current = state) {
                NavaUiState.Loading -> LoadingContent()
                is NavaUiState.SignedOut -> AuthScreen(
                    onSignIn = { email, password -> viewModel.onEvent(NavaEvent.SignIn(email, password)) },
                    onSignUp = { displayName, email, password ->
                        viewModel.onEvent(NavaEvent.SignUp(displayName, email, password))
                    },
                    isAuthenticating = current.isAuthenticating,
                    effects = viewModel.effects,
                )
                is NavaUiState.SignedIn -> key(current.session.userId) {
                    NavaAppShell(
                        session = current.session,
                        preferences = current.preferences,
                        onEvent = viewModel::onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
