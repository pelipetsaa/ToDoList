package com.pelypets.todolist.server.api

import com.pelypets.todolist.core.dto.TodoDto
import com.pelypets.todolist.core.dto.TodoInputDto
import com.pelypets.todolist.core.dto.TodoStatusDto
import com.pelypets.todolist.core.dto.TokenPairDto
import com.pelypets.todolist.core.dto.UserDto
import com.pelypets.todolist.server.domain.model.Todo
import com.pelypets.todolist.server.domain.model.TodoDraft
import com.pelypets.todolist.server.domain.model.TodoStatus
import com.pelypets.todolist.server.domain.model.TokenPair
import com.pelypets.todolist.server.domain.model.User

fun TokenPair.toDto() = TokenPairDto(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresInSeconds = expiresInSeconds,
)

fun User.toDto() = UserDto(id = id.toString(), email = email)

fun Todo.toDto() = TodoDto(
    id = id.toString(),
    title = title,
    description = description,
    dueAt = dueAt,
    status = status.toDto(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TodoInputDto.toDraft() = TodoDraft(
    title = title,
    description = description,
    dueAt = dueAt,
    status = status.toDomain(),
)

private fun TodoStatus.toDto() = when (this) {
    TodoStatus.TODO -> TodoStatusDto.TODO
    TodoStatus.IN_PROGRESS -> TodoStatusDto.IN_PROGRESS
    TodoStatus.DONE -> TodoStatusDto.DONE
}

private fun TodoStatusDto.toDomain() = when (this) {
    TodoStatusDto.TODO -> TodoStatus.TODO
    TodoStatusDto.IN_PROGRESS -> TodoStatus.IN_PROGRESS
    TodoStatusDto.DONE -> TodoStatus.DONE
}
