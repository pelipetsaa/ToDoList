package com.pelypets.todolist.server.domain

object Validation {
    const val EMAIL_MAX_LENGTH = 254
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_MAX_LENGTH = 128
    const val TITLE_MAX_LENGTH = 200
    const val DESCRIPTION_MAX_LENGTH = 2000

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Email must not be blank"
        email.length > EMAIL_MAX_LENGTH -> "Email must be at most $EMAIL_MAX_LENGTH characters"
        !emailRegex.matches(email) -> "Email has an invalid format"
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.length < PASSWORD_MIN_LENGTH -> "Password must be at least $PASSWORD_MIN_LENGTH characters"
        password.length > PASSWORD_MAX_LENGTH -> "Password must be at most $PASSWORD_MAX_LENGTH characters"
        else -> null
    }

    fun todoError(title: String, description: String): String? {
        val trimmed = title.trim()
        return when {
            trimmed.isEmpty() -> "Title must not be blank"
            trimmed.length > TITLE_MAX_LENGTH -> "Title must be at most $TITLE_MAX_LENGTH characters"
            description.length > DESCRIPTION_MAX_LENGTH -> "Description must be at most $DESCRIPTION_MAX_LENGTH characters"
            else -> null
        }
    }

    fun require(error: String?) {
        if (error != null) throw ValidationException(error)
    }
}
