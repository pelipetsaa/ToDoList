package com.pelypets.todolist.feature.todolist

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.Todo

data class TodoListState(
    val period: Period = Period.TODAY,
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    val error: DomainError? = null,
)

sealed interface TodoListIntent {
    /** Sent every time the screen becomes visible, e.g. after returning from the detail screen. */
    data object ScreenShown : TodoListIntent
    data object Refresh : TodoListIntent
    data object Retry : TodoListIntent
    data class PeriodSelected(val period: Period) : TodoListIntent
    data class TodoClicked(val id: String) : TodoListIntent
    data object AddClicked : TodoListIntent
    data object LogoutClicked : TodoListIntent
}

sealed interface TodoListEffect {
    data class OpenTodo(val id: String?) : TodoListEffect
    data class ShowError(val error: DomainError) : TodoListEffect
}

sealed interface TodoListMutation {
    data class PeriodChanged(val period: Period) : TodoListMutation
    data class LoadStarted(val pullToRefresh: Boolean) : TodoListMutation
    data class Loaded(val period: Period, val todos: List<Todo>) : TodoListMutation
    data class LoadFailed(val period: Period, val error: DomainError) : TodoListMutation
}
