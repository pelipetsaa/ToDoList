package com.pelypets.todolist.feature.auth.register

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.feature.auth.confirmPasswordErrorFrom
import com.pelypets.todolist.feature.auth.emailErrorFrom
import com.pelypets.todolist.feature.auth.passwordErrorFrom

object RegisterReducer {

    fun reduce(state: RegisterState, mutation: RegisterMutation): RegisterState = when (mutation) {
        is RegisterMutation.EmailChanged -> state.copy(email = mutation.value, emailError = null, error = null)
        is RegisterMutation.PasswordChanged -> state.copy(password = mutation.value, passwordError = null, error = null)
        is RegisterMutation.ConfirmPasswordChanged ->
            state.copy(confirmPassword = mutation.value, confirmPasswordError = null, error = null)
        RegisterMutation.SubmitStarted -> state.copy(isSubmitting = true, error = null)
        RegisterMutation.Succeeded -> state.copy(isSubmitting = false)
        is RegisterMutation.Failed -> failed(state.copy(isSubmitting = false), mutation.error)
    }

    private fun failed(state: RegisterState, error: DomainError): RegisterState = when {
        error is DomainError.Validation && error.errors.isNotEmpty() -> state.copy(
            emailError = emailErrorFrom(error.errors),
            passwordError = passwordErrorFrom(error.errors),
            confirmPasswordError = confirmPasswordErrorFrom(error.errors),
        )
        error == DomainError.Conflict -> state.copy(error = RegisterError.EmailTaken)
        else -> state.copy(error = RegisterError.Other(error))
    }
}
