package com.pelypets.todolist.domain.period

import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.TimeRange
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Weeks start on Monday. Boundaries are local midnights, so DST changes are handled by the time zone. */
class PeriodRangeCalculator(private val timeZone: TimeZone) {

    fun rangeFor(period: Period, now: Instant): TimeRange {
        val today = now.toLocalDateTime(timeZone).date
        val (start, endExclusive) = when (period) {
            Period.TODAY -> today to today.plus(1, DateTimeUnit.DAY)
            Period.WEEK -> {
                val monday = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
                monday to monday.plus(1, DateTimeUnit.WEEK)
            }
            Period.MONTH -> {
                val first = LocalDate(today.year, today.month, 1)
                first to first.plus(1, DateTimeUnit.MONTH)
            }
        }
        return TimeRange(
            from = start.atStartOfDayIn(timeZone),
            to = endExclusive.atStartOfDayIn(timeZone),
        )
    }
}
