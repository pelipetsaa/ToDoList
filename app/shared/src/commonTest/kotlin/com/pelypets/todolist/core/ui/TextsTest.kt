package com.pelypets.todolist.core.ui

import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import kotlin.test.Test
import kotlin.test.assertEquals

class TextsTest {

    @Test
    fun serverValidationMessageIsShownNextToTheLocalizedText() {
        val error = DomainError.Validation(serverMessage = "Title must be at most 200 characters")
        assertEquals(
            "Please check the entered data. (Title must be at most 200 characters)",
            error.withServerDetail("Please check the entered data."),
        )
    }

    @Test
    fun otherErrorsKeepTheLocalizedTextOnly() {
        val text = "Something went wrong."
        assertEquals(text, DomainError.Validation(errors = listOf(ValidationError.TITLE_BLANK)).withServerDetail(text))
        assertEquals(text, DomainError.Validation(serverMessage = " ").withServerDetail(text))
        assertEquals(text, DomainError.Unknown("HTTP 500").withServerDetail(text))
        assertEquals(text, DomainError.Network.withServerDetail(text))
    }
}
