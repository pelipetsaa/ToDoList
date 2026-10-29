package com.pelypets.todolist.feature.todolist

object TodoListReducer {

    fun reduce(state: TodoListState, mutation: TodoListMutation): TodoListState = when (mutation) {
        is TodoListMutation.PeriodChanged ->
            if (mutation.period == state.period) state
            else TodoListState(period = mutation.period)

        // Reloads of an already shown list stay silent instead of flashing a full-screen spinner.
        is TodoListMutation.LoadStarted -> state.copy(
            isRefreshing = mutation.pullToRefresh,
            isLoading = !mutation.pullToRefresh && !state.hasLoaded,
            error = null,
        )

        is TodoListMutation.Loaded ->
            if (mutation.period != state.period) state
            else state.copy(
                todos = mutation.todos.sortedBy { it.dueAt },
                isLoading = false,
                isRefreshing = false,
                hasLoaded = true,
                error = null,
            )

        is TodoListMutation.LoadFailed ->
            if (mutation.period != state.period) state
            else state.copy(
                isLoading = false,
                isRefreshing = false,
                error = mutation.error.takeIf { !state.hasLoaded },
            )
    }
}
