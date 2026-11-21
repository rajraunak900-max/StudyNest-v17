package com.studynest.app

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.os.Process
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application=Application::class)
abstract class PlatformMonitoringCases {
    @Test fun usageAccessDenialGrantAndPublicEventParsing() {
        val c=ApplicationProvider.getApplicationContext<Context>()
        val ops=c.getSystemService(AppOpsManager::class.java)
        shadowOf(ops).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),c.packageName,AppOpsManager.MODE_IGNORED)
        assertFalse(UsageAccess.allowed(c));assertNull(AndroidUsageSource(c).events(500,3000))
        shadowOf(ops).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),c.packageName,AppOpsManager.MODE_ALLOWED)
        shadowOf(c.getSystemService(UserManager::class.java)).setUserUnlocked(true)
        assertTrue(UsageAccess.allowed(c))
        val stats=c.getSystemService(UsageStatsManager::class.java)
        shadowOf(stats).addEvent("com.example.distraction",1000,1)
        shadowOf(stats).addEvent("com.example.distraction",2000,2)
        val events=AndroidUsageSource(c).events(500,3000)!!
        assertEquals(listOf(1,2),events.map { it.type })
        assertEquals("com.example.distraction",events.first().pkg)
    }
    @Test fun launcherCatalogUsesPackagesAndPreservesDuplicateLabels() {
        val c=ApplicationProvider.getApplicationContext<Context>()
        fun info(pkg:String,system:Boolean)=ResolveInfo().apply {
            activityInfo=ActivityInfo().apply {
                name="$pkg.Main";packageName=pkg;enabled=true;exported=true
                applicationInfo=ApplicationInfo().apply {
                    packageName=pkg;nonLocalizedLabel="Focus Tools";enabled=true
                    flags=if(system)ApplicationInfo.FLAG_SYSTEM else 0
                }
            }
        }
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        shadowOf(c.packageManager).addResolveInfoForIntent(intent,info("com.example.tools",false))
        shadowOf(c.packageManager).addResolveInfoForIntent(intent,info("com.example.system",true))
        val apps=AppCatalog.installed(c).filter { it.pkg.startsWith("com.example.") }
        assertEquals(2,apps.size)
        assertEquals(2,apps.map { it.pkg }.distinct().size)
        assertTrue(apps.all { it.label=="Focus Tools" })
        assertTrue(apps.single { it.pkg=="com.example.system" }.system)
    }
}

@org.robolectric.annotation.Config(sdk=[26])
class PlatformMonitoringApi26Test : PlatformMonitoringCases()
@org.robolectric.annotation.Config(sdk=[29])
class PlatformMonitoringApi29Test : PlatformMonitoringCases()
@org.robolectric.annotation.Config(sdk=[33])
class PlatformMonitoringApi33Test : PlatformMonitoringCases()
@org.robolectric.annotation.Config(sdk=[36])
class PlatformMonitoringApi36Test : PlatformMonitoringCases()
