package com.studynest.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = android.app.Application::class)
class SoundAutomationTest {
    private lateinit var db: StudyDatabase
    private lateinit var engine: SessionEngine
    private var config = Config(wifi = true, geo = false, tracking = false)
    private var now = 1_800_000_000_000L
    private var reminders = 0
    private var fail = false
    private var mode = 2
    private val clock = object : StudyClock {
        override fun now() = now
        override fun elapsed() = now
        override fun boot() = 1
    }
    private val sound = object : SoundControl {
        override fun current() = mode
        override suspend fun set(value: Int): Boolean {
            if (fail) return false
            mode = value
            return true
        }
    }
    private fun makeEngine() = SessionEngine(db, clock, { config }, sound, {}, {}, { reminders++ })
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), StudyDatabase::class.java).build()
        engine = makeEngine()
    }
    @After fun close() = db.close()

    @Test fun wifiWinsOverStaleGeofenceAndRunningManualTimer() = runBlocking {
        config = config.copy(geo = true)
        engine.manualStart()
        engine.signal("Wi-Fi", true)
        engine.signal("Location", true)
        assertEquals(1, mode)
        engine.signal("Wi-Fi", false)
        now += 89999; engine.tick(); assertEquals(1, mode)
        now++; engine.tick(); assertEquals(2, mode)
        engine.signal("Location", true); assertEquals(2, mode)
        assertNotNull(db.dao().active())
    }
    @Test fun departureAlwaysNormalFromAllPriorModes() = runBlocking {
        for (previous in listOf(0, 1, 2)) {
            mode = previous
            engine.signal("Wi-Fi", true); assertEquals(1, mode)
            engine.signal("Wi-Fi", false); now += SessionEngine.WIFI_SOUND_GRACE_MS
            engine.tick(); assertEquals(2, mode)
        }
    }
    @Test fun briefDropAndSwitchingSavedNetworksRetainVibration() = runBlocking {
        config = config.withNetworks(listOf("A", "B", "C", "D"))
        for (name in config.networks()) {
            engine.signal("Wi-Fi", config.matchesSsid(name)); assertEquals(1, mode)
            engine.signal("Wi-Fi", false); now += 20000
            engine.tick(); assertEquals(1, mode)
        }
        engine.signal("Wi-Fi", true); now += SessionEngine.WIFI_SOUND_GRACE_MS
        engine.tick(); assertEquals(1, mode)
    }
    @Test fun disabledGeofenceEventsDoNothing() = runBlocking {
        config = config.copy(tracking = true)
        engine.signal("Location", true)
        assertNull(db.dao().active()); assertEquals(2, mode)
        engine.manualStart()
        val id = db.dao().active()!!.id
        engine.signal("Location", false); now += 100000; engine.tick()
        assertEquals(id, db.dao().active()!!.id)
        assertFalse(db.dao().active()!!.paused)
        engine.signal("Wi-Fi", true); assertEquals(1, mode)
    }
    @Test fun turningGeofenceOffDoesNotStopTimerOrWifi() = runBlocking {
        config = config.copy(geo = true, tracking = true)
        engine.signal("Location", true)
        val id = db.dao().active()!!.id
        config = config.copy(geo = false)
        engine.reconfigure()
        engine.signal("Location", false)
        assertEquals(id, db.dao().active()!!.id)
        assertFalse(db.dao().active()!!.paused)
        engine.signal("Wi-Fi", true); assertEquals(1, mode)
    }
    @Test fun restoreModeWaitsFullFiveMinutes() = runBlocking {
        config = config.copy(overrideMode = "RESTORE")
        engine.signal("Wi-Fi", true); mode = 2; engine.tick()
        now += 299999; engine.tick(); assertEquals(2, mode)
        now++; engine.tick(); assertEquals(1, mode)
        assertEquals(0, reminders)
    }
    @Test fun notifyModeRemindsOnceAcrossRestartAndLongWait() = runBlocking {
        engine.signal("Wi-Fi", true); mode = 2; engine.tick()
        assertEquals(1, reminders)
        engine = makeEngine()
        engine.recover(true)
        repeat(20) { now += 60000; engine.tick() }
        assertEquals(1, reminders); assertEquals(2, mode)
    }
    @Test fun manualVibrationResetsOneTimeEvent() = runBlocking {
        engine.signal("Wi-Fi", true); mode = 2; engine.tick()
        mode = 1; engine.tick(); mode = 2; engine.tick()
        assertEquals(2, reminders)
    }
    @Test fun appChangesNeverTriggerOverrideReminder() = runBlocking {
        engine.signal("Wi-Fi", true)
        repeat(3) { engine.tick() }
        engine.signal("Wi-Fi", false); now += SessionEngine.WIFI_SOUND_GRACE_MS; engine.tick()
        assertEquals(0, reminders)
    }
    @Test fun leaveCancelsFiveMinuteRestoreDespiteStaleGeofence() = runBlocking {
        config = config.copy(geo = true, overrideMode = "RESTORE")
        engine.signal("Wi-Fi", true); engine.signal("Location", true)
        mode = 2; engine.tick(); now += 60000
        engine.signal("Wi-Fi", false); now += SessionEngine.WIFI_SOUND_GRACE_MS; engine.tick()
        now += 600000; engine.tick()
        assertEquals(2, mode); assertNull(db.dao().state()!!.overrideAt)
    }
    @Test fun manualVibrationCancelsFiveMinuteRestore() = runBlocking {
        config = config.copy(overrideMode = "RESTORE")
        engine.signal("Wi-Fi", true); mode = 2; engine.tick()
        mode = 1; engine.tick()
        assertNull(db.dao().state()!!.overrideAt)
        now += 300000; engine.tick(); assertEquals(1, mode)
    }
    @Test fun departureResetsReminderAndReentryRearms() = runBlocking {
        engine.signal("Wi-Fi", true); mode = 2; engine.tick()
        engine.signal("Wi-Fi", false); now += SessionEngine.WIFI_SOUND_GRACE_MS; engine.tick()
        engine.signal("Wi-Fi", true); mode = 2; engine.tick()
        assertEquals(2, reminders)
    }
    @Test fun failedNormalRequestRetriesAfterPermissionReturns() = runBlocking {
        engine.signal("Wi-Fi", true); engine.signal("Wi-Fi", false)
        fail = true; now += SessionEngine.WIFI_SOUND_GRACE_MS; engine.tick()
        assertTrue(db.dao().state()!!.normalSoundPending)
        fail = false; engine.tick()
        assertEquals(2, mode); assertFalse(db.dao().state()!!.normalSoundPending)
    }
    @Test fun pendingDepartureSurvivesRestart() = runBlocking {
        engine.signal("Wi-Fi", true); engine.signal("Wi-Fi", false)
        now += 10000; engine = makeEngine(); engine.recover(true)
        assertEquals(1, mode)
        now += SessionEngine.WIFI_SOUND_GRACE_MS-10000; engine.tick(); assertEquals(2, mode)
    }
    @Test fun briefWifiHandoffPreservesFiveMinuteDeadline() = runBlocking {
        config = config.copy(overrideMode = "RESTORE")
        engine.signal("Wi-Fi", true); mode = 2; engine.tick()
        now += 60000; engine.signal("Wi-Fi", false)
        now += 10000; engine.signal("Wi-Fi", true)
        now += 229999; engine.tick(); assertEquals(2, mode)
        now++; engine.tick(); assertEquals(1, mode)
    }
    @Test fun geofenceOnlyModeWorksWhenWifiIsDisabled() = runBlocking {
        config = config.copy(wifi = false, geo = true)
        engine.signal("Location", true); assertEquals(1, mode)
        engine.signal("Location", false); now += 90000; engine.tick()
        assertEquals(2, mode)
    }

    @Test fun geoOnlyRestoresAfterFiveMinutes() = runBlocking {
        config = config.copy(wifi = false, geo = true, overrideMode = "RESTORE")
        engine.signal("Location", true)
        mode = 2; engine.tick()
        now += 299999; engine.tick(); assertEquals(2, mode)
        now++; engine.tick(); assertEquals(1, mode)
        assertEquals(0, reminders)
    }
    @Test fun geoOnlyReminderIsOnceAndDepartureCancels() = runBlocking {
        config = config.copy(wifi = false, geo = true, overrideMode = "NOTIFY")
        engine.signal("Location", true)
        mode = 2; engine.tick(); assertEquals(1, reminders)
        now += 600000; engine.tick(); assertEquals(1, reminders); assertEquals(2, mode)
        engine.signal("Location", false); now += 90000; engine.tick()
        assertNull(db.dao().state()!!.overrideAt)
    }
    @Test fun silentThenSoundStartsARealOverrideDeadline() = runBlocking {
        config = config.copy(overrideMode = "RESTORE")
        engine.signal("Wi-Fi", true)
        mode = 0; engine.tick(); now += 600000; engine.tick(); assertEquals(0, mode)
        mode = 2; engine.tick(); assertNotNull(db.dao().state()!!.overrideAt)
        now += 300000; engine.tick(); assertEquals(1, mode)
    }
}
