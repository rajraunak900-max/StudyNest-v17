package com.studynest.app

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.Notifications
import com.studynest.app.data.*
import com.studynest.app.ui.StudyNest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.*

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = StudyNestApp::class)
class TimerRolloverUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var app: StudyNestApp
    @Before fun setup() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        app.preferences.save(Config(setup = true, wifi = true, geo = true, vibrate = false))
    }
    @After fun close() {
        rule.runOnIdle { rule.activity.setContent {} }; rule.waitForIdle()
        app.scope.cancel(); app.db.close()
    }
    @Test fun returningHomeClearsYesterdaysDepartedClockAndNotificationActions() = runBlocking {
        val midnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val start = midnight - 8 * 3600000L
        val end = start + (7 * 3600 + 2 * 60 + 29) * 1000L
        val id = app.db.dao().insertSession(StudySession(startedAt = start, trigger = "Wi-Fi",
            paused = true, pausedByDeparture = true))
        app.db.dao().insertSegment(Segment(sessionId = id, start = start, end = end,
            checkpoint = end, elapsedStart = 0, boot = 1))
        app.db.dao().saveState(AutomationState())
        val segments = app.db.dao().segments().first()
        val before = Notifications.current(app)
        // 1.6.9 deliberately displays today's library metrics, never yesterday's timer.
        assertEquals("Sitting: 00:00:00 • Actual: 00:00:00", before.extras.getString(android.app.Notification.EXTRA_TEXT))
        assertEquals("Paused", before.extras.getString("studynest.timerState"))
        assertEquals(0L, before.extras.getLong("studynest.sittingMs"))
        assertEquals(0L, before.extras.getLong("studynest.studyMs"))
        rule.setContent { StudyNest(app, refreshTimer = false) }
        rule.waitUntil(15000) { runBlocking { app.db.dao().active() == null } }
        rule.onNodeWithTag("primary-study-clock").assertTextEquals("00:00:00")
        assertEquals(segments, app.db.dao().segments().first())
        assertEquals(id, app.db.dao().sessions().first().single().id)
        val notification = Notifications.current(app)
        assertTrue(notification.actions.isNullOrEmpty())
        assertEquals("Sitting: 00:00:00 • Actual: 00:00:00", notification.extras.getString(android.app.Notification.EXTRA_TEXT))
        assertEquals("Monitoring", notification.extras.getString("studynest.timerState"))
        Unit
    }
}
