package com.studynest.app

import android.content.Intent
import android.content.pm.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.ui.StudyNest
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=StudyNestApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class MonitoringSearchUiCases {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    protected open val scale=1f
    private lateinit var app:StudyNestApp
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach {it.join()}
        app.preferences.save(Config(setup=true,vibrate=false,appMonitoring=false,monitorCallAudio=false))
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        for((pkg,label,system) in listOf(Triple("com.whatsapp","WhatsApp",false),Triple("com.pool","8 Ball Pool",false),Triple("com.insta","Instagram",false),Triple("com.face","Facebook",false),Triple("com.you","YouTube",false),Triple("com.system.tool","Android Tool",true))) {
            val info=ResolveInfo().apply {activityInfo=ActivityInfo().apply {
                name="$pkg.Main";packageName=pkg;enabled=true;exported=true
                applicationInfo=ApplicationInfo().apply {packageName=pkg;nonLocalizedLabel=label;enabled=true;if(system)flags=ApplicationInfo.FLAG_SYSTEM}
            }}
            shadowOf(app.packageManager).addResolveInfoForIntent(intent,info)
        }
    }
    @After fun close(){rule.runOnIdle{rule.activity.setContent{}};rule.waitForIdle();app.scope.cancel()}
    private fun content() {
        rule.setContent{CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)){StudyNest(app,refreshTimer=false)}}
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        rule.onNodeWithText("App Monitoring").performScrollTo().performClick()
    }
    private fun search(text:String) {
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-search"))
        rule.onNodeWithTag("app-search").performTextReplacement(text)
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasText("Distraction apps"))
        rule.waitForIdle()
    }
    private fun row(pkg:String):SemanticsNodeInteraction {
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-$pkg"))
        return rule.onNodeWithTag("app-$pkg")
    }
    private fun clear() {
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-search"))
        rule.onNodeWithContentDescription("Clear search").performClick()
    }
    @Test fun typingTrimmedPartialCaseInsensitiveNamesAndPackagesFiltersImmediately() {
        content();val original=Preferences(app).value
        for((text,pkg) in listOf("wHa" to "com.whatsapp","inst" to "com.insta","face" to "com.face","you" to "com.you")) {
            search(text);row(pkg).assertExists()
        }
        search("  wHaTs  ");row("com.whatsapp").assertExists()
        rule.onNodeWithTag("app-com.pool").assertDoesNotExist()
        search("  POOL  ");row("com.pool").assertExists()
        rule.onNodeWithTag("app-com.whatsapp").assertDoesNotExist()
        search("com.whats");row("com.whatsapp").assertExists()
        clear();row("com.pool").assertExists();row("com.whatsapp").assertExists()
        search("   ");row("com.pool").assertExists()
        rule.onNodeWithText("Save Changes").assertIsNotEnabled();assertEquals(original,Preferences(app).value)
    }
    @Test fun filteredSelectionsKeepCountAndDraftUntilExplicitSave() {
        content();search("Pool");row("com.pool").performClick().assertIsOn()
        rule.onNodeWithText("1 app selected • Unsaved changes").assertExists()
        assertTrue(Preferences(app).value.monitoredApps.isEmpty())
        search("Whats");row("com.whatsapp").performClick()
        search("Pool");row("com.pool").assertIsOn()
        rule.onNodeWithText("2 apps selected • Unsaved changes").assertExists()
        clear();row("com.pool").assertIsOn();row("com.whatsapp").assertIsOn()
        assertTrue(Preferences(app).value.monitoredApps.isEmpty())
        rule.onNodeWithText("Save Changes").performClick()
        rule.waitUntil(15000){app.preferences.value.monitoredApps.size==2}
        assertEquals(setOf("com.pool","com.whatsapp"),Preferences(app).value.monitoredApps)
        assertFalse(app.preferences.value.appMonitoring)
    }
    @Test fun systemToggleInteractsWithSearchWithoutLosingSelectedSystemApps() {
        content();search("Android")
        rule.waitUntil(15000){rule.onAllNodesWithText("No matching apps.").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasText("Show system apps"))
        rule.onNodeWithText("Show system apps").performClick()
        row("com.system.tool").performClick().assertIsOn()
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasText("Show system apps"))
        rule.onNodeWithText("Show system apps").performClick()
        row("com.system.tool").assertIsOn()
        clear();row("com.pool").assertExists();row("com.system.tool").assertIsOn()
        assertTrue(Preferences(app).value.monitoredApps.isEmpty())
        rule.onNodeWithContentDescription("Back to settings").performClick()
        assertTrue(Preferences(app).value.monitoredApps.isEmpty())
    }
}
@org.robolectric.annotation.Config(qualifiers="w411dp-h891dp-xhdpi")
class MonitoringSearchPhoneTest:MonitoringSearchUiCases()
@org.robolectric.annotation.Config(qualifiers="w320dp-h640dp-mdpi")
class MonitoringSearchSmallPhoneTest:MonitoringSearchUiCases(){override val scale=1.5f}
@org.robolectric.annotation.Config(qualifiers="w600dp-h960dp-mdpi")
class MonitoringSearchLargeScreenTest:MonitoringSearchUiCases()
