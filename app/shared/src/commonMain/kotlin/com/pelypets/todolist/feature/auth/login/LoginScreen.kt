package com.pelypets.todolist.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pelypets.todolist.auth.GoogleSignInResult
import com.pelypets.todolist.auth.LocalGoogleSignInProvider
import com.pelypets.todolist.core.ui.components.ProgressButton
import com.pelypets.todolist.core.ui.text
import com.pelypets.todolist.feature.auth.AuthHeader
import com.pelypets.todolist.feature.auth.AuthLayout
import com.pelypets.todolist.feature.auth.CredentialField
import com.pelypets.todolist.feature.auth.FormError
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import todolist.app.shared.generated.resources.Res
import todolist.app.shared.generated.resources.email
import todolist.app.shared.generated.resources.error_google_failed
import todolist.app.shared.generated.resources.error_invalid_credentials
import todolist.app.shared.generated.resources.login_subtitle
import todolist.app.shared.generated.resources.login_title
import todolist.app.shared.generated.resources.no_account
import todolist.app.shared.generated.resources.or
import todolist.app.shared.generated.resources.password
import todolist.app.shared.generated.resources.sign_in
import todolist.app.shared.generated.resources.sign_in_with_google

@Composable
fun LoginRoute(
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val googleSignIn = LocalGoogleSignInProvider.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToRegister -> onNavigateToRegister()
                LoginEffect.LaunchGoogleSignIn -> viewModel.onIntent(
                    when (val result = googleSignIn.signIn()) {
                        is GoogleSignInResult.Success -> LoginIntent.GoogleTokenReceived(result.idToken)
                        GoogleSignInResult.Cancelled -> LoginIntent.GoogleCancelled
                        is GoogleSignInResult.Failure -> LoginIntent.GoogleFailed(result.message)
                    },
                )
            }
        }
    }

    LoginScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun LoginScreen(state: LoginState, onIntent: (LoginIntent) -> Unit) {
    Scaffold { padding ->
        AuthLayout(contentPadding = padding) {
            AuthHeader(stringResource(Res.string.login_title), stringResource(Res.string.login_subtitle))
            CredentialField(
                value = state.email,
                onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
                label = stringResource(Res.string.email),
                error = state.emailError,
                enabled = !state.isBusy,
                isPassword = false,
                imeAction = ImeAction.Next,
            )
            CredentialField(
                value = state.password,
                onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                label = stringResource(Res.string.password),
                error = state.passwordError,
                enabled = !state.isBusy,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = { onIntent(LoginIntent.SubmitClicked) },
            )
            state.error?.let { FormError(it.message()) }
            ProgressButton(
                text = stringResource(Res.string.sign_in),
                inProgress = state.isSubmitting,
                enabled = !state.isBusy,
                onClick = { onIntent(LoginIntent.SubmitClicked) },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HorizontalDivider(Modifier.weight(1f))
                Text(stringResource(Res.string.or), style = MaterialTheme.typography.labelMedium)
                HorizontalDivider(Modifier.weight(1f))
            }
            ProgressButton(
                text = stringResource(Res.string.sign_in_with_google),
                inProgress = state.isGoogleInProgress,
                enabled = !state.isBusy,
                onClick = { onIntent(LoginIntent.GoogleClicked) },
                outlined = true,
            )
            TextButton(
                onClick = { onIntent(LoginIntent.RegisterClicked) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.no_account))
            }
        }
    }
}

@Composable
private fun LoginError.message(): String = when (this) {
    LoginError.InvalidCredentials -> stringResource(Res.string.error_invalid_credentials)
    LoginError.GoogleFailed -> stringResource(Res.string.error_google_failed)
    is LoginError.Other -> stringResource(error.text)
}
