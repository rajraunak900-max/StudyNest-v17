package com.studynest.app

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import com.studynest.app.automation.NotificationTiming
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.*

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=Application::class)
class DepartureAccountingTest {
    private lateinit var db:StudyDatabase
    private lateinit var engine:SessionEngine
    private var cfg=Config(setup=true,wifi=true,vibrate=true)
    private var now=LocalDate.of(2026,10,4).atTime(10,0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private var mode=2
    private val statements=java.util.concurrent.ConcurrentLinkedQueue<String>()
    private val clock=object:StudyClock {
        override fun now()=now;override fun elapsed()=now;override fun boot()=1
        override fun zone():ZoneId=ZoneOffset.UTC
    }
    @Before fun setup(){
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),StudyDatabase::class.java).allowMainThreadQueries().setQueryCallback({query,_->statements.add(query)},java.util.concurrent.Executor{it.run()}).build()
        engine=SessionEngine(db,clock,{cfg},object:SoundControl{
            override fun current()=mode
            override suspend fun set(mode:Int):Boolean{this@DepartureAccountingTest.mode=mode;return true}
        },{fail(it)})
    }
    @After fun close()=db.close()
    private fun advance(ms:Long){now+=ms}
    private suspend fun summary()=FocusMath.summary(db.focusDao().visits().first(),db.focusDao().windows().first(),db.focusDao().distractions().first(),now,breaks=db.breakDao().records().first())
    private suspend fun enter(){engine.signal("Wi-Fi",true)}
    private suspend fun leave(){engine.signal("Wi-Fi",false)}

    @Test fun confirmedDepartureFreezesAllMetricsUntilFinalization()=runBlocking{
        enter();advance(100*60000L);engine.pauseResume();advance(20*60000L);engine.pauseResume()
        val id=db.focusDao().openVisit()!!.id
        leave();advance(90000);engine.tick()
        val frozen=summary();assertEquals(120*60000L,frozen.libraryMs);assertEquals(100*60000L,frozen.libraryStudyMs)
        assertEquals(20*60000L,frozen.breakMs);assertEquals(0L,frozen.distractionMs)
        for(gap in listOf(60000L,300000L,600000L)){
            advance(gap);engine.tick();assertEquals(frozen,summary());assertEquals(id,db.focusDao().openVisit()!!.id)
        }
        advance(1800000);engine.tick();assertNull(db.focusDao().openVisit());assertEquals(frozen,summary())
    }
    @Test fun notificationClocksStayFrozenThroughoutPendingAway()=runBlocking{
        enter();advance(600000);leave();advance(90000);engine.tick()
        val t=NotificationTiming.snapshot(db.dao().active(),db.focusDao().openVisit(),db.focusDao().visits().first(),db.focusDao().windows().first(),emptyList(),now,now)
        assertFalse(t.libraryRunning);assertFalse(t.studyRunning);assertEquals(600000L,t.libraryMs)
        assertEquals(t.values(now),t.values(now+1200000))
    }
    @Test fun transientDisconnectRetainsToleranceWithoutPausing()=runBlocking{
        enter();advance(600000);leave();advance(89999);enter();advance(60000);engine.tick()
        assertEquals(749999L,summary().libraryMs);assertEquals(749999L,summary().libraryStudyMs)
        assertFalse(db.dao().active()!!.paused);assertEquals(1,mode)
    }
    @Test fun repeatedReturnsExcludeEveryConfirmedGapAndQualifyByPresence()=runBlocking{
        cfg=cfg.copy(minimumSessionMinutes=10);enter();val id=db.focusDao().openVisit()!!.id
        for(gap in listOf(300000L,600000L,1799000L)){
            advance(180000);leave();advance(gap);enter();assertEquals(id,db.focusDao().openVisit()!!.id)
        }
        leave();advance(1800000);engine.tick()
        val v=db.focusDao().visits().first().single();assertEquals(false,v.qualified)
        assertEquals(9*60000L,summary().libraryMs);assertEquals(9*60000L,summary().libraryStudyMs)
        assertEquals(100.0,summary().efficiency(summary().libraryMs)!!,0.00001);assertTrue(db.breakDao().records().first().isEmpty())
        assertEquals(1,db.dao().sessions().first().size)
    }
    @Test fun confirmedAwayAcrossMidnightContributesZeroToAwayDate()=runBlocking{
        now=LocalDate.of(2026,10,3).atTime(23,40).toInstant(ZoneOffset.UTC).toEpochMilli()
        enter();advance(15*60000);leave();advance(20*60000);engine.tick()
        val visits=db.focusDao().visits().first()
        val idx=HistoryIndex(visits,db.focusDao().windows().first(),emptyList(),emptyList(),ZoneOffset.UTC,now)
        val today=LocalDate.of(2026,10,4)
        assertEquals(0L,idx.summary(today,now).libraryMs);assertTrue(idx.visitCoverage(today,now).isEmpty())
        assertTrue(com.studynest.app.ui.activeLibrarySessions(visits,today,ZoneOffset.UTC,now).isEmpty())
        enter();advance(60000);engine.tick();assertEquals(16*60000L,summary().libraryMs)
    }
    @Test fun detectedLocationRetainsLegitimateLibraryTimeDespiteWifiLoss()=runBlocking{
        cfg=cfg.copy(geo=true);enter();engine.signal("Location",true);advance(600000);leave();advance(600000);engine.tick()
        assertEquals(1200000L,summary().libraryMs);assertEquals(1200000L,summary().libraryStudyMs)
        assertEquals(2,mode);assertFalse(db.dao().active()!!.paused)
    }
    @Test fun pendingAwayDoesNotNeedAnActiveStudyTimerToFreezeLibrary()=runBlocking{
        cfg=cfg.copy(tracking=false);enter();advance(600000);leave();advance(90000);engine.tick()
        assertNull(db.dao().active());assertNotNull(db.focusDao().openVisit());assertEquals(600000L,summary().libraryMs)
        advance(600000);engine.tick();assertEquals(600000L,summary().libraryMs)
        enter();advance(60000);engine.tick();assertEquals(660000L,summary().libraryMs)
    }

    @Test fun pendingGraceTicksDoNotWriteDatabaseOrCreateBreaks()=runBlocking{
        enter();advance(600000);leave();advance(90000);engine.tick();statements.clear()
        repeat(40){advance(30000);engine.tick()}
        assertTrue(statements.filter { it.trimStart().startsWith("UPDATE",true) || it.trimStart().startsWith("INSERT",true) || it.trimStart().startsWith("DELETE",true) }.isEmpty())
        assertEquals(600000L,summary().libraryMs);assertTrue(db.breakDao().records().first().isEmpty())
    }
    @Test fun awayDoesNotCreateDistractionAndReturnRespectsMonitoringSelections()=runBlocking{
        cfg=cfg.copy(appMonitoring=true,monitoredApps=setOf("com.example.fun"))
        enter();advance(600000);leave();advance(90000);engine.tick()
        val atExit=summary();assertEquals(600000L,atExit.libraryUnknownMs);assertNull(atExit.efficiency(atExit.libraryMs))
        advance(600000);engine.tick();assertEquals(atExit,summary())
        enter();advance(60000);engine.tick();val returned=summary()
        assertEquals(660000L,returned.libraryMs);assertEquals(660000L,returned.libraryUnknownMs)
        assertEquals(0L,returned.libraryDistractionMs);assertEquals(0L,returned.breakMs)
        assertEquals(setOf("com.example.fun"),cfg.monitoredApps)
    }
}
