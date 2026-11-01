package com.pelypets.todolist.feature.auth.login

import com.pelypets.todolist.auth.GoogleSignInProvider
import com.pelypets.todolist.auth.GoogleSignInResult
import com.pelypets.todolist.domain.usecase.LoginWithEmailUseCase
import com.pelypets.todolist.domain.usecase.LoginWithGoogleUseCase
import com.pelypets.todolist.testing.FakeAuthRepository
import com.pelypets.todolist.testing.ViewModelTest
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoogleSignInReportTest : ViewModelTest() {

    private val neverAnswers = GoogleSignInProvider { awaitCancellation() }

    @Test
    fun pickerResultsBecomeIntents() = runTest {
        val cases = mapOf(
            GoogleSignInResult.Success("id-token") to LoginIntent.GoogleTokenReceived("id-token"),
            GoogleSignInResult.Cancelled to LoginIntent.GoogleCancelled,
            GoogleSignInResult.Failure("no accounts") to LoginIntent.GoogleFailed("no accounts"),
        )
        for ((result, expected) in cases) {
            val intents = mutableListOf<LoginIntent>()
            GoogleSignInProvider { result }.signInAndReport { intents += it }
            assertEquals(listOf(expected), intents)
        }
    }

    @Test
    fun pickerCancelledTogetherWithTheScreenUnblocksTheLoginScreen() = runTest {
        val repository = FakeAuthRepository()
        val vm = LoginViewModel(LoginWithEmailUseCase(repository), LoginWithGoogleUseCase(repository))
        vm.onIntent(LoginIntent.GoogleClicked)
        assertTrue(vm.state.value.isBusy)

        val screen = launch { neverAnswers.signInAndReport(vm::onIntent) }
        runCurrent()
        assertTrue(vm.state.value.isGoogleInProgress)

        screen.cancel()
        runCurrent()

        assertFalse(vm.state.value.isBusy)
        assertTrue(repository.googleTokens.isEmpty())
    }
}
