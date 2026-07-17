package com.example.nava.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import com.example.nava.R
import com.example.nava.ui.NavaEffect
import com.example.nava.ui.theme.NavaSpacing
import kotlinx.coroutines.flow.Flow

@Composable
fun AuthScreen(
    onSignIn: (String, String) -> Unit,
    effects: Flow<NavaEffect>,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val invalidCredentials = stringResource(R.string.invalid_credentials)

    LaunchedEffect(effects) {
        effects.collect { effect ->
            if (effect is NavaEffect.InvalidCredentials) snackbar.showSnackbar(invalidCredentials)
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
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { onSignIn(email, password) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sign_in))
            }
            OutlinedButton(onClick = { onSignIn(email, password) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.create_account))
            }
        }
    }
}
