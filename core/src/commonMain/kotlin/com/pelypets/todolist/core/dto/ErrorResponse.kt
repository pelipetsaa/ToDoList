package com.pelypets.todolist.core.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)

object ErrorCodes {
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val NOT_FOUND = "NOT_FOUND"
    const val EMAIL_TAKEN = "EMAIL_TAKEN"
    const val TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
}

@Serializable
data class HealthDto(
    val status: String,
)
