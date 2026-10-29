package com.pelypets.todolist.data.mapper

import com.pelypets.todolist.core.dto.TodoDto
import com.pelypets.todolist.core.dto.TodoInputDto
import com.pelypets.todolist.core.dto.TodoStatusDto
import com.pelypets.todolist.core.dto.TokenPairDto
import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus

fun TokenPairDto.toStoredTokens() = StoredTokens(accessToken = accessToken, refreshToken = refreshToken)

fun TodoDto.toDomain() = Todo(
    id = id,
    title = title,
    description = description,
    dueAt = dueAt,
    status = status.toDomain(),
)

fun TodoDraft.toDto() = TodoInputDto(
    title = title,
    description = description,
    dueAt = dueAt,
    status = status.toDto(),
)

fun TodoStatusDto.toDomain() = when (this) {
    TodoStatusDto.TODO -> TodoStatus.TODO
    TodoStatusDto.IN_PROGRESS -> TodoStatus.IN_PROGRESS
    TodoStatusDto.DONE -> TodoStatus.DONE
}

fun TodoStatus.toDto() = when (this) {
    TodoStatus.TODO -> TodoStatusDto.TODO
    TodoStatus.IN_PROGRESS -> TodoStatusDto.IN_PROGRESS
    TodoStatus.DONE -> TodoStatusDto.DONE
}
