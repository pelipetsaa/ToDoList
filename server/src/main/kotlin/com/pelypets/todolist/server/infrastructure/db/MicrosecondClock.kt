package com.pelypets.todolist.server.infrastructure.db

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * PostgreSQL stores timestamps with microsecond precision, while the JVM clock on Linux has nanoseconds.
 * Truncating at the source keeps the value returned to clients identical to the value read back later.
 */
class MicrosecondClock(private val source: Clock = Clock.System) : Clock {
    override fun now(): Instant {
        val now = source.now()
        return Instant.fromEpochSeconds(now.epochSeconds, now.nanosecondsOfSecond / 1_000 * 1_000)
    }
}
