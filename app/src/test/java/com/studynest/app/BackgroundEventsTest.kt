package com.studynest.app

import com.studynest.app.domain.StudyMath

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.BackgroundEvents
import com.studynest.app.automation.Notifications
import com.studynest.app.data.Config
import com.studynest.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** No Activity is created. Radio delivery/Doze still require a physical-device check. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = StudyNestApp::class)
class BackgroundEventsTest {
    private lateinit var app: StudyNestApp
    private lateinit var events: BackgroundEvents
    private lateinit var eventScope: CoroutineScope
    private val waits = Channel<Long>(Channel.UNLIMITED)
    private val proceed = Channel<Unit>(Channel.UNLIMITED)
    @Volatile private var time = 1790000000000L
    @Volatile private var connected = false
    @Volatile private var mode = 2
    private var refreshes = 0

    @Before fun setup() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        withContext(Dispatchers.IO) { app.db.clearAllTables() }
        app.preferences.save(Config(setup = true, wifi = true, geo = false, tracking = true, vibrate = true))
        app.engine = SessionEngine(app.db, object : StudyClock {
            override fun now() = time
            override fun elapsed() = time
            override fun boot() = 1
        }, { app.preferences.value }, object : SoundControl {
            override fun current() = mode
            override suspend fun set(mode: Int): Boolean { this@BackgroundEventsTest.mode = mode; return true }
        }, { fail(it) })
        eventScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        events = BackgroundEvents(app, eventScope, { time }, { connected }, { refreshes++ }, { ms ->
            waits.send(ms)
            proceed.receive()
        })
    }

    @After fun cleanup() { eventScope.cancel(); waits.close(); proceed.close() }

    @Test fun soundDeadlineWakeIsSharedAndReleasedOnCancellation() = runBlocking {
        val deadline = System.currentTimeMillis() + 300000
        app.scheduleSoundDeadline(deadline)
        val wake = org.robolectric.shadows.ShadowPowerManager.getLatestWakeLock()
        assertTrue(wake.isHeld)
        app.scheduleSoundDeadline(deadline)
        assertSame(wake, org.robolectric.shadows.ShadowPowerManager.getLatestWakeLock())
        app.cancelSoundDeadline()
        withTimeout(5000) { while (wake.isHeld) delay(10) }
    }

    private suspend fun wifi(inside: Boolean) { connected = inside; events.signal("Wi-Fi", inside) }
    private suspend fun settle(expected: Long) {
        assertEquals(expected, withTimeout(5000) { waits.receive() })
        time += expected
        proceed.send(Unit)
    }
    private suspend fun awaitClosed()=withTimeout(5000){while(app.db.focusDao().openVisit()!=null)delay(10)}
    private suspend fun awaitPaused() = withTimeout(5000) {
        while (app.db.dao().active()?.paused != true) delay(10)
    }

    @Test fun departureRestoresSoundAndPausesAtExitWithoutActivity() = runBlocking {
        wifi(true)
        assertEquals(1, mode)
        val id = app.db.dao().active()!!.id
        time += 600000
        val exit = time
        wifi(false)
        val confirmationWake=org.robolectric.shadows.ShadowPowerManager.getLatestWakeLock()
        assertTrue(confirmationWake.isHeld)
        settle(SessionEngine.WIFI_SOUND_GRACE_MS)
        // Receiving the next wait proves the first deadline was processed.
        assertEquals(SessionEngine.WIFI_EXIT_GRACE_MS-SessionEngine.WIFI_SOUND_GRACE_MS,withTimeout(5000){waits.receive()})
        assertEquals(2,mode);assertTrue(app.db.dao().active()!!.pausedByDeparture)
        assertFalse("No wake protection held through thirty-minute grace",confirmationWake.isHeld)
        val visit=app.db.focusDao().openVisit()!!
        assertEquals(exit,visit.awaySince)
        assertEquals(600000L,LibraryPresence.duration(visit,time))
        assertNotNull(app.db.dao().state()!!.exitAt)
        time+=SessionEngine.WIFI_EXIT_GRACE_MS-SessionEngine.WIFI_SOUND_GRACE_MS;proceed.send(Unit)
        awaitClosed()
        assertEquals(2, mode)
        assertEquals(exit, app.db.dao().segmentsForSession(id).single().end)
        assertEquals(1, app.db.dao().sessions().first().size)
        assertTrue(refreshes >= 3)
        wifi(true)
        assertEquals(id, app.db.dao().active()!!.id)
        assertFalse(app.db.dao().active()!!.paused)
    }

    @Test fun reconnectCancelsDepartureAndKeepsSingleSession() = runBlocking {
        wifi(true)
        time += 60000
        wifi(false)
        time += 5000
        wifi(true)
        assertNull(app.db.dao().state()!!.exitAt)
        assertFalse(app.db.dao().active()!!.paused)
        assertEquals(1, app.db.dao().sessions().first().size)
    }

    @Test fun lockedScreenNinetyMinuteWifiVisitFinalizesAndQueriesWithoutOpeningUi() = runBlocking {
        org.robolectric.Shadows.shadowOf(app.getSystemService(android.os.PowerManager::class.java)).setIsInteractive(false)
        wifi(true)
        val original=app.db.focusDao().openVisit()!!
        assertEquals(10,original.minimumMinutes)
        time += 90*60000L
        val exit=time
        wifi(false);settle(SessionEngine.WIFI_SOUND_GRACE_MS);settle(SessionEngine.WIFI_EXIT_GRACE_MS-SessionEngine.WIFI_SOUND_GRACE_MS);awaitClosed()
        val visits=app.db.focusDao().visits().first()
        assertEquals(1,visits.size)
        val closed=visits.single()
        assertEquals(original.id,closed.id);assertEquals(exit,closed.end)
        assertEquals(90*60000L,closed.end!!-closed.start);assertEquals(true,closed.qualified)
        val day=java.time.Instant.ofEpochMilli(original.start).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        assertEquals(listOf(closed),VisitQualification.forDay(visits,day,java.time.ZoneId.systemDefault()))
        assertNull(app.db.focusDao().openVisit())
    }

    @Test fun locationPresenceKeepsTimerRunningAfterWifiExit() = runBlocking {
        app.preferences.save(app.preferences.value.copy(geo = true))
        wifi(true)
        events.signal("Location", true)
        time += 60000
        wifi(false)
        settle(SessionEngine.WIFI_SOUND_GRACE_MS)
        withTimeout(5000) { while (mode != 2) delay(10) }
        assertFalse(app.db.dao().active()!!.paused)
        assertNull(app.db.dao().state()!!.exitAt)
        events.signal("Location", false)
        settle(SessionEngine.EXIT_GRACE_MS)
        settle(SessionEngine.WIFI_EXIT_GRACE_MS-SessionEngine.EXIT_GRACE_MS)
        awaitClosed()
    }

    @Test fun manualDeparturePausesButReturnDoesNotResumeManualTimer() = runBlocking {
        app.preferences.save(app.preferences.value.copy(tracking = false))
        wifi(true)
        app.engine.manualStart()
        time += 120000
        wifi(false)
        settle(SessionEngine.WIFI_SOUND_GRACE_MS)
        awaitPaused()
        wifi(true)
        assertTrue(app.db.dao().active()!!.paused)
        assertEquals("Manual", app.db.dao().active()!!.trigger)
    }

    @Test fun notificationUsesOnlyCurrentSessionStudySegments() = runBlocking {
        app.preferences.save(app.preferences.value.copy(wifi = false, vibrate = false))
        app.engine.manualStart()
        time += 120000
        app.engine.pauseResume()
        time += 300000
        val paused = Notifications.current(app)
        // 1.6.9 fallback text agrees with the authoritative daily clocks, too.
        val library=paused.extras.getLong("studynest.sittingMs")
        val actual=paused.extras.getLong("studynest.studyMs")
        assertEquals("Sitting: ${StudyMath.clock(library)} • Actual: ${if(actual<0) "Unavailable" else StudyMath.clock(actual)}",paused.extras.getString(Notification.EXTRA_TEXT))
        assertFalse(paused.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        app.engine.pauseResume()
        time += 60000
        val current = Notifications.current(app)
        assertFalse(current.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(current.extras.containsKey("studynest.sittingMs"))
        assertTrue(current.extras.containsKey("studynest.studyMs"))
        assertNotNull(current.contentView);assertEquals("Pause",current.actions[0].title.toString())
        app.engine.finish()
        app.engine.manualStart()
        val newSession = Notifications.current(app)
        assertNotNull(newSession.contentView)
        assertEquals("Pause",newSession.actions[0].title.toString())
    }

    @Test fun schedulerRecoveryKeepsOriginalDeadlineAndIndependentSoundWithoutActivity() = runBlocking {
        org.robolectric.Shadows.shadowOf(app.getSystemService(android.os.PowerManager::class.java)).setIsInteractive(false)
        wifi(true);val visit=app.db.focusDao().openVisit()!!;val timer=app.db.dao().active()!!.id
        time+=90*60000L;val exit=time;wifi(false)
        assertEquals(90000L,withTimeout(5000){waits.receive()})
        assertTrue("First 90-second confirmation must retain bounded wake protection",org.robolectric.shadows.ShadowPowerManager.getLatestWakeLock().isHeld)
        eventScope.coroutineContext[Job]!!.cancelAndJoin()
        time+=5*60000L
        eventScope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        events=BackgroundEvents(app,eventScope,{time},{connected},{refreshes++},{ms->waits.send(ms);proceed.receive()})
        events.recover()
        assertEquals(2,mode);assertTrue(app.db.dao().active()!!.pausedByDeparture)
        assertEquals(visit.id,app.db.focusDao().openVisit()!!.id)
        // Recovery settled sound immediately; the visit deadline stays exit+30m.
        settle(25*60000L);awaitClosed()
        val closed=app.db.focusDao().visits().first().single()
        assertEquals(exit,closed.end);assertEquals(true,closed.qualified)
        assertEquals(visit.id,closed.id);assertEquals(timer,app.db.dao().active()!!.id)
    }

    @Test fun pausedStudyStillConfirmsLibraryDepartureAfterWifiSoundAlreadySettled()=runBlocking {
        app.preferences.save(app.preferences.value.copy(geo=true))
        wifi(true);events.signal("Location",true);time+=600000
        app.engine.pauseResume()
        wifi(false);settle(90000)
        withTimeout(5000){while(mode!=2)delay(10)}
        time+=60000
        val cutoff=time
        events.signal("Location",false)
        assertEquals(90000L,withTimeout(5000){waits.receive()})
        val wake=org.robolectric.shadows.ShadowPowerManager.getLatestWakeLock();assertTrue(wake.isHeld)
        time+=90000;proceed.send(Unit)
        assertEquals(1710000L,withTimeout(5000){waits.receive()})
        assertFalse(wake.isHeld);assertEquals(cutoff,app.db.focusDao().openVisit()!!.awaySince)
        assertEquals(750000L,LibraryPresence.duration(app.db.focusDao().openVisit()!!,time))
        assertFalse(app.db.dao().active()!!.pausedByDeparture)
        time+=300000;wifi(true)
        assertNull(app.db.focusDao().openVisit()!!.awaySince)
        assertTrue(app.db.dao().active()!!.paused) // Explicit manual pause remains respected.
    }
}
