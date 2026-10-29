package com.pelypets.todolist.domain.period

import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.TimeRange
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class PeriodRangeCalculatorTest {

    private val utc = PeriodRangeCalculator(TimeZone.UTC)

    private fun range(from: String, to: String) = TimeRange(Instant.parse(from), Instant.parse(to))

    @Test
    fun todayIsLocalMidnightToNextMidnight() {
        val result = utc.rangeFor(Period.TODAY, Instant.parse("2026-10-29T13:45:00Z"))
        assertEquals(range("2026-10-29T00:00:00Z", "2026-10-30T00:00:00Z"), result)
    }

    @Test
    fun weekStartsOnMonday() {
        // 2026-10-29 is a Thursday.
        val result = utc.rangeFor(Period.WEEK, Instant.parse("2026-10-29T13:45:00Z"))
        assertEquals(range("2026-10-26T00:00:00Z", "2026-11-02T00:00:00Z"), result)
    }

    @Test
    fun weekOnSundayBelongsToThePreviousMonday() {
        val result = utc.rangeFor(Period.WEEK, Instant.parse("2026-11-01T23:59:59Z"))
        assertEquals(range("2026-10-26T00:00:00Z", "2026-11-02T00:00:00Z"), result)
    }

    @Test
    fun weekCanSpanTheYearBoundary() {
        // 2027-01-01 is a Friday.
        val result = utc.rangeFor(Period.WEEK, Instant.parse("2027-01-01T08:00:00Z"))
        assertEquals(range("2026-12-28T00:00:00Z", "2027-01-04T00:00:00Z"), result)
    }

    @Test
    fun monthCoversTheWholeCalendarMonth() {
        val result = utc.rangeFor(Period.MONTH, Instant.parse("2026-02-15T10:00:00Z"))
        assertEquals(range("2026-02-01T00:00:00Z", "2026-03-01T00:00:00Z"), result)
    }

    @Test
    fun decemberRollsOverIntoTheNextYear() {
        val result = utc.rangeFor(Period.MONTH, Instant.parse("2026-12-31T23:00:00Z"))
        assertEquals(range("2026-12-01T00:00:00Z", "2027-01-01T00:00:00Z"), result)
    }

    @Test
    fun boundariesUseTheDeviceTimeZone() {
        val warsaw = PeriodRangeCalculator(TimeZone.of("Europe/Warsaw"))
        // 23:30 UTC on Oct 29 is already Oct 30 in Warsaw (UTC+1 after the DST switch).
        val result = warsaw.rangeFor(Period.TODAY, Instant.parse("2026-10-29T23:30:00Z"))
        assertEquals(range("2026-10-29T23:00:00Z", "2026-10-30T23:00:00Z"), result)
    }

    @Test
    fun dstChangeDayIsTwentyFiveHoursLong() {
        val warsaw = PeriodRangeCalculator(TimeZone.of("Europe/Warsaw"))
        // Clocks go back on 2026-10-25 in Europe.
        val result = warsaw.rangeFor(Period.TODAY, Instant.parse("2026-10-25T12:00:00Z"))
        assertEquals(range("2026-10-24T22:00:00Z", "2026-10-25T23:00:00Z"), result)
    }

    @Test
    fun rangeIsHalfOpen() {
        val result = utc.rangeFor(Period.TODAY, Instant.parse("2026-10-29T13:45:00Z"))
        assertTrue(Instant.parse("2026-10-29T00:00:00Z") in result)
        assertFalse(Instant.parse("2026-10-30T00:00:00Z") in result)
    }
}
