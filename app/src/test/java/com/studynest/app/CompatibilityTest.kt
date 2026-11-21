package com.studynest.app

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.*
import com.studynest.app.data.Config
import com.studynest.app.domain.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/** JVM framework simulations, not evidence of radio/Doze delivery on devices. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = StudyNestApp::class)
abstract class CompatibilityCases {
    private lateinit var app: StudyNestApp
    private var time = 1790000000000L
    private var mode = 2

    @Before fun setup() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        withContext(Dispatchers.IO) { app.db.clearAllTables() }
        app.preferences.save(Config(setup = true, wifi = true, ssid = "Library", vibrate = true))
        app.engine = SessionEngine(app.db, object : StudyClock {
            override fun now() = time
            override fun elapsed() = time
            override fun boot() = 1
        }, { app.preferences.value }, object : SoundControl {
            override fun current() = mode
            override suspend fun set(mode: Int): Boolean { this@CompatibilityCases.mode = mode; return true }
        }, { fail(it) })
    }

    @After fun cleanup() { app.cancelSoundDeadline(); app.scope.cancel() }

    @Test fun backgroundLocationUsesTheCorrectPermissionGeneration() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        assertEquals(Build.VERSION.SDK_INT < 29, app.backgroundLocationAllowed())
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        assertTrue(app.backgroundLocationAllowed())
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertFalse(app.backgroundLocationAllowed())
        app.locationEnabled() // Exercises the API 26/27 provider fallback.
    }

    @Test fun notificationPermissionIsOnlyRequiredFromAndroid13() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(Build.VERSION.SDK_INT < 33, app.notificationsAllowed())
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(app.notificationsAllowed())
        val n = Notifications.notification(app, "Current session", true, false, 60000)
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertNotNull(n.contentIntent)
        assertEquals(2, n.actions.size)
    }

    @Test fun networkCallbackConstructionAndDeliveryWorkAcrossApiLevels() {
        var changed = 0
        var lost = 0
        val cb = wifiCallback({ _, _ -> changed++ }, { lost++ })
        val network = org.robolectric.shadows.ShadowNetwork.newInstance(42)
        cb.onCapabilitiesChanged(network, NetworkCapabilities())
        cb.onLost(network)
        assertEquals(1, changed)
        assertEquals(1, lost)
        assertEquals(Build.VERSION.SDK_INT >= 31,
            geofencePendingFlags() and PendingIntent.FLAG_MUTABLE != 0)
    }

    @Test fun timerServicePromotesWithoutLocationAndRegistersReceivers() = runBlocking {
        app.preferences.save(app.preferences.value.copy(wifi = false, geo = false, vibrate = false))
        val controller = Robolectric.buildService(StudyService::class.java).create()
        val service = controller.get()
        service.onStartCommand(android.content.Intent(app, StudyService::class.java).setAction("START"), 0, 1)
        withTimeout(10000) {
            while (app.db.dao().active() == null) {
                shadowOf(android.os.Looper.getMainLooper()).idle()
                delay(10)
            }
        }
        assertEquals("Manual", app.db.dao().active()!!.trigger)
        assertTrue(app.monitoring.value)
        assertNotNull(shadowOf(service).lastForegroundNotification)
        if (Build.VERSION.SDK_INT >= 34)
            assertEquals(android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE, service.foregroundServiceType)
        controller.destroy()
        Unit
    }

    @Test fun automaticDepartureStopsCountingWithoutAnActivityAndReturnResumes() = runBlocking {
        app.engine.signal("Wi-Fi", true)
        val session = app.db.dao().active()!!
        assertEquals(1, mode)
        time += 120000
        val exit = time
        app.engine.signal("Wi-Fi", false)
        time += SessionEngine.WIFI_SOUND_GRACE_MS
        app.engine.tick()
        assertEquals(2, mode)
        assertTrue(app.db.dao().active()!!.pausedByDeparture)
        assertNotNull(app.db.focusDao().openVisit());assertNotNull(app.db.dao().state()!!.exitAt)
        time += SessionEngine.WIFI_EXIT_GRACE_MS - SessionEngine.WIFI_SOUND_GRACE_MS
        app.engine.tick()
        assertTrue(app.db.dao().active()!!.pausedByDeparture)
        assertEquals(exit, app.db.dao().segmentsForSession(session.id).single().end)
        time += 600000
        app.engine.signal("Wi-Fi", true)
        assertEquals(session.id, app.db.dao().active()!!.id)
        assertFalse(app.db.dao().active()!!.paused)
        assertEquals(1, mode)
    }

    @Test fun manualTimerPauseFinishAndPreferencesPersist() = runBlocking {
        app.preferences.save(app.preferences.value.copy(wifi = false, geo = false, vibrate = false))
        app.engine.manualStart()
        val id = app.db.dao().active()!!.id
        time += 120000
        app.engine.pauseResume()
        time += 300000
        app.engine.pauseResume()
        time += 60000
        app.engine.finish()
        assertNull(app.db.dao().active())
        assertEquals(180000L, app.db.dao().segmentsForSession(id).sumOf { app.engine.range(it).let { r -> r.end - r.start } })
        app.preferences.save(app.preferences.value.copy(goalMinutes = 420, overrideMode = "RESTORE"))
        assertEquals(420, com.studynest.app.data.Preferences(app).value.goalMinutes)
        assertEquals("RESTORE", com.studynest.app.data.Preferences(app).value.overrideMode)
    }
}

@org.robolectric.annotation.Config(sdk = [26])
class CompatibilityApi26Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [27])
class CompatibilityApi27Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [28])
class CompatibilityApi28Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [29])
class CompatibilityApi29Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [30])
class CompatibilityApi30Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [31])
class CompatibilityApi31Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [32])
class CompatibilityApi32Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [33])
class CompatibilityApi33Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [34])
class CompatibilityApi34Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [35])
class CompatibilityApi35Test : CompatibilityCases()

@org.robolectric.annotation.Config(sdk = [36])
class CompatibilityApi36Test : CompatibilityCases()

