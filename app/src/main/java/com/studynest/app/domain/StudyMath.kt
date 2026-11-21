package com.studynest.app.domain

import java.time.*

data class TimeRange(val start: Long, val end: Long)

data class Streaks(val current: Int, val longest: Int)

object StudyMath {
    /** Local midnight boundaries preserve daylight-saving transitions. */
    fun dailyTotals(ranges: List<TimeRange>, zone: ZoneId): Map<LocalDate, Long> {
        val result = sortedMapOf<LocalDate, Long>()
        for (r in ranges) {
            var cursor = r.start
            while (cursor < r.end) {
                val day = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
                val stop =
                    minOf(r.end, day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
                result[day] = (result[day] ?: 0) + (stop - cursor)
                cursor = stop
            }
        }
        return result
    }

    fun streaks(totals: Map<LocalDate, Long>, threshold: Long, today: LocalDate): Streaks {
        require(threshold > 0)
        val qualified = totals.filter { it.key <= today && it.value >= threshold }.keys.sorted()
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in qualified) {
            run = if (previous?.plusDays(1) == day) run + 1 else 1
            longest = maxOf(longest, run)
            previous = day
        }
        val days = qualified.toSet()
        var cursor = if (today in days) today else today.minusDays(1)
        var current = 0
        while (cursor in days) {
            current++
            cursor = cursor.minusDays(1)
        }
        return Streaks(current, longest)
    }

    fun clock(ms: Long): String {
        val s = ms.coerceAtLeast(0) / 1000
        return "%02d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
    }

    fun duration(ms: Long): String {
        val m = ms.coerceAtLeast(0) / 60000
        return "${m/60}h ${m%60}m"
    }
}
