package com.studynest.app

import android.Manifest
import android.app.AppOpsManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.ui.StudyNest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=StudyNestApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class OnboardingUiCases {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    protected open val fontScale=1f
    private lateinit var app:StudyNestApp
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        app.preferences.save(Config(vibrate=false,theme="Light"))
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION,Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(app.getSystemService(AppOpsManager::class.java)).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),app.packageName,AppOpsManager.MODE_IGNORED)
    }
    @After fun close() {
        rule.runOnIdle { rule.activity.setContent {} };rule.waitForIdle()
        // The Compose rule disposes its owned content after @After returns. Room
        // collectors can still run until then; let Robolectric reset the database
        // after the rule rather than closing it underneath those collectors.
        app.scope.cancel()
    }
    private fun content() { rule.setContent {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,fontScale)) {
            StudyNest(app,refreshTimer=false)
        }
    } }
    private fun skip() { rule.onNodeWithTag("setup-skip").assertIsDisplayed().performClick() }
    private fun allSkip() {
        repeat(5) { index ->
            rule.onNodeWithText("${index+1} of 5").assertExists()
            rule.onNodeWithTag("setup-continue").assertIsDisplayed()
            capture("step-${index+1}-light")
            skip()
        }
        rule.waitUntil(15000) { app.preferences.value.setup }
        rule.onNodeWithTag("onboarding").assertDoesNotExist()
    }
    private fun availableNetwork() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(app.getSystemService(LocationManager::class.java)).setLocationEnabled(true)
        val wifi=app.getSystemService(WifiManager::class.java)
        shadowOf(wifi).setWifiState(WifiManager.WIFI_STATE_ENABLED)
        shadowOf(wifi).setScanResults(listOf(ScanResult().apply { SSID="Library_WiFi";level=-40 }))
    }
    @Test fun skippingEveryStepOpensHomeAndLibraryCanBeConfiguredLater()=runBlocking {
        content();allSkip()
        assertFalse(app.preferences.value.wifi);assertFalse(app.preferences.value.geo)
        rule.onNodeWithText("Start manual timer").assertIsEnabled()
        rule.onNodeWithText("Tasks",useUnmergedTree=true).performClick()
        rule.waitUntil(15000) { rule.onAllNodesWithText("Add task").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Add task").assertExists()
        rule.onNodeWithText("History",useUnmergedTree=true).performClick();rule.onNodeWithText("Study hours").assertExists()
        rule.onNodeWithText("Home",useUnmergedTree=true).performClick()
        rule.onNodeWithText("Library Wi-Fi not configured").assertExists()
        availableNetwork()
        rule.onNodeWithText("Set up").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Enable Wi-Fi detection").performClick()
        rule.onNodeWithText("Add Wi-Fi Network").performScrollTo().performClick()
        rule.onNodeWithText("Library_WiFi").performClick()
        rule.onNodeWithText("Save configuration").performScrollTo().performClick()
        rule.waitUntil(15000) { app.preferences.value.networks()==listOf("Library_WiFi") }
        assertTrue(app.preferences.value.wifi)
        assertEquals(300,app.preferences.value.goalMinutes)
    }
    @Test fun configuringAvailableWifiReachesHomeAndDraftSavesOnlyAtCompletion() {
        availableNetwork();content()
        rule.onNodeWithTag("setup-continue").performClick()
        rule.onNodeWithText("Choose Wi-Fi Network").performScrollTo().performClick()
        rule.onNodeWithText("Library_WiFi").performClick()
        assertTrue(app.preferences.value.networks().isEmpty())
        rule.onNodeWithTag("setup-continue").performClick()
        skip();skip();rule.onNodeWithTag("setup-continue").performClick()
        rule.waitUntil(15000) { app.preferences.value.setup }
        assertTrue(app.preferences.value.wifi);assertFalse(app.preferences.value.geo)
        assertEquals(listOf("Library_WiFi"),Preferences(app).value.networks())
        rule.onNodeWithTag("today-study-summary").assertExists()
    }
    @Test fun skippedUsageAccessCanBeGrantedLaterAndRefreshesOnReturn() {
        content();allSkip()
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        rule.onNodeWithText("Permissions & System Access").performScrollTo().performClick()
        rule.onNodeWithTag("grant-usage").performScrollTo().performClick()
        val intent=shadowOf(rule.activity).nextStartedActivity
        assertEquals(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS,intent.action)
        assertEquals("package:${app.packageName}",intent.data.toString())
        shadowOf(app.getSystemService(AppOpsManager::class.java)).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),app.packageName,AppOpsManager.MODE_ALLOWED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.onNode(hasText("Granted ✓") and hasAnyAncestor(hasTestTag("access-usage"))).assertExists()
        rule.onNodeWithTag("grant-usage").assert(hasText("Manage"))
        capture("permissions-granted-light")
        app.preferences.save(app.preferences.value.copy(theme="Dark"));rule.waitForIdle();capture("permissions-granted-dark")
        assertFalse(app.preferences.value.appMonitoring)
    }
    @Test fun configuredUpgradeSkipsOnboardingAndPreservesExistingData()=runBlocking {
        val saved=Config(setup=true,vibrate=false,theme="Dark",goalMinutes=450,streakMinutes=90,
            ssid="ExistingLibrary",ssids=listOf("ExistingLibrary"),monitoredApps=setOf("com.example.fun"))
        app.preferences.save(saved)
        app.db.dao().saveTask(StudyTask(title="Saved task"))
        val now=System.currentTimeMillis()
        val session=app.db.dao().insertSession(StudySession(startedAt=now-60000,endedAt=now,trigger="Manual"))
        app.db.dao().insertSegment(Segment(sessionId=session,start=now-60000,end=now,elapsedStart=0,boot=1))
        content()
        rule.onNodeWithTag("onboarding").assertDoesNotExist()
        rule.onNodeWithTag("today-study-summary").assertExists()
        rule.onNodeWithText("Tasks",useUnmergedTree=true).performClick();rule.onNodeWithText("Saved task").assertExists()
        assertEquals(saved,Preferences(app).value)
        assertEquals(1,app.db.dao().sessions().first().size)
        assertEquals(1,app.db.dao().segments().first().size)
    }
    private fun capture(name:String) { rule.runOnIdle {
        val view=rule.activity.window.decorView
        val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val file=java.io.File("build/reports/screenshots/${javaClass.simpleName}-$name.png");file.parentFile!!.mkdirs()
        java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
    } }
}
@org.robolectric.annotation.Config(qualifiers="w411dp-h891dp-xhdpi")
class OnboardingPhoneTest:OnboardingUiCases()
@org.robolectric.annotation.Config(qualifiers="w320dp-h640dp-mdpi")
class OnboardingSmallPhoneTest:OnboardingUiCases() { override val fontScale=1.5f }
@org.robolectric.annotation.Config(qualifiers="w600dp-h960dp-mdpi")
class OnboardingLargeScreenTest:OnboardingUiCases()
