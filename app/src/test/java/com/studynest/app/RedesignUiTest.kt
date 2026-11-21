package com.studynest.app

import android.Manifest
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import com.studynest.app.ui.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.time.*

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=StudyNestApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class RedesignUiCases {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    protected open val scale=1f
    private lateinit var app:StudyNestApp
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach {it.join()}
        app.preferences.save(Config(setup=true,vibrate=false,theme="Light"))
    }
    @After fun close() {
        rule.runOnIdle {rule.activity.setContent {}};rule.waitForIdle()
        // Compose-rule disposal follows @After. Robolectric resets Room after
        // disposal; closing it here races the rule-owned lifecycle collectors.
        app.scope.cancel()
    }
    private fun content() {rule.setContent {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)){StudyNest(app,refreshTimer=false)}
    }}
    private fun settings(name:String) {
        rule.onNode(hasText("Settings") and hasAnyAncestor(hasTestTag("main-navigation")),useUnmergedTree=true).performClick()
        rule.onNode(hasText(name) and hasClickAction()).performScrollTo().performClick()
    }
    private fun input(tag:String,value:String) {rule.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)}
    private fun capture(name:String) {rule.runOnIdle {
        val v=rule.activity.window.decorView
        val bitmap=android.graphics.Bitmap.createBitmap(v.width,v.height,android.graphics.Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bitmap))
        val file=java.io.File("build/reports/screenshots/${javaClass.simpleName}-$name.png");file.parentFile!!.mkdirs()
        java.io.FileOutputStream(file).use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }}
    private fun fits(tag:String) {
        val nodes=rule.onAllNodes(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes()
        assertTrue(nodes.isNotEmpty())
        rule.runOnIdle {nodes.forEach {node ->
            val results=mutableListOf<TextLayoutResult>();node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.forEach {r ->
                for(line in 0 until r.lineCount) assertTrue("Clipped ${r.layoutInput.text.text}",r.getLineRight(line)-r.getLineLeft(line)<=r.size.width+1f)
                assertFalse("Truncated ${r.layoutInput.text.text}",r.multiParagraph.didExceedMaxLines)
            }
        }}
    }
    @Test fun goalsPresetsCustomValuesAndInvalidInputsSaveOnlyTogether() {
        content();settings("Goals & streak")
        rule.onNodeWithTag("goal-hours-preset-360").performScrollTo().performClick()
        rule.onNodeWithTag("goal-minimum-preset-15").performScrollTo().performClick()
        input("goal-streak","225");input("goal-minimum","46");input("goal-hours","7.5")
        assertEquals(10,app.preferences.value.minimumSessionMinutes)
        input("goal-minimum","0")
        rule.onNodeWithTag("goals-save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Enter a whole number from 1 to 1440 minutes.").assertExists()
        input("goal-minimum","46");fits("goal-fields");capture("goals-custom-light")
        rule.onNodeWithTag("goals-save").performScrollTo().performClick()
        rule.waitUntil(10000){app.preferences.value.minimumSessionMinutes==46}
        assertEquals(450,Preferences(app).value.goalMinutes);assertEquals(225,Preferences(app).value.streakMinutes)
        settings("Goals & streak");rule.onNodeWithTag("goal-minimum").assertTextContains("46")
        app.preferences.save(app.preferences.value.copy(theme="Dark"));rule.waitForIdle()
        rule.onNodeWithTag("goal-minimum").performScrollTo();fits("goal-fields");capture("goals-dark")
    }
    @Test fun onboardingUsesTheSameCustomControlsAndSkipRemainsAvailable() {
        app.preferences.save(Config(vibrate=false,theme="Light"));content()
        repeat(4){rule.onNodeWithTag("setup-skip").performClick()}
        rule.onNodeWithText("Goal & Streak").assertExists()
        rule.onNodeWithTag("goal-minimum").performScrollTo().assertTextContains("10")
        input("goal-hours","6.5");input("goal-streak","250");input("goal-minimum","45")
        capture("onboarding-goals");rule.onNodeWithTag("setup-continue").performClick()
        rule.waitUntil(10000){app.preferences.value.setup}
        assertEquals(390,app.preferences.value.goalMinutes);assertEquals(250,app.preferences.value.streakMinutes);assertEquals(45,app.preferences.value.minimumSessionMinutes)
        settings("Goals & streak");rule.onNodeWithTag("goal-minimum").assertTextContains("45")
    }
    @Test fun onboardingInvalidDurationCanStillBeSkipped() {
        app.preferences.save(Config(vibrate=false));content();repeat(4){rule.onNodeWithTag("setup-skip").performClick()}
        input("goal-minimum","-1");rule.onNodeWithTag("setup-continue").assertIsNotEnabled()
        rule.onNodeWithTag("setup-skip").performClick();rule.waitUntil(10000){app.preferences.value.setup}
        assertEquals(10,app.preferences.value.minimumSessionMinutes)
    }
    @Test fun wifiRowsPreserveExistingNetworksCapacityDraftAndSingleConnectedIndicator() {
        val long="Airtel_Alpha_StudyZone_123456789"
        app.preferences.save(app.preferences.value.withNetworks(listOf(long,"Campus2","Campus3","Campus4","Campus5")).copy(wifi=true))
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        shadowOf(app.getSystemService(LocationManager::class.java)).setLocationEnabled(true)
        val wifi=app.getSystemService(WifiManager::class.java);shadowOf(wifi).setWifiState(WifiManager.WIFI_STATE_ENABLED)
        val info=wifi.connectionInfo;shadowOf(info).setSSID(long)
        assertEquals("Connected SSID fixture",long,com.studynest.app.automation.Automation.currentSsid(app))
        shadowOf(wifi).setScanResults(listOf(ScanResult().apply {SSID="Replacement";level=-40},ScanResult().apply {SSID="ExtraLibrary";level=-50}))
        content();settings("Library Wi-Fi")
        rule.onAllNodesWithText("Currently connected").assertCountEquals(1)
        rule.onNodeWithText("5 saved networks • No fixed network limit").assertExists()
        rule.onNodeWithText(long).performScrollTo();fits("saved-wifi-networks");capture("wifi-long-light")
        rule.onNode(hasText("Remove") and hasAnyAncestor(hasTestTag("wifi-network-Campus5"))).performScrollTo().performClick()
        assertEquals(5,app.preferences.value.networks().size)
        rule.onNodeWithText("Back to settings").performScrollTo().performClick()
        settings("Library Wi-Fi");rule.onNodeWithTag("wifi-network-Campus5").assertExists()
        rule.onNode(hasText("Change") and hasAnyAncestor(hasTestTag("wifi-network-Campus5"))).performScrollTo().performClick()
        rule.onNodeWithText("Replacement").performClick()
        rule.onNodeWithText("Add Wi-Fi Network").performScrollTo().performClick()
        rule.onNodeWithText("ExtraLibrary").performClick()
        rule.onNodeWithText("Use Current Wi-Fi").performScrollTo().performClick()
        rule.onAllNodesWithText("Currently connected").assertCountEquals(1)
        rule.onNodeWithContentDescription("Enable Wi-Fi detection").performScrollTo().performClick()
        rule.onNodeWithTag("wifi-save").performScrollTo().performClick()
        rule.waitUntil(10000){"Replacement" in app.preferences.value.networks()}
        assertFalse(app.preferences.value.wifi);assertEquals(6,app.preferences.value.networks().size)
        settings("Library Wi-Fi");app.preferences.save(app.preferences.value.copy(theme="Dark"));rule.waitForIdle()
        capture("wifi-dark");fits("saved-wifi-networks")
    }
    @Test fun goalClearAndEveryPresetRemainAvailableWithoutSilentlySavingInvalidValues() {
        content();settings("Goals & streak")
        listOf(60,120,180,240,300,360,480,600).forEach {rule.onNodeWithTag("goal-hours-preset-$it").performScrollTo().performClick()}
        listOf(30,60,90,120,180,200).forEach {rule.onNodeWithTag("goal-streak-preset-$it").performScrollTo().performClick()}
        listOf(5,10,15,20,30).forEach {rule.onNodeWithTag("goal-minimum-preset-$it").performScrollTo().performClick()}
        rule.onNode(hasText("Clear") and hasAnyAncestor(hasTestTag("goal-minimum-card"))).performScrollTo().performClick()
        rule.onNodeWithTag("goals-save").performScrollTo().assertIsNotEnabled()
        assertEquals(10,app.preferences.value.minimumSessionMinutes)
        input("goal-minimum","45");input("goal-streak","46")
        rule.onNodeWithTag("goals-save").performScrollTo().performClick()
        rule.waitUntil(10000){app.preferences.value.minimumSessionMinutes==45}
        assertEquals(600,app.preferences.value.goalMinutes);assertEquals(46,app.preferences.value.streakMinutes)
    }
    @Test fun historyTabsExactBarsCalendarAndQualifiedRowsUseTheSameRawData()=runBlocking {
        val day=LocalDate.now();val zone=ZoneId.systemDefault();val a=day.atStartOfDay(zone).toInstant().toEpochMilli()
        val short=VisitQualification.finish(LibraryVisit(start=a,elapsedStart=0,boot=1,minimumMinutes=10),a+360000)
        val qualified=VisitQualification.finish(LibraryVisit(start=a+3600000,elapsedStart=0,boot=1,minimumMinutes=10),a+5400000)
        app.db.focusDao().insertVisit(short);app.db.focusDao().insertVisit(qualified)
        val id=app.db.dao().insertSession(StudySession(startedAt=a,endedAt=a+5400000,trigger="Wi-Fi",measurementStart=a))
        app.db.dao().insertSegment(Segment(sessionId=id,start=a,end=a+360000,elapsedStart=0,boot=1))
        app.db.dao().insertSegment(Segment(sessionId=id,start=a+3600000,end=a+5400000,elapsedStart=0,boot=1))
        app.db.focusDao().insertWindow(FocusWindow(id=1,sessionId=id,start=a,end=a+360000,elapsedStart=0,boot=1,mode="OFF"))
        app.db.focusDao().insertWindow(FocusWindow(id=2,sessionId=id,start=a+3600000,end=a+5400000,elapsedStart=0,boot=1,mode="OFF"))
        content();rule.onNodeWithText("History",useUnmergedTree=true).performClick()
        rule.waitUntil(10000){rule.onAllNodesWithText("Today's sessions: 1").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("chart-day-$day").performScrollTo().performClick()
        rule.onNodeWithText("Actual Study: 00:36:00",substring=true).assertExists()
        fits("history-navigation");fits("history-chart");capture("paired-chart-light")
        if(scale>1f) {
            rule.onAllNodes(hasAnyAncestor(hasTestTag("history-navigation")) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true)
                .fetchSemanticsNodes().forEach {node ->
                    rule.runOnIdle {
                        val layouts=mutableListOf<TextLayoutResult>()
                        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                        layouts.forEach {assertEquals("Large-text History labels stay whole",1,it.lineCount)}
                    }
                }
        }
        rule.onNodeWithText("Month").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Next period").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Previous period").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Next period").performClick()
        rule.onNodeWithTag("history-tab-Calendar").performScrollTo().performClick()
        rule.onNodeWithTag("calendar-day-$day").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Next month").assertIsNotEnabled()
        fits("history-calendar");capture("calendar-light")
        rule.onNodeWithText("Some activity").assertDoesNotExist()
        rule.onNodeWithText("View all sessions").performScrollTo().performClick()
        rule.onAllNodesWithText("Library visit").assertCountEquals(1)
        rule.onNodeWithText("Session 1").assertDoesNotExist()
        rule.onNodeWithText("Library visit").performScrollTo().performClick()
        rule.onNodeWithText("Full visit: 00:30:00").assertExists()
        app.preferences.save(app.preferences.value.copy(theme="Dark"));rule.waitForIdle();capture("library-session-dark")
        rule.onNodeWithText("Back to history").performScrollTo().performClick()
        rule.onNodeWithText("Back to history").performScrollTo().performClick()
        rule.onNodeWithTag("history-tab-Calendar").performScrollTo().performClick()
        fits("history-calendar");capture("calendar-dark")
        rule.onNodeWithTag("history-tab-Tracked study").performScrollTo().performClick()
        fits("history-chart");capture("paired-chart-dark")
        Unit
    }
}
@org.robolectric.annotation.Config(qualifiers="w411dp-h891dp-xhdpi")
class RedesignPhoneTest:RedesignUiCases()
@org.robolectric.annotation.Config(qualifiers="w320dp-h640dp-mdpi")
class RedesignSmallPhoneTest:RedesignUiCases(){override val scale=1.5f}
@org.robolectric.annotation.Config(qualifiers="w600dp-h960dp-mdpi")
class RedesignLargeScreenTest:RedesignUiCases()
