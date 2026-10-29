package com.pelypets.todolist.feature.tododetail

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.TodoStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

data class TodoDetailState(
    val todoId: String?,
    val title: String = "",
    val description: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val status: TodoStatus = TodoStatus.TODO,
    val titleError: ValidationError? = null,
    val descriptionError: ValidationError? = null,
    val isLoading: Boolean = false,
    val loadError: DomainError? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val dialog: TodoDetailDialog? = null,
) {
    val isNew: Boolean get() = todoId == null
    val isBusy: Boolean get() = isLoading || isSaving || isDeleting
    val canEdit: Boolean get() = !isBusy && loadError == null
}

enum class TodoDetailDialog { DATE_PICKER, TIME_PICKER, DELETE_CONFIRMATION }

sealed interface TodoDetailIntent {
    data object Retry : TodoDetailIntent
    data class TitleChanged(val value: String) : TodoDetailIntent
    data class DescriptionChanged(val value: String) : TodoDetailIntent
    data class StatusSelected(val status: TodoStatus) : TodoDetailIntent
    data object DateClicked : TodoDetailIntent
    data class DateSelected(val date: LocalDate) : TodoDetailIntent
    data object TimeClicked : TodoDetailIntent
    data class TimeSelected(val time: LocalTime) : TodoDetailIntent
    data object DialogDismissed : TodoDetailIntent
    data object SaveClicked : TodoDetailIntent
    data object DeleteClicked : TodoDetailIntent
    data object DeleteConfirmed : TodoDetailIntent
    data object BackClicked : TodoDetailIntent
}

sealed interface TodoDetailEffect {
    data object Close : TodoDetailEffect
    data class ShowError(val error: DomainError) : TodoDetailEffect
}

sealed interface TodoDetailMutation {
    data object LoadStarted : TodoDetailMutation
    data class Loaded(
        val title: String,
        val description: String,
        val dueAt: LocalDateTime,
        val status: TodoStatus,
    ) : TodoDetailMutation
    data class LoadFailed(val error: DomainError) : TodoDetailMutation
    data class TitleChanged(val value: String) : TodoDetailMutation
    data class DescriptionChanged(val value: String) : TodoDetailMutation
    data class StatusSelected(val status: TodoStatus) : TodoDetailMutation
    data class DateSelected(val date: LocalDate) : TodoDetailMutation
    data class TimeSelected(val time: LocalTime) : TodoDetailMutation
    data class DialogShown(val dialog: TodoDetailDialog?) : TodoDetailMutation
    data object SaveStarted : TodoDetailMutation
    data class SaveFailed(val error: DomainError) : TodoDetailMutation
    data object DeleteStarted : TodoDetailMutation
    data object DeleteFailed : TodoDetailMutation
}
