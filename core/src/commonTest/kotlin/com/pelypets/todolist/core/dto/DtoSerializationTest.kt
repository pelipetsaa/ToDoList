package com.pelypets.todolist.core.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class DtoSerializationTest {

    private val json = Json

    @Test
    fun todoDtoRoundTripsWithIsoInstants() {
        val dto = TodoDto(
            id = "1",
            title = "Buy milk",
            description = "",
            dueAt = Instant.parse("2026-10-29T13:00:00Z"),
            status = TodoStatusDto.IN_PROGRESS,
            createdAt = Instant.parse("2026-10-28T10:00:00Z"),
            updatedAt = Instant.parse("2026-10-28T10:00:00Z"),
        )

        val encoded = json.encodeToString(TodoDto.serializer(), dto)

        assertTrue(encoded.contains("\"dueAt\":\"2026-10-29T13:00:00Z\""), encoded)
        assertTrue(encoded.contains("\"status\":\"IN_PROGRESS\""), encoded)
        assertEquals(dto, json.decodeFromString(TodoDto.serializer(), encoded))
    }
}
