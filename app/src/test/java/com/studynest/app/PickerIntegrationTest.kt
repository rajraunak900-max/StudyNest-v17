package com.studynest.app

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.Config
import com.studynest.app.ui.WifiPicker
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = StudyNestApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PickerIntegrationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun availableNetworkSelectionPersistsWithoutTyping() {
        val app = ApplicationProvider.getApplicationContext<StudyNestApp>()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(app.getSystemService(LocationManager::class.java)).setLocationEnabled(true)
        val wifi = app.getSystemService(WifiManager::class.java)
        shadowOf(wifi).setWifiState(WifiManager.WIFI_STATE_ENABLED)
        shadowOf(wifi).setScanResults(listOf(ScanResult().apply { SSID = "Library_WiFi"; level = -40 }, ScanResult().apply { SSID = "Campus_WiFi"; level = -60 }))
        var selected = ""
        rule.setContent {
            MaterialTheme {
                WifiPicker(emptyList(), {}) { name ->
                    selected = name
                    app.preferences.save(Config(setup = true).withNetworks(listOf(name)))
                }
            }
        }
        rule.onNodeWithText("Library_WiFi").assertExists().performClick()
        rule.runOnIdle {
            assertEquals("Library_WiFi", selected)
            assertEquals(listOf("Library_WiFi"), app.preferences.value.networks())
        }
    }
    @Test fun adaptiveLauncherResourceRendersAttachedArtwork() {
        val app = ApplicationProvider.getApplicationContext<StudyNestApp>()
        val icon = app.getDrawable(R.mipmap.ic_launcher)!!
        assertTrue(icon is AdaptiveIconDrawable)
        assertNotNull((icon as AdaptiveIconDrawable).foreground)
        assertNotNull(icon.background)
        assertNotNull(icon.monochrome)
        assertTrue(app.getDrawable(R.mipmap.ic_launcher_round) is AdaptiveIconDrawable)
        val bitmap = Bitmap.createBitmap(432,432,Bitmap.Config.ARGB_8888)
        icon.setBounds(0,0,432,432)
        icon.draw(Canvas(bitmap))
        val file = java.io.File("build/reports/screenshots/launcher-icon.png")
        file.parentFile!!.mkdirs()
        java.io.FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        assertTrue(bitmap.getPixel(216,216) != 0)
    }
}
