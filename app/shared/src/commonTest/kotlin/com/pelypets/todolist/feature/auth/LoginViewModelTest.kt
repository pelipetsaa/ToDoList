package com.pelypets.todolist.feature.auth

import app.cash.turbine.test
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.usecase.LoginWithEmailUseCase
import com.pelypets.todolist.domain.usecase.LoginWithGoogleUseCase
import com.pelypets.todolist.feature.auth.login.LoginEffect
import com.pelypets.todolist.feature.auth.login.LoginError
import com.pelypets.todolist.feature.auth.login.LoginIntent
import com.pelypets.todolist.feature.auth.login.LoginViewModel
import com.pelypets.todolist.testing.FakeAuthRepository
import com.pelypets.todolist.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest : ViewModelTest() {

    private val repository = FakeAuthRepository()
    private fun viewModel() = LoginViewModel(LoginWithEmailUseCase(repository), LoginWithGoogleUseCase(repository))

    @Test
    fun invalidInputShowsFieldErrorsWithoutCallingTheBackend() {
        val vm = viewModel()
        vm.onIntent(LoginIntent.EmailChanged("not-an-email"))
        vm.onIntent(LoginIntent.SubmitClicked)

        val state = vm.state.value
        assertEquals(ValidationError.EMAIL_INVALID, state.emailError)
        assertEquals(ValidationError.PASSWORD_BLANK, state.passwordError)
        assertFalse(state.isSubmitting)
        assertTrue(repository.loginCalls.isEmpty())
    }

    @Test
    fun editingAFieldClearsItsError() {
        val vm = viewModel()
        vm.onIntent(LoginIntent.SubmitClicked)
        vm.onIntent(LoginIntent.EmailChanged("a"))

        assertNull(vm.state.value.emailError)
        assertEquals(ValidationError.PASSWORD_BLANK, vm.state.value.passwordError)
    }

    @Test
    fun submitShowsProgressAndSendsTrimmedEmail() {
        val vm = viewModel()
        repository.gate.close()
        vm.onIntent(LoginIntent.EmailChanged(" user@example.com "))
        vm.onIntent(LoginIntent.PasswordChanged("password123"))
        vm.onIntent(LoginIntent.SubmitClicked)

        assertTrue(vm.state.value.isSubmitting)
        assertEquals(listOf("user@example.com" to "password123"), repository.loginCalls)

        vm.onIntent(LoginIntent.SubmitClicked)
        assertEquals(1, repository.loginCalls.size, "a second tap while busy is ignored")

        repository.gate.open(DomainResult.Success(Unit))
        assertFalse(vm.state.value.isSubmitting)
        assertNull(vm.state.value.error)
    }

    @Test
    fun rejectedCredentialsShowInvalidCredentialsError() {
        repository.result = DomainResult.Failure(DomainError.Unauthorized)
        val vm = viewModel()
        vm.onIntent(LoginIntent.EmailChanged("user@example.com"))
        vm.onIntent(LoginIntent.PasswordChanged("wrong-password"))
        vm.onIntent(LoginIntent.SubmitClicked)

        assertEquals(LoginError.InvalidCredentials, vm.state.value.error)
    }

    @Test
    fun networkFailureIsShownAsIs() {
        repository.result = DomainResult.Failure(DomainError.Network)
        val vm = viewModel()
        vm.onIntent(LoginIntent.EmailChanged("user@example.com"))
        vm.onIntent(LoginIntent.PasswordChanged("password123"))
        vm.onIntent(LoginIntent.SubmitClicked)

        assertEquals(LoginError.Other(DomainError.Network), vm.state.value.error)
    }

    @Test
    fun googleSignInLaunchesThePickerAndSendsTheTokenToTheBackend() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onIntent(LoginIntent.GoogleClicked)
            assertEquals(LoginEffect.LaunchGoogleSignIn, awaitItem())
            assertTrue(vm.state.value.isGoogleInProgress)

            vm.onIntent(LoginIntent.GoogleTokenReceived("id-token"))
            assertEquals(listOf("id-token"), repository.googleTokens)
            assertFalse(vm.state.value.isGoogleInProgress)
        }
    }

    @Test
    fun cancelledGooglePickerResetsProgressWithoutError() {
        val vm = viewModel()
        vm.onIntent(LoginIntent.GoogleClicked)
        vm.onIntent(LoginIntent.GoogleCancelled)

        assertFalse(vm.state.value.isGoogleInProgress)
        assertNull(vm.state.value.error)
    }

    @Test
    fun tokenRejectedByTheBackendShowsGoogleError() {
        repository.result = DomainResult.Failure(DomainError.Unauthorized)
        val vm = viewModel()
        vm.onIntent(LoginIntent.GoogleClicked)
        vm.onIntent(LoginIntent.GoogleTokenReceived("bad-token"))

        assertEquals(LoginError.GoogleFailed, vm.state.value.error)
    }

    @Test
    fun registerLinkNavigates() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onIntent(LoginIntent.RegisterClicked)
            assertEquals(LoginEffect.NavigateToRegister, awaitItem())
        }
    }
}
