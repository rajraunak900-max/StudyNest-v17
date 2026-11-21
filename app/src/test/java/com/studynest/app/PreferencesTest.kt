package com.studynest.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = android.app.Application::class)
class PreferencesTest {
    @Test fun legacyWifiMigratesAndFourNetworksPersist() {
        val c = ApplicationProvider.getApplicationContext<Context>()
        val old = c.getSharedPreferences("settings", Context.MODE_PRIVATE)
        old.edit().clear().putString("ssid", "Original Library").putBoolean("geo", false)
            .putInt("goal", 450).putString("theme", "Dark").commit()
        val prefs = Preferences(c)
        assertEquals(listOf("Original Library"), prefs.value.networks())
        prefs.save(prefs.value.withNetworks(listOf("Original Library", "Library 2", "Library 3", "Library 4"))
            .copy(overrideMode = "RESTORE"))
        val restored = Preferences(c).value
        listOf("Original Library", "Library 2", "Library 3", "Library 4").forEach { assertTrue(restored.matchesSsid(it)) }
        assertFalse(restored.matchesSsid("Home"))
        assertEquals(450, restored.goalMinutes)
        assertEquals("Dark", restored.theme)
        assertFalse(restored.geo)
        assertEquals("RESTORE", restored.overrideMode)
        prefs.save(restored.withNetworks(emptyList()))
        assertTrue(Preferences(c).value.networks().isEmpty())
    }
}
