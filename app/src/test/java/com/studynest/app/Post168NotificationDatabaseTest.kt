package com.studynest.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.*
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=StudyNestApp::class)
class Post168NotificationDatabaseTest {
    private lateinit var app:StudyNestApp
    private val writes=AtomicInteger()
    @Before fun setup()=runBlocking {
        app=ApplicationProvider.getApplicationContext()
        app.scope.coroutineContext[Job]!!.children.toList().forEach{it.join()}
        app.db.close()
        app.db=Room.inMemoryDatabaseBuilder(app,StudyDatabase::class.java).allowMainThreadQueries()
            .setQueryCallback({sql,_ -> if(Regex("^(INSERT|UPDATE|DELETE)",RegexOption.IGNORE_CASE).containsMatchIn(sql.trim()))writes.incrementAndGet()},java.util.concurrent.Executor{it.run()}).build()
    }
    @After fun close()=runBlocking {app.breakReminder.close();app.scope.coroutineContext[Job]!!.cancelAndJoin();app.db.close()}
    @Test fun foregroundDatabaseProjectionUsesDailyHistoryTotalsWithoutWrites()=runBlocking {
        val now=System.currentTimeMillis()
        // Split elapsed local-day time so the fixture also works in early morning.
        val day=java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val oldStart=day-7200000;val oldEnd=day+(now-day)/3
        val currentStart=day+2*((now-day)/3)
        val expected=(oldEnd-day)+(now-currentStart)
        val previous=app.db.dao().insertSession(StudySession(startedAt=oldStart,endedAt=oldEnd,trigger="Wi-Fi",measurementStart=oldStart))
        val active=app.db.dao().insertSession(StudySession(startedAt=currentStart,trigger="Wi-Fi",measurementStart=currentStart))
        val visits=listOf(LibraryVisit(start=oldStart,end=oldEnd,elapsedStart=0,boot=1),LibraryVisit(start=currentStart,elapsedStart=0,boot=1))
        visits.forEach{app.db.focusDao().insertVisit(it)}
        app.db.focusDao().insertWindow(FocusWindow(sessionId=previous,start=oldStart,end=oldEnd,elapsedStart=0,boot=1,mode="OFF"))
        app.db.focusDao().insertWindow(FocusWindow(sessionId=active,start=currentStart,elapsedStart=0,boot=1,mode="OFF"))
        writes.set(0)
        repeat(20){
            val notification=Notifications.current(app)
            val library=notification.extras.getLong("studynest.sittingMs")
            val actual=notification.extras.getLong("studynest.studyMs")
            assertEquals(library,actual)
            assertTrue(kotlin.math.abs(library-expected)<10000)
            assertTrue(notification.extras.getBoolean("studynest.sittingRunning"));assertTrue(notification.extras.getBoolean("studynest.studyRunning"))
            assertEquals("Sitting: ${StudyMath.clock(library)} • Actual: ${StudyMath.clock(actual)}",notification.extras.getString(android.app.Notification.EXTRA_TEXT))
            assertNotNull(notification.contentView);assertNotNull(notification.bigContentView)
        }
        assertEquals(0,writes.get())
    }
}
