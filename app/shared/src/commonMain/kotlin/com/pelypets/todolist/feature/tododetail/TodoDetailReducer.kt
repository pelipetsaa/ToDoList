package com.pelypets.todolist.feature.tododetail

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError

object TodoDetailReducer {

    private val titleErrors = setOf(ValidationError.TITLE_BLANK, ValidationError.TITLE_TOO_LONG)

    fun reduce(state: TodoDetailState, mutation: TodoDetailMutation): TodoDetailState = when (mutation) {
        TodoDetailMutation.LoadStarted -> state.copy(isLoading = true, loadError = null)

        is TodoDetailMutation.Loaded -> state.copy(
            title = mutation.title,
            description = mutation.description,
            date = mutation.dueAt.date,
            time = mutation.dueAt.time,
            status = mutation.status,
            isLoading = false,
            loadError = null,
        )

        is TodoDetailMutation.LoadFailed -> state.copy(isLoading = false, loadError = mutation.error)

        is TodoDetailMutation.TitleChanged -> state.copy(title = mutation.value, titleError = null)

        is TodoDetailMutation.DescriptionChanged -> state.copy(description = mutation.value, descriptionError = null)

        is TodoDetailMutation.StatusSelected -> state.copy(status = mutation.status)

        is TodoDetailMutation.DateSelected -> state.copy(date = mutation.date, dialog = null)

        is TodoDetailMutation.TimeSelected -> state.copy(time = mutation.time, dialog = null)

        is TodoDetailMutation.DialogShown -> state.copy(dialog = mutation.dialog)

        TodoDetailMutation.SaveStarted -> state.copy(isSaving = true)

        is TodoDetailMutation.SaveFailed -> {
            val errors = (mutation.error as? DomainError.Validation)?.errors.orEmpty()
            state.copy(
                isSaving = false,
                titleError = errors.firstOrNull { it in titleErrors },
                descriptionError = errors.firstOrNull { it == ValidationError.DESCRIPTION_TOO_LONG },
            )
        }

        TodoDetailMutation.DeleteStarted -> state.copy(isDeleting = true, dialog = null)

        TodoDetailMutation.DeleteFailed -> state.copy(isDeleting = false)
    }
}
