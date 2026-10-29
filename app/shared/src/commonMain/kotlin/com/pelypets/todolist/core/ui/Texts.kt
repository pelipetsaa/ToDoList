package com.pelypets.todolist.core.ui

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.TodoStatus
import org.jetbrains.compose.resources.StringResource
import todolist.app.shared.generated.resources.Res
import todolist.app.shared.generated.resources.error_conflict
import todolist.app.shared.generated.resources.error_network
import todolist.app.shared.generated.resources.error_not_found
import todolist.app.shared.generated.resources.error_too_many_requests
import todolist.app.shared.generated.resources.error_unauthorized
import todolist.app.shared.generated.resources.error_unknown
import todolist.app.shared.generated.resources.error_validation
import todolist.app.shared.generated.resources.period_month
import todolist.app.shared.generated.resources.period_today
import todolist.app.shared.generated.resources.period_week
import todolist.app.shared.generated.resources.status_done
import todolist.app.shared.generated.resources.status_in_progress
import todolist.app.shared.generated.resources.status_todo
import todolist.app.shared.generated.resources.validation_description_too_long
import todolist.app.shared.generated.resources.validation_email_blank
import todolist.app.shared.generated.resources.validation_email_invalid
import todolist.app.shared.generated.resources.validation_password_blank
import todolist.app.shared.generated.resources.validation_password_mismatch
import todolist.app.shared.generated.resources.validation_password_too_long
import todolist.app.shared.generated.resources.validation_password_too_short
import todolist.app.shared.generated.resources.validation_title_blank
import todolist.app.shared.generated.resources.validation_title_too_long

val DomainError.text: StringResource
    get() = when (this) {
        DomainError.Network -> Res.string.error_network
        DomainError.Unauthorized -> Res.string.error_unauthorized
        DomainError.NotFound -> Res.string.error_not_found
        DomainError.Conflict -> Res.string.error_conflict
        DomainError.TooManyRequests -> Res.string.error_too_many_requests
        is DomainError.Validation -> Res.string.error_validation
        is DomainError.Unknown -> Res.string.error_unknown
    }

val ValidationError.text: StringResource
    get() = when (this) {
        ValidationError.EMAIL_BLANK -> Res.string.validation_email_blank
        ValidationError.EMAIL_INVALID -> Res.string.validation_email_invalid
        ValidationError.PASSWORD_BLANK -> Res.string.validation_password_blank
        ValidationError.PASSWORD_TOO_SHORT -> Res.string.validation_password_too_short
        ValidationError.PASSWORD_TOO_LONG -> Res.string.validation_password_too_long
        ValidationError.PASSWORD_MISMATCH -> Res.string.validation_password_mismatch
        ValidationError.TITLE_BLANK -> Res.string.validation_title_blank
        ValidationError.TITLE_TOO_LONG -> Res.string.validation_title_too_long
        ValidationError.DESCRIPTION_TOO_LONG -> Res.string.validation_description_too_long
    }

val TodoStatus.text: StringResource
    get() = when (this) {
        TodoStatus.TODO -> Res.string.status_todo
        TodoStatus.IN_PROGRESS -> Res.string.status_in_progress
        TodoStatus.DONE -> Res.string.status_done
    }

val Period.text: StringResource
    get() = when (this) {
        Period.TODAY -> Res.string.period_today
        Period.WEEK -> Res.string.period_week
        Period.MONTH -> Res.string.period_month
    }
