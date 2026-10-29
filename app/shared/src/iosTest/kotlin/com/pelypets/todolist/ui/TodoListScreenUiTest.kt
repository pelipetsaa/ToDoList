package com.pelypets.todolist.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoStatus
import com.pelypets.todolist.feature.todolist.TodoListIntent
import com.pelypets.todolist.feature.todolist.TodoListScreen
import com.pelypets.todolist.feature.todolist.TodoListState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class TodoListScreenUiTest {

    private val intents = mutableListOf<TodoListIntent>()
    private val todos = listOf(
        Todo("t1", "Buy milk", "", Instant.parse("2026-10-29T10:00:00Z"), TodoStatus.IN_PROGRESS),
        Todo("t2", "Call mom", "", Instant.parse("2026-10-29T18:30:00Z"), TodoStatus.DONE),
    )

    @Test
    fun todosAreListedWithTheirStatusAndOpenOnTap() = runComposeUiTest {
        setContent { TodoListScreen(TodoListState(todos = todos, hasLoaded = true), SnackbarHostState(), intents::add) }

        onNodeWithText("Buy milk").assertIsDisplayed()
        onNodeWithText("In progress").assertIsDisplayed()
        onNodeWithText("Done").assertIsDisplayed()
        onNodeWithText("Call mom").performClick()

        assertEquals(listOf<TodoListIntent>(TodoListIntent.TodoClicked("t2")), intents)
    }

    @Test
    fun emptyPeriodShowsAHint() = runComposeUiTest {
        setContent {
            TodoListScreen(TodoListState(period = Period.WEEK, hasLoaded = true), SnackbarHostState(), intents::add)
        }

        onNodeWithText("Nothing planned for this week").assertIsDisplayed()
        onNodeWithText("Tap + to add a todo").assertIsDisplayed()
    }

    @Test
    fun errorOffersRetry() = runComposeUiTest {
        setContent { TodoListScreen(TodoListState(error = DomainError.Network), SnackbarHostState(), intents::add) }

        onNodeWithText("No connection to the server. Check your network and try again.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()

        assertEquals(listOf<TodoListIntent>(TodoListIntent.Retry), intents)
    }

    @Test
    fun tabsAddAndLogoutSendIntents() = runComposeUiTest {
        setContent { TodoListScreen(TodoListState(hasLoaded = true), SnackbarHostState(), intents::add) }

        onNodeWithText("This month").performClick()
        onNodeWithContentDescription("Add todo").performClick()
        onNodeWithContentDescription("Log out").performClick()

        assertEquals(
            listOf(TodoListIntent.PeriodSelected(Period.MONTH), TodoListIntent.AddClicked, TodoListIntent.LogoutClicked),
            intents,
        )
    }
}
