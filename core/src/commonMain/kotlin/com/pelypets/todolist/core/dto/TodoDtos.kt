package com.pelypets.todolist.core.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
enum class TodoStatusDto { TODO, IN_PROGRESS, DONE }

@Serializable
data class TodoInputDto(
    val title: String,
    val description: String = "",
    val dueAt: Instant,
    val status: TodoStatusDto = TodoStatusDto.TODO,
)

@Serializable
data class TodoDto(
    val id: String,
    val title: String,
    val description: String,
    val dueAt: Instant,
    val status: TodoStatusDto,
    val createdAt: Instant,
    val updatedAt: Instant,
)
