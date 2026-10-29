package com.pelypets.todolist.domain.validation

import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ValidatorsTest {

    @Test
    fun emailRules() {
        assertEquals(ValidationError.EMAIL_BLANK, CredentialsValidator.validateEmail("  "))
        assertEquals(ValidationError.EMAIL_INVALID, CredentialsValidator.validateEmail("no-at-sign"))
        assertEquals(ValidationError.EMAIL_INVALID, CredentialsValidator.validateEmail("a@b"))
        assertNull(CredentialsValidator.validateEmail(" user@example.com "))
    }

    @Test
    fun registrationRules() {
        assertEquals(
            listOf(ValidationError.PASSWORD_TOO_SHORT, ValidationError.PASSWORD_MISMATCH),
            CredentialsValidator.validateRegistration("user@example.com", "short", "other"),
        )
        assertTrue(CredentialsValidator.validateRegistration("user@example.com", "password123", "password123").isEmpty())
    }

    @Test
    fun loginOnlyRequiresANonEmptyPassword() {
        assertTrue(CredentialsValidator.validateLogin("user@example.com", "x").isEmpty())
        assertEquals(listOf(ValidationError.PASSWORD_BLANK), CredentialsValidator.validateLogin("user@example.com", ""))
    }

    @Test
    fun todoRules() {
        fun draft(title: String, description: String = "") =
            TodoDraft(title, description, Instant.parse("2026-10-29T10:00:00Z"), TodoStatus.TODO)

        assertEquals(listOf(ValidationError.TITLE_BLANK), TodoValidator.validate(draft("   ")))
        assertEquals(listOf(ValidationError.TITLE_TOO_LONG), TodoValidator.validate(draft("x".repeat(201))))
        assertEquals(
            listOf(ValidationError.DESCRIPTION_TOO_LONG),
            TodoValidator.validate(draft("ok", "d".repeat(2001))),
        )
        assertTrue(TodoValidator.validate(draft("Buy milk")).isEmpty())
    }
}
