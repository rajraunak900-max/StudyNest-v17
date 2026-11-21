package com.studynest.app

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.ui.*
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.*

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=StudyNestApp::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ChartPolishUiCases {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    protected open val scale=1f
    private lateinit var app:StudyNestApp
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach {it.join()}
        app.preferences.save(Config(setup=true,vibrate=false,theme="Light"))
    }
    @After fun close(){rule.runOnIdle {rule.activity.setContent{}};rule.waitForIdle();app.scope.cancel()}
    @Composable private fun chart(today:LocalDate) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)) {
            MaterialTheme { Column(androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) {
                ComparativeStudyChart(emptyMap(),today,today,{})
            } }
        }
    }
    private fun visible(day:LocalDate) {
        rule.waitForIdle()
        val viewport=rule.onNodeWithTag("history-chart-viewport").getUnclippedBoundsInRoot()
        val cell=rule.onNodeWithTag("chart-day-$day").getUnclippedBoundsInRoot()
        assertTrue("$day left clipped",cell.left.value>=viewport.left.value-1)
        assertTrue("$day right clipped",cell.right.value<=viewport.right.value+1)
        rule.onNodeWithTag("chart-day-$day").assertIsDisplayed()
    }
    private fun capture(name:String)=rule.runOnIdle {
        val view=rule.activity.window.decorView
        val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val file=java.io.File("build/reports/screenshots/${javaClass.simpleName}-$name.png");file.parentFile!!.mkdirs()
        java.io.FileOutputStream(file).use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        Unit
    }
    @Test fun currentWeekShowsMondayThursdayAndSundayImmediately() {
        var day by mutableStateOf(LocalDate.of(2026,10,5))
        rule.setContent {key(day){chart(day)}}
        for(d in listOf(day,day.plusDays(3),day.plusDays(6))) {rule.runOnIdle{day=d};visible(d)}
        capture("week-sunday")
    }
    @Test fun currentMonthShowsFirstMiddleAndLastImmediately() {
        var day by mutableStateOf(LocalDate.of(2026,10,1))
        rule.setContent {key(day){chart(day)}}
        for(d in listOf(day,day.withDayOfMonth(15),day.withDayOfMonth(31))) {
            rule.runOnIdle{day=d};rule.onNodeWithText("Month").performClick();visible(d)
        }
        capture("month-last-day")
    }
    @Test fun manualScrollingRotationAndHistoricalNavigationDoNotSnapBack() {
        val day=LocalDate.of(2026,10,20);var revision by mutableIntStateOf(0)
        val restoration=StateRestorationTester(rule)
        restoration.setContent {revision;chart(day)}
        rule.onNodeWithText("Month").performClick();visible(day)
        rule.onNodeWithTag("chart-day-${day.withDayOfMonth(1)}").performScrollTo();rule.waitForIdle()
        visible(day.withDayOfMonth(1))
        rule.runOnIdle{revision++};visible(day.withDayOfMonth(1))
        restoration.emulateSavedInstanceStateRestore();visible(day.withDayOfMonth(1))
        rule.onNodeWithContentDescription("Previous period").performClick()
        visible(LocalDate.of(2026,9,1));rule.onNodeWithTag("chart-day-2026-10-20").assertDoesNotExist()
        rule.onNodeWithContentDescription("Next period").performClick();visible(day)
        rule.onNodeWithText("Week").performClick();visible(day)
        rule.onNodeWithText("Month").performClick();visible(day)
    }
    @Test fun oldWeekMonthReportAtBottomShowsRealAggregateMetrics()=runBlocking {
        val day=LocalDate.now();val a=day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val id=app.db.dao().insertSession(StudySession(startedAt=a,endedAt=a+7200000,trigger="Manual",measurementStart=a))
        app.db.dao().insertSegment(Segment(sessionId=id,start=a,end=a+7200000,elapsedStart=0,boot=1))
        app.db.focusDao().insertVisit(VisitQualification.finish(LibraryVisit(start=a,elapsedStart=0,boot=1,minimumMinutes=10),a+7200000))
        app.db.focusDao().insertWindow(FocusWindow(sessionId=id,start=a,end=a+7200000,elapsedStart=0,boot=1,mode="OFF"))
        rule.setContent {CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)){StudyNest(app,refreshTimer=false)}}
        rule.onNodeWithText("History",useUnmergedTree=true).performClick()
        rule.onNodeWithText("Week & Month Report").performScrollTo().performClick()
        rule.waitUntil(15000){rule.onAllNodes(hasTestTag("metric-Sitting Time") and hasAnyAncestor(hasTestTag("period-report"))).fetchSemanticsNodes().isNotEmpty()}
        // 1.6.9 removes duplicate labels only; the real report metrics must remain.
        for(label in listOf("Tracked study:","All-time tracked study:","Daily average:","Best streak:"))
            rule.onNode(hasText(label,substring=true) and hasAnyAncestor(hasTestTag("period-report"))).assertDoesNotExist()
        for(label in listOf("Sitting Time","Actual Study","Distraction","Break","Efficiency"))
            rule.onNode(hasText(label) and hasAnyAncestor(hasTestTag("period-report"))).assertExists()
        rule.onNodeWithText("This month").performScrollTo().performClick()
        rule.onAllNodes(hasText("02:00:00") and hasAnyAncestor(hasTestTag("period-report"))).assertCountEquals(2)
        for(label in listOf("Tracked study:","All-time tracked study:","Daily average:","Best streak:"))
            rule.onNode(hasText(label,substring=true) and hasAnyAncestor(hasTestTag("period-report"))).assertDoesNotExist()
        rule.onNodeWithTag("history-tab-Session info").assertDoesNotExist()
        rule.onNodeWithTag("period-report").performScrollTo();capture("bottom-report")
    }
    @Test fun reopeningHistoryMakesTodayVisibleAfterManualScroll() {
        rule.setContent{CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)){StudyNest(app,refreshTimer=false)}}
        val today=LocalDate.now();val monday=today.minusDays((today.dayOfWeek.value-1).toLong())
        rule.onNodeWithText("History",useUnmergedTree=true).performClick()
        rule.onNodeWithTag("history-chart").performScrollTo();visible(today)
        rule.onNodeWithTag("chart-day-$monday").performScrollTo();visible(monday)
        rule.onNodeWithText("Home",useUnmergedTree=true).performClick()
        rule.onNodeWithText("History",useUnmergedTree=true).performClick()
        rule.onNodeWithTag("history-chart").performScrollTo();visible(today)
    }
    @Test fun periodSummaryDisplaysCorrectTotalAverageBestAndUnknownValues() {
        val monday=LocalDate.of(2026,10,5);val today=monday.plusDays(3)
        var unknown by mutableStateOf(false)
        rule.setContent{MaterialTheme {
            val data=mapOf(monday to HistoryDay(if(unknown)null else 7200000,9000000,7200000,1800000),
                monday.plusDays(2) to HistoryDay(3600000,3600000,3600000,0))
            PeriodSummary(HistoryMath.period(data,monday,monday.plusDays(6),today),"Actual Study period total")
        }}
        rule.onNodeWithText("3h 0m").assertExists();rule.onNodeWithText("0h 45m").assertExists()
        rule.onNodeWithText("2h 0m").assertExists();rule.onNodeWithText("Mon, 5 Oct").assertExists()
        rule.onNodeWithText("4 elapsed days").assertExists();rule.onNodeWithText("Sitting Time: 3h 30m").assertExists()
        capture("known-period-summary")
        rule.runOnIdle{unknown=true}
        rule.onAllNodesWithText("Unavailable").assertCountEquals(3)
        rule.onNodeWithText("Sitting Time: 3h 30m").assertExists();rule.onNodeWithText("4 elapsed days").assertExists()
    }
    @Test fun aboutCreditsAuthorAndBriefFeatures() {
        rule.setContent{StudyNest(app,refreshTimer=false)}
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        rule.onNodeWithText("About StudyNest").performScrollTo().performClick()
        rule.onNodeWithText("Created by RAUNAK RAJ").assertExists()
        rule.onNodeWithText("Manual and automatic study timers",substring=true).assertExists()
        rule.onNodeWithText("calendar and week/month reports",substring=true).assertExists()
        rule.onNodeWithText("Plan tasks",substring=true).assertExists()
        rule.onNodeWithText("Optional setup and permission guidance",substring=true).assertExists()
    }
}
@org.robolectric.annotation.Config(qualifiers="w411dp-h891dp-xhdpi")
class ChartPolishPhoneTest:ChartPolishUiCases()
@org.robolectric.annotation.Config(qualifiers="w320dp-h640dp-mdpi")
class ChartPolishSmallPhoneTest:ChartPolishUiCases(){override val scale=1.5f}
@org.robolectric.annotation.Config(qualifiers="w600dp-h960dp-mdpi")
class ChartPolishLargeScreenTest:ChartPolishUiCases()
