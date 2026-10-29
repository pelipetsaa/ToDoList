package com.pelypets.todolist.feature.tododetail

import androidx.lifecycle.viewModelScope
import com.pelypets.todolist.core.mvi.MviViewModel
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.usecase.DefaultDueDateUseCase
import com.pelypets.todolist.domain.usecase.DeleteTodoUseCase
import com.pelypets.todolist.domain.usecase.GetTodoUseCase
import com.pelypets.todolist.domain.usecase.SaveTodoUseCase
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class TodoDetailViewModel(
    private val todoId: String?,
    private val getTodo: GetTodoUseCase,
    private val saveTodo: SaveTodoUseCase,
    private val deleteTodo: DeleteTodoUseCase,
    defaultDueDate: DefaultDueDateUseCase,
    private val timeZone: TimeZone,
) : MviViewModel<TodoDetailState, TodoDetailIntent, TodoDetailEffect>(
    initialState(todoId, defaultDueDate().toLocalDateTime(timeZone)),
) {

    init {
        if (todoId != null) load(todoId)
    }

    override fun onIntent(intent: TodoDetailIntent) {
        when (intent) {
            TodoDetailIntent.Retry -> todoId?.let(::load)
            is TodoDetailIntent.TitleChanged -> reduce(TodoDetailMutation.TitleChanged(intent.value))
            is TodoDetailIntent.DescriptionChanged -> reduce(TodoDetailMutation.DescriptionChanged(intent.value))
            is TodoDetailIntent.StatusSelected -> reduce(TodoDetailMutation.StatusSelected(intent.status))
            TodoDetailIntent.DateClicked -> showDialog(TodoDetailDialog.DATE_PICKER)
            is TodoDetailIntent.DateSelected -> reduce(TodoDetailMutation.DateSelected(intent.date))
            TodoDetailIntent.TimeClicked -> showDialog(TodoDetailDialog.TIME_PICKER)
            is TodoDetailIntent.TimeSelected -> reduce(TodoDetailMutation.TimeSelected(intent.time))
            TodoDetailIntent.DialogDismissed -> reduce(TodoDetailMutation.DialogShown(null))
            TodoDetailIntent.SaveClicked -> save()
            TodoDetailIntent.DeleteClicked -> if (!currentState.isNew) showDialog(TodoDetailDialog.DELETE_CONFIRMATION)
            TodoDetailIntent.DeleteConfirmed -> delete()
            TodoDetailIntent.BackClicked -> if (!currentState.isBusy) sendEffect(TodoDetailEffect.Close)
        }
    }

    private fun load(id: String) {
        reduce(TodoDetailMutation.LoadStarted)
        viewModelScope.launch {
            when (val result = getTodo(id)) {
                is DomainResult.Success -> with(result.value) {
                    reduce(TodoDetailMutation.Loaded(title, description, dueAt.toLocalDateTime(timeZone), status))
                }
                is DomainResult.Failure -> reduce(TodoDetailMutation.LoadFailed(result.error))
            }
        }
    }

    private fun showDialog(dialog: TodoDetailDialog) {
        if (currentState.canEdit) reduce(TodoDetailMutation.DialogShown(dialog))
    }

    private fun save() {
        val state = currentState
        if (!state.canEdit) return
        reduce(TodoDetailMutation.SaveStarted)
        val draft = TodoDraft(
            title = state.title,
            description = state.description,
            dueAt = LocalDateTime(state.date, state.time).toInstant(timeZone),
            status = state.status,
        )
        viewModelScope.launch {
            when (val result = saveTodo(state.todoId, draft)) {
                is DomainResult.Success -> sendEffect(TodoDetailEffect.Close)
                is DomainResult.Failure -> {
                    reduce(TodoDetailMutation.SaveFailed(result.error))
                    if (result.error.isReportable()) sendEffect(TodoDetailEffect.ShowError(result.error))
                }
            }
        }
    }

    private fun delete() {
        val id = currentState.todoId ?: return
        if (currentState.isBusy) return
        reduce(TodoDetailMutation.DeleteStarted)
        viewModelScope.launch {
            when (val result = deleteTodo(id)) {
                is DomainResult.Success -> sendEffect(TodoDetailEffect.Close)
                is DomainResult.Failure -> {
                    reduce(TodoDetailMutation.DeleteFailed)
                    if (result.error.isReportable()) sendEffect(TodoDetailEffect.ShowError(result.error))
                }
            }
        }
    }

    private fun reduce(mutation: TodoDetailMutation) = updateState { TodoDetailReducer.reduce(it, mutation) }

    private companion object {
        fun initialState(todoId: String?, defaultDueAt: LocalDateTime) = TodoDetailState(
            todoId = todoId,
            date = defaultDueAt.date,
            time = defaultDueAt.time,
            isLoading = todoId != null,
        )

        /** Field errors are shown inline and an expired session is handled by the auth gate. */
        fun DomainError.isReportable(): Boolean =
            !(this is DomainError.Validation && errors.isNotEmpty()) && this != DomainError.Unauthorized
    }
}
