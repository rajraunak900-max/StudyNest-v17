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
@org.robolectric.annotation.Config(sdk=[33],application=Application::class)
class Post168SessionTest {
    private lateinit var db:StudyDatabase
    private lateinit var engine:SessionEngine
    private var config=Config(wifi=true,tracking=true,vibrate=false)
    private var now=LocalDate.of(2026,10,5).atTime(10,0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private var reminders=0
    private var scheduled:ManualBreak?=null
    private val clock=object:StudyClock {
        override fun now()=now;override fun elapsed()=now;override fun boot()=1
        override fun zone():ZoneId=ZoneOffset.UTC
    }
    @Before fun setup(){
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),StudyDatabase::class.java).allowMainThreadQueries().build()
        engine=SessionEngine(db,clock,{config},object:SoundControl {
            override fun current()=2;override suspend fun set(mode:Int)=true
        },{},breakDeadline={scheduled=it},vibrateBreak={reminders++})
    }
    @After fun close()=db.close()
    private fun advance(minutes:Long){now+=minutes*60000}
    private suspend fun summary(from:Long=0)=FocusMath.summary(db.focusDao().visits().first(),db.focusDao().windows().first(),db.focusDao().distractions().first(),now,TimeRange(from,now),breaks=db.breakDao().records().first())
    private suspend fun pauseAndLeave():Triple<Long,Long,Long> {
        engine.signal("Wi-Fi",true);val timer=db.dao().active()!!.id
        advance(60);engine.pauseResume();advance(20);val exit=now
        val breakId=db.breakDao().active()!!.id
        engine.signal("Wi-Fi",false)
        assertNull(db.breakDao().active());assertNull(scheduled)
        now+=90000;engine.tick()
        return Triple(timer,breakId,exit)
    }
    @Test fun fullManualPauseDepartureExpiryArrivalAndFreshBreakSequence()=runBlocking {
        val (timer,oldBreak,exit)=pauseAndLeave()
        val before=summary();assertEquals(80*60000L,before.libraryMs);assertEquals(60*60000L,before.libraryStudyMs)
        assertEquals(20*60000L,before.breakMs);assertEquals(75.0,before.efficiency(before.libraryMs)!!,0.0001)
        advance(30);engine.tick();assertNull(db.dao().active());assertNull(db.focusDao().openVisit())
        val old=db.focusDao().visits().first().single();assertEquals(exit,old.end);assertEquals(true,old.qualified)
        val saved=db.dao().sessions().first().single();assertEquals(timer,saved.id);assertEquals(exit,saved.endedAt)
        assertFalse(saved.paused);assertFalse(saved.pausedByDeparture)
        assertEquals(exit,db.breakDao().records().first().single().end)
        assertEquals(before,summary());engine.checkBreak(oldBreak);assertEquals(0,reminders)
        val arrival=now;engine.signal("Wi-Fi",true)
        val newTimer=db.dao().active()!!;val visit=db.focusDao().openVisit()!!
        assertNotEquals(timer,newTimer.id);assertNotEquals(old.id,visit.id)
        assertEquals(arrival,newTimer.startedAt);assertEquals(arrival,visit.start);assertFalse(newTimer.paused)
        assertNull(db.breakDao().active());advance(15);engine.tick()
        val fresh=summary(arrival);assertEquals(15*60000L,fresh.libraryMs);assertEquals(fresh.libraryMs,fresh.libraryStudyMs)
        assertEquals(0L,fresh.breakMs);assertEquals(0L,fresh.distractionMs);assertEquals(100.0,fresh.efficiency(fresh.libraryMs)!!,0.0001)
        assertEquals(95*60000L,summary().libraryMs);assertEquals(75*60000L,summary().libraryStudyMs)
        assertEquals(20*60000L,summary().breakMs)
        val index=HistoryIndex(db.focusDao().visits().first(),db.focusDao().windows().first(),emptyList(),db.breakDao().records().first(),ZoneOffset.UTC,now)
        assertEquals(summary().libraryMs,index.summary(LocalDate.of(2026,10,5),now).libraryMs)
        assertEquals(1,VisitQualification.forDay(listOf(old),LocalDate.of(2026,10,5),ZoneOffset.UTC).size)
        engine.pauseResume();val next=db.breakDao().active()!!
        assertNotEquals(oldBreak,next.id);assertEquals(visit.id,next.visitId);assertEquals(newTimer.id,next.sessionId)
        now+=BreakRecorder.LIMIT_MS-1;engine.checkBreak(oldBreak);engine.checkBreak(next.id);assertEquals(0,reminders)
        now++;engine.checkBreak(next.id);assertEquals(1,reminders)
        advance(45);engine.tick();engine.checkBreak(next.id);assertEquals(1,reminders)
        engine.pauseResume();engine.pauseResume();advance(45);engine.tick();assertEquals(2,reminders)
    }
    @Test fun withinGraceKeepsManualPauseWithoutRevivingOldBreak()=runBlocking {
        val (timer,oldBreak,_)=pauseAndLeave();advance(8);engine.signal("Wi-Fi",true)
        assertEquals(timer,db.dao().active()!!.id);assertTrue(db.dao().active()!!.paused)
        assertFalse(db.dao().active()!!.pausedByDeparture);assertEquals(1,db.focusDao().visits().first().size)
        advance(60);engine.tick();engine.checkBreak(oldBreak)
        assertNull(db.breakDao().active());assertEquals(0,reminders);assertEquals(20*60000L,summary().breakMs)
    }
    @Test fun expiredArrivalWithoutDeadlineTickStartsFreshTimer()=runBlocking {
        val(timer,_,_)=pauseAndLeave();advance(31);engine.signal("Wi-Fi",true)
        assertNotEquals(timer,db.dao().active()!!.id);assertFalse(db.dao().active()!!.paused)
        assertEquals(2,db.focusDao().visits().first().size);assertEquals(2,db.dao().sessions().first().size)
    }
    @Test fun exactThirtyMinuteBoundaryExpiresOnlyOnce()=runBlocking {
        val(timer,_,exit)=pauseAndLeave();now=exit+1800000-1;engine.tick();assertEquals(timer,db.dao().active()!!.id)
        now++;engine.tick();engine.tick();assertNull(db.dao().active())
        engine.signal("Wi-Fi",true);engine.signal("Wi-Fi",true)
        assertEquals(2,db.dao().sessions().first().size);assertEquals(2,db.focusDao().visits().first().size)
    }
    @Test fun explicitManualTimerPauseIsNotResetByLibraryGrace()=runBlocking {
        config=config.copy(tracking=false);engine.signal("Wi-Fi",true);engine.manualStart()
        val timer=db.dao().active()!!.id;engine.pauseResume();engine.signal("Wi-Fi",false)
        advance(60);engine.tick();engine.signal("Wi-Fi",true)
        assertEquals(timer,db.dao().active()!!.id);assertTrue(db.dao().active()!!.paused)
    }
    @Test fun runningAutomaticDepartureKeepsExistingTimerSemantics()=runBlocking {
        engine.signal("Wi-Fi",true);val timer=db.dao().active()!!.id;advance(60);engine.signal("Wi-Fi",false)
        advance(31);engine.tick();engine.signal("Wi-Fi",true)
        assertEquals(timer,db.dao().active()!!.id);assertFalse(db.dao().active()!!.paused)
    }
    @Test fun manualPauseExpiryAcrossMidnightStartsNewDayNormally()=runBlocking {
        now=LocalDate.of(2026,10,5).atTime(22,30).toInstant(ZoneOffset.UTC).toEpochMilli()
        val(timer,_,_)=pauseAndLeave();advance(40);engine.tick();engine.signal("Wi-Fi",true);advance(10);engine.tick()
        assertNotEquals(timer,db.dao().active()!!.id);assertFalse(db.dao().active()!!.paused)
        val midnight=LocalDate.of(2026,10,6).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(10*60000L,summary(midnight).libraryStudyMs);assertEquals(0L,summary(midnight).breakMs)
    }
}
