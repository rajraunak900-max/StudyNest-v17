package com.studynest.app

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import com.studynest.app.automation.Notifications
import com.studynest.app.ui.StudyNest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.*

/** Real native Compose rendering and Room flow replay; not a physical-device test. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=StudyNestApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class Post169HomeCases {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    protected open val scale=1f
    private lateinit var app:StudyNestApp
    private val day=LocalDate.now()
    private val start=day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()+3600000L
    private var sessionId=0L
    private var visitId=0L
    private var windowId=0L
    private var segmentId=0L
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach{it.join()}
        app.preferences.save(Config(setup=true,theme="Light",wifi=true,vibrate=false,goalMinutes=120,streakMinutes=120,
            appMonitoring=true,monitorCallAudio=false,monitoredApps=setOf("com.distracting")))
    }
    @After fun close(){
        rule.runOnIdle{rule.activity.setContent{}};rule.waitForIdle();rule.activityRule.scenario.close()
        runBlocking{app.scope.coroutineContext[Job]!!.cancelAndJoin()};app.db.close()
    }
    private fun seed(actual:Long,distraction:Long,unknown:Long=0,offset:Long=0,library:Boolean=true)=runBlocking {
        val start=this@Post169HomeCases.start+offset*60000L
        val end=start+(actual+distraction+unknown)*60000L
        sessionId=app.db.dao().insertSession(StudySession(startedAt=start,trigger=if(library) "Wi-Fi" else "Manual",paused=true,measurementStart=start))
        segmentId=app.db.dao().insertSegment(Segment(sessionId=sessionId,start=start,end=end,elapsedStart=0,boot=1))
        visitId=if(library) app.db.focusDao().insertVisit(VisitQualification.finish(LibraryVisit(start=start,elapsedStart=0,boot=1,minimumMinutes=10),end).copy(id=0)) else 0L
        windowId=app.db.focusDao().insertWindow(FocusWindow(sessionId=sessionId,start=start,end=end,elapsedStart=0,boot=1,
            mode="ON",packages="[\"com.distracting\"]",cursor=end,tracker=UsageState(known=true).encode(),seeded=true))
        val pieces=mutableListOf<DistractionPeriod>()
        if(distraction>0)pieces+=DistractionPeriod(windowId=windowId,start=start,end=start+distraction*60000L,reason="APP",packageName="com.distracting")
        if(unknown>0)pieces+=DistractionPeriod(windowId=windowId,start=start+distraction*60000L,end=start+(distraction+unknown)*60000L,reason="UNKNOWN")
        app.db.focusDao().insertPeriods(pieces)
    }
    private fun content(){rule.setContent{
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)){StudyNest(app,refreshTimer=false)}
    };rule.waitForIdle()}
    private fun goalText(text:String){
        val match=hasText(text) and hasAnyAncestor(hasTestTag("today-study-summary"))
        rule.waitUntil(15000){rule.onAllNodes(match,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        rule.onNode(match,useUnmergedTree=true).assertExists()
    }
    private fun progress():Float {
        val node=rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo) and
            hasAnyAncestor(hasTestTag("today-study-summary")),useUnmergedTree=true).fetchSemanticsNode()
        return node.config[SemanticsProperties.ProgressBarRangeInfo].current
    }
    private fun capture(name:String){rule.runOnIdle{
        val view=rule.activity.window.decorView
        val b=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(b));val f=java.io.File("build/reports/screenshots/1610-${javaClass.simpleName}-$name.png")
        f.parentFile!!.mkdirs();java.io.FileOutputStream(f).use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }}
    private fun finishCurrent()=runBlocking {
        val dao=app.db.dao();val active=dao.active()!!
        val end=dao.segmentsForSession(active.id).last().end!!
        dao.updateSession(active.copy(endedAt=end,endedExplicitly=true))
    }
    @Test fun activeManualShowsItsSessionMetricsWhileHistoryKeepsDailyTotals(){
        seed(60,10,library=false);finishCurrent();seed(30,15,offset=120,library=false);content();goalText("1h 30m")
        rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("00:30:00")
        rule.onNodeWithTag("live-focus-card").performScrollTo()
        rule.onNode(hasText("00:15:00") and hasAnyAncestor(hasTestTag("metric-Distraction")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("66.7%") and hasAnyAncestor(hasTestTag("metric-Efficiency")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("66.7%") and hasAnyAncestor(hasTestTag("study-timer-card")),useUnmergedTree=true).assertDoesNotExist()
        assertEquals(0.75f,progress(),0.00001f)
        rule.onNodeWithText("History").performClick()
        rule.waitUntil(15000){rule.onAllNodesWithTag("daily-library-summary").fetchSemanticsNodes().isNotEmpty()}
        rule.onNode(hasText("01:30:00") and hasAnyAncestor(hasTestTag("daily-library-summary")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("00:25:00") and hasAnyAncestor(hasTestTag("daily-library-summary")),useUnmergedTree=true).assertExists()
    }
    @Test fun distractionsInTwoVisitsNeverInflateDailyClock(){
        seed(60,20);finishCurrent();seed(30,10,offset=120);content();goalText("1h 30m")
        rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("00:30:00")
        val notification=runBlocking{Notifications.current(app)}
        assertEquals(90*60000L,notification.extras.getLong("studynest.studyMs"))
        assertEquals(120*60000L,notification.extras.getLong("studynest.sittingMs"))
    }
    @Test fun endingThenStartingFreshSessionKeepsDailyTotalAndAddsNewActual(){
        seed(120,0);content();goalText("2h 0m");finishCurrent()
        rule.waitUntil(15000){rule.onAllNodesWithText("Wi-Fi • Paused").fetchSemanticsNodes().isEmpty()}
        rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("02:00:00")
        seed(0,0,offset=180);goalText("2h 0m")
        rule.onNodeWithTag("primary-study-clock").assertTextEquals("00:00:00")
        runBlocking{
            val end=start+190*60000L;val db=app.db.openHelper.writableDatabase
            db.execSQL("UPDATE segments SET end=? WHERE id=?",arrayOf(end,segmentId))
            db.execSQL("UPDATE focus_windows SET end=?,cursor=? WHERE id=?",arrayOf(end,end,windowId))
            db.execSQL("UPDATE library_visits SET end=? WHERE id=?",arrayOf(end,visitId))
            app.db.invalidationTracker.refreshVersionsAsync()
        }
        goalText("2h 10m");rule.onNodeWithTag("primary-study-clock").assertTextEquals("00:10:00")
    }
    @Test fun activityRecreationRestoresStoredManualOutsideActual(){
        seed(135,20,library=false);finishCurrent();content();goalText("2h 15m")
        rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("02:15:00")
        rule.activityRule.scenario.recreate()
        rule.activity.setContent{
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)){StudyNest(app,refreshTimer=false)}
        }
        goalText("2h 15m");rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("02:15:00")
        // The Home summary and notification use the same all-location Actual total.
        assertEquals(135*60000L,runBlocking{Notifications.current(app)}.extras.getLong("studynest.studyMs"))
    }
    @Test fun activeManualOutsideLibraryAdvancesHomeDayAndNotificationActual(){
        val now=System.currentTimeMillis();val begin=now-45*60000L
        val elapsed=android.os.SystemClock.elapsedRealtime()-45*60000L
        runBlocking {
            val id=app.db.dao().insertSession(StudySession(startedAt=begin,trigger="Manual",measurementStart=begin))
            app.db.dao().insertSegment(Segment(sessionId=id,start=begin,elapsedStart=elapsed,boot=-1))
            app.db.focusDao().insertWindow(FocusWindow(sessionId=id,start=begin,elapsedStart=elapsed,boot=-1,mode="OFF"))
        }
        content();goalText("0h 45m")
        val notification=runBlocking{Notifications.current(app)}
        assertTrue(notification.extras.getLong("studynest.sittingMs") in 45*60000L..(45*60000L+10_000L))
        assertTrue(notification.extras.getLong("studynest.studyMs")>=45*60000L)
        assertTrue(notification.extras.getBoolean("studynest.studyRunning"))
        rule.onNode(hasText("00:45:00") and hasAnyAncestor(hasTestTag("metric-Sitting Time")),useUnmergedTree=true).assertExists()
    }
    @Test fun manualDaySummaryShowsSittingDistractionAndUnifiedEfficiency(){
        seed(60,30,library=false);content()
        rule.onNode(hasText("01:30:00") and hasAnyAncestor(hasTestTag("metric-Sitting Time")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("00:30:00") and hasAnyAncestor(hasTestTag("metric-Distraction")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("66.7%") and hasAnyAncestor(hasTestTag("metric-Efficiency")),useUnmergedTree=true).assertExists()
        rule.onNodeWithTag("today-sitting-efficiency").assertTextEquals("Sitting Time: 1h 30m • Efficiency: 66.7%")
        rule.onNodeWithText("History").performClick()
        rule.waitUntil(15000){rule.onAllNodesWithTag("daily-library-summary").fetchSemanticsNodes().isNotEmpty()}
        rule.onNode(hasText("00:30:00") and hasAnyAncestor(hasTestTag("daily-library-summary")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("66.7%") and hasAnyAncestor(hasTestTag("daily-library-summary")),useUnmergedTree=true).assertExists()
        rule.onNodeWithText("Week & Month Report").performScrollTo().performClick()
        rule.onNodeWithText("This week").assertExists()
        assertManualPeriodMetrics()
        rule.onNodeWithText("This month").performClick()
        assertManualPeriodMetrics()
    }
    private fun assertManualPeriodMetrics(){
        rule.onNode(hasText("01:00:00") and hasAnyAncestor(hasTestTag("period-report")) and hasAnyAncestor(hasTestTag("metric-Actual Study")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("00:30:00") and hasAnyAncestor(hasTestTag("period-report")) and hasAnyAncestor(hasTestTag("metric-Distraction")),useUnmergedTree=true).assertExists()
        rule.onNode(hasText("66.7%") and hasAnyAncestor(hasTestTag("period-report")) and hasAnyAncestor(hasTestTag("metric-Efficiency")),useUnmergedTree=true).assertExists()
    }
    @Test fun previousDayActualIsPreservedButNewDayClockIsZero(){
        seed(180,30,offset=-1440);finishCurrent();content();goalText("0h 0m")
        rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("00:00:00")
        val before=runBlocking{app.db.focusDao().windows().first()}
        val periods=runBlocking{app.db.focusDao().distractions().first()}
        val actual=HistoryIndex(emptyList(),before,periods,emptyList(),ZoneId.systemDefault(),System.currentTimeMillis()).live(System.currentTimeMillis())
        assertEquals(180*60000L,actual[day.minusDays(1)]!!.studyMs)
        assertEquals(before,runBlocking{app.db.focusDao().windows().first()})
    }
    @Test fun threeHourGoalAndDailyClockAgreeOnTwoActualHours(){
        runBlocking{app.preferences.save(app.preferences.value.copy(goalMinutes=180))}
        seed(120,45);content();goalText("2h 0m")
        rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("02:00:00")
        assertEquals(2f/3f,progress(),0.00001f);assertTrue(progress()<1f)
    }
    @Test fun dailyGoalAndIndependentStreakExcludeDistraction(){
        seed(105,30);content();capture("goal-before-assertion")
        goalText("1h 45m");assertEquals(0.875f,progress(),0.00001f)
        // Separate streak threshold is preserved, but only Actual earns credit.
        rule.waitUntil(15000){rule.onAllNodesWithText("0-day streak").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("0-day best").assertExists()
        assertTrue(progress()<1f)
    }
    @Test fun exactlyTwoHoursActualCompletesGoalRegardlessOfDistraction(){
        seed(120,30);content();goalText("2h 0m");assertEquals(1f,progress(),0.00001f)
        rule.waitUntil(15000){rule.onAllNodesWithText("1-day streak").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("1-day best").assertExists()
    }
    @Test fun homeClockShowsFiftyActualInsteadOfSeventyGrossMinutes(){
        seed(50,20);content();capture("timer-before-assertion")
        rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("00:50:00")
        goalText("0h 50m");assertEquals(50f/120f,progress(),0.00001f)
        rule.onNodeWithText("Today's focus").performScrollTo()
        rule.onNode(hasText("00:50:00") and hasAnyAncestor(hasTestTag("metric-Actual Study")),useUnmergedTree=true).assertExists()
        val notification=runBlocking{Notifications.current(app)}
        assertEquals(50*60000L,notification.extras.getLong("studynest.studyMs"))
        assertEquals(70*60000L,notification.extras.getLong("studynest.sittingMs"))
    }
    @Test fun unknownCoverageNeverBecomesGoalCreditOrAnInventedClock(){
        seed(50,20,30);content();goalText("0h 50m");assertEquals(50f/120f,progress(),0.00001f)
        rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("Unavailable")
        rule.onNodeWithText("Today's focus").performScrollTo()
        rule.onNode(hasText("Unavailable") and hasAnyAncestor(hasTestTag("metric-Actual Study")),useUnmergedTree=true).assertExists()
    }
    @Test fun roomUsageReplayFreezesThenResumesClockAndGoalWhileLibraryContinues(){
        seed(50,0);content();goalText("0h 50m")
        rule.onNodeWithTag("primary-study-clock").performScrollTo().assertTextEquals("00:50:00")
        fun extend(minutes:Long,distraction:Boolean)=runBlocking {
            val end=start+minutes*60000L
            val dao=app.db.openHelper.writableDatabase
            dao.execSQL("UPDATE segments SET end=? WHERE id=?",arrayOf(end,segmentId))
            dao.execSQL("UPDATE focus_windows SET end=?, cursor=? WHERE id=?",arrayOf(end,end,windowId))
            dao.execSQL("UPDATE library_visits SET end=? WHERE id=?",arrayOf(end,visitId))
            if(distraction)app.db.focusDao().insertPeriods(listOf(DistractionPeriod(windowId=windowId,start=start+50*60000L,end=end,reason="APP",packageName="com.distracting")))
            app.db.invalidationTracker.refreshVersionsAsync()
            app.db.focusDao().windows().first { rows -> rows.any { it.id==windowId && it.end==end } }
        }
        extend(70,true);goalText("0h 50m")
        rule.waitUntil(15000){progress()==50f/120f};rule.onNodeWithTag("primary-study-clock").assertTextEquals("00:50:00")
        val distracted=runBlocking{Notifications.current(app)}
        assertEquals(70*60000L,distracted.extras.getLong("studynest.sittingMs"));assertEquals(50*60000L,distracted.extras.getLong("studynest.studyMs"))
        extend(80,false);goalText("1h 0m");assertEquals(0.5f,progress(),0.00001f)
        rule.onNodeWithTag("primary-study-clock").assertTextEquals("01:00:00")
        val resumed=runBlocking{Notifications.current(app)}
        assertEquals(80*60000L,resumed.extras.getLong("studynest.sittingMs"));assertEquals(60*60000L,resumed.extras.getLong("studynest.studyMs"))
        capture("usage-replay-actual-resumed")
    }
}
@org.robolectric.annotation.Config(qualifiers="w411dp-h891dp-xhdpi")
class Post169HomePhoneTest:Post169HomeCases()
@org.robolectric.annotation.Config(qualifiers="w320dp-h640dp-mdpi")
class Post169HomeSmallPhoneTest:Post169HomeCases(){override val scale=1.5f}
@org.robolectric.annotation.Config(qualifiers="w600dp-h960dp-mdpi")
class Post169HomeLargeScreenTest:Post169HomeCases()
