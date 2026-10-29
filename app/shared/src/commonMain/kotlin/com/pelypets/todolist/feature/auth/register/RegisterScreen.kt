package com.pelypets.todolist.feature.auth.register

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pelypets.todolist.core.ui.components.ProgressButton
import com.pelypets.todolist.core.ui.text
import com.pelypets.todolist.feature.auth.AuthHeader
import com.pelypets.todolist.feature.auth.AuthLayout
import com.pelypets.todolist.feature.auth.CredentialField
import com.pelypets.todolist.feature.auth.FormError
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import todolist.app.shared.generated.resources.Res
import todolist.app.shared.generated.resources.back
import todolist.app.shared.generated.resources.confirm_password
import todolist.app.shared.generated.resources.create_account
import todolist.app.shared.generated.resources.email
import todolist.app.shared.generated.resources.error_email_taken
import todolist.app.shared.generated.resources.ic_arrow_back
import todolist.app.shared.generated.resources.password
import todolist.app.shared.generated.resources.register_subtitle
import todolist.app.shared.generated.resources.register_title

@Composable
fun RegisterRoute(
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RegisterEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    RegisterScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun RegisterScreen(state: RegisterState, onIntent: (RegisterIntent) -> Unit) {
    val enabled = !state.isSubmitting
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onIntent(RegisterIntent.BackClicked) }, enabled = enabled) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        AuthLayout(contentPadding = padding) {
            AuthHeader(stringResource(Res.string.register_title), stringResource(Res.string.register_subtitle))
            CredentialField(
                value = state.email,
                onValueChange = { onIntent(RegisterIntent.EmailChanged(it)) },
                label = stringResource(Res.string.email),
                error = state.emailError,
                enabled = enabled,
                isPassword = false,
                imeAction = ImeAction.Next,
            )
            CredentialField(
                value = state.password,
                onValueChange = { onIntent(RegisterIntent.PasswordChanged(it)) },
                label = stringResource(Res.string.password),
                error = state.passwordError,
                enabled = enabled,
                isPassword = true,
                imeAction = ImeAction.Next,
            )
            CredentialField(
                value = state.confirmPassword,
                onValueChange = { onIntent(RegisterIntent.ConfirmPasswordChanged(it)) },
                label = stringResource(Res.string.confirm_password),
                error = state.confirmPasswordError,
                enabled = enabled,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = { onIntent(RegisterIntent.SubmitClicked) },
            )
            state.error?.let { error ->
                FormError(
                    when (error) {
                        RegisterError.EmailTaken -> stringResource(Res.string.error_email_taken)
                        is RegisterError.Other -> stringResource(error.error.text)
                    },
                )
            }
            ProgressButton(
                text = stringResource(Res.string.create_account),
                inProgress = state.isSubmitting,
                enabled = enabled,
                onClick = { onIntent(RegisterIntent.SubmitClicked) },
            )
        }
    }
}
