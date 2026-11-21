package com.studynest.app

import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.*
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.Preferences
import com.studynest.app.ui.StudyNest
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33,34,35,36],application=StudyNestApp::class,qualifiers="w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonitoringUiTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private lateinit var app:StudyNestApp
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        app.preferences.save(app.preferences.value.copy(setup=true,wifi=false,geo=false,vibrate=false,
            monitoredApps=emptySet(),appMonitoring=false,monitorCallAudio=false))
        shadowOf(app.getSystemService(AppOpsManager::class.java)).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),app.packageName,AppOpsManager.MODE_IGNORED)
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        listOf("com.example.entertainment" to "Entertainment App","com.example.tools" to "Focus Tools").forEach { (pkg,label) ->
            val info=ResolveInfo().apply { activityInfo=ActivityInfo().apply {
                name="$pkg.Main";packageName=pkg;exported=true;enabled=true
                applicationInfo=ApplicationInfo().apply { packageName=pkg;nonLocalizedLabel=label;enabled=true }
            } }
            shadowOf(app.packageManager).addResolveInfoForIntent(intent,info)
        }
    }
    @After fun close() {
        rule.runOnIdle { rule.activity.setContent {} }
        rule.waitForIdle()
        app.scope.cancel();app.db.close()
    }
    private fun open() {
        rule.onNodeWithText("App Monitoring").performScrollTo().performClick()
        rule.onNodeWithText("Save Changes").assertIsDisplayed()
    }
    private fun content() {
        rule.setContent { StudyNest(app,refreshTimer=false) }
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        open()
    }
    private fun appRow(pkg:String):SemanticsNodeInteraction {
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasText("Distraction apps"))
        rule.waitUntil(15000) { rule.onAllNodesWithTag("app-$pkg").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-$pkg"))
        return rule.onNodeWithTag("app-$pkg")
    }
    @Test fun selectionsRequireSaveAndSearchFiltersApps() {
        content()
        rule.onNodeWithTag("monitoring-calls").performClick()
        appRow("com.example.entertainment").performClick().assertIsOn()
        appRow("com.example.tools").performClick().assertIsOn()
        assertTrue(app.preferences.value.monitoredApps.isEmpty())
        assertFalse(Preferences(app).value.monitorCallAudio)
        rule.onNodeWithText("com.example.entertainment").assertDoesNotExist()
        rule.onNodeWithText("Save Changes").performClick()
        rule.waitUntil(15000) { app.preferences.value.monitoredApps.size==2 }
        assertEquals(setOf("com.example.entertainment","com.example.tools"),Preferences(app).value.monitoredApps)
        assertTrue(Preferences(app).value.monitorCallAudio)
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-search"))
        rule.onNodeWithTag("app-search").performTextInput("Focus")
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasText("Focus Tools"))
        rule.onNodeWithText("Focus Tools").assertExists()
        rule.onNodeWithText("Entertainment App").assertDoesNotExist()
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-search"))
        rule.onNodeWithContentDescription("Clear search").performClick()
        appRow("com.example.entertainment").assertIsOn()
        assertFalse(app.preferences.value.appMonitoring)
    }
    @Test fun leavingDiscardsUnsavedAppAndMasterEdits() {
        content()
        rule.onNodeWithTag("monitoring-master").performClick()
        appRow("com.example.entertainment").performClick()
        rule.onNodeWithContentDescription("Back to settings").performClick()
        assertFalse(Preferences(app).value.appMonitoring)
        assertTrue(Preferences(app).value.monitoredApps.isEmpty())
        open()
        rule.onNodeWithTag("monitoring-master").assertIsOff()
        appRow("com.example.entertainment").assertIsOff()
        rule.onNodeWithText("Save Changes").assertIsNotEnabled()
    }
    @Test fun masterOffKeepsSavedSelectionsAndReopeningRestoresThem() {
        app.preferences.save(app.preferences.value.copy(appMonitoring=true,monitoredApps=setOf("com.example.entertainment")))
        content()
        rule.onNodeWithTag("monitoring-master").performClick()
        assertTrue(app.preferences.value.appMonitoring)
        rule.onNodeWithText("Save Changes").performClick()
        rule.waitUntil(15000) { !app.preferences.value.appMonitoring }
        assertEquals(setOf("com.example.entertainment"),Preferences(app).value.monitoredApps)
        rule.onNodeWithContentDescription("Back to settings").performClick()
        open()
        appRow("com.example.entertainment").assertIsOn()
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("monitoring-master"))
        rule.onNodeWithTag("monitoring-master").performClick()
        rule.onNodeWithText("Save Changes").performClick()
        rule.waitUntil(15000) { app.preferences.value.appMonitoring }
        assertEquals(setOf("com.example.entertainment"),Preferences(app).value.monitoredApps)
    }
    @Test fun usageAccessReturnRefreshesStatusWithoutSavingDraft() {
        content()
        appRow("com.example.entertainment").performClick()
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("restricted-app-info"))
        rule.onNodeWithTag("restricted-app-info").performClick()
        val info=shadowOf(rule.activity).nextStartedActivity
        assertEquals(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,info.action)
        assertEquals("package:${app.packageName}",info.data.toString())
        assertTrue(Preferences(app).value.monitoredApps.isEmpty())
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasText("Grant Access"))
        rule.onNodeWithText("Grant Access").performClick()
        assertEquals(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS,shadowOf(rule.activity).nextStartedActivity.action)
        shadowOf(app.getSystemService(AppOpsManager::class.java)).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),app.packageName,AppOpsManager.MODE_ALLOWED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.onNodeWithText("Granted ✓").assertExists()
        rule.onNodeWithText("Grant Access").assertDoesNotExist()
        rule.onNodeWithTag("restricted-app-info").assertDoesNotExist()
        assertTrue(Preferences(app).value.monitoredApps.isEmpty())
        appRow("com.example.entertainment").assertIsOn()
    }
    @Test fun leavingViaBottomNavigationDiscardsUnsavedSelections() {
        content()
        appRow("com.example.entertainment").performClick()
        rule.onNodeWithText("Home",useUnmergedTree=true).performClick()
        rule.onNodeWithTag("study-timer-card").assertExists()
        assertTrue(Preferences(app).value.monitoredApps.isEmpty())
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        if (rule.onAllNodesWithTag("monitoring-master").fetchSemanticsNodes().isEmpty()) open()
        appRow("com.example.entertainment").assertIsOff()
    }
}
