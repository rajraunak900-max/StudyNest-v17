package com.studynest.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.Config
import com.studynest.app.ui.StudyNest
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(
    sdk = [33], application = StudyNestApp::class, qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bottomNavigationAndTaskCreation() {
        val app = ApplicationProvider.getApplicationContext<StudyNestApp>()
        app.preferences.save(Config(setup = true, vibrate = false))
        kotlinx.coroutines.runBlocking {
            val now = System.currentTimeMillis()
            val id = app.db.dao().insertSession(com.studynest.app.data.StudySession(startedAt = now - 600000, endedAt = now, trigger = "Manual"))
            app.db.dao().insertSegment(com.studynest.app.data.Segment(sessionId = id, start = now - 600000, end = now, elapsedStart = 0, boot = 0))
            app.db.dao().saveTask(com.studynest.app.data.StudyTask(title = "Review chapter", priority = 2))
            app.db.dao().saveTask(com.studynest.app.data.StudyTask(title = "Practise vocabulary", priority = 1))
        }
        rule.setContent { StudyNest(app, refreshTimer = false) }
        rule.onNodeWithText("Start manual timer").assertExists()
        rule.onNodeWithText("Tasks", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Add task").performClick()
        rule.onNodeWithText("What will you work on?").performTextInput("Read chapter one")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Read chapter one").fetchSemanticsNodes().isNotEmpty()
        }
        capture("tasks-light")
        rule.onNodeWithText("History", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("history-tab-Tracked study").assertExists()
        rule.onNodeWithText("Study hours").assertExists()
        rule.onNodeWithText("View all sessions").performScrollTo().performClick()
        rule.onNodeWithText("Study session 1").assertExists()
        rule.onNodeWithText("Study session 1").performScrollTo().performClick()
        rule.onNodeWithText("Tracking activity").assertExists()
        rule.onNodeWithText("Total study time: 0h 10m").assertExists()
        capture("session-details")
        rule.onNodeWithText("Back to history").performClick()
        rule.onNodeWithTag("session-information").assertExists()
        rule.onNodeWithText("Back to history").performClick()
        rule.onNodeWithTag("history-tab-Tracked study").performScrollTo().performClick()
        rule.onNodeWithText("Month", useUnmergedTree = true).performScrollTo().performClick()
        rule.onNodeWithContentDescription("Previous period").performClick()
        capture("history")
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        rule.onNodeWithText("StudyNest automation").assertExists()
        rule.onNodeWithText("Goals & streak").performScrollTo().performClick()
        rule.onNodeWithTag("goal-hours").performScrollTo().performTextReplacement("7.5")
        rule.onNodeWithText("Save goals").performScrollTo().performClick()
        rule.waitUntil(5000) { app.preferences.value.goalMinutes == 450 }
        rule.onNodeWithText("Appearance").performScrollTo().performClick()
        rule.onNodeWithText("Dark", useUnmergedTree = true).performScrollTo().performClick()
        rule.waitUntil(5000) { app.preferences.value.theme == "Dark" }
        rule.onNodeWithText("Back to settings").performClick()
        capture("settings-dark")
        rule.onNodeWithText("Home", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Start manual timer").assertExists()
        capture("home")
        rule.runOnIdle { Assert.assertEquals("No startup or settings-save error", "", app.message.value) }
    }

    private fun capture(name: String) {
        rule.runOnIdle {
            val view = rule.activity.window.decorView
            val image =
                android.graphics.Bitmap.createBitmap(
                    view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            val file = java.io.File("build/reports/screenshots/$name.png")
            file.parentFile!!.mkdirs()
            java.io.FileOutputStream(file).use {
                image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
