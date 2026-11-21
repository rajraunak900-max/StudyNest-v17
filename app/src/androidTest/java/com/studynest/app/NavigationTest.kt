package com.studynest.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.studynest.app.data.Config
import com.studynest.app.ui.StudyNest
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bottomNavigationAndTaskCreation() {
        val app = ApplicationProvider.getApplicationContext<StudyNestApp>()
        app.preferences.save(Config(setup = true, vibrate = false))
        rule.setContent { StudyNest(app, refreshTimer = false) }
        rule.onNodeWithText("Start Study Session").assertExists()
        rule.onNodeWithText("Tasks", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Add task").performClick()
        rule.onNodeWithText("What will you work on?").performTextInput("Read chapter one")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Read chapter one").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("History", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Your progress").assertExists()
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Automatic study detection").assertExists()
        rule.onNodeWithText("Home", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Start Study Session").assertExists()
    }
}
