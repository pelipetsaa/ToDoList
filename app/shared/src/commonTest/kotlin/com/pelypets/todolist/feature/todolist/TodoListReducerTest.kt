package com.pelypets.todolist.feature.todolist

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.testing.todo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TodoListReducerTest {

    private val loaded = TodoListState(
        period = Period.TODAY,
        todos = listOf(todo("a", "2026-10-29T10:00:00Z")),
        hasLoaded = true,
    )

    @Test
    fun resultsForAPreviouslySelectedPeriodAreIgnored() {
        val state = loaded.copy(period = Period.WEEK, hasLoaded = false, todos = emptyList())
        val result = TodoListReducer.reduce(state, TodoListMutation.Loaded(Period.TODAY, loaded.todos))
        assertSame(state, result)
    }

    @Test
    fun changingThePeriodResetsTheList() {
        val result = TodoListReducer.reduce(loaded, TodoListMutation.PeriodChanged(Period.MONTH))
        assertEquals(TodoListState(period = Period.MONTH), result)
    }

    @Test
    fun reloadOfAShownListDoesNotShowTheFullScreenSpinner() {
        val result = TodoListReducer.reduce(loaded, TodoListMutation.LoadStarted(pullToRefresh = false))
        assertFalse(result.isLoading)
        assertFalse(result.isRefreshing)
    }

    @Test
    fun pullToRefreshShowsTheRefreshIndicator() {
        val result = TodoListReducer.reduce(loaded, TodoListMutation.LoadStarted(pullToRefresh = true))
        assertTrue(result.isRefreshing)
    }

    @Test
    fun failureAfterContentWasShownKeepsTheContent() {
        val result = TodoListReducer.reduce(loaded, TodoListMutation.LoadFailed(Period.TODAY, DomainError.Network))
        assertEquals(loaded.todos, result.todos)
        assertEquals(null, result.error)
    }
}
