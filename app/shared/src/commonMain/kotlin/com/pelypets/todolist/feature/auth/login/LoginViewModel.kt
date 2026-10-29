package com.pelypets.todolist.feature.auth.login

import androidx.lifecycle.viewModelScope
import com.pelypets.todolist.core.mvi.MviViewModel
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.usecase.LoginWithEmailUseCase
import com.pelypets.todolist.domain.usecase.LoginWithGoogleUseCase
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginWithEmail: LoginWithEmailUseCase,
    private val loginWithGoogle: LoginWithGoogleUseCase,
) : MviViewModel<LoginState, LoginIntent, LoginEffect>(LoginState()) {

    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> reduce(LoginMutation.EmailChanged(intent.value))
            is LoginIntent.PasswordChanged -> reduce(LoginMutation.PasswordChanged(intent.value))
            LoginIntent.SubmitClicked -> submit()
            LoginIntent.GoogleClicked -> startGoogleSignIn()
            is LoginIntent.GoogleTokenReceived -> completeGoogleSignIn(intent.idToken)
            LoginIntent.GoogleCancelled -> reduce(LoginMutation.GoogleCancelled)
            is LoginIntent.GoogleFailed -> reduce(LoginMutation.GoogleFailed)
            LoginIntent.RegisterClicked -> if (!currentState.isBusy) sendEffect(LoginEffect.NavigateToRegister)
        }
    }

    private fun submit() {
        if (currentState.isBusy) return
        reduce(LoginMutation.SubmitStarted)
        val state = currentState
        viewModelScope.launch {
            reduce(loginWithEmail(state.email, state.password).toMutation(viaGoogle = false))
        }
    }

    private fun startGoogleSignIn() {
        if (currentState.isBusy) return
        reduce(LoginMutation.GoogleStarted)
        sendEffect(LoginEffect.LaunchGoogleSignIn)
    }

    private fun completeGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            reduce(loginWithGoogle(idToken).toMutation(viaGoogle = true))
        }
    }

    private fun DomainResult<Unit>.toMutation(viaGoogle: Boolean): LoginMutation = when (this) {
        is DomainResult.Success -> LoginMutation.Succeeded
        is DomainResult.Failure -> LoginMutation.Failed(error, viaGoogle)
    }

    private fun reduce(mutation: LoginMutation) = updateState { LoginReducer.reduce(it, mutation) }
}
