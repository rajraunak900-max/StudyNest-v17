package com.studynest.app
import android.Manifest
import android.app.*
import android.os.*
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.*
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[26,33,36],application=StudyNestApp::class)
class Post168ReminderTest {
    private lateinit var app:StudyNestApp
    private lateinit var engine:SessionEngine
    private lateinit var manager:NotificationManager
    private lateinit var vibrator:Vibrator
    private var now=System.currentTimeMillis()
    private var deliveries=0
    private var deadline:ManualBreak?=null
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext();app.scope.coroutineContext[Job]!!.children.toList().forEach{it.join()}
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        manager=app.getSystemService(NotificationManager::class.java)
        vibrator=if(Build.VERSION.SDK_INT>=31)app.getSystemService(VibratorManager::class.java).defaultVibrator else app.getSystemService(Vibrator::class.java)
        shadowOf(vibrator).setHasVibrator(true)
        engine=SessionEngine(app.db,object:StudyClock {override fun now()=now;override fun elapsed()=now;override fun boot()=1},
            {Config(wifi=true,tracking=true,vibrate=false)},object:SoundControl {override fun current()=2;override suspend fun set(mode:Int)=true},{},
            breakDeadline={deadline=it},vibrateBreak={deliveries++;BreakReminder.vibrate(app)})
        engine.signal("Wi-Fi",true)
    }
    @After fun close()=runBlocking {app.breakReminder.close();app.scope.coroutineContext[Job]!!.cancelAndJoin();app.db.close()}
    @Test fun fortyFiveMinuteEventVibratesAndPostsOnceWithoutActivityOrTrackingReplacement()=runBlocking {
        val tracking=Notifications.notification(app,"Tracking",true,true)
        manager.notify(1,tracking);engine.pauseResume();val id=app.db.breakDao().active()!!.id
        now+=BreakRecorder.LIMIT_MS-1;engine.checkBreak(id);assertEquals(0,deliveries);assertNull(shadowOf(manager).getNotification(3))
        now++;engine.checkBreak(id);assertEquals(1,deliveries)
        assertEquals(2000L,shadowOf(vibrator).milliseconds)
        val notice=shadowOf(manager).getNotification(3)!!
        assertEquals("Break Reminder",notice.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(notice.extras.getString(Notification.EXTRA_TEXT)!!.contains("45 minutes"))
        assertEquals(Notifications.SOUND_CHANNEL,notice.channelId)
        assertFalse(manager.getNotificationChannel(notice.channelId).canShowBadge())
        assertEquals(0,notice.number);assertEquals(Notification.BADGE_ICON_NONE,notice.badgeIconType)
        assertNotNull(notice.contentIntent);assertSame(tracking,shadowOf(manager).getNotification(1))
        now+=BreakRecorder.LIMIT_MS;engine.tick();engine.checkBreak(id)
        assertEquals(1,deliveries);assertSame(notice,shadowOf(manager).getNotification(3))
        engine.pauseResume();engine.pauseResume();now+=BreakRecorder.LIMIT_MS;engine.tick()
        assertEquals(2,deliveries);assertNotSame(notice,shadowOf(manager).getNotification(3));assertSame(tracking,shadowOf(manager).getNotification(1))
    }
    @Test fun departureExpiryAndStaleWorkerNeverDeliverOldReminder()=runBlocking {
        engine.pauseResume();val old=app.db.breakDao().active()!!.id;now+=20*60000L
        engine.signal("Wi-Fi",false);assertNull(deadline)
        now+=60*60000L;engine.tick();engine.checkBreak(old)
        assertEquals(0,deliveries);assertNull(shadowOf(manager).getNotification(3))
        engine.signal("Wi-Fi",true);engine.checkBreak(old);assertEquals(0,deliveries)
        engine.pauseResume();val fresh=app.db.breakDao().active()!!.id;assertNotEquals(old,fresh)
        now+=BreakRecorder.LIMIT_MS-1;engine.checkBreak(old);engine.checkBreak(fresh);assertEquals(0,deliveries)
        now++;engine.checkBreak(fresh);assertEquals(1,deliveries)
    }
    @Test fun absentVibratorDoesNotClaimSuccessfulVibrationOrShowEvent(){
        shadowOf(vibrator).setHasVibrator(false);BreakReminder.vibrate(app)
        assertNull(shadowOf(manager).getNotification(3))
    }
}
