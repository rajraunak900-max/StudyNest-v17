package com.studynest.app
import android.app.Application
import com.studynest.app.automation.NotificationTiming
import com.studynest.app.data.*
import com.studynest.app.domain.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.*

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[26,33,36],application=Application::class)
class Post168DailyNotificationTest {
    private val zone=ZoneId.of("Asia/Kolkata")
    private val start=LocalDate.of(2026,10,5).atStartOfDay(zone).toInstant().toEpochMilli()
    private fun visit(id:Long,a:Long,b:Long?)=LibraryVisit(id=id,start=a,end=b,elapsedStart=0,boot=1)
    private fun window(id:Long,session:Long,a:Long,b:Long?)=FocusWindow(id=id,sessionId=session,start=a,end=b,elapsedStart=0,boot=1,mode="OFF")
    @Test fun multipleVisitsAndPriorSessionsAggregateIntoTodaysClocks(){
        val old=visit(1,start,start+7200000);val current=visit(2,start+10800000,null);val now=start+12600000
        val windows=listOf(window(1,1,start,old.end),window(2,2,current.start,null))
        val active=StudySession(id=2,startedAt=current.start,trigger="Wi-Fi")
        val t=NotificationTiming.snapshot(active,current,listOf(old,current),windows,emptyList(),now,100000,zone)
        assertEquals(9000000L to 9000000L,t.values(100000));assertEquals(9060000L to 9060000L,t.values(160000))
        assertEquals(FocusMath.summary(listOf(old,current),windows,emptyList(),now,TimeRange(start,now)).libraryStudyMs,t.studyMs)
    }
    @Test fun yesterdayIsExcludedButOutsideManualStudyAddsToDailyActual(){
        val yesterday=visit(1,start-7200000,start);val today=visit(2,start,start+600000)
        val windows=listOf(window(1,1,yesterday.start,yesterday.end),window(2,2,start,today.end),window(3,3,start+1200000,start+1800000))
        val t=NotificationTiming.snapshot(null,null,listOf(yesterday,today),windows,emptyList(),start+2400000,1000,zone)
        assertEquals(600000L to 1200000L,t.values(999999));assertFalse(t.libraryRunning);assertFalse(t.studyRunning)
    }
    @Test fun pendingAndResumedAwayIntervalsNeverInflateDailyClocks(){
        val away=visit(1,start,null).copy(awaySince=start+600000)
        val active=StudySession(id=1,startedAt=start,trigger="Wi-Fi",paused=true,pausedByDeparture=true)
        val w=window(1,1,start,start+600000)
        val t=NotificationTiming.snapshot(active,away,listOf(away),listOf(w),emptyList(),start+1200000,1000,zone)
        assertEquals(600000L to 600000L,t.values(999999));assertEquals("Temporarily away",t.state)
        val returned=LibraryPresence.resume(away,start+1200000)
        val r=NotificationTiming.snapshot(active.copy(paused=false),returned,listOf(returned),listOf(w,window(2,1,start+1200000,null)),emptyList(),start+1800000,1000,zone)
        assertEquals(1200000L to 1200000L,r.values(1000))
    }
    @Test fun localMidnightClipsCrossingVisitAndKnownWindow(){
        val v=visit(1,start-600000,null);val w=window(1,1,v.start,null)
        val a=StudySession(id=1,startedAt=v.start,trigger="Wi-Fi")
        val t=NotificationTiming.snapshot(a,v,listOf(v),listOf(w),emptyList(),start+300000,1000,zone)
        assertEquals(300000L to 300000L,t.values(1000))
        val zero=NotificationTiming.snapshot(a,v,listOf(v),listOf(w),emptyList(),start,1000,zone)
        assertEquals(0L to 0L,zero.values(1000))
    }
    @Test fun anyUnknownDailyLibraryCoverageKeepsActualUnavailable(){
        val old=visit(1,start,start+600000);val v=visit(2,start+1200000,null)
        val windows=listOf(window(1,1,start,old.end).copy(mode="UNKNOWN"),window(2,2,v.start,null))
        val a=StudySession(id=2,startedAt=v.start,trigger="Wi-Fi")
        val t=NotificationTiming.snapshot(a,v,listOf(old,v),windows,emptyList(),start+1800000,1000,zone)
        assertEquals(1200000L,t.libraryMs);assertNull(t.studyMs);assertFalse(t.studyRunning)
    }
    @Test fun outsideManualTimerAdvancesActualWhileLibraryRemainsStopped(){
        val active=StudySession(id=1,startedAt=start,trigger="Manual",measurementStart=start)
        val w=window(1,1,start,null)
        val t=NotificationTiming.snapshot(active,null,emptyList(),listOf(w),emptyList(),start+600000,1000,zone)
        assertEquals(0L to 600000L,t.values(1000));assertEquals(0L to 660000L,t.values(61000))
        assertTrue(t.studyRunning);assertFalse(t.libraryRunning)
        assertEquals("Studying",t.state)
    }

}
