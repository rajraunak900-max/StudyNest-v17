package com.studynest.app

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import com.studynest.app.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.*
import java.util.TimeZone

/** Replays exported structural records; does not pretend to reproduce past UI state. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=StudyNestApp::class,qualifiers="w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhoneSessionReplayTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private lateinit var app:StudyNestApp
    private lateinit var priorZone:TimeZone
    private lateinit var sessions:List<StudySession>
    private lateinit var segments:List<Segment>
    private val day=LocalDate.of(2026,10,3)
    private val zone=ZoneId.of("Asia/Kolkata")
    private fun JSONObject.nullLong(key:String)=if(isNull(key))null else getLong(key)
    @Before fun setup():Unit=runBlocking {
        priorZone=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone(zone))
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach{it.join()}
        app.preferences.save(Config(setup=true,wifi=true,geo=false,vibrate=false,minimumSessionMinutes=10))
        val fixture=JSONObject(javaClass.getResourceAsStream("/phone-session-2026-10-03.json")!!.bufferedReader().use{it.readText()})
        val visits=fixture.getJSONArray("library_visits")
        for(i in 0 until visits.length()) {val r=visits.getJSONObject(i)
            app.db.focusDao().insertVisit(LibraryVisit(id=r.getLong("id"),start=r.getLong("start"),end=r.nullLong("end"),
                checkpoint=r.getLong("checkpoint"),elapsedStart=r.getLong("elapsedStart"),boot=r.getInt("boot"),
                minimumMinutes=r.nullLong("minimumMinutes")?.toInt(),qualified=r.nullLong("qualified")?.let{it==1L}))
        }
        val rows=fixture.getJSONArray("sessions")
        for(i in 0 until rows.length()) {val r=rows.getJSONObject(i)
            app.db.dao().insertSession(StudySession(id=r.getLong("id"),startedAt=r.getLong("startedAt"),endedAt=r.nullLong("endedAt"),
                trigger=r.getString("trigger"),paused=r.getInt("paused")==1,pausedByDeparture=r.getInt("pausedByDeparture")==1,
                endedExplicitly=r.getInt("endedExplicitly")==1,measurementStart=r.nullLong("measurementStart")))
        }
        val parts=fixture.getJSONArray("segments")
        for(i in 0 until parts.length()) {val r=parts.getJSONObject(i)
            app.db.dao().insertSegment(Segment(id=r.getLong("id"),sessionId=r.getLong("sessionId"),start=r.getLong("start"),
                end=r.nullLong("end"),checkpoint=r.getLong("checkpoint"),elapsedStart=r.getLong("elapsedStart"),boot=r.getInt("boot")))
        }
        sessions=app.db.dao().sessions().first();segments=app.db.dao().segments().first()
        val saved=app.db.focusDao().visits().first()
        assertEquals(listOf(13L),VisitQualification.forDay(saved,day,zone).map{it.id})
        assertTrue(VisitQualification.forDay(saved,day.plusDays(1),zone).isEmpty())
        val visit=saved.single{it.id==13L}
        assertEquals(6_884_220L,visit.end!!-visit.start);assertEquals(10,visit.minimumMinutes)
        val index=HistoryIndex(saved,emptyList(),emptyList(),emptyList(),zone,1791087413170L)
        assertEquals(2,HistoryTrackingIndex(sessions,segments,zone).periods(day,index.visitCoverage(day,1791087413170L),app.engine::range).size)
    }
    @After fun close() {
        rule.runOnIdle{rule.activity.setContent {}};rule.waitForIdle()
        app.scope.cancel();app.db.close();TimeZone.setDefault(priorZone)
    }
    private fun content(stable:Boolean) {
        val totals=StudyMath.dailyTotals(segments.map(app.engine::range),zone)
        rule.setContent {MaterialTheme {
            if(stable)PhoneHistory165Replay(app,sessions,segments,totals,Streaks(0,0),day.plusDays(1))
            else HistoryScreen(app,sessions,segments,totals,Streaks(0,0),day.plusDays(1))
        }}
        rule.onNodeWithTag("history-tab-Calendar").performScrollTo().performClick()
        rule.onNodeWithTag("calendar-day-$day").performScrollTo().performClick()
    }
    private fun capture(name:String) {rule.runOnIdle {
        val view=rule.activity.window.decorView
        val image=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(image))
        val file=java.io.File("build/reports/screenshots/phone-session-$name.png");file.parentFile!!.mkdirs()
        java.io.FileOutputStream(file).use{image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }}
    @Test fun stable165HistoryIncludesActualCompletedEveningVisitOnSelectedDate() {
        content(true)
        rule.waitUntil(15000){rule.onAllNodesWithText("Qualifying library sessions: 1").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("Qualifying library sessions: 1").assertExists()
        rule.onNodeWithText("View all sessions").performScrollTo().performClick()
        rule.onAllNodesWithTag("library-session-13").assertCountEquals(1)
        rule.onNodeWithTag("library-session-13").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1h 54m • threshold 10 min").assertExists();capture("stable165")
    }
    @Test fun current166ReportAndSessionListPreserveActualEveningVisitAndEarlierPeriods() {
        content(false)
        rule.waitUntil(15000){rule.onAllNodesWithText("Sessions: 3").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("session-count").assertTextEquals("Sessions: 3")
        rule.onNodeWithTag("view-all-sessions").performScrollTo().performClick()
        rule.onAllNodesWithTag("library-session-13").assertCountEquals(1)
        rule.onNodeWithTag("library-session-12").assertDoesNotExist()
        rule.onNodeWithTag("library-session-13").performScrollTo().assertIsDisplayed();capture("current166")
        rule.onNodeWithTag("library-session-13").performClick()
        rule.onNodeWithText("Full visit: 01:54:44").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Minimum at arrival: 10 minutes").assertExists()
        assertEquals(13,runBlocking{app.db.focusDao().visits().first().size})
    }
    @Test fun stable165OpenSessionListRefreshesWhenSameEveningRowCompletes():Unit=runBlocking {
        val completed=app.db.focusDao().visits().first().single{it.id==13L}
        app.db.focusDao().updateVisit(completed.copy(end=null,qualified=null))
        content(true)
        rule.waitUntil(15000){rule.onAllNodesWithText("Qualifying library sessions: 0").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("View all sessions").performScrollTo().performClick()
        rule.onNodeWithTag("library-session-13").assertDoesNotExist()
        // Normal DAO invalidation must update the existing screen, with no
        // tab switch, app reopen or new visit identity required.
        app.db.focusDao().updateVisit(completed)
        rule.waitUntil(15000){rule.onAllNodesWithTag("library-session-13").fetchSemanticsNodes().isNotEmpty()}
        rule.onAllNodesWithTag("library-session-13").assertCountEquals(1)
        rule.onNodeWithText("Qualifying library sessions: 1").assertExists()
        rule.onNodeWithTag("library-session-13").performScrollTo().assertIsDisplayed()
        assertEquals(completed,app.db.focusDao().visits().first().single{it.id==13L})
    }
}
