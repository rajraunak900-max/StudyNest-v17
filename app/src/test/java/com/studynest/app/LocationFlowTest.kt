package com.studynest.app

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.backgroundLocationAllowed
import com.studynest.app.ui.PermissionPanel
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Intercepts the public ActivityResult launch boundary. OEM rendering remains a
 * phone check; request order, settings fallback and lifecycle refresh are real UI. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[26,29,30,33,34,35,36],application=StudyNestApp::class)
class LocationFlowTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private lateinit var app:StudyNestApp
    private var requestCode=0
    private val inputs=mutableListOf<Any?>()
    private val registry=object:ActivityResultRegistry() {
        override fun <I,O> onLaunch(code:Int,contract:ActivityResultContract<I,O>,input:I,options:ActivityOptionsCompat?) {
            requestCode=code;inputs+=input
        }
    }
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        rule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalActivityResultRegistryOwner provides object:ActivityResultRegistryOwner {
                override val activityResultRegistry get()=registry
            }) {
                MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { PermissionPanel(true,false) } }
            }
        }
    }
    @After fun close() {
        rule.runOnIdle { rule.activity.setContent {} };rule.waitForIdle()
        app.breakReminder.close();app.scope.cancel();app.db.close()
    }
    private fun foreground() {
        rule.onNodeWithTag("grant-location").performScrollTo().performClick()
        val actual=inputs.last() as Array<*>
        assertEquals(setOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),actual.toSet())
        assertFalse(actual.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        rule.runOnIdle { registry.dispatchResult(requestCode,mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.ACCESS_COARSE_LOCATION to true)) }
    }
    @Test fun foregroundSuccessOffersOnlyASeparateOptionalBackgroundStep() {
        foreground()
        if(android.os.Build.VERSION.SDK_INT<29) {
            rule.onNodeWithText("Continue to Location access").assertDoesNotExist();assertEquals(1,inputs.size)
        } else {
            rule.onNodeWithText("Set up later").assertExists()
            rule.onNodeWithText("Continue to Location access").performClick()
            assertEquals(2,inputs.size);assertEquals(Manifest.permission.ACCESS_BACKGROUND_LOCATION,inputs.last())
            assertFalse(app.backgroundLocationAllowed())
        }
    }
    @Test fun skippingBackgroundDoesNotGrantItOrIssueAnotherRequest() {
        foreground()
        if(android.os.Build.VERSION.SDK_INT>=29) {
            rule.onNodeWithText("Set up later").performClick()
            assertFalse(app.backgroundLocationAllowed())
            rule.onNode(hasText("Action required for this feature") and hasAnyAncestor(hasTestTag("access-background"))).assertExists()
        }
        assertEquals(1,inputs.size)
    }
    @Test fun settingsReturnRefreshesBackgroundStatusAndRetainsForeground() {
        foreground()
        if(android.os.Build.VERSION.SDK_INT>=29) {
            rule.onNodeWithText("Continue to Location access").performClick()
            rule.runOnIdle { registry.dispatchResult(requestCode,false) }
            rule.onNodeWithText("Background location is still off.",substring=true).assertExists()
            rule.onNodeWithTag("grant-background").performScrollTo().performClick()
            rule.onNodeWithText("Open app settings").performClick()
            val intent=shadowOf(rule.activity).nextStartedActivity
            assertEquals(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,intent.action)
            assertEquals("package:${app.packageName}",intent.data.toString())
            shadowOf(app).grantPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            rule.onNode(hasText("Granted ✓") and hasAnyAncestor(hasTestTag("access-background"))).assertExists()
            rule.onNodeWithText("Background location is still off.",substring=true).assertDoesNotExist()
        }
        assertTrue(app.backgroundLocationAllowed())
    }
}
