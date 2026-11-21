package com.studynest.app

import android.app.Application
import com.studynest.app.data.*
import com.studynest.app.domain.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.*

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=Application::class)
class HistoryAnalyticsTest {
    private val day=LocalDate.of(2026,10,3)
    private val zone=ZoneOffset.UTC
    private val start=day.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun visit(a:Long,b:Long,id:Long=1)=VisitQualification.finish(LibraryVisit(id=id,start=start+a,elapsedStart=0,boot=1,minimumMinutes=10),start+b)
    private fun window(a:Long,b:Long)=FocusWindow(id=1,sessionId=1,start=start+a,end=start+b,elapsedStart=0,boot=1,mode="OFF")
    @Test fun comparativeMetricsUseLibraryStudyAndPreserveDistractionAndShortVisits() {
        val v=visit(0,360000);val w=window(0,360000).copy(mode="ON",cursor=start+360000)
        val p=DistractionPeriod(windowId=1,start=start+120000,end=start+180000,reason="APP")
        val index=HistoryIndex(listOf(v),listOf(w),listOf(p),emptyList(),zone,start+400000)
        val summary=index.live(start+500000)[day]!!
        assertEquals(360000L,summary.libraryMs);assertEquals(300000L,summary.libraryStudyMs)
        assertEquals(60000L,summary.libraryDistractionMs);assertEquals(false,v.qualified)
        assertEquals(0,VisitQualification.forDay(listOf(v),day,zone).size)
    }
    @Test fun calendarAndPeriodActualIncludeManualStudyOutsideLibrary() {
        val outside=window(0,360000).copy(mode="ON",cursor=start+360000)
        val period=DistractionPeriod(windowId=1,start=start+120000,end=start+180000,reason="APP")
        val index=HistoryIndex(emptyList(),listOf(outside),listOf(period),emptyList(),zone,start+360000)
        val summary=index.live(start+360000)[day]!!
        val calendar=HistoryDay.from(summary,360000)
        assertEquals(300000L,calendar.actual)
        assertEquals(0L,calendar.library)
        assertEquals(300000L,HistoryMath.period(mapOf(day to calendar),day,day,day).actual)
    }
    @Test fun periodTotalsAverageIncludeRestDaysAndExcludeFutureDays() {
        val data=mapOf(day to HistoryDay(7200000,10800000,7200000,0))
        val p=HistoryMath.period(data,day.minusDays(2),day.plusDays(4),day)
        assertEquals(7200000L,p.actual);assertEquals(10800000L,p.library);assertEquals(2400000L,p.average)
        assertEquals(3,p.elapsedDays);assertEquals(day to 7200000L,p.best)
    }
    @Test fun missingMeasurementsNeverBecomeFabricatedZero() {
        val d=HistoryDay.from(null,600000);assertNull(d.actual);assertFalse(d.libraryRecorded)
        val p=HistoryMath.period(mapOf(day to d),day,day,day)
        assertNull(p.actual);assertNull(p.average);assertNull(p.best)
    }
    @Test fun dailyWindowAndVisitBucketsMatchTheOriginalPeriodSummary() {
        val visits=listOf(visit(-240000,480000),visit(3600000,5400000,2))
        val windows=listOf(window(-240000,480000),window(3600000,5400000).copy(id=2))
        val index=HistoryIndex(visits,windows,emptyList(),emptyList(),zone,start+5400000)
        val combined=combineFocusDays(index.live(start+5400000).values.toList())
        val original=FocusMath.summary(visits,windows,emptyList(),start+5400000)
        assertEquals(original,combined)
        val covered=index.visitCoverage(day,start+5400000)
        assertEquals(listOf(TimeRange(start,start+480000),TimeRange(start+3600000,start+5400000)),covered)
    }
    @Test fun indexedTrackingPreservesLegacyAndManualWhileExcludingNewVisits() {
        val session=StudySession(id=1,startedAt=start,endedAt=start+3600000,trigger="Manual")
        val segment=Segment(id=1,sessionId=1,start=start,end=start+3600000,elapsedStart=0,boot=1)
        val index=HistoryTrackingIndex(listOf(session),listOf(segment),zone)
        val remaining=index.periods(day,listOf(TimeRange(start,start+1800000))){TimeRange(it.start,it.end!!)}
        assertEquals(1,remaining.size);assertEquals(1800000L,remaining.single().studyMs)
        assertEquals(3600000L,index.periods(day,emptyList()){TimeRange(it.start,it.end!!)}.single().studyMs)
    }
    @Test fun unavailableUsageCoverageStaysUnavailable() {
        val w=window(0,600000).copy(mode="UNKNOWN")
        val index=HistoryIndex(listOf(visit(0,600000)),listOf(w),emptyList(),emptyList(),zone,start+600000)
        val data=HistoryDay.from(index.live(start+600000)[day],600000)
        assertNull(data.actual);assertEquals(600000L,data.library)
    }
    @Test fun midnightIndexSplitsFourAndEightMinutesWithoutDuplicatingVisit() {
        val v=visit(-240000,480000)
        val index=HistoryIndex(listOf(v),listOf(window(-240000,480000)),emptyList(),emptyList(),zone,start+480000)
        val result=index.live(start+480000)
        assertEquals(240000L,result[day.minusDays(1)]!!.libraryMs);assertEquals(480000L,result[day]!!.libraryMs)
        assertEquals(240000L,result[day.minusDays(1)]!!.libraryStudyMs);assertEquals(480000L,result[day]!!.libraryStudyMs)
    }
    @Test fun activeIntervalReachesMidnightBeforeNextRoomEmission() {
        val v=LibraryVisit(id=1,start=start-240000,elapsedStart=0,boot=1,minimumMinutes=10)
        val w=window(-240000,0).copy(end=null)
        val index=HistoryIndex(listOf(v),listOf(w),emptyList(),emptyList(),zone,start-1)
        assertEquals(480000L,index.live(start+480000)[day]!!.libraryStudyMs)
    }
    @Test fun localDstBoundaryUsesRealDateLengths() {
        val z=ZoneId.of("America/New_York");val d=LocalDate.of(2026,11,1)
        val a=d.atStartOfDay(z).toInstant().toEpochMilli();val b=d.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli()
        val v=LibraryVisit(id=1,start=a,end=b,elapsedStart=0,boot=1,minimumMinutes=10,qualified=true)
        val index=HistoryIndex(listOf(v),emptyList(),emptyList(),emptyList(),z,b)
        assertEquals(25*3600000L,index.live(b)[d]!!.libraryMs)
    }
    @Test fun closedHistoryIsCachedWhileOpenTimeUpdates() {
        val closed=visit(-86400000,-85800000)
        val open=LibraryVisit(id=2,start=start,elapsedStart=0,boot=1,minimumMinutes=10)
        val index=HistoryIndex(listOf(closed,open),emptyList(),emptyList(),emptyList(),zone,start)
        val old=index.closed[day.minusDays(1)]!!
        assertSame(old,index.live(start+60000)[day.minusDays(1)])
        assertSame(old,index.live(start+120000)[day.minusDays(1)])
        assertEquals(120000L,index.live(start+120000)[day]!!.libraryMs)
    }
    @Test fun largeCompletedTrackingHistoryIsNotReprocessedOnClockTicks() {
        val segments=(0 until 5000).map {i -> Segment(id=i.toLong()+1,sessionId=i.toLong()+1,
            start=start-(i+1L)*86400000,end=start-(i+1L)*86400000+600000,elapsedStart=0,boot=1)}
        var calls=0
        val range:(Segment)->TimeRange={calls++;TimeRange(it.start,it.end ?: start+60000)}
        val closed=TrackingTotalsIndex(segments,zone,range)
        assertEquals(5000,calls)
        repeat(20){assertSame(closed.completed,closed.current(range))}
        assertEquals(5000,calls)
        val open=Segment(id=5001,sessionId=5001,start=start,elapsedStart=0,boot=1)
        val active=TrackingTotalsIndex(segments+open,zone,range);val before=calls
        val current=active.current(range)
        assertEquals(before+1,calls)
        assertEquals(StudyMath.dailyTotals((segments+open).map {TimeRange(it.start,it.end ?: start+60000)},zone),current)
    }
    @Test fun customGoalAndStreakValidationIsExact() {
        assertEquals(450,GoalInput.hours("7.5"));assertEquals(1440,GoalInput.hours("24"))
        listOf(200,225,250,46,45,1440).forEach {assertEquals(it,GoalInput.minutes(it.toString()))}
        listOf("","0","-1","1.5","1441","abc","99999999999999999999").forEach {assertNull(GoalInput.minutes(it))}
        listOf("","0","-1","24.1","NaN","Infinity","abc","1e3").forEach {assertNull(GoalInput.hours(it))}
    }
}
