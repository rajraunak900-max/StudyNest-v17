package com.studynest.app

import android.content.*
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[26,33,34,35,36],application=android.app.Application::class)
class AccessNavigationTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun packageTargetedNotificationAndUsageRoutesHaveSpecificFallbacks() {
        val notification=AccessNavigation.intents(c,AccessDestination.NOTIFICATIONS).first()
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS,notification.action)
        assertEquals(c.packageName,notification.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        val usage=AccessNavigation.intents(c,AccessDestination.USAGE)
        assertEquals("package:${c.packageName}",usage[0].data.toString())
        assertEquals(Settings.ACTION_USAGE_ACCESS_SETTINGS,usage[1].action)
        assertNull(usage[1].data)
    }
    @Test fun unsupportedPerAppUsageRouteFallsBackToAccessList() {
        val seen=mutableListOf<Intent>()
        val phone=object:ContextWrapper(c) {
            override fun startActivity(intent:Intent) {
                seen+=intent
                if(intent.data!=null) throw ActivityNotFoundException()
            }
        }
        assertEquals("Select StudyNest and enable Usage Access.",AccessNavigation.open(phone,AccessDestination.USAGE))
        assertEquals(2,seen.size);assertEquals(Settings.ACTION_USAGE_ACCESS_SETTINGS,seen.last().action)
    }
    @Test fun unavailableSettingsNeverCrashAndExplainManualFallback() {
        val phone=object:ContextWrapper(c) {
            override fun startActivity(intent:Intent) { throw ActivityNotFoundException() }
        }
        for(destination in AccessDestination.entries) assertNotNull(AccessNavigation.open(phone,destination))
    }
    @Test fun onlyExistingAccessAndPublicSettingsActionsAreUsed() {
        assertEquals(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,AccessNavigation.intents(c,AccessDestination.SOUND).first().action)
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,AccessNavigation.intents(c,AccessDestination.BATTERY).first().action)
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS,AccessNavigation.intents(c,AccessDestination.LOCATION).first().action)
        assertEquals(Settings.ACTION_WIFI_SETTINGS,AccessNavigation.intents(c,AccessDestination.WIFI).first().action)
        for(destination in AccessDestination.entries) assertFalse(AccessNavigation.intents(c,destination).any { it.action==Settings.ACTION_SETTINGS || it.component!=null })
    }
}
