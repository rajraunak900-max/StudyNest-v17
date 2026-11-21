package com.studynest.app

import com.studynest.app.data.*
import com.studynest.app.domain.*
import java.time.*
import org.junit.Test
import org.junit.Assert.*

class StudyPeriodsTest {
    private val day = LocalDate.of(2026, 9, 30)
    private val zone = ZoneOffset.UTC
    private val base = day.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun session(id: Long, start: Long = 0, finish: Boolean = false) = StudySession(id, base + start, base + start + 60000, if (id == 1L) "Wi-Fi" else "Manual", endedExplicitly = finish)
    private fun segment(id: Long, session: Long, start: Long, end: Long) = Segment(id, session, base + start, base + end, 0, 0)
    private fun groups(s: List<StudySession>, p: List<Segment>) = StudyPeriods.forDay(s, p, day, zone) { TimeRange(it.start, it.end!!) }
    @Test fun threeHoursWithShortBreaksAndMixedTrackingIsOnePeriod() {
        val result = groups(listOf(session(1), session(2)), listOf(segment(1,1,0,7200000), segment(2,2,7500000,11100000)))
        assertEquals(1, result.size); assertEquals(10800000, result.single().studyMs)
        assertEquals(300000, result.single().breakMs)
        assertEquals(listOf("Wi-Fi", "Manual"), result.single().parts.map { it.trigger })
    }
    @Test fun laterReturnSplitsEvenTheSameAutomaticRecord() {
        assertEquals(2, groups(listOf(session(1)), listOf(segment(1,1,0,3600000), segment(2,1,7200000,10800000))).size)
    }
    @Test fun explicitFinishAlwaysCreatesNewPeriod() {
        assertEquals(2, groups(listOf(session(1,finish=true), session(2)), listOf(segment(1,1,0,60000), segment(2,2,61000,121000))).size)
    }
    @Test fun zeroAndSubMinuteGroupsAreHiddenWithoutModifyingSource() {
        val parts = listOf(segment(1,1,0,0), segment(2,1,1000,2000))
        assertTrue(groups(listOf(session(1)), parts).isEmpty()); assertEquals(2, parts.size)
    }
    @Test fun midnightSplitsPresentationAndPreservesDurations() {
        val parts = listOf(segment(1,1,-3600000,3600000))
        assertEquals(3600000, groups(listOf(session(1)), parts).single().studyMs)
        assertEquals(3600000, StudyPeriods.forDay(listOf(session(1)), parts, day.minusDays(1), zone) { TimeRange(it.start,it.end!!) }.single().studyMs)
    }
    @Test fun shortReconnectKeepsSamePeriodAndExcludesGap() {
        val group = groups(listOf(session(1)), listOf(segment(1,1,0,60000),segment(2,1,90000,150000))).single()
        assertEquals(120000,group.studyMs); assertEquals(30000,group.breakMs)
    }
    @Test fun configuredMinimumAppliesToManualAndAutomaticPeriodsOutsideLibrary() {
        val manual=session(1).copy(trigger="Manual")
        val automatic=session(2).copy(trigger="Wi-Fi")
        val parts=listOf(segment(1,1,0,9*60000L),segment(2,2,40*60000L,50*60000L))
        val visible=StudyPeriods.forDay(listOf(manual,automatic),parts,day,zone,{TimeRange(it.start,it.end!!)},emptyList(),10*60000L)
        assertEquals(1,visible.size)
        assertEquals(listOf(2L),visible.single().parts.map { it.sessionId })
        // Filtering History rows never removes the underlying tracked totals.
        assertEquals(9*60000L+10*60000L,parts.sumOf { it.end!!-it.start })
    }
    @Test fun manualAndAutomaticPeriodsAtTheSameMinimumBothAppear() {
        val manual=session(1).copy(trigger="Manual")
        val automatic=session(2).copy(trigger="Wi-Fi")
        val parts=listOf(segment(1,1,0,10*60000L),segment(2,2,40*60000L,50*60000L))
        val visible=StudyPeriods.forDay(listOf(manual,automatic),parts,day,zone,{TimeRange(it.start,it.end!!)},emptyList(),10*60000L)
        assertEquals(2,visible.size)
        assertEquals(setOf(1L,2L),visible.flatMap { it.parts }.map { it.sessionId }.toSet())
    }
}
