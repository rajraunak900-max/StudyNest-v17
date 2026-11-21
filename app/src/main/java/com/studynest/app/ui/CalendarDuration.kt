package com.studynest.app.ui

import java.util.Locale

/** Overview formatting only; exact millisecond values remain untouched. */
internal fun calendarDuration(milliseconds: Long, locale: Locale = Locale.getDefault()): String {
    require(milliseconds >= 0)
    return when {
        milliseconds >= 3_600_000L -> String.format(locale,"%.1fh",milliseconds / 3_600_000.0)
        milliseconds in 1 until 60_000L -> "<1m"
        else -> "${milliseconds / 60_000}m"
    }
}
