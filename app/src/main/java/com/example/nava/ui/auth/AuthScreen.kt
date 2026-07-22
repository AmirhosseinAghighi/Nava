package com.example.nava.ui.auth

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.nava.R
import com.example.nava.ui.NavaEffect
import com.example.nava.ui.theme.NavaDimensions
import com.example.nava.ui.theme.NavaMotion
import com.example.nava.ui.theme.NavaSpacing
import kotlinx.coroutines.flow.Flow

private enum class AuthMode { SIGN_IN, SIGN_UP }

@Composable
fun AuthScreen(
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    isAuthenticating: Boolean,
    effects: Flow<NavaEffect>,
) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }
    val snackbar = remember { SnackbarHostState() }
    val invalidCredentials = stringResource(R.string.invalid_credentials)
    val invalidRegistration = stringResource(R.string.invalid_registration)
    val authenticationFailed = stringResource(R.string.authentication_failed)
    val accountConfirmationSent = stringResource(R.string.account_confirmation_sent)

    LaunchedEffect(effects) {
        effects.collect { effect ->
            if (effect == NavaEffect.AccountConfirmationSent) mode = AuthMode.SIGN_IN
            snackbar.showSnackbar(
                when (effect) {
                    NavaEffect.InvalidCredentials -> invalidCredentials
                    NavaEffect.InvalidRegistration -> invalidRegistration
                    NavaEffect.AuthenticationFailed -> authenticationFailed
                    NavaEffect.AccountConfirmationSent -> accountConfirmationSent
                },
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(contentPadding)
                    .padding(horizontal = NavaSpacing.Xl, vertical = NavaSpacing.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                NavaBrand()
                Spacer(Modifier.size(NavaSpacing.Xl))
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        fadeIn(tween(NavaMotion.Standard)) togetherWith fadeOut(tween(NavaMotion.Fast))
                    },
                    label = "auth_mode",
                    modifier = Modifier.fillMaxWidth(),
                ) { currentMode ->
                    when (currentMode) {
                        AuthMode.SIGN_IN -> SignInCard(
                            isAuthenticating = isAuthenticating,
                            onSignIn = onSignIn,
                            onOpenRegistration = { mode = AuthMode.SIGN_UP },
                        )

                        AuthMode.SIGN_UP -> SignUpCard(
                            isAuthenticating = isAuthenticating,
                            onSignUp = onSignUp,
                            onBackToSignIn = { mode = AuthMode.SIGN_IN },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavaBrand() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(NavaDimensions.AuthLogoSize)
                .shadow(NavaSpacing.Sm, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.auth_logo_content_description),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.size(NavaSpacing.Md))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.auth_brand_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SignInCard(
    isAuthenticating: Boolean,
    onSignIn: (String, String) -> Unit,
    onOpenRegistration: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSignIn(email, password)
    }

    AuthCard {
        AuthHeading(R.string.auth_sign_in_title, R.string.auth_sign_in_subtitle)
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = R.string.email,
            icon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            enabled = !isAuthenticating,
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            enabled = !isAuthenticating,
            onDone = submit,
        )
        PrimaryAuthButton(
            label = R.string.sign_in,
            loading = isAuthenticating,
            onClick = submit,
        )
        AuthSwitchPrompt(
            prompt = R.string.no_account,
            action = R.string.register_now,
            enabled = !isAuthenticating,
            onClick = onOpenRegistration,
        )
    }
}

@Composable
private fun SignUpCard(
    isAuthenticating: Boolean,
    onSignUp: (String, String, String) -> Unit,
    onBackToSignIn: () -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSignUp(displayName, email, password)
    }

    AuthCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToSignIn, enabled = !isAuthenticating) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_sign_in),
                )
            }
            Text(
                text = stringResource(R.string.back_to_sign_in),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AuthHeading(R.string.auth_sign_up_title, R.string.auth_sign_up_subtitle)
        AuthTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = R.string.username,
            supportingText = R.string.display_name_hint,
            icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            enabled = !isAuthenticating,
        )
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = R.string.email,
            icon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            enabled = !isAuthenticating,
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            enabled = !isAuthenticating,
            onDone = submit,
        )
        PrimaryAuthButton(
            label = R.string.create_account,
            loading = isAuthenticating,
            onClick = submit,
        )
        AuthSwitchPrompt(
            prompt = R.string.already_have_account,
            action = R.string.sign_in_now,
            enabled = !isAuthenticating,
            onClick = onBackToSignIn,
        )
    }
}

@Composable
private fun AuthCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = NavaDimensions.AuthContentMaxWidth),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = NavaSpacing.Xs),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(NavaSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(NavaSpacing.Lg),
            content = content,
        )
    }
}

@Composable
private fun AuthHeading(@StringRes title: Int, @StringRes subtitle: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(NavaSpacing.Xs)) {
        Text(text = stringResource(title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    icon: @Composable () -> Unit,
    keyboardOptions: KeyboardOptions,
    enabled: Boolean,
    @StringRes supportingText: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        leadingIcon = icon,
        supportingText = supportingText?.let { text -> { Text(stringResource(text)) } },
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onDone: () -> Unit,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.password)) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(if (visible) R.string.hide_password else R.string.show_password),
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        supportingText = { Text(stringResource(R.string.password_hint)) },
        enabled = enabled,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PrimaryAuthButton(
    @StringRes label: Int,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NavaDimensions.AuthButtonMinHeight),
    ) {
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NavaSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(NavaSpacing.Lg),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = NavaDimensions.AuthProgressStrokeWidth,
                )
                Text(stringResource(R.string.auth_working))
            }
        } else {
            Text(stringResource(label))
        }
    }
}

@Composable
private fun AuthSwitchPrompt(
    @StringRes prompt: Int,
    @StringRes action: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClick, enabled = enabled) {
            Text(stringResource(action))
        }
    }
}
