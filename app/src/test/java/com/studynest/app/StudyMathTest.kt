package com.studynest.app

import com.studynest.app.domain.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class StudyMathTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun ms(s: String) = LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun splitsMidnight() {
        val t =
            StudyMath.dailyTotals(
                listOf(TimeRange(ms("2026-09-29T23:30:00"), ms("2026-09-30T01:00:00"))), zone)
        assertEquals(1800000L, t[LocalDate.of(2026, 9, 29)])
        assertEquals(3600000L, t[LocalDate.of(2026, 9, 30)])
    }

    @Test
    fun sumsSessionsAndExcludesBreaks() {
        val t =
            StudyMath.dailyTotals(
                listOf(
                    TimeRange(ms("2026-09-29T10:05:00"), ms("2026-09-29T13:20:00")),
                    TimeRange(ms("2026-09-29T16:00:00"), ms("2026-09-29T18:00:00"))),
                zone)
        assertEquals(315 * 60000L, t.values.sum())
    }

    @Test
    fun handlesDaylightSaving() {
        val z = ZoneId.of("America/New_York")
        val d = LocalDate.of(2026, 3, 8)
        assertEquals(
            23 * 3600000L,
            StudyMath.dailyTotals(
                    listOf(
                        TimeRange(
                            d.atStartOfDay(z).toInstant().toEpochMilli(),
                            d.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli())),
                    z)[d])
    }

    @Test
    fun streakKeepsYesterdayUntilTodayQualifies() {
        val d = LocalDate.of(2026, 9, 29)
        val t = mapOf(d.minusDays(2) to 120L, d.minusDays(1) to 120L, d to 119L)
        assertEquals(Streaks(2, 2), StudyMath.streaks(t, 120, d))
        assertEquals(Streaks(3, 3), StudyMath.streaks(t + (d to 120L), 120, d))
    }

    @Test
    fun gapBreaksCurrentButPreservesLongest() {
        val d = LocalDate.of(2026, 9, 29)
        assertEquals(
            Streaks(0, 2),
            StudyMath.streaks(mapOf(d.minusDays(4) to 120L, d.minusDays(3) to 120L), 120, d))
    }

    @Test
    fun emptyAndFutureDaysDontCount() {
        val d = LocalDate.now()
        assertEquals(Streaks(0, 0), StudyMath.streaks(emptyMap(), 1, d))
        assertEquals(Streaks(0, 0), StudyMath.streaks(mapOf(d.plusDays(1) to 99L), 1, d))
    }
}
