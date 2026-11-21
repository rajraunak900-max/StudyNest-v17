package com.studynest.app

import com.studynest.app.domain.*
import org.junit.Assert.*
import org.junit.Test

class UsageTimelineTest {
    private val allowed = UsageState(known = true)
    private val selected = setOf("instagram", "whatsapp")
    private fun events(vararg e: AppEvent) = e.toList()
    @Test fun exactFourHourExample() {
        val m = 60000L
        val result = UsageTimeline.reduce(0, 240*m, allowed, events(
            AppEvent(30*m, 1, "instagram"), AppEvent(45*m, 1, "allowed"),
            AppEvent(120*m, 1, "whatsapp"), AppEvent(140*m, 1, "allowed")), selected, false).first
        assertEquals(35*m, result.sumOf { it.end-it.start })
        assertEquals(listOf("instagram", "whatsapp"), result.map { it.pkg })
    }
    @Test fun switchingSelectedAppsNeverResumesOrOverlaps() {
        val result = UsageTimeline.reduce(0, 10000, allowed, events(
            AppEvent(1000, 1, "instagram"), AppEvent(4000, 2, "instagram"),
            AppEvent(4500, 1, "whatsapp"), AppEvent(8000, 1, "allowed")), selected, false).first
        assertEquals(7000L, result.sumOf { it.end-it.start })
        assertEquals(result[0].end, result[1].start)
    }
    @Test fun pauseWithoutReplacementExpiresAfterOneSecond() {
        val result = UsageTimeline.reduce(0, 10000, UsageState("instagram", true),
            events(AppEvent(4000, 2, "instagram")), selected, false)
        assertEquals(5000L, result.first.sumOf { it.end-it.start })
        assertNull(result.second.pkg)
    }
    @Test fun lockScreenNeverCountsAsAppDistraction() {
        val result = UsageTimeline.reduce(0, 10000, UsageState("instagram", true),
            events(AppEvent(3000, 4)), selected, false)
        assertEquals(3000L, result.first.sumOf { it.end-it.start })
        assertFalse(result.second.interactive)
    }
    @Test fun overlappingCallAndAppIsCountedOnlyOnce() {
        val result = UsageTimeline.reduce(0, 10000, UsageState("instagram", true),
            events(AppEvent(2000, 5), AppEvent(6000, 6), AppEvent(8000, 1, "allowed")), selected, true).first
        assertEquals(8000L, result.sumOf { it.end-it.start })
        assertEquals(4000L, result.filter { it.reason=="CALL" }.sumOf { it.end-it.start })
    }
    @Test fun allowedAppIsProductive() {
        assertTrue(UsageTimeline.reduce(0, 10000, allowed,
            events(AppEvent(1000, 1, "calculator")), selected, false).first.isEmpty())
    }
    @Test fun unknownForegroundIsNotInventedAsStudy() {
        val result = UsageTimeline.reduce(0, 10000, UsageState(),
            events(AppEvent(3000, 1, "allowed")), selected, false).first.single()
        assertEquals("UNKNOWN", result.reason); assertEquals(3000L, result.end)
    }
    @Test fun splitQueriesPreservePendingSwitchGrace() {
        val first = UsageTimeline.reduce(0, 4200, UsageState("instagram", true),
            events(AppEvent(4000, 2, "instagram")), selected, false)
        val next = UsageTimeline.reduce(4200, 10000, first.second,
            events(AppEvent(4500, 1, "whatsapp"), AppEvent(8000, 1, "allowed")), selected, false)
        assertEquals(8000L, (first.first+next.first).sumOf { it.end-it.start })
    }
    @Test fun pauseFromPreviousActivityDoesNotClearNewActivityInSameApp() {
        val result = UsageTimeline.reduce(0,10000,allowed, events(
            AppEvent(1000,1,"instagram",1), AppEvent(4000,1,"instagram",2),
            AppEvent(4500,2,"instagram",1), AppEvent(8000,1,"allowed",3)),selected,false)
        assertEquals(7000L,result.first.sumOf { it.end-it.start })
    }
}
