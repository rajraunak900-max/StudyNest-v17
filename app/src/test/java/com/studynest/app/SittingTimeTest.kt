package com.studynest.app

import com.studynest.app.data.*
import com.studynest.app.domain.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class SittingTimeTest {
    private fun visit(start:Long,end:Long)=LibraryVisit(start=start,end=end,checkpoint=start,elapsedStart=start,boot=1,minimumMinutes=1,qualified=true)
    private fun session(id:Long,trigger:String)=StudySession(id=id,startedAt=0,trigger=trigger)
    private fun segment(id:Long,start:Long,end:Long?)=Segment(id=id,sessionId=id,start=start,end=end,elapsedStart=start,boot=1)
    private fun total(visits:List<LibraryVisit>,sessions:List<StudySession>,segments:List<Segment>,now:Long)=
        SittingMath.forRange(visits,sessions,segments,now,TimeRange(0,now)){TimeRange(it.start,it.end ?: now)}

    @Test fun sittingUnionsLibraryAndManualTimeWithoutDoubleCountingOverlap() {
        val result=total(listOf(visit(0,60)),listOf(session(1,"Manual")),listOf(segment(1,30,90)),90)
        assertEquals(90,result)
    }

    @Test fun manualStudyAtHomeAddsToLibrarySittingTime() {
        val result=total(listOf(visit(0,60)),listOf(session(1,"Manual")),listOf(segment(1,90,120)),120)
        assertEquals(90,result)
    }

    @Test fun automaticTimerOutsideLibraryDoesNotAddManualSittingTime() {
        val result=total(emptyList(),listOf(session(1,"Wi-Fi")),listOf(segment(1,0,60)),60)
        assertEquals(0,result)
    }

    @Test fun distractionDoesNotReduceSittingAndManualTimerCanCountAtHome() {
        val day=LocalDate.of(2026,10,9)
        val start=day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val oneHour=60*60*1000L
        val result=SittingMath.forDay(emptyList(),listOf(session(1,"Manual")),listOf(segment(1,start,start+oneHour)),
            day,ZoneOffset.UTC,start+oneHour){TimeRange(it.start,it.end ?: start+oneHour)}
        assertEquals(oneHour,result)
    }

    @Test fun distractionReducesActualStudyButNotManualSittingDuration() {
        val duration=60*60*1000L
        val window=FocusWindow(id=1,sessionId=1,start=0,end=duration,elapsedStart=0,boot=1,mode="OFF",cursor=duration)
        val actual=FocusMath.summary(emptyList(),listOf(window),listOf(DistractionPeriod(windowId=1,
            start=0,end=duration/2,reason="APP")),duration,TimeRange(0,duration))
        val sitting=total(emptyList(),listOf(session(1,"Manual")),listOf(segment(1,0,duration)),duration)
        assertEquals(duration/2,actual.studyMs)
        assertEquals(duration,sitting)
    }

    @Test fun calendarAndPeriodReportsExposeSittingTime() {
        val day=LocalDate.of(2026,10,9)
        val row=HistoryDay(actual=30,library=60,tracked=30,distraction=10,sitting=90)
        assertEquals(90,HistoryMath.period(mapOf(day to row),day,day,day).sitting)
    }
}
