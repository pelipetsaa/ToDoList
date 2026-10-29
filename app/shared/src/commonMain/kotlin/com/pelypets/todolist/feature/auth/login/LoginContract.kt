package com.pelypets.todolist.feature.auth.login

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError

data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val isSubmitting: Boolean = false,
    val isGoogleInProgress: Boolean = false,
    val error: LoginError? = null,
) {
    val isBusy: Boolean get() = isSubmitting || isGoogleInProgress
}

sealed interface LoginError {
    data object InvalidCredentials : LoginError
    data object GoogleFailed : LoginError
    data class Other(val error: DomainError) : LoginError
}

sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object SubmitClicked : LoginIntent
    data object GoogleClicked : LoginIntent
    data class GoogleTokenReceived(val idToken: String) : LoginIntent
    data object GoogleCancelled : LoginIntent
    data class GoogleFailed(val message: String?) : LoginIntent
    data object RegisterClicked : LoginIntent
}

sealed interface LoginEffect {
    data object LaunchGoogleSignIn : LoginEffect
    data object NavigateToRegister : LoginEffect
}

/** Results of intents and use cases, folded into [LoginState] by [LoginReducer]. */
sealed interface LoginMutation {
    data class EmailChanged(val value: String) : LoginMutation
    data class PasswordChanged(val value: String) : LoginMutation
    data object SubmitStarted : LoginMutation
    data object GoogleStarted : LoginMutation
    data object GoogleCancelled : LoginMutation
    data object GoogleFailed : LoginMutation
    data object Succeeded : LoginMutation
    data class Failed(val error: DomainError, val viaGoogle: Boolean = false) : LoginMutation
}
