package com.pelypets.todolist.feature.auth.register

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError

data class RegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val confirmPasswordError: ValidationError? = null,
    val isSubmitting: Boolean = false,
    val error: RegisterError? = null,
)

sealed interface RegisterError {
    data object EmailTaken : RegisterError
    data class Other(val error: DomainError) : RegisterError
}

sealed interface RegisterIntent {
    data class EmailChanged(val value: String) : RegisterIntent
    data class PasswordChanged(val value: String) : RegisterIntent
    data class ConfirmPasswordChanged(val value: String) : RegisterIntent
    data object SubmitClicked : RegisterIntent
    data object BackClicked : RegisterIntent
}

sealed interface RegisterEffect {
    data object NavigateBack : RegisterEffect
}

sealed interface RegisterMutation {
    data class EmailChanged(val value: String) : RegisterMutation
    data class PasswordChanged(val value: String) : RegisterMutation
    data class ConfirmPasswordChanged(val value: String) : RegisterMutation
    data object SubmitStarted : RegisterMutation
    data object Succeeded : RegisterMutation
    data class Failed(val error: DomainError) : RegisterMutation
}
