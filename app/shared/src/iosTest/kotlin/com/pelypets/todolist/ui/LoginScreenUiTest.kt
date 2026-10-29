package com.pelypets.todolist.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.feature.auth.login.LoginError
import com.pelypets.todolist.feature.auth.login.LoginIntent
import com.pelypets.todolist.feature.auth.login.LoginScreen
import com.pelypets.todolist.feature.auth.login.LoginState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LoginScreenUiTest {

    private val intents = mutableListOf<LoginIntent>()

    @Test
    fun typingAndSubmittingSendIntents() = runComposeUiTest {
        setContent { LoginScreen(LoginState(), intents::add) }

        onNode(hasSetTextAction() and hasText("Email")).performTextInput("user@example.com")
        onAllNodesWithText("Sign in").filterToOne(hasClickAction()).performClick()
        onNodeWithText("No account yet? Create one").performClick()

        assertEquals(
            listOf(LoginIntent.EmailChanged("user@example.com"), LoginIntent.SubmitClicked, LoginIntent.RegisterClicked),
            intents,
        )
    }

    @Test
    fun fieldAndFormErrorsAreShown() = runComposeUiTest {
        setContent {
            LoginScreen(
                LoginState(emailError = ValidationError.EMAIL_INVALID, error = LoginError.InvalidCredentials),
                intents::add,
            )
        }

        onNodeWithText("Enter a valid email").assertIsDisplayed()
        onNodeWithText("Invalid email or password.").assertIsDisplayed()
    }

    @Test
    fun buttonsAreDisabledWhileSigningIn() = runComposeUiTest {
        setContent { LoginScreen(LoginState(isGoogleInProgress = true), intents::add) }

        onAllNodesWithText("Sign in").filterToOne(hasClickAction()).assertIsNotEnabled()
        onNodeWithText("No account yet? Create one").assertIsNotEnabled()
        onAllNodesWithText("Sign in").filterToOne(hasClickAction()).performClick()
        assertTrue(intents.isEmpty())
    }
}
