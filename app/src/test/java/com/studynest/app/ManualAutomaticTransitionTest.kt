package com.studynest.app

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = Application::class)
class ManualAutomaticTransitionTest {
    private lateinit var db: StudyDatabase
    private lateinit var engine: SessionEngine
    private var config = Config(setup = true, wifi = true, geo = true, tracking = true, vibrate = false)
    private var now = LocalDate.of(2026, 10, 9).atTime(8, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private var elapsed = 0L
    private val clock = object : StudyClock {
        override fun now() = now
        override fun elapsed() = elapsed
        override fun boot() = 1
        override fun zone() = ZoneOffset.UTC
    }

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), StudyDatabase::class.java)
            .allowMainThreadQueries().build()
        engine = SessionEngine(db, clock, { config }, object : SoundControl {
            override fun current() = 2
            override suspend fun set(mode: Int) = true
        }, { fail(it) })
    }

    @After fun close() = db.close()
    private fun advance(ms: Long) { now += ms; elapsed += ms }
    private suspend fun actual() = FocusMath.summary(
        db.focusDao().visits().first(), db.focusDao().windows().first(), emptyList(), now).studyMs

    @Test fun manualHomeStudyTransitionsIntoSameDetectedLibrarySession() = runBlocking {
        config = config.copy(geo = false)
        engine.manualStart()
        val id = db.dao().active()!!.id
        advance(60 * 60_000L)
        engine.signal("Wi-Fi", true)
        assertEquals(id, db.dao().active()!!.id)
        assertEquals(SessionEngine.MANUAL_AUTO_TRIGGER, db.dao().active()!!.trigger)
        assertEquals(1, db.dao().sessions().first().size)
        advance(30 * 60_000L)
        engine.finish()
        val summary = FocusMath.summary(db.focusDao().visits().first(), db.focusDao().windows().first(), emptyList(), now)
        assertEquals(90 * 60_000L, summary.studyMs)
        assertEquals(30 * 60_000L, summary.libraryMs)
        assertEquals(30 * 60_000L, summary.libraryStudyMs)
    }

    @Test fun disabledAutomaticTrackingLeavesManualSessionUnchangedOnLibraryEntry() = runBlocking {
        config = config.copy(tracking = false, geo = false)
        engine.manualStart()
        val id = db.dao().active()!!.id
        advance(60_000)
        engine.signal("Wi-Fi", true)
        assertEquals(id, db.dao().active()!!.id)
        assertEquals("Manual", db.dao().active()!!.trigger)
        assertFalse(db.dao().active()!!.paused)
    }

    @Test fun pausedManualSessionIsAdoptedButNeverAutomaticallyResumed() = runBlocking {
        config = config.copy(geo = false)
        engine.manualStart()
        engine.pauseResume()
        engine.signal("Wi-Fi", true)
        assertEquals(SessionEngine.MANUAL_AUTO_TRIGGER, db.dao().active()!!.trigger)
        assertTrue(db.dao().active()!!.paused)
        assertFalse(db.dao().active()!!.pausedByDeparture)
    }

    @Test fun manuallyStartedInsideLibraryPausesOnExitAndReturnsAsNewVisitAfterTwoHours() = runBlocking {
        // The automatic visit can already be present while its timer is stopped.
        engine.signal("Wi-Fi", true)
        engine.finish()
        engine.manualStart()
        val first = db.dao().active()!!
        assertEquals(SessionEngine.MANUAL_AUTO_TRIGGER, first.trigger)

        advance(60 * 60_000L)
        val departure = now
        engine.signal("Wi-Fi", false)
        advance(2 * 60 * 60_000L)
        engine.tick()
        val firstClosed = db.dao().sessions().first().single { it.id == first.id }
        assertEquals(departure, firstClosed.endedAt)
        assertFalse(firstClosed.paused)

        engine.signal("Wi-Fi", true)
        val second = db.dao().active()!!
        assertNotEquals(first.id, second.id)
        assertEquals("Wi-Fi", second.trigger)
        advance(2 * 60 * 60_000L)
        engine.finish()

        val summary = FocusMath.summary(db.focusDao().visits().first(), db.focusDao().windows().first(), emptyList(), now)
        assertEquals(3 * 60 * 60_000L, summary.studyMs)
        assertEquals(3 * 60 * 60_000L, summary.libraryMs)
        assertEquals(3 * 60 * 60_000L, summary.libraryStudyMs)
        assertEquals(0L, summary.studyMs - summary.libraryStudyMs)
    }

    @Test fun reentryWithinWifiGraceResumesSameManualAutoSessionAndVisit() = runBlocking {
        engine.signal("Wi-Fi", true)
        engine.finish()
        engine.manualStart()
        val sessionId = db.dao().active()!!.id
        val visitId = db.focusDao().openVisit()!!.id
        advance(60 * 60_000L)
        engine.signal("Wi-Fi", false)
        advance(SessionEngine.EXIT_GRACE_MS)
        engine.tick()
        assertTrue(db.dao().active()!!.pausedByDeparture)
        advance(10 * 60_000L)
        engine.signal("Wi-Fi", true)
        assertEquals(sessionId, db.dao().active()!!.id)
        assertEquals(visitId, db.focusDao().openVisit()!!.id)
        assertFalse(db.dao().active()!!.paused)
        advance(10 * 60_000L)
        engine.finish()
        val summary = FocusMath.summary(db.focusDao().visits().first(), db.focusDao().windows().first(), emptyList(), now)
        assertEquals(70 * 60_000L, summary.studyMs)
        assertEquals(70 * 60_000L, summary.libraryMs)
        assertEquals(70 * 60_000L, summary.libraryStudyMs)
        assertEquals(1, db.dao().sessions().first().count { it.trigger == SessionEngine.MANUAL_AUTO_TRIGGER })
    }

    @Test fun userPauseIsNotClearedByLibraryReentry() = runBlocking {
        engine.signal("Wi-Fi", true)
        engine.finish()
        engine.manualStart()
        val sessionId = db.dao().active()!!.id
        advance(60 * 60_000L)
        engine.pauseResume()
        engine.signal("Wi-Fi", false)
        advance(SessionEngine.EXIT_GRACE_MS)
        engine.tick()
        assertTrue(db.dao().active()!!.paused)
        assertFalse(db.dao().active()!!.pausedByDeparture)
        advance(5 * 60_000L)
        engine.signal("Wi-Fi", true)
        assertEquals(sessionId, db.dao().active()!!.id)
        assertTrue(db.dao().active()!!.paused)
        assertFalse(db.dao().active()!!.pausedByDeparture)
    }

    @Test fun wifiDropCoveredByGeofenceDoesNotPauseAdoptedManualSession() = runBlocking {
        engine.signal("Wi-Fi", true)
        engine.finish()
        engine.manualStart()
        val id = db.dao().active()!!.id
        engine.signal("Location", true)
        engine.signal("Wi-Fi", false)
        advance(SessionEngine.EXIT_GRACE_MS + 1)
        engine.tick()
        assertEquals(id, db.dao().active()!!.id)
        assertFalse(db.dao().active()!!.paused)
        assertNull(db.dao().state()!!.exitAt)
    }
}
