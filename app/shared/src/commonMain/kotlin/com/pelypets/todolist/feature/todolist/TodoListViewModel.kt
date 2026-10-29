package com.pelypets.todolist.feature.todolist

import androidx.lifecycle.viewModelScope
import com.pelypets.todolist.core.mvi.MviViewModel
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.usecase.GetTodosForPeriodUseCase
import com.pelypets.todolist.domain.usecase.LogoutUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TodoListViewModel(private val getTodosForPeriod: GetTodosForPeriodUseCase, private val logout: LogoutUseCase) :
    MviViewModel<TodoListState, TodoListIntent, TodoListEffect>(TodoListState()) {

    private var loadJob: Job? = null

    override fun onIntent(intent: TodoListIntent) {
        when (intent) {
            TodoListIntent.ScreenShown, TodoListIntent.Retry -> load(pullToRefresh = false)

            TodoListIntent.Refresh -> load(pullToRefresh = true)

            is TodoListIntent.PeriodSelected -> {
                if (intent.period == currentState.period) return
                reduce(TodoListMutation.PeriodChanged(intent.period))
                load(pullToRefresh = false)
            }

            is TodoListIntent.TodoClicked -> sendEffect(TodoListEffect.OpenTodo(intent.id))

            TodoListIntent.AddClicked -> sendEffect(TodoListEffect.OpenTodo(null))

            TodoListIntent.LogoutClicked -> viewModelScope.launch { logout() }
        }
    }

    private fun load(pullToRefresh: Boolean) {
        val period = currentState.period
        loadJob?.cancel()
        reduce(TodoListMutation.LoadStarted(pullToRefresh))
        loadJob = viewModelScope.launch {
            when (val result = getTodosForPeriod(period)) {
                is DomainResult.Success -> reduce(TodoListMutation.Loaded(period, result.value))

                is DomainResult.Failure -> {
                    val hadContent = currentState.hasLoaded
                    reduce(TodoListMutation.LoadFailed(period, result.error))
                    // An expired session is handled by the auth gate, which navigates to login.
                    if (hadContent && result.error != DomainError.Unauthorized) {
                        sendEffect(TodoListEffect.ShowError(result.error))
                    }
                }
            }
        }
    }

    private fun reduce(mutation: TodoListMutation) = updateState { TodoListReducer.reduce(it, mutation) }
}
