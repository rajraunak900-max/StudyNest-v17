package com.studynest.app

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.*

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33], application=Application::class)
class ManualBreakTest {
    private lateinit var db: StudyDatabase
    private lateinit var engine: SessionEngine
    private var config = Config(wifi=true,tracking=true,vibrate=false)
    private var time = 1_800_000_000_000L
    private var elapsed = 0L
    private var boot = 1
    private var vibrations = 0
    private val deadlines = mutableListOf<ManualBreak?>()
    private val clock = object : StudyClock {
        override fun now()=time; override fun elapsed()=elapsed; override fun boot()=boot
    }
    private fun make() = SessionEngine(db,clock,{config},object : SoundControl {
        override fun current()=2; override suspend fun set(mode:Int)=true
    },{},breakDeadline={deadlines+=it},vibrateBreak={vibrations++})
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),StudyDatabase::class.java)
            .allowMainThreadQueries().build()
        engine=make(); engine.signal("Wi-Fi",true)
    }
    @After fun close()=db.close()
    private fun advance(ms:Long) { time+=ms; elapsed+=ms }
    private suspend fun records()=db.breakDao().records().first()
    private suspend fun metrics(range:TimeRange=TimeRange(0,Long.MAX_VALUE))=FocusMath.summary(
        db.focusDao().visits().first(),db.focusDao().windows().first(),db.focusDao().distractions().first(),time,
        range,breaks=records())
    @Test fun resumeAtTwentyMinutesHasNoReminder()=runBlocking {
        engine.pauseResume(); advance(20*60000);engine.pauseResume();advance(60*60000);engine.tick()
        assertEquals(0,vibrations);assertEquals(20*60000L,metrics().breakMs);assertNull(db.breakDao().active())
    }
    @Test fun resumeAtFortyFourFiftyNineHasNoReminder()=runBlocking {
        engine.pauseResume();advance(BreakRecorder.LIMIT_MS-1000);engine.pauseResume()
        advance(60000);engine.tick();assertEquals(0,vibrations)
    }
    @Test fun reachesFortyFiveMinutesOnceEvenAtNinetyMinutes()=runBlocking {
        engine.pauseResume();advance(BreakRecorder.LIMIT_MS-1);engine.tick();assertEquals(0,vibrations)
        advance(1);engine.tick();assertEquals(1,vibrations);assertTrue(db.breakDao().active()!!.reminderAttempted)
        advance(BreakRecorder.LIMIT_MS);engine.tick();engine.tick();assertEquals(1,vibrations)
        assertEquals(90*60000L,metrics().breakMs);assertNull(deadlines.last())
    }
    @Test fun separateBreakResetsEligibilityAndStaleWorkerCannotClaimIt()=runBlocking {
        engine.pauseResume();val old=db.breakDao().active()!!.id
        advance(BreakRecorder.LIMIT_MS);engine.tick();engine.pauseResume();engine.pauseResume()
        val next=db.breakDao().active()!!.id;assertNotEquals(old,next)
        advance(BreakRecorder.LIMIT_MS);engine.checkBreak(old);assertEquals(1,vibrations)
        engine.checkBreak(next);assertEquals(2,vibrations)
    }
    @Test fun departureAtThirtyMinutesCancelsWithoutLateVibration()=runBlocking {
        engine.pauseResume();advance(30*60000);engine.signal("Wi-Fi",false)
        assertNull(db.breakDao().active());assertNull(deadlines.last())
        advance(60*60000);engine.tick();assertEquals(0,vibrations)
        assertEquals(30*60000L,metrics().breakMs)
    }
    @Test fun sessionEndAtThirtyMinutesCancelsWithoutLateVibration()=runBlocking {
        engine.pauseResume();advance(30*60000);engine.finish()
        advance(60*60000);engine.tick();assertEquals(0,vibrations)
        assertEquals(30*60000L,metrics().breakMs);assertNull(db.breakDao().active())
    }
    @Test fun outsidePauseAndIdleLibraryPresenceAreNotBreaks()=runBlocking {
        engine.finish();advance(60000);engine.tick();assertTrue(records().isEmpty())
        engine.signal("Wi-Fi",false);engine.manualStart();engine.pauseResume();advance(60000);engine.tick()
        assertTrue(records().isEmpty());assertEquals(0,vibrations)
    }
    @Test fun returnWhilePausedDoesNotInventANewManualPause()=runBlocking {
        engine.pauseResume();advance(60000);engine.signal("Wi-Fi",false)
        advance(100000);engine.tick();engine.signal("Wi-Fi",true);advance(BreakRecorder.LIMIT_MS);engine.tick()
        assertEquals(1,records().size);assertNull(db.breakDao().active());assertEquals(0,vibrations)
    }
    @Test fun selectedAppDistractionIsNotAManualBreakOrDoubleSubtracted()=runBlocking {
        advance(10*60000);engine.syncFocus();val w=db.focusDao().openWindow()!!
        db.focusDao().insertPeriods(listOf(DistractionPeriod(windowId=w.id,start=w.start,end=time,reason="APP",packageName="test.app")))
        engine.pauseResume();advance(5*60000);engine.pauseResume();advance(5*60000);engine.tick()
        val s=metrics();assertEquals(20*60000L,s.libraryMs);assertEquals(10*60000L,s.distractionMs)
        assertEquals(5*60000L,s.breakMs);assertEquals(5*60000L,s.libraryStudyMs)
        assertEquals(25.0,s.efficiency(s.libraryMs)!!,0.001)
    }
    @Test fun clocksChangingAndRebootDoNotFireEarlyOrInventTime()=runBlocking {
        engine.pauseResume();time+=90*60000;elapsed+=10*60000;engine.tick();assertEquals(0,vibrations)
        assertEquals(10*60000L,db.breakDao().active()!!.checkpoint-db.breakDao().active()!!.start)
        boot++;engine.recover(true);assertNull(db.breakDao().active());assertEquals(0,vibrations)
        assertEquals(10*60000L,records().single().end!!-records().single().start)
    }
    @Test fun engineRecreationDoesNotRepeatAnAttemptedReminder()=runBlocking {
        engine.pauseResume();advance(BreakRecorder.LIMIT_MS);engine.tick();assertEquals(1,vibrations)
        engine=make();advance(BreakRecorder.LIMIT_MS);engine.tick();assertEquals(1,vibrations)
    }
    @Test fun breakAndLibrarySplitAtLocalMidnightWithoutActivity()=runBlocking {
        engine.finish();engine.signal("Wi-Fi",false);advance(SessionEngine.WIFI_EXIT_GRACE_MS);engine.tick()
        val zone=ZoneId.of("Asia/Kolkata");val day=LocalDate.of(2026,10,1)
        time=day.atTime(23,30).atZone(zone).toInstant().toEpochMilli();elapsed=0
        engine.signal("Wi-Fi",true);advance(15*60000);engine.pauseResume();advance(30*60000);engine.pauseResume()
        advance(15*60000);engine.signal("Wi-Fi",false);advance(SessionEngine.WIFI_EXIT_GRACE_MS);engine.tick()
        val midnight=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val a=metrics(TimeRange(day.atStartOfDay(zone).toInstant().toEpochMilli(),midnight))
        val b=metrics(TimeRange(midnight,day.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()))
        assertEquals(30*60000L,a.libraryMs);assertEquals(30*60000L,b.libraryMs)
        assertEquals(15*60000L,a.breakMs);assertEquals(15*60000L,b.breakMs)
        assertEquals(15*60000L,a.libraryStudyMs);assertEquals(15*60000L,b.libraryStudyMs)
        assertEquals(50.0,a.efficiency(a.libraryMs)!!,0.001);assertEquals(50.0,b.efficiency(b.libraryMs)!!,0.001)
    }
}
