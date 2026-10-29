package com.pelypets.todolist.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.stringArrayResource
import todolist.app.shared.generated.resources.Res
import todolist.app.shared.generated.resources.day_names_short
import todolist.app.shared.generated.resources.month_names_short

private val timeFormat = LocalTime.Format {
    hour()
    char(':')
    minute()
}

/** "Thu, 29 Oct 2026" / "Чт, 29 окт. 2026": day and month names come from the current locale's resources. */
@Composable
fun LocalDate.formatted(): String {
    val days = stringArrayResource(Res.array.day_names_short)
    val months = stringArrayResource(Res.array.month_names_short)
    val dateFormat = remember(days, months) {
        LocalDate.Format {
            dayOfWeek(DayOfWeekNames(days))
            chars(", ")
            day()
            char(' ')
            monthName(MonthNames(months))
            char(' ')
            year()
        }
    }
    return format(dateFormat)
}

fun LocalTime.formatted(): String = format(timeFormat)
