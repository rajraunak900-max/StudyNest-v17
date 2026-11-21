package com.studynest.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.*

/** Persisted-engine tests: no Activity, midnight alarm or real-phone claim. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = android.app.Application::class)
class TimerRolloverTest {
    private lateinit var db: StudyDatabase
    private lateinit var engine: SessionEngine
    private lateinit var context: Context
    private var config = Config(wifi = true, geo = true, vibrate = false)
    private val day = LocalDate.of(2026, 10, 2)
    private val clock = object : StudyClock {
        var zoneId = ZoneId.of("Asia/Kolkata")
        var wall = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 60 * 60000
        var uptime = 10000000L
        override fun now() = wall
        override fun elapsed() = uptime
        override fun boot() = 1
        override fun zone() = zoneId
        fun advance(ms: Long) { wall += ms; uptime += ms }
    }
    private val midnight get() = day.plusDays(1).atStartOfDay(clock.zoneId).toInstant().toEpochMilli()
    private fun makeEngine() = SessionEngine(db, clock, { config }, object : SoundControl {
        override fun current() = 2
        override suspend fun set(mode: Int) = true
    }, { fail(it) })
    private fun openDatabase() = Room.databaseBuilder(context, StudyDatabase::class.java, "rollover-164")
        .allowMainThreadQueries().build()
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("rollover-164")
        db = openDatabase(); engine = makeEngine()
    }
    @After fun close() { db.close(); context.deleteDatabase("rollover-164") }
    private suspend fun sessionTime(id: Long) = db.dao().segmentsForSession(id)
        .sumOf { engine.range(it).let { r -> r.end - r.start } }
    private suspend fun primaryClock(): String {
        val active = db.dao().active()
        return StudyMath.clock(if (active == null) 0 else sessionTime(active.id))
    }
    private suspend fun departBeforeMidnight(): Long {
        engine.signal("Wi-Fi", true)
        val id = db.dao().active()!!.id
        clock.advance(5 * 60000)
        engine.signal("Wi-Fi", false)
        clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS); engine.tick()
        val startedDay=Instant.ofEpochMilli(db.dao().sessions().first().single {it.id==id}.startedAt).atZone(clock.zone()).toLocalDate()
        val currentDay=Instant.ofEpochMilli(clock.wall).atZone(clock.zone()).toLocalDate()
        if(startedDay==currentDay) assertTrue(db.dao().active()!!.pausedByDeparture)
        else assertNull(db.dao().active())
        return id
    }

    @Test fun A_departedOldValueClearsAtMidnightWithoutDeletingSession() = runBlocking {
        val id = departBeforeMidnight()
        assertEquals("00:05:00", primaryClock())
        clock.advance(midnight - clock.wall)
        engine.tick()
        assertEquals("00:00:00", primaryClock())
        assertNull(db.dao().active())
        assertEquals(id, db.dao().sessions().first().single().id)
        assertEquals(5 * 60000L, sessionTime(id))
    }
    @Test fun B_wifiSessionContinuesThroughExactMidnight() = runBlocking {
        clock.wall = midnight - 150 * 60000
        engine.signal("Wi-Fi", true); val id = db.dao().active()!!.id
        clock.advance(150 * 60000 - 1000); engine.tick()
        assertEquals("02:29:59", primaryClock())
        clock.advance(1000); engine.tick()
        assertEquals("02:30:00", primaryClock())
        clock.advance(1000); engine.tick()
        assertEquals("02:30:01", primaryClock())
        assertEquals(id, db.dao().active()!!.id)
        assertFalse(db.dao().active()!!.paused)
    }
    @Test fun C_geofenceAlonePreservesContinuousSession() = runBlocking {
        engine.signal("Location", true); val id = db.dao().active()!!.id
        engine.signal("Wi-Fi", false)
        clock.advance(31 * 60000); engine.tick()
        assertEquals("00:31:00", primaryClock())
        assertEquals(id, db.dao().active()!!.id)
        assertTrue(db.dao().state()!!.geoInside)
    }
    @Test fun D_1130To0030KeepsOneHourSessionAndSplitsHistory() = runBlocking {
        clock.wall = midnight - 30 * 60000
        engine.signal("Wi-Fi", true); val id = db.dao().active()!!.id
        clock.advance(60 * 60000); engine.tick()
        assertEquals("01:00:00", primaryClock())
        val totals = StudyMath.dailyTotals(db.dao().segments().first().map(engine::range), clock.zone())
        assertEquals(30 * 60000L, totals[day]); assertEquals(30 * 60000L, totals[day.plusDays(1)])
        val windows = db.focusDao().windows().first()
        val visits = db.focusDao().visits().first()
        fun summary(from: Long, until: Long) = FocusMath.summary(visits, windows, emptyList(), clock.wall, TimeRange(from, until))
        val old = summary(midnight - 30 * 60000, midnight)
        val next = summary(midnight, clock.wall)
        assertEquals(30 * 60000L, old.libraryMs); assertEquals(old.libraryMs, next.libraryMs)
        assertEquals(30 * 60000L, old.studyMs); assertEquals(old.studyMs, next.studyMs)
        engine.signal("Wi-Fi", false)
        clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS); engine.tick()
        assertNull(db.dao().active()); assertEquals("00:00:00", primaryClock())
        assertEquals(60 * 60000L, sessionTime(id))
    }
    @Test fun E_closedDatabaseReopensAfterSeveralMidnightsAndReconcilesWithoutUI() = runBlocking {
        val id = departBeforeMidnight()
        db.close(); clock.advance(3 * 86400000L)
        db = openDatabase(); engine = makeEngine(); engine.recover(true)
        assertNull(db.dao().active()); assertEquals("00:00:00", primaryClock())
        assertEquals(5 * 60000L, sessionTime(id))
        engine.signal("Wi-Fi", true)
        assertNotEquals(id, db.dao().active()!!.id)
    }
    @Test fun F_closedMetricsBreaksTasksAndStreaksStayIdenticalAfterReset() = runBlocking {
        clock.wall = midnight - 4 * 3600000
        engine.signal("Wi-Fi", true); val id = db.dao().active()!!.id
        clock.advance(60 * 60000); engine.pauseResume()
        clock.advance(10 * 60000); engine.pauseResume()
        clock.advance(60 * 60000); engine.signal("Wi-Fi", false)
        clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS); engine.tick()
        val windows = db.focusDao().windows().first()
        db.focusDao().insertPeriods(listOf(DistractionPeriod(windowId = windows.first().id,
            start = windows.first().start, end = windows.first().start + 5 * 60000, reason = "APP", packageName = "selected.app")))
        db.dao().saveTask(StudyTask(title = "Keep task", priority = 2, dueDate = day.toString()))
        val segments = db.dao().segments().first()
        val visits = db.focusDao().visits().first()
        val periods = db.focusDao().distractions().first()
        val breaks = db.breakDao().records().first()
        val tasks = db.dao().tasks().first(); val coverage = db.breakDao().tracking()
        val totals = StudyMath.dailyTotals(segments.map(engine::range), clock.zone())
        val summary = FocusMath.summary(visits, windows, periods, clock.wall, breaks = breaks)
        assertEquals(10 * 60000L, summary.breakMs); assertEquals(5 * 60000L, summary.distractionMs)
        assertEquals(115 * 60000L, summary.libraryStudyMs)
        val streak = StudyMath.streaks(totals, 120 * 60000L, day.plusDays(1))
        clock.advance(midnight - clock.wall); engine.tick()
        assertNull(db.dao().active())
        assertEquals(segments, db.dao().segments().first())
        assertEquals(visits, db.focusDao().visits().first()); assertEquals(windows, db.focusDao().windows().first())
        assertEquals(periods, db.focusDao().distractions().first()); assertEquals(breaks, db.breakDao().records().first())
        assertEquals(tasks, db.dao().tasks().first()); assertEquals(coverage, db.breakDao().tracking())
        assertEquals(totals, StudyMath.dailyTotals(db.dao().segments().first().map(engine::range), clock.zone()))
        assertEquals(summary, FocusMath.summary(db.focusDao().visits().first(), db.focusDao().windows().first(),
            db.focusDao().distractions().first(), clock.wall, breaks = db.breakDao().records().first()))
        assertEquals(summary.efficiency(summary.libraryMs), FocusMath.summary(visits, windows, periods, clock.wall, breaks = breaks).efficiency(summary.libraryMs))
        assertEquals(streak, StudyMath.streaks(totals, 120 * 60000L, day.plusDays(1)))
        assertEquals(120 * 60000L, sessionTime(id))
    }
    @Test fun sameDayDepartureReturnStillResumesTheSameSession() = runBlocking {
        val id = departBeforeMidnight(); engine.signal("Wi-Fi", true)
        assertEquals(id, db.dao().active()!!.id); assertFalse(db.dao().active()!!.paused)
        assertEquals("00:05:00", primaryClock())
    }
    @Test fun arrivalAfterMidnightRetiresYesterdaysPausedSessionBeforeStartingNewOne() = runBlocking {
        val id = departBeforeMidnight(); clock.advance(midnight - clock.wall + 60000)
        engine.signal("Wi-Fi", true)
        assertNotEquals(id, db.dao().active()!!.id); assertEquals("00:00:00", primaryClock())
        assertEquals(2, db.dao().sessions().first().size); assertEquals(5 * 60000L, sessionTime(id))
    }
    @Test fun pendingDepartureAtMidnightHonoursGraceAndReconnection() = runBlocking {
        clock.wall = midnight - 60000; engine.signal("Wi-Fi", true)
        val id = db.dao().active()!!.id
        clock.advance(50000); engine.signal("Wi-Fi", false)
        clock.advance(20000); engine.tick()
        assertEquals(id, db.dao().active()!!.id); assertFalse(db.dao().active()!!.paused)
        engine.signal("Wi-Fi", true); engine.tick()
        assertEquals(id, db.dao().active()!!.id); assertEquals("00:01:10", primaryClock())
    }
    @Test fun pendingDepartureConfirmedAfterMidnightClosesAtOriginalExit() = runBlocking {
        clock.wall = midnight - 60000; engine.signal("Wi-Fi", true)
        val id = db.dao().active()!!.id
        clock.advance(50000); engine.signal("Wi-Fi", false)
        clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS); engine.tick()
        assertNull(db.dao().active()); assertEquals(50000L, sessionTime(id))
    }
    @Test fun delayedArrivalAfterMidnightSettlesExpiredDepartureBeforeResuming() = runBlocking {
        clock.wall = midnight - 60000; engine.signal("Wi-Fi", true)
        val id = db.dao().active()!!.id
        clock.advance(50000); engine.signal("Wi-Fi", false)
        clock.advance(30 * 60000) // Android did not execute the confirmation at its deadline.
        engine.signal("Wi-Fi", true)
        assertNotEquals(id, db.dao().active()!!.id)
        assertEquals(50000L, sessionTime(id)); assertEquals("00:00:00", primaryClock())
    }
    @Test fun automaticUserPauseInsideSurvivesMidnightUntilDeparture() = runBlocking {
        clock.wall=midnight-30*60000
        engine.signal("Wi-Fi", true); clock.advance(60000); engine.pauseResume()
        val id = db.dao().active()!!.id
        clock.advance(30 * 60000); engine.tick()
        assertEquals(id, db.dao().active()!!.id); assertTrue(db.dao().active()!!.paused)
        assertEquals("00:01:00", primaryClock())
        engine.signal("Wi-Fi", false); clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS); engine.tick()
        assertNull(db.dao().active()); assertEquals(60000L, sessionTime(id))
    }
    @Test fun manualRunningOutsideContinuesAcrossMultipleDaysAndProcessRecovery() = runBlocking {
        engine.manualStart(); val id = db.dao().active()!!.id
        clock.advance(2 * 86400000L); engine.tick(); engine = makeEngine(); engine.recover(true)
        assertEquals(id, db.dao().active()!!.id); assertFalse(db.dao().active()!!.paused)
        assertEquals(2 * 86400000L, sessionTime(id))
    }
    @Test fun manualPausedOutsideRemainsAvailableForExplicitResume() = runBlocking {
        engine.manualStart(); clock.advance(60000); engine.pauseResume()
        val id = db.dao().active()!!.id
        clock.advance(86400000L); engine.tick(); engine.reconcileDay()
        assertEquals(id, db.dao().active()!!.id); assertTrue(db.dao().active()!!.paused)
        engine.pauseResume(); clock.advance(60000)
        assertEquals(120000L, sessionTime(id))
    }
    @Test fun repeatedDailyTransitionsAreIdempotentAndNeverDisableAutomaticTracking() = runBlocking {
        repeat(4) {
            engine.signal("Wi-Fi", true); val id = db.dao().active()!!.id
            clock.advance(60000); engine.signal("Wi-Fi", false)
            clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS); engine.tick()
            val nextDay = Instant.ofEpochMilli(clock.wall).atZone(clock.zone()).toLocalDate().plusDays(1)
            clock.advance(nextDay.atStartOfDay(clock.zone()).toInstant().toEpochMilli() - clock.wall)
            assertTrue(engine.reconcileDay()); assertFalse(engine.reconcileDay())
            assertEquals("00:00:00", primaryClock()); assertEquals(60000L, sessionTime(id))
            assertTrue(config.tracking); assertFalse(db.dao().state()!!.suppressed)
        }
        assertEquals(4, db.dao().sessions().first().size)
    }
    @Test fun timezoneChangeUsesCurrentDeviceDateWithoutTouchingElapsedHistory() = runBlocking {
        clock.zoneId = ZoneId.of("UTC")
        clock.wall = day.atTime(18, 25).toInstant(ZoneOffset.UTC).toEpochMilli()
        val id = departBeforeMidnight()
        clock.zoneId = ZoneId.of("Asia/Kolkata") // The saved session now crosses local midnight; elapsed time is unchanged.
        assertTrue(engine.reconcileDay()); assertEquals(5 * 60000L, sessionTime(id))
    }
    @Test fun backwardDateCorrectionDoesNotDestroyManualOrPresentAutomaticSession() = runBlocking {
        engine.signal("Wi-Fi", true); val id = db.dao().active()!!.id
        clock.advance(60000); clock.wall -= 86400000L; engine.tick()
        assertEquals(id, db.dao().active()!!.id); assertEquals(60000L, sessionTime(id))
        engine.finish(); engine.manualStart(); val manual = db.dao().active()!!.id
        clock.advance(60000); clock.wall -= 86400000L; engine.tick()
        assertEquals(manual, db.dao().active()!!.id); assertEquals(60000L, sessionTime(manual))
    }
    @Test fun staleOutsideOpenAutomaticSegmentEndsAtCheckpointWithoutInventedTime() = runBlocking {
        engine.signal("Wi-Fi", true); val id = db.dao().active()!!.id
        clock.advance(60000); engine.tick()
        db.dao().saveState(db.dao().state()!!.copy(wifiInside = false, geoInside = false, exitAt = null))
        clock.advance(86400000L); engine.tick()
        assertNull(db.dao().active()); assertEquals(60000L, sessionTime(id))
    }
    @Test fun disabledSourceDoesNotCountAsLibraryPresenceAtRollover() = runBlocking {
        engine.signal("Location", true); val id = db.dao().active()!!.id
        clock.advance(60000); engine.pauseResume(); config = config.copy(geo = false)
        clock.advance(86400000L); engine.tick()
        assertNull(db.dao().active()); assertEquals(60000L, sessionTime(id))
    }
    @Test fun springAndAutumnDeviceMidnightsDoNotAssumeTwentyFourHours() = runBlocking {
        clock.zoneId = ZoneId.of("America/New_York")
        for (date in listOf(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 11, 1))) {
            clock.wall = date.atTime(23, 50).atZone(clock.zone()).toInstant().toEpochMilli()
            val id = departBeforeMidnight()
            clock.advance(10 * 60000); engine.tick()
            assertNull(db.dao().active()); assertEquals(5 * 60000L, sessionTime(id))
        }
    }
}
