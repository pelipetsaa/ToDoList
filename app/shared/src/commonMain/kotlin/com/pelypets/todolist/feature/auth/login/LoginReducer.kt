package com.pelypets.todolist.feature.auth.login

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.feature.auth.emailErrorFrom
import com.pelypets.todolist.feature.auth.passwordErrorFrom

object LoginReducer {

    fun reduce(state: LoginState, mutation: LoginMutation): LoginState = when (mutation) {
        is LoginMutation.EmailChanged -> state.copy(email = mutation.value, emailError = null, error = null)
        is LoginMutation.PasswordChanged -> state.copy(password = mutation.value, passwordError = null, error = null)
        LoginMutation.SubmitStarted -> state.copy(isSubmitting = true, error = null)
        LoginMutation.GoogleStarted -> state.copy(isGoogleInProgress = true, error = null)
        LoginMutation.GoogleCancelled -> state.copy(isGoogleInProgress = false)
        LoginMutation.GoogleFailed -> state.copy(isGoogleInProgress = false, error = LoginError.GoogleFailed)
        LoginMutation.Succeeded -> state.copy(isSubmitting = false, isGoogleInProgress = false)
        is LoginMutation.Failed -> failed(state.copy(isSubmitting = false, isGoogleInProgress = false), mutation)
    }

    private fun failed(state: LoginState, mutation: LoginMutation.Failed): LoginState {
        val error = mutation.error
        return when {
            error is DomainError.Validation && error.errors.isNotEmpty() -> state.copy(
                emailError = emailErrorFrom(error.errors),
                passwordError = passwordErrorFrom(error.errors),
            )
            error == DomainError.Unauthorized -> state.copy(
                error = if (mutation.viaGoogle) LoginError.GoogleFailed else LoginError.InvalidCredentials,
            )
            else -> state.copy(error = LoginError.Other(error))
        }
    }
}
