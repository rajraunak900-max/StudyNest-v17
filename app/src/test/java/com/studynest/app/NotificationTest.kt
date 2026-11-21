package com.studynest.app

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.Notifications
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33], application = android.app.Application::class)
class NotificationTest {
    @Test fun migratesOldChannelToBadgeFreeChannelsAndNewIcon() {
        val c = ApplicationProvider.getApplicationContext<Context>()
        val manager = c.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(android.app.NotificationChannel("study", "Old", NotificationManager.IMPORTANCE_LOW))
        Notifications.create(c)
        assertNull(manager.getNotificationChannel("study"))
        assertFalse(manager.getNotificationChannel(Notifications.SERVICE_CHANNEL).canShowBadge())
        assertFalse(manager.getNotificationChannel(Notifications.SOUND_CHANNEL).canShowBadge())
        val notification = Notifications.notification(c, "Studying", true, false)
        assertEquals(R.drawable.ic_study_notification, notification.smallIcon.resId)
        assertEquals(0, notification.number)
        assertEquals(android.app.Notification.BADGE_ICON_NONE, notification.badgeIconType)
        assertEquals(Notifications.SERVICE_CHANNEL, notification.channelId)
    }

    @Test fun runningChronometerUsesElapsedStudyTimeAndPauseFreezesIt() {
        val c = ApplicationProvider.getApplicationContext<Context>()
        val before = System.currentTimeMillis()
        val active = Notifications.notification(c, "Current session • Manual", true, false, 3723000)
        assertTrue(active.extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(kotlin.math.abs(active.`when` - (before - 3723000)) < 1000)
        assertEquals(2, active.actions.size)
        assertEquals("Pause", active.actions[0].title.toString())
        val paused = Notifications.notification(c, "Session paused • 01:02:03", true, true, 3723000)
        assertFalse(paused.extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals("Resume", paused.actions[0].title.toString())
        val finished = Notifications.notification(c, "Watching for library Wi-Fi", false, false)
        assertFalse(finished.extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(finished.actions.isNullOrEmpty())
    }
}
