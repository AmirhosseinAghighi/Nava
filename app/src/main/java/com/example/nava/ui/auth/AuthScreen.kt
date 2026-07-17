package com.example.nava.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import com.example.nava.R
import com.example.nava.ui.NavaEffect
import com.example.nava.ui.theme.NavaSpacing
import kotlinx.coroutines.flow.Flow

@Composable
fun AuthScreen(
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    isAuthenticating: Boolean,
    effects: Flow<NavaEffect>,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val invalidCredentials = stringResource(R.string.invalid_credentials)
    val authenticationFailed = stringResource(R.string.authentication_failed)
    val accountConfirmationSent = stringResource(R.string.account_confirmation_sent)

    LaunchedEffect(effects) {
        effects.collect { effect ->
            snackbar.showSnackbar(
                when (effect) {
                    NavaEffect.InvalidCredentials -> invalidCredentials
                    NavaEffect.AuthenticationFailed -> authenticationFailed
                    NavaEffect.AccountConfirmationSent -> accountConfirmationSent
                },
            )
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(NavaSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg),
        ) {
            Text(text = stringResource(R.string.auth_title), style = MaterialTheme.typography.displaySmall)
            Text(text = stringResource(R.string.auth_subtitle), style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                enabled = !isAuthenticating,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                enabled = !isAuthenticating,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSignIn(email, password) },
                enabled = !isAuthenticating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AuthButtonLabel(
                    loading = isAuthenticating,
                    label = R.string.sign_in,
                )
            }
            OutlinedButton(
                onClick = { onSignUp(email, password) },
                enabled = !isAuthenticating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AuthButtonLabel(
                    loading = isAuthenticating,
                    label = R.string.create_account,
                )
            }
        }
    }
}

@Composable
private fun AuthButtonLabel(loading: Boolean, label: Int) {
    if (loading) {
        Row(horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(NavaSpacing.Lg))
            Text(stringResource(R.string.auth_working))
        }
    } else {
        Text(stringResource(label))
    }
}
