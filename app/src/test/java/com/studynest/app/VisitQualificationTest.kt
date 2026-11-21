package com.studynest.app

import android.app.Application
import android.content.Context
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
class VisitQualificationTest {
    private lateinit var db: StudyDatabase
    private lateinit var engine: SessionEngine
    private var c=Config(setup=true,wifi=true,geo=true,vibrate=false)
    private var now=LocalDate.of(2026,10,2).atTime(12,0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private var elapsed=0L
    private val clock=object: StudyClock {
        override fun now()=now; override fun elapsed()=elapsed; override fun boot()=1
        override fun zone(): ZoneId=ZoneOffset.UTC
    }
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),StudyDatabase::class.java).allowMainThreadQueries().build()
        engine=SessionEngine(db,clock,{c},object:SoundControl {
            override fun current()=2;override suspend fun set(mode:Int)=true
        },{fail(it)})
    }
    @After fun close()=db.close()
    private fun advance(ms:Long) {now+=ms;elapsed+=ms}
    private suspend fun depart(source:String="Wi-Fi") {engine.signal(source,false);advance(SessionEngine.departureGraceMs(c,db.dao().active()?.trigger));engine.tick()}
    private suspend fun visit(ms:Long,source:String="Wi-Fi"):LibraryVisit {
        engine.signal(source,true);advance(ms);depart(source);return db.focusDao().visits().first().last()
    }
    private suspend fun total()=FocusMath.summary(db.focusDao().visits().first(),db.focusDao().windows().first(),emptyList(),now).libraryMs
    @Test fun oneMinuteRetainedWithoutSession()=runBlocking {assertEquals(false,visit(60000).qualified);assertEquals(60000,total())}
    @Test fun nineMinutesFiftyNineSecondsDoesNotQualify()=runBlocking {assertEquals(false,visit(599000).qualified);assertEquals(599000,total())}
    @Test fun exactTenMinutesQualifies()=runBlocking {assertEquals(true,visit(600000).qualified);assertEquals(600000,total())}
    @Test fun thirtyMinutesQualifies()=runBlocking {assertEquals(true,visit(1800000).qualified)}
    @Test fun completedHourPersistsSameVisitAndIsIncludedInTodaysQueryWithoutUi()=runBlocking {
        engine.signal("Wi-Fi",true)
        val arrival=db.focusDao().openVisit()!!
        assertEquals(10,arrival.minimumMinutes);assertNull(arrival.qualified)
        advance(3600000)
        assertTrue(VisitQualification.forDay(db.focusDao().visits().first(),LocalDate.of(2026,10,2),ZoneOffset.UTC).isEmpty())
        depart()
        val closed=db.focusDao().visits().first().single()
        assertEquals(arrival.id,closed.id);assertEquals(arrival.start,closed.start)
        assertEquals(3600000L,closed.end!!-closed.start);assertEquals(true,closed.qualified)
        assertNull(db.focusDao().openVisit())
        assertEquals(listOf(closed),VisitQualification.forDay(db.focusDao().visits().first(),LocalDate.of(2026,10,2),ZoneOffset.UTC))
        assertEquals(3600000L,total())
    }
    @Test fun tenMinutesOneSecondAndAllCustomBoundariesUseContinuousVisitDuration()=runBlocking {
        assertEquals(true,visit(601000).qualified)
        for(minimum in listOf(5,15,30,45,46)) {
            c=c.copy(minimumSessionMinutes=minimum)
            assertEquals(false,visit(minimum*60000L-1000).qualified)
            assertEquals(true,visit(minimum*60000L).qualified)
            assertEquals(true,visit(minimum*60000L+1000).qualified)
        }
    }
    @Test fun twoRealVisitsKeepDistinctVisitIdsEvenWhenAutomaticTimerResumes()=runBlocking {
        val first=visit(3600000);advance(120000);val second=visit(2700000)
        assertNotEquals(first.id,second.id)
        assertEquals(2,VisitQualification.forDay(db.focusDao().visits().first(),LocalDate.of(2026,10,2),ZoneOffset.UTC).size)
        assertEquals(6300000,total())
    }
    @Test fun fiveAndSixMinuteVisitsDoNotBecomeOneElevenMinuteSession()=runBlocking {
        visit(300000);advance(120000);visit(360000)
        assertEquals(660000,total());assertEquals(0,db.focusDao().visits().first().count{it.qualified==true})
    }
    @Test fun briefWifiInterruptionKeepsOneContinuousVisit()=runBlocking {
        engine.signal("Wi-Fi",true);advance(1800000);engine.signal("Wi-Fi",false)
        advance(30000);engine.signal("Wi-Fi",true);advance(1770000);depart()
        val closed=db.focusDao().visits().first().single()
        assertEquals(3600000L,closed.end!!-closed.start);assertEquals(true,closed.qualified)
    }
    @Test fun expiredDepartureMustFinalizeBeforeDelayedSameDayReentry()=runBlocking {
        engine.signal("Wi-Fi",true);advance(3600000);engine.signal("Wi-Fi",false)
        // Deadline delivery can be delayed. Arrival must not erase expired departure.
        advance(SessionEngine.WIFI_EXIT_GRACE_MS+1000);engine.signal("Wi-Fi",true);advance(2700000);depart()
        val visits=db.focusDao().visits().first()
        assertEquals("Two real visits cannot merge when the deadline callback is late",2,visits.size)
        assertEquals(listOf(3600000L,2700000L),visits.map{it.end!!-it.start})
    }
    @Test fun pauseAndDistractionDoNotReduceOneHourQualification()=runBlocking {
        engine.signal("Wi-Fi",true);advance(1800000);engine.pauseResume()
        advance(900000);engine.pauseResume();advance(900000);depart()
        val closed=db.focusDao().visits().first().single()
        assertEquals(3600000L,closed.end!!-closed.start);assertEquals(true,closed.qualified)
        assertEquals(3600000,total())
    }
    @Test fun completedMidnightHourIsOneVisitWithTwoExactDailyAllocations()=runBlocking {
        val day=LocalDate.of(2026,10,2);now=day.atTime(23,30).toInstant(ZoneOffset.UTC).toEpochMilli()
        engine.signal("Wi-Fi",true);advance(1800000);engine.reconcileDay();advance(1800000);depart()
        val v=db.focusDao().visits().first().single()
        assertEquals(true,v.qualified);assertEquals(3600000L,v.end!!-v.start)
        val totals=StudyMath.dailyTotals(listOf(TimeRange(v.start,v.end!!)),ZoneOffset.UTC)
        assertEquals(1800000L,totals[day]);assertEquals(1800000L,totals[day.plusDays(1)])
    }
    @Test fun manualHourStopKeepsExactlyOneOriginalManualPeriod()=runBlocking {
        c=c.copy(wifi=false,geo=false);engine.manualStart();advance(3600000);engine.finish()
        val sessions=db.dao().sessions().first();val segments=db.dao().segments().first()
        assertEquals(1,sessions.size);assertEquals("Manual",sessions.single().trigger)
        val periods=StudyPeriods.forDay(sessions,segments,LocalDate.of(2026,10,2),ZoneOffset.UTC,engine::range)
        assertEquals(1,periods.size);assertEquals(3600000L,periods.single().studyMs)
    }
    @Test fun threeShortVisitsRemainSeparateAndUncounted()=runBlocking {
        repeat(3){visit(300000);advance(120000)}
        assertEquals(900000,total());assertEquals(3,db.focusDao().visits().first().size)
        assertEquals(0,db.focusDao().visits().first().count {it.qualified==true})
        val sessions=db.dao().sessions().first();val segments=db.dao().segments().first()
        assertTrue(StudyPeriods.forDay(sessions,segments,LocalDate.of(2026,10,2),ZoneOffset.UTC,engine::range,
            db.focusDao().visits().first().map {TimeRange(it.start,it.end!!)}).isEmpty())
    }
    @Test fun sixPlusThirtyKeepsThirtySixMinutesAndOneSession()=runBlocking {visit(360000);advance(120000);visit(1800000);assertEquals(2160000,total());assertEquals(1,db.focusDao().visits().first().count {it.qualified==true})}
    @Test fun settingChangesOnlyFutureVisits()=runBlocking {
        val old=visit(900000);c=c.copy(minimumSessionMinutes=20);val future=visit(900000)
        assertEquals(true,old.qualified);assertEquals(false,future.qualified)
        assertEquals(old,db.focusDao().visits().first().first())
    }
    @Test fun activeVisitKeepsArrivalThreshold()=runBlocking {
        engine.signal("Wi-Fi",true);advance(300000);c=c.copy(minimumSessionMinutes=30);engine.syncFocus();advance(300000);depart()
        val closed=db.focusDao().visits().first().single();assertEquals(10,closed.minimumMinutes);assertEquals(true,closed.qualified)
        assertEquals(30,visit(600000).minimumMinutes)
    }
    @Test fun fortyFiveMinuteBoundary()=runBlocking {c=c.copy(minimumSessionMinutes=45);assertEquals(false,visit(2699000).qualified);assertEquals(true,visit(2700000).qualified)}
    @Test fun fortySixMinuteBoundary()=runBlocking {c=c.copy(minimumSessionMinutes=46);assertEquals(false,visit(2759000).qualified);assertEquals(true,visit(2760000).qualified)}
    @Test fun midnightDoesNotSplitQualificationAndDailyTimesAreFourAndEight()=runBlocking {
        val day=LocalDate.of(2026,10,2);now=day.atTime(23,56).toInstant(ZoneOffset.UTC).toEpochMilli()
        engine.signal("Wi-Fi",true);advance(4*60000);engine.reconcileDay();advance(8*60000);depart()
        val v=db.focusDao().visits().first().single();assertEquals(true,v.qualified)
        val totals=StudyMath.dailyTotals(listOf(TimeRange(v.start,v.end!!)),ZoneOffset.UTC)
        assertEquals(240000L,totals[day]);assertEquals(480000L,totals[day.plusDays(1)])
        assertEquals(1,VisitQualification.forDay(listOf(v),day,ZoneOffset.UTC).size)
        assertEquals(1,VisitQualification.forDay(listOf(v),day.plusDays(1),ZoneOffset.UTC).size)
        assertEquals(1,listOf(v).count {it.qualified==true})
    }
    @Test fun backgroundReconciliationUsesTimestampNotUiTicks()=runBlocking {engine.signal("Wi-Fi",true);advance(600000);depart();assertEquals(true,db.focusDao().visits().first().single().qualified)}
    @Test fun geofenceUsesTheSameThreshold()=runBlocking {assertEquals(false,visit(599000,"Location").qualified);assertEquals(true,visit(600000,"Location").qualified)}
    @Test fun combinedPresenceClosesOnlyOnLastSourceDeparture()=runBlocking {
        engine.signal("Wi-Fi",true);engine.signal("Location",true);advance(300000);engine.signal("Wi-Fi",false)
        advance(300000);depart("Location");val visits=db.focusDao().visits().first()
        assertEquals(1,visits.size);assertEquals(true,visits.single().qualified);assertEquals(600000,total())
    }
    @Test fun pendingVisitIsNotACompletedSession()=runBlocking {engine.signal("Wi-Fi",true);advance(900000);engine.tick();val v=db.focusDao().visits().first().single();assertNull(v.qualified);assertNull(v.end);assertEquals(900000,total())}
    @Test fun recoveryClosesAtCheckpointWithoutInventingUnobservedTime()=runBlocking {
        engine.signal("Wi-Fi",true);advance(600000);engine.tick();advance(3600000)
        FocusRecorder(db,clock,{c},{true}).recoverVisit()
        val v=db.focusDao().visits().first().single();assertEquals(true,v.qualified);assertEquals(600000,v.end!!-v.start)
    }
    @Test fun processRecoveryDuringDepartureGraceMustNotQualifyAnActuallyShortVisit()=runBlocking {
        engine.signal("Wi-Fi",true);advance(599000);val exit=now;engine.signal("Wi-Fi",false)
        advance(30000);engine.tick();engine.recover(processRestart=true)
        val visits=db.focusDao().visits().first()
        assertEquals("Recovery must not invent an arrival from exit grace alone",1,visits.size)
        assertEquals(exit,visits.single().end)
        assertEquals(false,visits.single().qualified)
        advance(60000);engine.tick()
        assertEquals(1,db.focusDao().visits().first().size)
    }
    @Test fun historicalLegacyAndFinalizedRecordsAreImmutable() {
        val legacy=LibraryVisit(start=0,end=60000,elapsedStart=0,boot=1)
        assertEquals(legacy,VisitQualification.finish(legacy,9999999))
        val finished=VisitQualification.finish(legacy.copy(end=null,minimumMinutes=10),600000)
        assertEquals(finished,VisitQualification.finish(finished,9999999))
    }
    @Test fun preferencesPersistAndRejectInvalidThresholds() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("settings",0).edit().clear().commit()
        val preferences=Preferences(context);assertEquals(10,preferences.value.minimumSessionMinutes)
        preferences.save(preferences.value.copy(minimumSessionMinutes=46,streakMinutes=225,goalMinutes=450))
        assertEquals(preferences.value,Preferences(context).value)
        listOf(0,-1,1441,Int.MAX_VALUE).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) {preferences.save(preferences.value.copy(minimumSessionMinutes=value))}
        }
    }
}
