package com.studynest.app

import android.content.Intent
import android.content.pm.*
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
import com.studynest.app.domain.UsageState
import com.studynest.app.ui.StudyNest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.time.*

/** Native simulated layouts; no claim of real-phone/OEM testing. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=StudyNestApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class DashboardUiCases {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    protected open val fontScale=1f
    private lateinit var app:StudyNestApp
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        app.preferences.save(Config(setup=true,vibrate=false,theme="Light"))
    }
    @After fun close() {
        rule.runOnIdle { rule.activity.setContent {} }
        rule.waitForIdle()
        // Destroy the test Activity before closing Room: an animated outgoing
        // screen can still have lifecycle collectors querying its coverage row.
        rule.activityRule.scenario.close()
        runBlocking { app.scope.coroutineContext[Job]!!.cancelAndJoin() }
        app.db.close()
    }
    private fun content() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,fontScale)) {
                StudyNest(app,refreshTimer=false)
            }
        }
    }
    private fun fits(tag:String) {
        val matcher=hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult)
        val nodes=rule.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes()
        assertTrue("Expected text inside $tag",nodes.isNotEmpty())
        rule.runOnIdle {
            nodes.forEach { node ->
                val results=mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                results.forEach { result ->
                    // Native glyph advances are fractional; layout size is rounded
                    // to whole pixels. Permit only that one-pixel rounding margin.
                    for (line in 0 until result.lineCount) assertTrue("Clipped width: ${result.layoutInput.text.text}",
                        result.getLineRight(line)-result.getLineLeft(line)<=result.size.width+1f)
                    assertTrue("Clipped height: ${result.layoutInput.text.text}",result.multiParagraph.height<=result.size.height+1f)
                    assertFalse("Truncated text: ${result.layoutInput.text.text}",result.multiParagraph.didExceedMaxLines)
                }
            }
        }
    }
    private fun capture(name:String) {
        rule.runOnIdle {
            val view=rule.activity.window.decorView
            val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val file=java.io.File("build/reports/screenshots/${javaClass.simpleName}-$name.png")
            file.parentFile!!.mkdirs()
            java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        }
    }
    @Test fun idleClockAndRealTaskSummaryLinkToExistingTasks() {
        val day=LocalDate.now()
        content()
        rule.onNodeWithText("0 tasks • 0 completed").assertExists()
        // Block only while seeding Room. UI collection must run outside the
        // main-thread runBlocking event loop, including asynchronous emissions.
        runBlocking { listOf(
            StudyTask(title="Due today",dueDate=day.toString()),
            StudyTask(title="Overdue",dueDate=day.minusDays(1).toString()),
            StudyTask(title="Finished today",dueDate=day.toString(),completed=true),
            StudyTask(title="Finished earlier",dueDate=day.minusDays(1).toString(),completed=true),
            StudyTask(title="Future",dueDate=day.plusDays(1).toString()),
            StudyTask(title="No deadline")
        ).forEach { app.db.dao().saveTask(it) }
            assertEquals(6,app.db.dao().tasks().first().size)
        }
        try {
            rule.waitUntil(15000) { rule.onAllNodesWithText("6 tasks • 2 completed").fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
            println("Activity lifecycle: " + rule.activity.lifecycle.currentState)
            println(rule.onRoot(useUnmergedTree=true).printToString())
            capture("task-summary-timeout")
            println("Task fixture rows: " + runBlocking { app.db.dao().tasks().first() })
            throw failure
        }
        rule.onNodeWithText("1 due today • 1 overdue").assertExists()
        rule.onNodeWithTag("primary-study-clock").assertTextEquals("00:00:00")
        fits("today-study-summary");fits("study-timer-card")
        capture("home-idle-tasks")
        rule.onNodeWithTag("view-tasks").performScrollTo().performClick()
        rule.onNodeWithText("Due today").assertExists()
        rule.onNodeWithText("Overdue").assertExists()
    }
    @Test fun emptyTasksAndIdleClockRemainUsable() {
        content()
        rule.onNodeWithText("0 tasks • 0 completed").assertExists()
        rule.onNodeWithText("0 due today • 0 overdue").assertExists()
        rule.onNodeWithTag("primary-study-clock").assertTextEquals("00:00:00")
        rule.onNodeWithTag("view-tasks").performScrollTo().performClick()
        rule.onNodeWithText("0 tasks • 0 completed").assertDoesNotExist()
    }
    @Test fun historyBreakIsStoredPauseRatherThanTheWholeUntrackedGap()=runBlocking {
        val start=LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()+3600000
        val visit=app.db.focusDao().insertVisit(LibraryVisit(start=start,end=start+3600000,elapsedStart=0,boot=1))
        app.db.breakDao().begin(BreakTracking(startedAt=start))
        app.db.breakDao().insert(ManualBreak(sessionId=1,visitId=visit,start=start+600000,end=start+1800000,elapsedStart=0,boot=1))
        content()
        rule.onNodeWithText("History",useUnmergedTree=true).performClick()
        rule.waitUntil(15000) { rule.onAllNodesWithTag("metric-Break").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("metric-Break").performScrollTo()
        rule.onNodeWithText("00:20:00").assertExists()
        rule.onNodeWithText("Actual Study outside library").assertDoesNotExist()
        rule.onNodeWithText("Break / not studying").assertDoesNotExist()
        fits("daily-library-summary");capture("history-stored-break")
    }
    @Test fun openTodayReportFollowsMidnightButChosenHistoricalDateStaysSelected() {
        val day=LocalDate.of(2026,10,2)
        val current=androidx.compose.runtime.mutableStateOf(day)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,fontScale)) {
                androidx.compose.material3.MaterialTheme {
                    com.studynest.app.ui.HistoryScreen(app,emptyList(),emptyList(),emptyMap(),
                        com.studynest.app.domain.Streaks(0,0),current.value)
                }
            }
        }
        fun dateText(date:LocalDate)=date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
        rule.onNodeWithText(dateText(day)).assertExists()
        rule.runOnIdle { current.value=day.plusDays(1) }
        rule.onNodeWithText(dateText(day.plusDays(1))).assertExists()
        rule.onNodeWithTag("history-tab-Calendar").performScrollTo().performClick()
        // The calendar has its own horizontal scroller. Bring its card into
        // the outer vertical viewport before scrolling/clicking a day cell.
        rule.onNodeWithTag("history-calendar").performScrollTo()
        rule.onNodeWithTag("calendar-day-${day.minusDays(1)}")
            .performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText(dateText(day.minusDays(1))).assertExists()
        rule.runOnIdle { current.value=day.plusDays(2) }
        rule.onNodeWithText(dateText(day.minusDays(1))).assertExists()
        rule.onNodeWithText("${day.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("d MMMM"))} Session Report").assertExists()
    }
    @Test fun timerPrecedesLiveVisitAndFitsBothThemes()=runBlocking {
        val now=System.currentTimeMillis();val start=now-600000
        val elapsed=android.os.SystemClock.elapsedRealtime()
        val boot=android.provider.Settings.Global.getInt(app.contentResolver,android.provider.Settings.Global.BOOT_COUNT,-1)
        app.preferences.save(app.preferences.value.copy(wifi=true,ssid="Library",appMonitoring=true))
        app.db.dao().saveState(AutomationState(wifiInside=true))
        val id=app.db.dao().insertSession(StudySession(startedAt=start,trigger="Wi-Fi",measurementStart=start))
        app.db.dao().insertSegment(Segment(sessionId=id,start=start,elapsedStart=elapsed-600000,boot=boot))
        app.db.focusDao().insertVisit(LibraryVisit(start=now-660000,elapsedStart=elapsed-660000,boot=boot))
        val window=app.db.focusDao().insertWindow(FocusWindow(sessionId=id,start=start,elapsedStart=elapsed-600000,
            boot=boot,mode="ON",cursor=now,tracker=UsageState(known=true).encode(),seeded=true))
        app.db.focusDao().insertPeriods(listOf(DistractionPeriod(windowId=window,start=now-120000,end=now-60000,reason="APP",packageName="com.example.entertainment")))
        content()
        // Visible bounds are empty when a card is below the scroll viewport.
        // Compare unclipped placement, then verify the metrics can be reached.
        val timer=rule.onNodeWithTag("study-timer-card").fetchSemanticsNode().positionInRoot.y
        val visit=rule.onNodeWithTag("live-focus-card").fetchSemanticsNode().positionInRoot.y
        val summary=rule.onNodeWithTag("today-study-summary").fetchSemanticsNode().positionInRoot.y
        val quote=rule.onNodeWithTag("motivational-quote").fetchSemanticsNode().positionInRoot.y
        assertTrue("Today's summary must come first",summary<timer)
        assertTrue("Timer must come before current visit",timer<visit)
        assertTrue("Quote must follow current visit",visit<quote)
        rule.onNodeWithText("Pause").assertExists()
        rule.onNodeWithText("Stop automatic").assertExists()
        rule.onNodeWithText("Current library visit").assertExists()
        rule.onNodeWithText("Updates from Android usage timestamps about every 15 seconds. The original study timer remains available below.").assertDoesNotExist()
        capture("home-light");fits("study-timer-card");fits("live-focus-card");fits("main-navigation")
        rule.onNodeWithText("Efficiency").performScrollTo().assertIsDisplayed()
        capture("home-focus-light")
        app.preferences.save(app.preferences.value.copy(theme="Dark"))
        rule.waitForIdle();fits("study-timer-card");fits("live-focus-card");capture("home-dark")
    }
    @Test fun dailySummaryCombinesVisitsAndRetainsRawRecords()=runBlocking {
        val zone=ZoneId.systemDefault();val day=LocalDate.now()
        val midnight=day.atStartOfDay(zone).toInstant().toEpochMilli()
        for ((index,range) in listOf(0L to 2L,3L to 5L,6L to 9L).withIndex()) {
            val a=midnight+range.first*3600000;val b=midnight+range.second*3600000
            app.db.focusDao().insertVisit(LibraryVisit(start=a,end=b,elapsedStart=0,boot=1))
            val session=app.db.dao().insertSession(StudySession(startedAt=a,endedAt=b,trigger="Wi-Fi",measurementStart=a))
            app.db.dao().insertSegment(Segment(sessionId=session,start=a,end=b,elapsedStart=0,boot=1))
            val window=app.db.focusDao().insertWindow(FocusWindow(sessionId=session,start=a,end=b,elapsedStart=0,
                boot=1,mode="ON",cursor=b,seeded=true))
            if(index<2) app.db.focusDao().insertPeriods(listOf(DistractionPeriod(windowId=window,start=a,end=a+3600000,
                reason="APP",packageName="com.example.entertainment")))
        }
        content()
        rule.onNodeWithText("History",useUnmergedTree=true).performClick()
        rule.waitUntil(15000) { rule.onAllNodesWithText("07:00:00").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithTag("daily-library-summary").assertCountEquals(1)
        rule.onNodeWithText("07:00:00").assertExists()
        rule.onNodeWithText("05:00:00").assertExists()
        rule.onNodeWithText("02:00:00").assertExists()
        rule.onNodeWithText("71.4%").assertExists()
        rule.onNodeWithText("Library visit").assertDoesNotExist()
        assertEquals(3,app.db.focusDao().visits().first().size)
        val ordered=listOf("history-chart","daily-library-summary","daily-session-report","break-distraction-trend","period-report")
            .map { rule.onNodeWithTag(it).fetchSemanticsNode().positionInRoot.y }
        assertTrue("Tracked study sections must retain their order",ordered.zipWithNext().all { (a,b) -> a<b })
        rule.onNodeWithTag("history-tab-Calendar").performScrollTo().performClick()
        rule.onNodeWithTag("history-calendar").assertExists()
        rule.onNodeWithText("View all sessions").performScrollTo().performClick()
        rule.onNodeWithTag("session-information").assertExists()
        rule.onNodeWithText("Back to history").performScrollTo().performClick()
        rule.onNodeWithTag("history-tab-Tracked study").performScrollTo().performClick()
        rule.onNodeWithText("Tracked study overview").assertDoesNotExist()
        fits("daily-library-summary");capture("history-light")
        rule.onNodeWithText("Efficiency").performScrollTo().assertIsDisplayed();capture("history-report-light")
        app.preferences.save(app.preferences.value.copy(theme="Dark"))
        rule.waitForIdle();fits("daily-library-summary");capture("history-dark")
        assertEquals(3,app.db.focusDao().visits().first().size)
    }
    @Test fun monitoringHasVisibleSaveAndAccessibleRowsInBothThemes() {
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val info=ResolveInfo().apply { activityInfo=ActivityInfo().apply {
            name="com.example.tools.Main";packageName="com.example.tools";exported=true;enabled=true
            applicationInfo=ApplicationInfo().apply { packageName="com.example.tools";nonLocalizedLabel="Study Reference Tools";enabled=true }
        } }
        shadowOf(app.packageManager).addResolveInfoForIntent(intent,info)
        content()
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        rule.onNodeWithText("App Monitoring").performScrollTo().performClick()
        rule.onNodeWithText("Save Changes").assertIsDisplayed()
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasText("Distraction apps"))
        rule.waitUntil(15000) { rule.onAllNodesWithTag("app-com.example.tools").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-com.example.tools"))
        rule.onNodeWithTag("app-com.example.tools").performClick().assertIsOn()
        rule.onNodeWithText("com.example.tools").assertDoesNotExist()
        rule.onNodeWithText("Save Changes").assertIsDisplayed()
        fits("app-com.example.tools");capture("monitoring-light")
        assertTrue(app.preferences.value.monitoredApps.isEmpty())
        app.preferences.save(app.preferences.value.copy(theme="Dark"))
        rule.waitForIdle();fits("app-com.example.tools");capture("monitoring-dark")
    }
}

@org.robolectric.annotation.Config(qualifiers="w411dp-h891dp-xhdpi")
class DashboardPhoneTest:DashboardUiCases()
@org.robolectric.annotation.Config(qualifiers="w320dp-h640dp-mdpi")
class DashboardSmallPhoneTest:DashboardUiCases() { override val fontScale=1.5f }
@org.robolectric.annotation.Config(qualifiers="w600dp-h960dp-mdpi")
class DashboardLargeScreenTest:DashboardUiCases()
