package com.studynest.app

import com.studynest.app.data.*
import com.studynest.app.domain.*
import org.junit.Test
import org.junit.Assert.*
import java.time.*

class MidnightMetricsTest {
    @Test fun libraryBreakdownExcludesOutsideUseAndClipsCrossingEvents() {
        val visits=listOf(LibraryVisit(id=1,start=10000,end=20000,elapsedStart=0,boot=1))
        val windows=listOf(FocusWindow(id=2,sessionId=3,start=0,end=30000,elapsedStart=0,boot=1,mode="ON",cursor=30000,seeded=true))
        val periods=listOf(
            DistractionPeriod(windowId=2,start=0,end=5000,reason="APP",packageName="outside.app"),
            DistractionPeriod(windowId=2,start=5000,end=15000,reason="APP",packageName="crossing.app"),
            DistractionPeriod(windowId=2,start=15000,end=17000,reason="CALL"))
        val summary=FocusMath.summary(visits,windows,periods,30000)
        assertEquals(17000L,summary.distractionMs)
        assertEquals(7000L,summary.libraryDistractionMs)
        assertEquals(mapOf("crossing.app" to 5000L,"Call audio" to 2000L),summary.libraryBreakdown)
        assertEquals(5000L,summary.breakdown["outside.app"])
    }
    @Test fun libraryStudyDistractionBreakAndSegmentsSplitWithoutAMidnightCallback() {
        val zone=ZoneId.of("Asia/Kolkata");val day=LocalDate.of(2026,10,1)
        val midnight=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val a=midnight-30*60000;val b=midnight+30*60000
        val visits=listOf(LibraryVisit(id=1,start=a,end=b,elapsedStart=0,boot=1))
        val windows=listOf(FocusWindow(id=2,sessionId=3,start=a,end=b,elapsedStart=0,boot=1,mode="ON",cursor=b,seeded=true))
        val periods=listOf(DistractionPeriod(windowId=2,start=midnight-5*60000,end=midnight+5*60000,reason="APP",packageName="selected.app"))
        fun summary(from:Long,until:Long)=FocusMath.summary(visits,windows,periods,b,TimeRange(from,until))
        val old=summary(a,midnight);val next=summary(midnight,b)
        assertEquals(30*60000L,old.libraryMs);assertEquals(30*60000L,next.libraryMs)
        assertEquals(5*60000L,old.distractionMs);assertEquals(5*60000L,next.distractionMs)
        assertEquals(25*60000L,old.studyMs);assertEquals(25*60000L,next.studyMs)
        val totals=StudyMath.dailyTotals(listOf(TimeRange(a,b)),zone)
        assertEquals(30*60000L,totals[day]);assertEquals(30*60000L,totals[day.plusDays(1)])
        val pauses=listOf(ManualBreak(sessionId=3,visitId=1,start=midnight-1000,end=midnight+1000,elapsedStart=0,boot=1))
        assertEquals(1000L,FocusMath.summary(visits,emptyList(),emptyList(),b,TimeRange(a,midnight),breaks=pauses).breakMs)
        assertEquals(1000L,FocusMath.summary(visits,emptyList(),emptyList(),b,TimeRange(midnight,b),breaks=pauses).breakMs)
    }
    @Test fun midnightAndDaylightSavingBoundariesUseTheDevicesZone() {
        val zone=ZoneId.of("America/New_York");val day=LocalDate.of(2026,11,1)
        val start=day.atStartOfDay(zone).toInstant().toEpochMilli();val end=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(25*3600000L,StudyMath.dailyTotals(listOf(TimeRange(start,end)),zone)[day])
        val next=StudyMath.dailyTotals(listOf(TimeRange(end-60000,end+60000)),zone)
        assertEquals(60000L,next[day]);assertEquals(60000L,next[day.plusDays(1)])
    }
}
