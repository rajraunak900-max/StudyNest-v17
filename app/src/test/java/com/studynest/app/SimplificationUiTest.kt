package com.studynest.app

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import com.studynest.app.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** Real Compose/native rendering with stored test fixtures, not physical phone tests. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=StudyNestApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class SimplificationUiCases {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    protected open val scale=1f
    private lateinit var app:StudyNestApp
    private val today=LocalDate.now()
    private val historical=today.minusDays(1)
    private val quiet=today.minusDays(2)
    private fun start(day:LocalDate)=day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach{it.join()}
        app.preferences.save(Config(setup=true,vibrate=false,theme="Light"))
        app.db.breakDao().begin(BreakTracking(startedAt=start(quiet)))
        // App startup already inserts today's coverage marker with IGNORE.
        // These historical test records were measured before that marker.
        app.db.openHelper.writableDatabase.execSQL("UPDATE break_tracking SET startedAt=? WHERE id=1",arrayOf(start(quiet)))
        suspend fun seed(day:LocalDate,minutes:Int,monitor:Boolean=false) {
            val a=start(day);val b=a+minutes*60_000L
            val v=VisitQualification.finish(LibraryVisit(start=a,elapsedStart=0,boot=1,minimumMinutes=10),b)
            val visit=app.db.focusDao().insertVisit(v)
            val session=app.db.dao().insertSession(StudySession(startedAt=a,endedAt=b,trigger="Wi-Fi",measurementStart=a))
            app.db.dao().insertSegment(Segment(sessionId=session,start=a,end=b,elapsedStart=0,boot=1))
            val firstEnd=if(monitor) a+60*60_000L else b
            val window=app.db.focusDao().insertWindow(FocusWindow(sessionId=session,start=a,end=firstEnd,elapsedStart=0,boot=1,
                mode=if(monitor) "ON" else "OFF",cursor=firstEnd,seeded=true))
            if(monitor) {
                // A manual pause closes the eligible focus window. Resume opens
                // another window; break records themselves are not study windows.
                app.db.focusDao().insertWindow(FocusWindow(sessionId=session,start=a+75*60_000L,end=b,
                    elapsedStart=0,boot=1,mode="ON",cursor=b,seeded=true))
                app.db.focusDao().insertPeriods(listOf(DistractionPeriod(windowId=window,start=a,end=a+20*60_000L,reason="APP",packageName="com.example.recorded")))
                app.db.breakDao().insert(ManualBreak(sessionId=session,visitId=visit,start=a+60*60_000L,end=a+75*60_000L,elapsedStart=0,boot=1))
            }
        }
        seed(historical,265,true);seed(quiet,52);seed(today,8)
    }
    @After fun close() {
        rule.runOnIdle{rule.activity.setContent{}};rule.waitForIdle()
        app.scope.cancel();app.db.close()
    }
    private fun content() {rule.setContent {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)){StudyNest(app,refreshTimer=false)}
    }}
    private fun history() {
        content();rule.onNode(hasText("History") and hasAnyAncestor(hasTestTag("main-navigation")),useUnmergedTree=true).performClick()
        rule.waitUntil(15000){rule.onAllNodesWithTag("metric-Sitting Time").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun calendar() {rule.onNodeWithTag("history-tab-Calendar").performScrollTo().performClick()}
    private fun date(day:LocalDate) {
        // If a fixture crosses a month boundary, navigate to that month first.
        val difference=java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(day),YearMonth.from(today)).toInt()
        repeat(difference){rule.onNodeWithContentDescription("Previous month").performScrollTo().performClick()}
        rule.onNodeWithTag("history-calendar").performScrollTo()
        rule.onNodeWithTag("calendar-day-$day").performScrollTo().assertIsDisplayed().performClick()
    }
    private fun fits(tag:String) {
        val nodes=rule.onAllNodes(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes()
        assertTrue(nodes.isNotEmpty())
        rule.runOnIdle{nodes.forEach{n ->
            val layouts=mutableListOf<TextLayoutResult>();n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            layouts.forEach{r ->
                for(line in 0 until r.lineCount)assertTrue("Clipped ${r.layoutInput.text}",r.getLineRight(line)-r.getLineLeft(line)<=r.size.width+1)
                assertFalse("Truncated ${r.layoutInput.text}",r.multiParagraph.didExceedMaxLines)
            }
        }}
    }
    private fun capture(name:String) {rule.runOnIdle{
        val v=rule.activity.window.decorView
        val b=android.graphics.Bitmap.createBitmap(v.width,v.height,android.graphics.Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(b))
        val f=java.io.File("build/reports/screenshots/${javaClass.simpleName}-$name.png");f.parentFile!!.mkdirs()
        java.io.FileOutputStream(f).use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }}
    @Test fun twoTabsMetricsThenSimpleReportThenTwoSeriesTrend() {
        history()
        rule.onNodeWithTag("history-tab-Tracked study").assertExists()
        rule.onNodeWithTag("history-tab-Calendar").assertExists()
        rule.onNodeWithTag("history-tab-Session info").assertDoesNotExist()
        val order=listOf("history-chart","daily-library-summary","daily-session-report","break-distraction-trend","period-report")
            .map{rule.onNodeWithTag(it).fetchSemanticsNode().positionInRoot.y}
        assertTrue(order.zipWithNext().all{(a,b)->a<b})
        rule.onNodeWithTag("session-count").assertTextEquals("Today's sessions: 0")
        rule.onNodeWithText("Qualifying library sessions",substring=true).assertDoesNotExist()
        rule.onNodeWithText("Manual / earlier study periods",substring=true).assertDoesNotExist()
        fits("daily-session-report");capture("simple-report")
        rule.onNodeWithTag("trend-toggle").performScrollTo().performClick()
        rule.onNode(hasText("● Distraction") and hasAnyAncestor(hasTestTag("break-distraction-trend")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("● Break") and hasAnyAncestor(hasTestTag("break-distraction-trend")),useUnmergedTree=true).assertExists()
        rule.onAllNodes(hasText("Study",substring=true) and hasAnyAncestor(hasTestTag("break-distraction-trend")),useUnmergedTree=true).assertCountEquals(0)
        // On Monday, yesterday's recorded fixture is in the previous week.
        // Select its actual period rather than expecting history in an empty new week.
        if (today.dayOfWeek == DayOfWeek.MONDAY) {
            rule.onNode(hasContentDescription("Earlier graph period") and
                hasAnyAncestor(hasTestTag("break-distraction-trend")),useUnmergedTree=true)
                .performScrollTo().performClick()
        }
        rule.onNodeWithText("Distraction total: 0h 20m").assertExists()
        rule.onNodeWithText("Break total: 0h 15m").assertExists()
        rule.onNodeWithText("Break total: 0h 15m").performScrollTo()
        fits("break-distraction-trend");capture("two-series-trend")
    }
    @Test fun shortVisitKeepsAllLibraryTimeButNoQualifyingSession():Unit=runBlocking {
        history()
        rule.onNodeWithTag("metric-Sitting Time").assertExists()
        rule.onNode(hasText("00:08:00") and hasAnyAncestor(hasTestTag("metric-Sitting Time")),useUnmergedTree=true).assertExists()
        rule.onNodeWithTag("view-all-sessions").performScrollTo().performClick()
        rule.onNodeWithTag("session-information").assertExists()
        rule.onNodeWithText("No sessions for this date.").assertExists()
        assertEquals(3,app.db.focusDao().visits().first().size)
        rule.onNodeWithText("Back to history").performScrollTo().performClick()
        rule.onNodeWithTag("history-tab-Tracked study").assertExists()
    }
    @Test fun calendarHoursMinutesEmptyAndFutureCellsRemainReadable() {
        history();calendar();date(historical)
        rule.onNode(hasText("A 3.8h") and hasAnyAncestor(hasTestTag("calendar-day-$historical")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("S 4.4h") and hasAnyAncestor(hasTestTag("calendar-day-$historical")),useUnmergedTree=true).assertExists()
        fits("calendar-grid");capture("calendar-compact")
        if(YearMonth.from(historical)==YearMonth.from(today)) {
            rule.onNode(hasText("A 8m") and hasAnyAncestor(hasTestTag("calendar-day-$today")),useUnmergedTree=true).assertExists()
            rule.onNode(hasText("S 8m") and hasAnyAncestor(hasTestTag("calendar-day-$today")),useUnmergedTree=true).assertExists()
        }
        val empty=historical.withDayOfMonth(1)
        if(empty<quiet) rule.onAllNodes(hasAnyAncestor(hasTestTag("calendar-day-$empty")) and hasText("A",substring=true),useUnmergedTree=true).assertCountEquals(0)
        val future=today.plusDays(1)
        if(YearMonth.from(future)==YearMonth.from(historical)) {
            rule.onNodeWithTag("calendar-day-$future").assertIsNotEnabled()
            rule.onAllNodes(hasAnyAncestor(hasTestTag("calendar-day-$future")) and hasText("L",substring=true),useUnmergedTree=true).assertCountEquals(0)
        }
        rule.onNodeWithText("Some activity").assertDoesNotExist()
    }
    @Test fun historicalExactMetricsPerAppBreakdownBeforeDatedReportWithoutCharts() {
        history();calendar();date(historical)
        rule.onNodeWithTag("metric-Sitting Time").performScrollTo()
        rule.onNodeWithText("04:25:00").assertExists();rule.onNodeWithText("03:50:00").assertExists()
        rule.onNodeWithText("00:20:00").assertExists();rule.onNodeWithText("00:15:00").assertExists()
        rule.onNodeWithText("86.8%").assertExists()
        rule.onNodeWithText("Distraction breakdown").performScrollTo().performClick()
        rule.onNodeWithText("com.example.recorded").assertExists()
        rule.onNodeWithText("0h 20m").assertExists()
        val report="${historical.format(DateTimeFormatter.ofPattern("d MMMM"))} Session Report"
        rule.onNodeWithText(report).assertExists();rule.onNodeWithTag("session-count").assertTextEquals("Sessions: 1")
        val metricOrder=listOf("Sitting Time","Actual Study","Distraction","Break","Efficiency")
            .map{rule.onNodeWithTag("metric-$it").fetchSemanticsNode().positionInRoot.y}
        // Dashboard becomes multiple columns on larger displays, retaining row-major order.
        assertTrue(metricOrder.zipWithNext().all{(a,b)->a<=b})
        val breakdown=rule.onNodeWithText("Distraction breakdown").fetchSemanticsNode().positionInRoot.y
        assertTrue(breakdown<rule.onNodeWithTag("daily-session-report").fetchSemanticsNode().positionInRoot.y)
        rule.onNodeWithTag("history-chart").assertDoesNotExist()
        rule.onNodeWithTag("break-distraction-trend").assertDoesNotExist()
        rule.onNodeWithTag("period-report").assertDoesNotExist()
        fits("daily-library-summary");capture("historical-apps-light")
        app.preferences.save(app.preferences.value.copy(theme="Dark"));rule.waitForIdle();capture("historical-apps-dark")
    }
    @Test fun quietHistoricalDayHasPreciseValuesAndNoEmptyAppBreakdown() {
        history();calendar();date(quiet)
        rule.onNode(hasText("00:52:00") and hasAnyAncestor(hasTestTag("metric-Sitting Time")),useUnmergedTree=true).assertExists()
        rule.onNodeWithText("Distraction breakdown").assertDoesNotExist()
        rule.onNodeWithTag("session-count").assertTextEquals("Sessions: 1")
        rule.onNodeWithTag("break-distraction-trend").assertDoesNotExist()
        rule.onNodeWithTag("history-chart").assertDoesNotExist()
    }
    @Test fun historicalSessionListIsDateFilteredAndCountMatchesVisibleVisits() {
        history();calendar();date(historical)
        rule.onNodeWithTag("view-all-sessions").performScrollTo().performClick()
        rule.onNodeWithText(historical.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))).assertExists()
        rule.onAllNodesWithText("Library visit").assertCountEquals(1)
        rule.onNodeWithText("4h 25m • threshold 10 min").assertExists()
        rule.onNodeWithText("Library visit").performScrollTo().performClick()
        rule.onNodeWithText("Full visit: 04:25:00").assertExists()
        rule.onNodeWithText("Back to history").performScrollTo().performClick()
        rule.onNodeWithTag("session-information").assertExists()
        rule.onNodeWithText("Back to history").performScrollTo().performClick()
        rule.onNodeWithTag("session-count").assertTextEquals("Sessions: 1")
    }
    @Test fun existingActiveVisitTransitionsToOneCompletedItemAndConsistentReport() {
        val a=System.currentTimeMillis()-3600000
        var open=LibraryVisit(start=a,elapsedStart=0,boot=1,minimumMinutes=10)
        var window:FocusWindow?=null
        runBlocking {
            val id=app.db.focusDao().insertVisit(open);open=open.copy(id=id)
            val session=app.db.dao().insertSession(StudySession(startedAt=a,trigger="Wi-Fi",measurementStart=a))
            app.db.dao().insertSegment(Segment(sessionId=session,start=a,elapsedStart=0,boot=1))
            val w=FocusWindow(sessionId=session,start=a,elapsedStart=0,boot=1,mode="OFF")
            window=w.copy(id=app.db.focusDao().insertWindow(w))
        }
        history()
        rule.onNodeWithTag("session-count").assertTextEquals("Today's sessions: 0")
        rule.onNodeWithTag("session-in-progress").assertTextEquals("1 in progress")
        rule.onNodeWithTag("view-all-sessions").performScrollTo().performClick()
        rule.onNodeWithTag("active-library-session-${open.id}").assertExists()
        rule.onNodeWithText("Current session — In progress").assertExists()
        capture("active-session")
        runBlocking {
            val closed=VisitQualification.finish(open,System.currentTimeMillis())
            app.db.focusDao().updateVisit(closed)
            app.db.focusDao().updateWindow(window!!.copy(end=closed.end))
        }
        rule.waitUntil(10000){rule.onAllNodesWithTag("library-session-${open.id}").fetchSemanticsNodes().size==1}
        rule.onNodeWithTag("active-library-session-${open.id}").assertDoesNotExist()
        rule.onAllNodesWithText("Library visit").assertCountEquals(1)
        rule.onNodeWithText("Back to history").performScrollTo().performClick()
        rule.onNodeWithTag("session-count").assertTextEquals("Today's sessions: 1")
        rule.onNodeWithTag("session-in-progress").assertDoesNotExist()
        assertEquals(4,runBlocking{app.db.focusDao().visits().first().size})
    }
    @Test fun pendingAndCompletedSessionCardsExcludeAwayTimeAndRefreshSameRow() {
        val a=System.currentTimeMillis()-3600000L
        val cutoff=a+50*60000L
        var visit=LibraryVisit(start=a,elapsedStart=0,boot=1,minimumMinutes=10,checkpoint=cutoff,awaySince=cutoff)
        runBlocking {
            visit=visit.copy(id=app.db.focusDao().insertVisit(visit))
        }
        history()
        rule.onNodeWithTag("session-in-progress").assertTextEquals("1 in progress")
        rule.onNodeWithTag("view-all-sessions").performScrollTo().performClick()
        rule.onNodeWithText("Visit duration: 00:50:00").assertExists()
        rule.onNodeWithText("Temporarily away — awaiting return").assertExists()
        capture("pending-away-frozen")
        rule.mainClock.advanceTimeBy(5000);rule.waitForIdle()
        rule.onNodeWithText("Visit duration: 00:50:00").assertExists()
        runBlocking {app.db.focusDao().updateVisit(VisitQualification.finish(visit,System.currentTimeMillis()))}
        rule.waitUntil(10000){rule.onAllNodesWithTag("library-session-${visit.id}").fetchSemanticsNodes().size==1}
        rule.onNodeWithTag("active-library-session-${visit.id}").assertDoesNotExist()
        rule.onNodeWithText("0h 50m • threshold 10 min").assertExists()
        rule.onNodeWithTag("library-session-${visit.id}").performScrollTo().performClick()
        rule.onNodeWithText("Full visit: 00:50:00").assertExists()
        capture("completed-away-excluded")
        rule.onNodeWithText("Back to history").performScrollTo().performClick()
        rule.onNodeWithText("Back to history").performScrollTo().performClick()
        rule.onNodeWithTag("session-count").assertTextEquals("Today's sessions: 1")
    }
    @Test fun manualAndAutomaticHistorySessionsShareOneCardLayout()=runBlocking {
        val a=start(today)+60_000L
        val visit=VisitQualification.finish(LibraryVisit(start=a,elapsedStart=0,boot=1,minimumMinutes=10),a+20*60_000L)
        app.db.focusDao().insertVisit(visit)
        val id=app.db.dao().insertSession(StudySession(startedAt=a+60*60_000L,endedAt=a+80*60_000L,trigger="Manual"))
        app.db.dao().insertSegment(Segment(sessionId=id,start=a+60*60_000L,end=a+80*60_000L,elapsedStart=0,boot=1))
        history()
        rule.onNodeWithTag("view-all-sessions").performScrollTo().performClick()
        rule.onAllNodesWithTag("history-session-card-layout",useUnmergedTree=true).assertCountEquals(2)
        rule.onNodeWithText("Library visit • one continuous visit").assertExists()
        rule.onNodeWithText("Manual timer").assertExists()
        Unit
    }
    @Test fun equalPresetGeometryBalancedRowsAndSelectedStates() {
        content();rule.onNode(hasText("Settings") and hasAnyAncestor(hasTestTag("main-navigation")),useUnmergedTree=true).performClick()
        rule.onNodeWithText("Goals & streak").performScrollTo().performClick()
        val groups=mapOf("goal-hours" to listOf(60,120,180,240,300,360,480,600),
            "goal-streak" to listOf(30,60,90,120,180,200),"goal-minimum" to listOf(5,10,15,20,30))
        groups.forEach{(tag,values)->
            rule.onNodeWithTag("$tag-presets").performScrollTo()
            val nodes=values.map{rule.onNodeWithTag("$tag-preset-$it").fetchSemanticsNode()}
            capture("$tag-geometry")
            val bounds=values.map{rule.onNodeWithTag("$tag-preset-$it").getUnclippedBoundsInRoot()}
            val widths=bounds.map{it.right.value-it.left.value};val heights=bounds.map{it.bottom.value-it.top.value}
            assertTrue("$tag widths $widths, raw node sizes ${nodes.map{it.size}}",widths.max()-widths.min()<=1)
            assertEquals("$tag heights $heights",1,heights.toSet().size)
            val rows=nodes.groupBy{it.positionInRoot.y}
            val firstCount=rows.values.first().size
            if(scale==1f)assertEquals(if(tag=="goal-hours")4 else 3,firstCount)
            else assertTrue(firstCount<if(tag=="goal-hours")4 else 3)
            rows.values.forEach{row->
                row.zipWithNext().forEach{(a,b)->assertTrue(b.positionInRoot.x>=a.positionInRoot.x+a.size.width)}
            }
            val grid=rule.onNodeWithTag("$tag-presets").fetchSemanticsNode()
            val last=rows.values.last()
            val left=last.first().positionInRoot.x-grid.positionInRoot.x
            val right=grid.positionInRoot.x+grid.size.width-last.last().positionInRoot.x-last.last().size.width
            assertTrue("Last row is centered",abs(left-right)<=2)
            fits("$tag-presets");capture("$tag-aligned")
        }
        rule.onNodeWithTag("goal-minimum-preset-15").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithTag("goal-minimum").performScrollTo().assertTextContains("15")
        rule.onNodeWithTag("goal-minimum").performTextReplacement("46")
        rule.onNodeWithTag("goal-minimum-preset-15").assertIsNotSelected()
        rule.onNodeWithTag("goals-save").performScrollTo().performClick()
        rule.waitUntil(10000){Preferences(app).value.minimumSessionMinutes==46}
        rule.onNodeWithText("Goals & streak").performScrollTo().performClick()
        rule.onNodeWithTag("goal-minimum").performScrollTo().assertTextContains("46")
    }
    @Test fun sharedCustomInputsSurviveSaveableStateRestorationAndRecomposition() {
        val restoration=StateRestorationTester(rule)
        val config=mutableStateOf(Config(goalMinutes=450,streakMinutes=200,minimumSessionMinutes=45))
        val dark=mutableStateOf(false)
        restoration.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)) {
                MaterialTheme(colorScheme=if(dark.value) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                        GoalFields(config.value){config.value=it}
                    }
                }
            }
        }
        rule.onNodeWithTag("goal-minimum").performScrollTo().performTextReplacement("46")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("goal-minimum").performScrollTo().assertTextContains("46")
        rule.onNodeWithTag("goal-hours").assertTextContains("7.5")
        rule.runOnIdle{dark.value=true};fits("goal-fields");capture("goals-restored-dark")
        assertEquals(46,config.value.minimumSessionMinutes)
    }
}
@org.robolectric.annotation.Config(qualifiers="w411dp-h891dp-xhdpi")
class SimplificationPhoneTest:SimplificationUiCases()
@org.robolectric.annotation.Config(qualifiers="w320dp-h640dp-mdpi")
class SimplificationSmallPhoneTest:SimplificationUiCases(){override val scale=1.5f}
@org.robolectric.annotation.Config(qualifiers="w600dp-h960dp-mdpi")
class SimplificationLargeScreenTest:SimplificationUiCases()
