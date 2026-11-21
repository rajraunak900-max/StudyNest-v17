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
class WifiGraceTest {
    private lateinit var db:StudyDatabase
    private lateinit var engine:SessionEngine
    private var config=Config(setup=true,wifi=true,vibrate=false)
    private var now=LocalDate.of(2026,10,3).atTime(12,0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private var elapsed=0L
    private var mode=2
    private val clock=object:StudyClock {
        override fun now()=now;override fun elapsed()=elapsed;override fun boot()=1
        override fun zone():ZoneId=ZoneOffset.UTC
    }
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),StudyDatabase::class.java).allowMainThreadQueries().build()
        engine=SessionEngine(db,clock,{config},object:SoundControl {
            override fun current()=mode;override suspend fun set(mode:Int):Boolean {this@WifiGraceTest.mode=mode;return true}
        },{fail(it)})
    }
    @After fun close()=db.close()
    private fun advance(ms:Long){now+=ms;elapsed+=ms}
    private suspend fun arrive(){engine.signal("Wi-Fi",true)}
    private suspend fun disconnect(){engine.signal("Wi-Fi",false)}
    private suspend fun reconnectAfter(gap:Long) {
        arrive();val visit=db.focusDao().openVisit()!!;val session=db.dao().active()!!
        advance(3600000);disconnect();advance(gap);engine.tick()
        assertEquals(visit.id,db.focusDao().openVisit()!!.id);assertTrue(db.dao().active()!!.pausedByDeparture)
        arrive();assertNull(db.dao().state()!!.exitAt)
        assertEquals(visit.id,db.focusDao().openVisit()!!.id);assertEquals(session.id,db.dao().active()!!.id)
        advance(600000);val exit=now;disconnect();advance(SessionEngine.WIFI_EXIT_GRACE_MS);engine.tick()
        val closed=db.focusDao().visits().first().single()
        assertEquals(visit.id,closed.id);assertEquals(visit.start,closed.start);assertEquals(exit,closed.end)
        // Visit identity/span includes the gap, accumulated presence excludes it.
        assertEquals(4200000L+gap,closed.end!!-closed.start)
        assertEquals(4200000L,LibraryPresence.duration(closed,now))
        assertEquals(true,closed.qualified);assertEquals(1,db.dao().sessions().first().size)
        assertEquals(4200000L,db.dao().segments().first().sumOf{engine.range(it).let{r->r.end-r.start}})
        assertEquals(4200000L,FocusMath.summary(listOf(closed),db.focusDao().windows().first(),emptyList(),now).libraryStudyMs)
    }
    @Test fun fiveMinuteReturnKeepsOriginalVisit()=runBlocking{reconnectAfter(5*60000)}
    @Test fun tenMinuteReturnKeepsOriginalVisit()=runBlocking{reconnectAfter(10*60000)}
    @Test fun twentyNineMinutesFiftyNineSecondsKeepsOriginalVisit()=runBlocking{reconnectAfter(1799000)}
    @Test fun exactDeadlineClosesAtActualExitNotThirtyMinutesLater()=runBlocking {
        arrive();advance(600000);val exit=now;disconnect()
        advance(1799999);engine.tick();assertNotNull(db.focusDao().openVisit())
        advance(1);engine.tick();val closed=db.focusDao().visits().first().single()
        assertEquals(exit,closed.end);assertEquals(true,closed.qualified)
        assertEquals(exit,db.dao().segments().first().single().end)
        assertEquals(600000L,FocusMath.summary(listOf(closed),db.focusDao().windows().first(),emptyList(),now).libraryMs)
        assertEquals(1,VisitQualification.forDay(listOf(closed),LocalDate.of(2026,10,3),ZoneOffset.UTC).size)
    }
    @Test fun lateArrivalWithoutDeadlineTickFinalizesOldAndCreatesNewVisit()=runBlocking {
        arrive();advance(599000);val exit=now;disconnect();advance(1800001);arrive()
        val visits=db.focusDao().visits().first();assertEquals(2,visits.size)
        assertEquals(exit,visits.first().end);assertEquals(false,visits.first().qualified)
        assertNull(visits.last().end);assertNotEquals(visits.first().id,visits.last().id)
        assertEquals(1,db.dao().sessions().first().size)
    }
    @Test fun repeatedDisconnectsAndConfiguredNetworkSwitchCreateNoDuplicates()=runBlocking {
        config=config.withNetworks(listOf("Library A","Library B"));arrive()
        val id=db.focusDao().openVisit()!!.id
        for(gap in listOf(300000L,600000L,1799000L)) {
            advance(60000);disconnect();advance(gap);engine.tick()
            engine.signal("Wi-Fi",config.matchesSsid("Library B"))
            assertEquals(id,db.focusDao().openVisit()!!.id);assertNull(db.dao().state()!!.exitAt)
        }
        assertEquals(1,db.focusDao().visits().first().size);assertEquals(1,db.dao().sessions().first().size)
    }
    @Test fun locationPresencePreventsWifiDepartureAndLocationOnlyKeepsNinetySeconds()=runBlocking {
        config=config.copy(geo=true);arrive();engine.signal("Location",true)
        advance(600000);disconnect();advance(1800001);engine.tick()
        assertNotNull(db.focusDao().openVisit());assertNull(db.dao().state()!!.exitAt)
        engine.signal("Location",false);advance(1800000);engine.tick();assertNull(db.focusDao().openVisit())
        config=config.copy(wifi=false);engine.reconfigure();engine.signal("Location",true)
        advance(600000);engine.signal("Location",false);advance(89999);engine.tick();assertNotNull(db.focusDao().openVisit())
        advance(1);engine.tick();assertNull(db.focusDao().openVisit())
    }
    @Test fun explicitManualTimerRetainsDeparturePauseRule()=runBlocking {
        config=config.copy(tracking=false);arrive();engine.manualStart();advance(600000);disconnect()
        advance(90000);engine.tick();assertTrue(db.dao().active()!!.paused)
        arrive();assertTrue(db.dao().active()!!.paused)
    }
    @Test fun oldWifiDepartureDoesNotPauseManualSessionStartedAfterConfirmation()=runBlocking {
        arrive();advance(600000);val exit=now;disconnect()
        advance(SessionEngine.WIFI_SOUND_GRACE_MS);engine.tick()
        val automatic=db.dao().active()!!
        assertTrue(automatic.pausedByDeparture);assertNotNull(db.focusDao().openVisit())
        engine.finish();engine.manualStart();val manual=db.dao().active()!!
        assertEquals("Manual",manual.trigger);assertTrue(manual.startedAt>exit)
        advance(15000);engine.tick()
        assertEquals(manual.id,db.dao().active()!!.id);assertFalse(db.dao().active()!!.paused)
        assertTrue(db.dao().openSegments().any { it.sessionId==manual.id && it.start>=manual.startedAt })
        advance(SessionEngine.WIFI_EXIT_GRACE_MS-SessionEngine.WIFI_SOUND_GRACE_MS-15000)
        engine.tick()
        assertEquals(manual.id,db.dao().active()!!.id);assertFalse(db.dao().active()!!.paused)
        assertNull(db.focusDao().openVisit())
        assertEquals(exit,db.focusDao().visits().first().single().end)
        assertEquals(1,db.dao().sessions().first().count { it.id==automatic.id && it.endedAt!=null })
    }
    @Test fun graceCrossingMidnightReconnectKeepsIdentityAndDailyAllocation()=runBlocking {
        now=LocalDate.of(2026,10,3).atTime(23,40).toInstant(ZoneOffset.UTC).toEpochMilli()
        arrive();val id=db.focusDao().openVisit()!!.id;val session=db.dao().active()!!.id
        advance(15*60000);disconnect();advance(10*60000);engine.reconcileDay();arrive()
        assertEquals(id,db.focusDao().openVisit()!!.id);assertEquals(session,db.dao().active()!!.id)
        advance(5*60000);val exit=now;disconnect();advance(1800000);engine.tick()
        val v=db.focusDao().visits().first().single();assertEquals(exit,v.end);assertEquals(true,v.qualified)
        val days=HistoryIndex(listOf(v),db.focusDao().windows().first(),emptyList(),emptyList(),ZoneOffset.UTC,now).live(now)
        assertEquals(15*60000L,days[LocalDate.of(2026,10,3)]!!.libraryMs)
        assertEquals(5*60000L,days[LocalDate.of(2026,10,4)]!!.libraryMs)
    }
    @Test fun genuineDepartureBeforeMidnightDoesNotAddNextDayGraceTime()=runBlocking {
        now=LocalDate.of(2026,10,3).atTime(23,40).toInstant(ZoneOffset.UTC).toEpochMilli()
        arrive();advance(15*60000);val exit=now;disconnect();advance(1800000);engine.tick()
        val v=db.focusDao().visits().first().single();assertEquals(exit,v.end)
        val days=HistoryIndex(listOf(v),db.focusDao().windows().first(),emptyList(),emptyList(),ZoneOffset.UTC,now).live(now)
        assertFalse(days.containsKey(LocalDate.of(2026,10,4)));assertNull(db.dao().active())
        arrive();assertEquals(2,db.dao().sessions().first().size)
    }
    @Test fun everyMinimumBoundaryExcludesFinalizationDelay()=runBlocking {
        for(minimum in listOf(5,10,15,30,45,46)) for(delta in listOf(-1000L,0L,1000L)) {
            config=config.copy(minimumSessionMinutes=minimum);arrive();advance(minimum*60000L+delta)
            val exit=now;disconnect();advance(1800000);engine.tick()
            val v=db.focusDao().visits().first().last();assertEquals(exit,v.end);assertEquals(delta>=0,v.qualified)
            assertEquals(minimum,v.minimumMinutes)
        }
    }

    @Test fun soundRestoresAtNinetySecondsAndTenMinuteReturnReactivatesVibrationOnSameVisit()=runBlocking {
        config=config.copy(vibrate=true);arrive();assertEquals(1,mode)
        val visit=db.focusDao().openVisit()!!;val session=db.dao().active()!!.id
        advance(90*60000L);disconnect();advance(89999);engine.tick();assertEquals(1,mode)
        advance(1);engine.tick();assertEquals(2,mode)
        assertNotNull(db.dao().state()!!.exitAt);assertTrue(db.dao().active()!!.pausedByDeparture)
        assertEquals(visit.id,db.focusDao().openVisit()!!.id)
        assertEquals(90*60000L,FocusMath.summary(db.focusDao().visits().first(),db.focusDao().windows().first(),emptyList(),now).libraryStudyMs)
        advance(10*60000L-90000);arrive();assertEquals(1,mode)
        assertNull(db.dao().state()!!.exitAt);assertEquals(visit.id,db.focusDao().openVisit()!!.id)
        assertEquals(visit.start,db.focusDao().openVisit()!!.start);assertEquals(session,db.dao().active()!!.id)
        assertFalse(db.dao().active()!!.paused);advance(60000);engine.tick()
        assertEquals(91*60000L,FocusMath.summary(db.focusDao().visits().first(),db.focusDao().windows().first(),emptyList(),now).libraryStudyMs)
        assertEquals(1,db.focusDao().visits().first().size)
    }
    @Test fun normalSoundDoesNotFinalizeVisitAndVisitDeadlineDoesNotDelaySound()=runBlocking {
        config=config.copy(vibrate=true);arrive();advance(600000);val exit=now;disconnect()
        advance(90000);engine.tick();assertEquals(2,mode);assertNotNull(db.focusDao().openVisit())
        advance(1800000-90000-1);engine.tick();assertEquals(2,mode);assertNotNull(db.focusDao().openVisit())
        advance(1);engine.tick();assertEquals(2,mode);assertNull(db.focusDao().openVisit())
        val visit=db.focusDao().visits().first().single();assertEquals(exit,visit.end)
        assertEquals(600000L,visit.end!!-visit.start);assertEquals(true,visit.qualified)
    }
    @Test fun delayedTimerDeadlineThenTenMinuteReturnExcludesAwayAndKeepsIdentity()=runBlocking {
        arrive();val visit=db.focusDao().openVisit()!!.id;val timer=db.dao().active()!!.id
        advance(600000);val exit=now;disconnect();advance(600000)
        // No tick/Activity ran during the absence. Arrival must first settle the old pause.
        arrive();assertEquals(visit,db.focusDao().openVisit()!!.id);assertEquals(timer,db.dao().active()!!.id)
        assertFalse(db.dao().active()!!.paused);advance(600000);engine.tick()
        assertEquals(listOf(exit),db.dao().segments().first().mapNotNull{it.end})
        assertEquals(1200000L,db.dao().segments().first().sumOf{engine.range(it).let{r->r.end-r.start}})
        val focus=FocusMath.summary(db.focusDao().visits().first(),db.focusDao().windows().first(),emptyList(),now)
        assertEquals(1200000L,focus.libraryMs);assertEquals(1200000L,focus.libraryStudyMs)
        assertEquals(0L,focus.breakMs);assertEquals(1,db.focusDao().visits().first().size)
    }
    @Test fun overnightTemporaryPauseResumesSameVisitWithExactDailyStudyAndNoBreakReminder()=runBlocking {
        now=LocalDate.of(2026,10,3).atTime(23,40).toInstant(ZoneOffset.UTC).toEpochMilli()
        arrive();val visit=db.focusDao().openVisit()!!.id;val timer=db.dao().active()!!.id
        advance(15*60000);disconnect();advance(90000);engine.tick()
        assertTrue(db.dao().active()!!.pausedByDeparture);assertNotNull(db.focusDao().openVisit())
        assertNull(db.breakDao().active())
        advance(10*60000-90000);engine.reconcileDay();arrive();advance(5*60000);val exit=now
        disconnect();advance(1800000);engine.tick()
        val closed=db.focusDao().visits().first().single();assertEquals(visit,closed.id);assertEquals(exit,closed.end)
        assertEquals(timer,db.dao().sessions().first().single().id);assertEquals(true,closed.qualified)
        val days=HistoryIndex(listOf(closed),db.focusDao().windows().first(),emptyList(),emptyList(),ZoneOffset.UTC,now).live(now)
        assertEquals(15*60000L,days[LocalDate.of(2026,10,3)]!!.libraryStudyMs)
        assertEquals(5*60000L,days[LocalDate.of(2026,10,4)]!!.libraryStudyMs)
        assertTrue(db.breakDao().records().first().isEmpty())
    }
}
