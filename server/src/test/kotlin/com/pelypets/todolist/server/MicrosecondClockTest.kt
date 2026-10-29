package com.pelypets.todolist.server

import com.pelypets.todolist.server.infrastructure.db.MicrosecondClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class MicrosecondClockTest {

    private fun clockAt(instant: Instant) = MicrosecondClock(
        object : Clock {
            override fun now() = instant
        },
    )

    @Test
    fun `drops nanoseconds below a microsecond`() {
        val clock = clockAt(Instant.parse("2026-10-29T14:52:26.359440846Z"))

        assertEquals(Instant.parse("2026-10-29T14:52:26.359440Z"), clock.now())
    }

    @Test
    fun `keeps microsecond values unchanged`() {
        val instant = Instant.parse("2026-10-29T14:52:26.359441Z")

        assertEquals(instant, clockAt(instant).now())
    }
}
