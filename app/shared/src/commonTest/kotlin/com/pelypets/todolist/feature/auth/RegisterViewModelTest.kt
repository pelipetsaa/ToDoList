package com.pelypets.todolist.feature.auth

import app.cash.turbine.test
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.usecase.RegisterUseCase
import com.pelypets.todolist.feature.auth.register.RegisterEffect
import com.pelypets.todolist.feature.auth.register.RegisterError
import com.pelypets.todolist.feature.auth.register.RegisterIntent
import com.pelypets.todolist.feature.auth.register.RegisterViewModel
import com.pelypets.todolist.testing.FakeAuthRepository
import com.pelypets.todolist.testing.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterViewModelTest : ViewModelTest() {

    private val repository = FakeAuthRepository()
    private val vm = RegisterViewModel(RegisterUseCase(repository))

    private fun fill(email: String, password: String, confirm: String) {
        vm.onIntent(RegisterIntent.EmailChanged(email))
        vm.onIntent(RegisterIntent.PasswordChanged(password))
        vm.onIntent(RegisterIntent.ConfirmPasswordChanged(confirm))
    }

    @Test
    fun mismatchedAndShortPasswordsAreReportedPerField() {
        fill("user@example.com", "short", "different")
        vm.onIntent(RegisterIntent.SubmitClicked)

        val state = vm.state.value
        assertNull(state.emailError)
        assertEquals(ValidationError.PASSWORD_TOO_SHORT, state.passwordError)
        assertEquals(ValidationError.PASSWORD_MISMATCH, state.confirmPasswordError)
        assertTrue(repository.registerCalls.isEmpty())
    }

    @Test
    fun validInputRegisters() {
        fill("user@example.com", "password123", "password123")
        vm.onIntent(RegisterIntent.SubmitClicked)

        assertEquals(listOf("user@example.com" to "password123"), repository.registerCalls)
        assertFalse(vm.state.value.isSubmitting)
        assertNull(vm.state.value.error)
    }

    @Test
    fun takenEmailIsReported() {
        repository.result = DomainResult.Failure(DomainError.Conflict)
        fill("user@example.com", "password123", "password123")
        vm.onIntent(RegisterIntent.SubmitClicked)

        assertEquals(RegisterError.EmailTaken, vm.state.value.error)
    }

    @Test
    fun backNavigatesUnlessSubmitting() = runTest {
        vm.effects.test {
            vm.onIntent(RegisterIntent.BackClicked)
            assertEquals(RegisterEffect.NavigateBack, awaitItem())

            repository.gate.close()
            fill("user@example.com", "password123", "password123")
            vm.onIntent(RegisterIntent.SubmitClicked)
            vm.onIntent(RegisterIntent.BackClicked)
            expectNoEvents()
            repository.gate.open(DomainResult.Success(Unit))
        }
    }
}
