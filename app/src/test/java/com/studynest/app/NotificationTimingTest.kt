package com.studynest.app

import android.app.Notification
import android.content.Context
import android.view.View
import android.widget.*
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.*
import com.studynest.app.data.*
import com.studynest.app.domain.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[26,33,36],application=android.app.Application::class)
class NotificationTimingTest {
    private val now=601000L
    private val anchor=900000L
    private val session=StudySession(id=1,startedAt=1000,trigger="Wi-Fi",measurementStart=1000)
    private val visit=LibraryVisit(id=1,start=1000,elapsedStart=0,boot=1)
    private val off=FocusWindow(id=5,sessionId=1,start=1000,elapsedStart=0,boot=1,mode="OFF",cursor=now)
    private fun snapshot(active:StudySession?=session,open:LibraryVisit?=visit,visits:List<LibraryVisit> = listOf(visit),
        windows:List<FocusWindow> = listOf(off),periods:List<DistractionPeriod> = emptyList(),at:Long=now,elapsed:Long=anchor)=
        NotificationTiming.snapshot(active,open,visits,windows,periods,at,elapsed)

    @Test fun normalStudyAdvancesBothClocksWithoutAppWork() {
        val value=snapshot()
        assertEquals(600000L to 600000L,value.values(anchor))
        assertEquals(617000L to 617000L,value.values(anchor+17000))
        assertEquals(600000L to 600000L,value.values(anchor-1000))
        assertEquals("Studying",value.state)
    }
    @Test fun manualStudyOutsideLibraryAdvancesActualButNotLibraryClock() {
        val manual=session.copy(trigger="Manual")
        val value=snapshot(active=manual,open=null,visits=emptyList(),windows=listOf(off))
        assertEquals(0L,value.libraryMs)
        assertEquals(600000L,value.studyMs)
        assertFalse(value.libraryRunning)
        assertTrue(value.studyRunning)
        assertEquals(0L to 600000L,value.values(anchor))
        assertEquals(0L to 615000L,value.values(anchor+15000))
        assertEquals("Studying",value.state)
    }
    @Test fun sittingNotificationUnionsLibraryAndManualTimeAndAdvancesOnce() {
        val manual=session.copy(trigger="Manual")
        val libraryPart=visit.copy(end=301000)
        val timerPart=Segment(sessionId=manual.id,start=301000,elapsedStart=0,boot=1)
        val value=NotificationTiming.snapshot(manual,null,listOf(libraryPart),listOf(off),emptyList(),now,anchor,
            sessions=listOf(manual),segments=listOf(timerPart))
        assertEquals(600000L,value.sittingMs)
        assertTrue(value.sittingRunning)
        assertEquals(615000L,value.values(anchor+15000).first)
    }
    @Test fun overlappingManualAndLibraryIntervalsCountOnlyOnceInNotification() {
        val manual=session.copy(trigger="Manual")
        val libraryPart=visit.copy(end=601000)
        val timerPart=Segment(sessionId=manual.id,start=301000,elapsedStart=0,boot=1)
        val value=NotificationTiming.snapshot(manual,null,listOf(libraryPart),listOf(off),emptyList(),900000,anchor,
            sessions=listOf(manual),segments=listOf(timerPart))
        assertEquals(899000L,value.sittingMs)
        assertEquals(909000L,value.values(anchor+10000).first)
    }
    @Test fun selectedDistractionFreezesStudyButNotLibrary() {
        val window=off.copy(mode="ON",packages="[\"com.example.fun\"]",
            tracker=UsageState(pkg="com.example.fun",known=true).encode())
        val value=snapshot(windows=listOf(window),periods=listOf(DistractionPeriod(windowId=5,
            start=now-120000,end=now,reason="APP",packageName="com.example.fun")))
        assertEquals(610000L to 480000L,value.values(anchor+10000))
        assertFalse(value.studyRunning);assertTrue(value.libraryRunning)
    }
    @Test fun manualPauseFreezesStudyWhilePresenceContinues() {
        val value=snapshot(active=session.copy(paused=true),windows=listOf(off.copy(end=now-60000)))
        assertEquals(610000L to 540000L,value.values(anchor+10000))
        assertEquals("Paused",value.state)
    }
    @Test fun departureKeepsFrozenLibraryAndStudyTotals() {
        val closed=visit.copy(end=now-90000)
        val value=snapshot(active=session.copy(paused=true,pausedByDeparture=true),open=null,
            visits=listOf(closed),windows=listOf(off.copy(end=now-90000)))
        assertEquals(510000L to 510000L,value.values(anchor+3600000))
        assertFalse(value.libraryRunning);assertFalse(value.studyRunning)
    }
    @Test fun stoppedSessionCannotKeepAdvancingActualStudy() {
        val value=snapshot(active=null,windows=listOf(off.copy(end=now)))
        // 1.6.9 retains today's previously earned Actual after the timer stops.
        assertEquals(615000L to 600000L,value.values(anchor+15000))
        assertFalse(value.studyRunning)
        assertEquals("Library visit • No active session",value.state)
    }
    @Test fun lockedScreenClearStateContinuesStudyAndCallsPauseIt() {
        val locked=off.copy(mode="ON",cursor=now,packages="[\"com.example.fun\"]",
            tracker=UsageState(known=true,interactive=false).encode())
        assertEquals(660000L to 660000L,snapshot(windows=listOf(locked)).values(anchor+60000))
        val call=locked.copy(calls=true,tracker=UsageState(known=true,interactive=false,call=true).encode())
        assertFalse(snapshot(windows=listOf(call)).studyRunning)
    }
    @Test fun missingAccessOrStaleInteractiveCoverageNeverInventsStudy() {
        val unavailable=snapshot(windows=listOf(off.copy(mode="UNKNOWN")))
        assertNull(unavailable.studyMs);assertFalse(unavailable.studyRunning)
        val stale=snapshot(windows=listOf(off.copy(mode="ON",cursor=now-61000,
            tracker=UsageState(known=true).encode())))
        assertNull(stale.studyMs);assertFalse(stale.studyRunning)
    }
    @Test fun currentSessionStudyDoesNotIncludePreviousSessionsInVisit() {
        val previous=off.copy(id=6,sessionId=2,end=121000)
        val current=off.copy(start=121000)
        val value=snapshot(windows=listOf(previous,current))
        // 1.6.9 includes previous same-day sessions in the daily Actual clock.
        assertEquals(600000L,value.libraryMs);assertEquals(600000L,value.studyMs)
    }
    @Test fun reconciliationRebasesAuthoritativeTimeWithoutDoubleCounting() {
        val clear=UsageState(known=true)
        val window=off.copy(mode="ON",packages="[\"com.example.fun\"]",tracker=clear.encode())
        val reduced=UsageTimeline.reduce(now,now+15000,clear,
            listOf(AppEvent(now+8000,UsageTimeline.RESUME,"com.example.fun")),setOf("com.example.fun"),false)
        val reconciled=snapshot(windows=listOf(window.copy(cursor=now+15000,tracker=reduced.second.encode())),
            periods=reduced.first.map { DistractionPeriod(windowId=5,start=it.start,end=it.end,reason=it.reason,packageName=it.pkg) },
            at=now+15000,elapsed=anchor+15000)
        assertEquals(615000L to 608000L,reconciled.values(anchor+15000))
        assertEquals(625000L to 608000L,reconciled.values(anchor+25000))
        // The initial display was based on the last observed foreground state.
        // Newly delivered Android events correct that projection at reconciliation.
    }
    @Test fun remoteViewsCarryMonotonicBasesAndFrozenTextWithActions() {
        val c=ApplicationProvider.getApplicationContext<Context>()
        val value=snapshot()
        val view=value.views(c,true).apply(c,FrameLayout(c))
        assertEquals(anchor-600000,view.findViewById<Chronometer>(R.id.library_clock).base)
        assertEquals(View.VISIBLE,view.findViewById<View>(R.id.study_clock).visibility)
        val frozen=value.copy(studyRunning=false,studyMs=480000)
        val paused=frozen.views(c,false).apply(c,FrameLayout(c))
        assertEquals(View.GONE,paused.findViewById<View>(R.id.study_clock).visibility)
        assertEquals("08:00",paused.findViewById<TextView>(R.id.study_frozen).text.toString())
        val n=Notifications.notification(c,"Current session",true,false,600000,value)
        assertFalse(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertNotNull(n.contentView);assertNotNull(n.bigContentView)
        assertEquals(2,n.actions.size)
        assertEquals("Pause",n.actions[0].title.toString());assertEquals("Finish",n.actions[1].title.toString())
    }

    @Test fun temporarilyAwayOpenVisitFreezesNativeViewsUntilSameVisitReturns() {
        val away=visit.copy(awaySince=now-90000)
        val value=snapshot(active=session.copy(paused=true,pausedByDeparture=true),open=away,
            visits=listOf(away),windows=listOf(off.copy(end=now-90000)))
        assertEquals(510000L to 510000L,value.values(anchor+3600000))
        assertEquals("Temporarily away",value.state)
        val c=ApplicationProvider.getApplicationContext<Context>()
        val view=value.views(c,true).apply(c,FrameLayout(c))
        assertEquals(View.GONE,view.findViewById<View>(R.id.library_clock).visibility)
        assertEquals(View.GONE,view.findViewById<View>(R.id.study_clock).visibility)
        assertEquals("08:30",view.findViewById<TextView>(R.id.library_frozen).text.toString())
        val resumed=LibraryPresence.resume(away,now)
        val returned=snapshot(open=resumed,visits=listOf(resumed),
            windows=listOf(off.copy(end=now-90000),off.copy(id=6,start=now)),at=now+60000,elapsed=anchor+60000)
        assertTrue(returned.libraryRunning);assertTrue(returned.studyRunning)
        assertEquals(570000L to 570000L,returned.values(anchor+60000))
    }
}
