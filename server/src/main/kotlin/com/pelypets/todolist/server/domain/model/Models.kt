package com.pelypets.todolist.server.domain.model

import java.util.UUID
import kotlin.time.Instant

data class User(val id: UUID, val email: String, val passwordHash: String?, val googleSubject: String?, val createdAt: Instant)

enum class TodoStatus { TODO, IN_PROGRESS, DONE }

data class Todo(
    val id: UUID,
    val userId: UUID,
    val title: String,
    val description: String,
    val dueAt: Instant,
    val status: TodoStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TodoDraft(val title: String, val description: String, val dueAt: Instant, val status: TodoStatus)

data class TokenPair(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long)

data class RefreshTokenRecord(val id: UUID, val userId: UUID, val tokenHash: String, val expiresAt: Instant)

data class GoogleIdentity(val subject: String, val email: String)
