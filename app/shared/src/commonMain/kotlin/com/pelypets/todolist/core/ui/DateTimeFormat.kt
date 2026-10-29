package com.pelypets.todolist.core.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char

private val dateFormat = LocalDate.Format {
    dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
    chars(", ")
    day()
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    year()
}

private val timeFormat = LocalTime.Format {
    hour()
    char(':')
    minute()
}

fun LocalDate.formatted(): String = format(dateFormat)

fun LocalTime.formatted(): String = format(timeFormat)
