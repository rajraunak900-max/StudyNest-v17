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
abstract class Post169HelpCases {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    protected open val scale=1f
    private lateinit var app:StudyNestApp
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext();app.scope.coroutineContext[Job]!!.children.toList().forEach{it.join()}
        app.preferences.save(Config(setup=true,wifi=false,geo=false,vibrate=false,appMonitoring=false,monitorCallAudio=false))
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        for((pkg,label) in listOf("com.whatsapp" to "WhatsApp")+(1..20).map{"com.fixture.$it" to "Example app $it"}) {
            val info=ResolveInfo().apply{activityInfo=ActivityInfo().apply{
                name="$pkg.Main";packageName=pkg;enabled=true;exported=true
                applicationInfo=ApplicationInfo().apply{packageName=pkg;nonLocalizedLabel=label;enabled=true}
            }}
            shadowOf(app.packageManager).addResolveInfoForIntent(intent,info)
        }
    }
    @After fun close(){rule.runOnIdle{rule.activity.setContent{}};rule.waitForIdle();rule.activityRule.scenario.close()
        runBlocking{app.scope.coroutineContext[Job]!!.cancelAndJoin()};app.db.close()}
    private fun capture(name:String){rule.runOnIdle{
        val view=rule.activity.window.decorView;val b=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(b));val f=java.io.File("build/reports/screenshots/1610-${javaClass.simpleName}-$name.png")
        f.parentFile!!.mkdirs();java.io.FileOutputStream(f).use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }}
    @Test fun expandingAtBottomImmediatelyShowsExplanationAndKeepsSearchAndDraft(){
        val original=Preferences(app).value
        rule.setContent{CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)){StudyNest(app,refreshTimer=false)}}
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        rule.onNodeWithText("App Monitoring").performScrollTo().performClick()
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-search"))
        rule.onNodeWithTag("app-search").performTextReplacement("wha")
        rule.waitUntil(15000){rule.onAllNodesWithTag("app-com.whatsapp").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-com.whatsapp"))
        rule.onNodeWithTag("app-com.whatsapp").performClick().assertIsOn()
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasText("How App Monitoring works"))
        rule.onNodeWithText("How App Monitoring works").assertIsDisplayed();capture("help-collapsed")
        rule.onNodeWithText("How App Monitoring works").performClick();rule.waitForIdle();capture("help-expanded")
        val text="Sitting Time continues while selected apps pause Actual Study."
        val next="Turning monitoring off keeps your app selections saved and subtracts no app time."
        // The original handler exists, but off-screen expansion looks like a dead control.
        rule.onNodeWithText(text).assertIsDisplayed()
        // The useful first explanation must be visible immediately. At 320dp
        // with large fonts the rest necessarily needs ordinary vertical scrolling.
        rule.onNodeWithText(next).performScrollTo().assertIsDisplayed()
        assertEquals(original,Preferences(app).value)
        rule.onNodeWithText("1 app selected • Unsaved changes").assertExists()
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasText("How App Monitoring works"))
        rule.onNodeWithText("How App Monitoring works").performClick();rule.onNodeWithText(text).assertDoesNotExist()
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-search"))
        rule.onNodeWithTag("app-search").assertTextContains("wha")
        rule.onNodeWithTag("monitoring-list").performScrollToNode(hasTestTag("app-com.whatsapp"))
        rule.onNodeWithTag("app-com.whatsapp").assertIsOn();assertEquals(original,Preferences(app).value)
        rule.onNodeWithText("Save Changes").performClick();rule.waitUntil(15000){"com.whatsapp" in app.preferences.value.monitoredApps}
        assertFalse(app.preferences.value.appMonitoring)
        assertEquals(setOf("com.whatsapp"),Preferences(app).value.monitoredApps)
    }
}
@org.robolectric.annotation.Config(qualifiers="w411dp-h891dp-xhdpi")
class Post169HelpPhoneTest:Post169HelpCases()
@org.robolectric.annotation.Config(qualifiers="w320dp-h640dp-mdpi")
class Post169HelpSmallPhoneTest:Post169HelpCases(){override val scale=1.5f}
@org.robolectric.annotation.Config(qualifiers="w600dp-h960dp-mdpi")
class Post169HelpLargeScreenTest:Post169HelpCases()
