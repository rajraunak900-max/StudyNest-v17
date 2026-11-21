package com.studynest.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.automation.*
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RoboConfig

@RunWith(RobolectricTestRunner::class)
@RoboConfig(sdk=[33], application=Application::class)
class FocusIntegrationTest {
    private lateinit var db: StudyDatabase
    private lateinit var engine: SessionEngine
    private lateinit var monitor: UsageMonitor
    private var config = Config(setup=true, wifi=true, vibrate=false,
        appMonitoring=true, monitoredApps=setOf("instagram","whatsapp"))
    private var time = 1790000000000L
    private var elapsed = 0L
    private var boot = 1
    private var permission = true
    private var interactive = true
    private var call = false
    private var sourceAvailable = true
    private val events = mutableListOf<AppEvent>()
    private val clock = object : StudyClock {
        override fun now()=time; override fun elapsed()=elapsed; override fun boot()=boot
    }
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), StudyDatabase::class.java)
            .allowMainThreadQueries().build()
        events += AppEvent(time-1000, UsageTimeline.RESUME, "allowed")
        monitor=UsageMonitor(db, object : UsageSource {
            override fun events(from: Long, until: Long) = if (sourceAvailable)
                events.filter { it.at in from..until } else null
            override fun interactive()=interactive
            override fun callAudio()=call
        }, { time })
        engine=SessionEngine(db, clock, {config}, object : SoundControl {
            override fun current()=2; override suspend fun set(mode:Int)=true
        }, { fail(it) }, usageAllowed={permission}, beforeChange={monitor.poll()})
    }
    @After fun close() { db.close() }
    private fun advance(ms:Long) { time+=ms;elapsed+=ms }
    private fun event(pkg:String, type:Int=UsageTimeline.RESUME) { events+=AppEvent(time,type,pkg) }
    private suspend fun summary() = FocusMath.summary(db.focusDao().visits().first(),
        db.focusDao().windows().first(),db.focusDao().distractions().first(),time)
    private suspend fun depart() { engine.signal("Wi-Fi",false);advance(SessionEngine.departureGraceMs(config,db.dao().active()?.trigger));engine.tick();monitor.poll() }

    @Test fun fourHourLibraryExampleStoresExactValues() = runBlocking {
        engine.signal("Wi-Fi",true)
        advance(30*60000);event("instagram")
        advance(15*60000);event("allowed")
        advance(75*60000);event("whatsapp")
        advance(20*60000);event("allowed")
        advance(100*60000);depart()
        val s=summary()
        assertEquals(4*3600000L,s.libraryMs)
        assertEquals(35*60000L,s.distractionMs)
        assertEquals(205*60000L,s.studyMs)
        assertEquals(85.416666,s.efficiency(s.libraryMs)!!,0.0001)
        assertEquals(15*60000L,s.breakdown["instagram"]!!)
        assertEquals(20*60000L,s.breakdown["whatsapp"]!!)
        // The longer grace can cross midnight and retire the old automatic timer.
        val session=db.dao().sessions().first().single()
        val currentDay=java.time.Instant.ofEpochMilli(time).atZone(clock.zone()).toLocalDate()
        val startedDay=java.time.Instant.ofEpochMilli(session.startedAt).atZone(clock.zone()).toLocalDate()
        if(currentDay==startedDay) assertTrue(db.dao().active()!!.pausedByDeparture)
        else { assertNull(db.dao().active());assertNotNull(session.endedAt) }
    }
    @Test fun selectedAppsDoNotPauseOrDeleteOriginalSession() = runBlocking {
        engine.signal("Wi-Fi",true);val id=db.dao().active()!!.id
        advance(60000);event("instagram");advance(60000);monitor.poll()
        assertEquals(id,db.dao().active()!!.id);assertFalse(db.dao().active()!!.paused)
        assertEquals(120000L,summary().libraryMs);assertEquals(60000L,summary().studyMs)
        event("allowed");advance(60000);monitor.poll()
        assertEquals(120000L,summary().studyMs)
    }
    @Test fun manualBreakDoesNotPauseLibraryPresence() = runBlocking {
        engine.signal("Wi-Fi",true);advance(30*60000);engine.pauseResume()
        advance(15*60000);assertEquals(45*60000L,summary().libraryMs)
        engine.pauseResume();advance(15*60000);depart()
        assertEquals(60*60000L,summary().libraryMs)
        assertEquals(45*60000L,summary().studyMs)
    }
    @Test fun presenceWorksWithAutomaticTrackingDisabled() = runBlocking {
        config=config.copy(tracking=false)
        engine.signal("Wi-Fi",true);advance(60000);engine.tick()
        assertNull(db.dao().active());assertEquals(60000L,summary().libraryMs)
        assertEquals(0L,summary().studyMs)
    }
    @Test fun manualSessionOutsideLibrarySubtractsApps() = runBlocking {
        config=config.copy(wifi=false)
        engine.manualStart();advance(60000);event("instagram");advance(60000);engine.finish()
        val s=summary();assertEquals(0L,s.libraryMs);assertEquals(60000L,s.studyMs)
        assertEquals(60000L,s.manualOutsideMs);assertEquals(100.0,s.efficiency(60000L)!!,0.0001)
    }
    @Test fun activeManualOutsideLibraryFeedsHomeAndNotificationActual() = runBlocking {
        config=config.copy(wifi=false,appMonitoring=false)
        engine.manualStart();advance(60*60000L)
        val windows=db.focusDao().windows().first()
        val visits=db.focusDao().visits().first()
        val periods=db.focusDao().distractions().first()
        val daily=HistoryIndex(emptyList(),windows,periods,emptyList(),clock.zone(),time).live(time)
        val today=java.time.Instant.ofEpochMilli(time).atZone(clock.zone()).toLocalDate()
        assertEquals(60*60000L,daily[today]!!.studyMs)
        assertEquals(0L,daily[today]!!.libraryMs)
        val timing=NotificationTiming.snapshot(db.dao().active(),db.focusDao().openVisit(),visits,
            windows,periods,time,elapsed)
        assertEquals(60*60000L,timing.studyMs)
        assertEquals(0L,timing.libraryMs)
        assertTrue(timing.studyRunning)
        assertFalse(timing.libraryRunning)
    }
    @Test fun masterOffBehavesLikeExistingTimer() = runBlocking {
        config=config.copy(appMonitoring=false)
        engine.signal("Wi-Fi",true);event("instagram");advance(60000);depart()
        assertEquals(60000L,summary().studyMs);assertEquals(0L,summary().distractionMs)
    }
    @Test fun removingAndAddingAppAffectsOnlyFutureWindows() = runBlocking {
        engine.signal("Wi-Fi",true);event("instagram");advance(60000)
        config=config.copy(monitoredApps=setOf("whatsapp"));engine.syncFocus()
        advance(60000)
        config=config.copy(monitoredApps=setOf("instagram","whatsapp"));engine.syncFocus()
        advance(60000);depart()
        assertEquals(120000L,summary().distractionMs)
        assertEquals(60000L,summary().studyMs)
    }
    @Test fun missingUsagePermissionIsUnavailableNotProductive() = runBlocking {
        permission=false;engine.signal("Wi-Fi",true);advance(60000);depart()
        assertEquals(60000L,summary().unknownMs);assertEquals(0L,summary().studyMs)
        assertNull(summary().efficiency(summary().libraryMs))
    }
    @Test fun grantingUsageAccessCreatesKnownFutureWindow() = runBlocking {
        permission=false;engine.signal("Wi-Fi",true);advance(60000)
        permission=true;engine.syncFocus();advance(60000);depart()
        assertEquals(60000L,summary().unknownMs);assertEquals(60000L,summary().studyMs)
    }
    @Test fun lockedScreenDoesNotContinueSelectedApp() = runBlocking {
        engine.signal("Wi-Fi",true);event("instagram");advance(60000)
        interactive=false;monitor.poll(UsageTimeline.LOCK)
        advance(60000);monitor.poll();depart()
        assertEquals(60000L,summary().distractionMs);assertEquals(60000L,summary().studyMs)
    }
    @Test fun callAudioOverlapsSelectedAppOnlyOnce() = runBlocking {
        config=config.copy(monitorCallAudio=true)
        engine.signal("Wi-Fi",true);event("instagram");advance(60000)
        call=true;monitor.poll();advance(60000);call=false;monitor.poll()
        advance(60000);event("allowed");advance(60000);depart()
        assertEquals(180000L,summary().distractionMs)
        assertEquals(60000L,summary().breakdown["Call audio"]!!)
    }
    @Test fun briefWifiLossWithinGraceDoesNotSplitVisit() = runBlocking {
        engine.signal("Wi-Fi",true);advance(60000);engine.signal("Wi-Fi",false)
        advance(15000);engine.signal("Wi-Fi",true);advance(45000);depart()
        assertEquals(1,db.focusDao().visits().first().size)
        assertEquals(120000L,summary().libraryMs)
    }
    @Test fun bothPresenceSourcesMustBeOutside() = runBlocking {
        config=config.copy(geo=true)
        engine.signal("Wi-Fi",true);engine.signal("Location",true)
        advance(60000);engine.signal("Wi-Fi",false);advance(120000);engine.tick()
        assertNull(db.focusDao().openVisit()!!.end)
        engine.signal("Location",false);advance(SessionEngine.WIFI_EXIT_GRACE_MS);engine.tick()
        assertEquals(180000L,summary().libraryMs)
    }
    @Test fun restartClosesUnobservedPresenceAtCheckpoint() = runBlocking {
        engine.signal("Wi-Fi",true);advance(60000);engine.tick();advance(600000)
        engine.recover(true)
        assertEquals(60000L,db.focusDao().visits().first().first().end!!-1790000000000L)
        assertEquals(60000L,summary().studyMs)
        assertEquals(0L,summary().manualOutsideMs)
    }
    @Test fun zeroDurationHasNoEfficiencyDivision() = runBlocking {
        engine.signal("Wi-Fi",true)
        assertNull(summary().efficiency(summary().libraryMs))
        assertEquals(0L,summary().distractionMs)
    }
    @Test fun previousCompletedSessionDoesNotInventNewMetrics() = runBlocking {
        db.dao().insertSession(StudySession(startedAt=time-60000,endedAt=time,trigger="Manual"))
        engine.recover()
        assertFalse(summary().recorded)
        assertNull(db.dao().sessions().first().single().measurementStart)
    }
    @Test fun selectionsAndExistingSettingsSurviveReopen() {
        val c=ApplicationProvider.getApplicationContext<Context>()
        val prefs=Preferences(c)
        prefs.save(config.copy(ssid="Library",theme="Dark",monitorCallAudio=true))
        val next=Preferences(c).value
        assertEquals(setOf("instagram","whatsapp"),next.monitoredApps)
        assertTrue(next.appMonitoring);assertTrue(next.monitorCallAudio)
        assertEquals("Library",next.ssid);assertEquals("Dark",next.theme)
    }
    @Test fun longUnobservedCallGapIsUnavailableNotInventedAsCallTime() = runBlocking {
        config=config.copy(monitorCallAudio=true)
        engine.signal("Wi-Fi",true);advance(1000);call=true;monitor.poll()
        advance(300000);call=false;monitor.poll()
        assertEquals(300000L,summary().unknownMs)
        assertEquals(0L,summary().distractionMs)
    }
    @Test fun dailyMeasurementsSplitAcrossMidnight() = runBlocking {
        time=java.time.LocalDateTime.of(2026,10,1,23,30).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        events.clear();events+=AppEvent(time-1000,UsageTimeline.RESUME,"allowed")
        val midnight=time+1800000
        engine.signal("Wi-Fi",true);advance(3600000);depart()
        val visits=db.focusDao().visits().first();val windows=db.focusDao().windows().first()
        val periods=db.focusDao().distractions().first()
        val middle=midnight
        val first=FocusMath.summary(visits,windows,periods,time,TimeRange(0,middle))
        val second=FocusMath.summary(visits,windows,periods,time,TimeRange(middle,Long.MAX_VALUE))
        assertEquals(1800000L,first.libraryMs);assertEquals(1800000L,second.libraryMs)
        assertEquals(3600000L,first.studyMs+second.studyMs)
    }
    @Test fun knownLockedTimeRemainsProductiveDuringLongSleep() = runBlocking {
        engine.signal("Wi-Fi",true);advance(1000)
        interactive=false;monitor.poll(UsageTimeline.LOCK)
        advance(3600000)
        assertEquals(0L,summary().unknownMs)
        assertEquals(3601000L,summary().studyMs)
        assertEquals(3601000L,summary().libraryMs)
    }
    @Test fun newRecordsDoNotInventMeasurementsForOlderHistory() = runBlocking {
        engine.signal("Wi-Fi",true);advance(60000);depart()
        val old=FocusMath.summary(db.focusDao().visits().first(),db.focusDao().windows().first(),
            db.focusDao().distractions().first(),time,TimeRange(0,1790000000000L-86400000))
        assertFalse(old.recorded)
        assertEquals(0L,old.libraryMs)
    }
    @Test fun currentInteractiveStateRecoversMissedUnlockBroadcast() = runBlocking {
        engine.signal("Wi-Fi",true);advance(1000)
        interactive=false;monitor.poll(UsageTimeline.LOCK)
        advance(60000);interactive=true;event("instagram");monitor.poll()
        advance(15000);monitor.poll()
        assertEquals(15000L,summary().distractionMs)
        assertTrue(UsageState.decode(db.focusDao().openWindow()!!.tracker).interactive)
    }
}
