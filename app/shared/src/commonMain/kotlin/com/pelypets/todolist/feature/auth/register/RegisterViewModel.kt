package com.pelypets.todolist.feature.auth.register

import androidx.lifecycle.viewModelScope
import com.pelypets.todolist.core.mvi.MviViewModel
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.usecase.RegisterUseCase
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val register: RegisterUseCase,
) : MviViewModel<RegisterState, RegisterIntent, RegisterEffect>(RegisterState()) {

    override fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.EmailChanged -> reduce(RegisterMutation.EmailChanged(intent.value))
            is RegisterIntent.PasswordChanged -> reduce(RegisterMutation.PasswordChanged(intent.value))
            is RegisterIntent.ConfirmPasswordChanged -> reduce(RegisterMutation.ConfirmPasswordChanged(intent.value))
            RegisterIntent.SubmitClicked -> submit()
            RegisterIntent.BackClicked -> if (!currentState.isSubmitting) sendEffect(RegisterEffect.NavigateBack)
        }
    }

    private fun submit() {
        if (currentState.isSubmitting) return
        reduce(RegisterMutation.SubmitStarted)
        val state = currentState
        viewModelScope.launch {
            val mutation = when (val result = register(state.email, state.password, state.confirmPassword)) {
                is DomainResult.Success -> RegisterMutation.Succeeded
                is DomainResult.Failure -> RegisterMutation.Failed(result.error)
            }
            reduce(mutation)
        }
    }

    private fun reduce(mutation: RegisterMutation) = updateState { RegisterReducer.reduce(it, mutation) }
}
