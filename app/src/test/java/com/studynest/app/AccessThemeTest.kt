package com.studynest.app

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.Config
import com.studynest.app.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = StudyNestApp::class, qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessThemeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Test fun configuredAndAttentionButtonsBothNavigate() {
        val app = ApplicationProvider.getApplicationContext<StudyNestApp>()
        val location = app.getSystemService(android.location.LocationManager::class.java)
        rule.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { PermissionPanel(false, false) } } }
        for (state in listOf(false, true)) {
            Shadows.shadowOf(location).setLocationEnabled(state)
            rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.onNodeWithTag("grant-device-location").performScrollTo().assertIsEnabled()
                .assert(hasText(if (state) "Manage" else "Open Settings")).performClick()
            rule.runOnIdle { assertEquals(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS,
                Shadows.shadowOf(rule.activity).nextStartedActivity.action) }
        }
    }
    @Test fun systemThemeReactsAndExplicitThemesOverrideSystem() {
        val app = ApplicationProvider.getApplicationContext<StudyNestApp>()
        app.preferences.save(Config(setup = true, vibrate = false, theme = "System"))
        var configuration by mutableStateOf(Configuration(rule.activity.resources.configuration).apply { uiMode = Configuration.UI_MODE_NIGHT_NO })
        rule.setContent { CompositionLocalProvider(LocalConfiguration provides configuration) { StudyNest(app, refreshTimer = false) } }
        fun pixel(): Int {
            var value = 0
            rule.waitForIdle()
            rule.runOnIdle {
                val view = rule.activity.window.decorView
                val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap)); value = bitmap.getPixel(10, 10)
            }
            return value
        }
        val light = pixel()
        rule.runOnIdle { configuration = Configuration(configuration).apply { uiMode = Configuration.UI_MODE_NIGHT_YES } }
        val dark = pixel()
        assertNotEquals(light, dark)
        rule.runOnIdle { app.preferences.save(app.preferences.value.copy(theme = "Light")) }
        assertEquals(light, pixel())
        rule.runOnIdle { app.preferences.save(app.preferences.value.copy(theme = "Dark")); configuration = Configuration(configuration).apply { uiMode = Configuration.UI_MODE_NIGHT_NO } }
        assertEquals(dark, pixel())
    }
}
