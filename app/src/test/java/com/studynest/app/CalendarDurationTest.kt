package com.studynest.app

import com.studynest.app.ui.calendarDuration
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class CalendarDurationTest {
    @Test fun hoursUseOneDecimalWithoutChangingRawMilliseconds() {
        val exact=230*60_000L+123
        assertEquals("3.8h",calendarDuration(exact,Locale.US))
        assertEquals(13_800_123L,exact)
        assertEquals("4.4h",calendarDuration(265*60_000L,Locale.US))
        assertEquals("1.5h",calendarDuration(90*60_000L,Locale.US))
    }
    @Test fun belowOneHourUsesMinutesAndNonzeroSecondsAreNotHidden() {
        assertEquals("52m",calendarDuration(52*60_000L,Locale.US))
        assertEquals("8m",calendarDuration(8*60_000L,Locale.US))
        assertEquals("<1m",calendarDuration(1,Locale.US))
        assertEquals("59m",calendarDuration(3_599_999,Locale.US))
        assertEquals("1.0h",calendarDuration(3_600_000,Locale.US))
    }
    @Test fun zeroAndDayLongValuesAreExplicit() {
        assertEquals("0m",calendarDuration(0,Locale.US))
        assertEquals("24.0h",calendarDuration(86_400_000,Locale.US))
    }
    @Test fun decimalsRespectLocale() {assertEquals("1,5h",calendarDuration(5_400_000,Locale.GERMANY))}
    @Test(expected=IllegalArgumentException::class) fun invalidNegativeDurationsAreRejected() {calendarDuration(-1)}
}
