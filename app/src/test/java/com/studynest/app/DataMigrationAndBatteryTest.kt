package com.studynest.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.*
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=Application::class)
class DataMigrationAndBatteryTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun versionFiveDatabaseMigratesWithoutLosingAnyExistingTablesOrRows()=runBlocking {
        context.deleteDatabase("studynest.db")
        val schema=JSONObject(java.io.File("schemas/com.studynest.app.data.StudyDatabase/5.json").readText()).getJSONObject("database")
        val helper=FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("studynest.db").callback(object:SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db:SupportSQLiteDatabase) {
                    val entities=schema.getJSONArray("entities")
                    for(i in 0 until entities.length()) {
                        val entity=entities.getJSONObject(i)
                        val table=entity.getString("tableName")
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                        val indices=entity.getJSONArray("indices")
                        for(j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
                    }
                    val setup=schema.getJSONArray("setupQueries")
                    for(i in 0 until setup.length()) db.execSQL(setup.getString(i))
                }
                override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int) { error("Unexpected") }
            }).build())
        val old=helper.writableDatabase
        old.execSQL("INSERT INTO sessions (id,startedAt,endedAt,trigger,paused,note,pausedByDeparture,endedExplicitly,measurementStart) VALUES (7,1000,5000,'Manual',0,'Keep me',0,1,1000)")
        old.execSQL("INSERT INTO segments (id,sessionId,start,end,elapsedStart,boot,checkpoint) VALUES (8,7,1000,5000,1000,1,5000)")
        old.execSQL("INSERT INTO tasks (id,title,dueDate,priority,completed) VALUES (9,'Preserve task','2026-10-02',2,1)")
        old.execSQL("INSERT INTO automation (id,wifiInside,geoInside,exitAt,suppressed,previousRinger,soundOwned,soundOverridden,wifiExitAt,normalSoundPending,overrideAt,overrideNotified,soundWasVibrate) VALUES (1,1,0,NULL,0,NULL,1,0,NULL,0,NULL,0,1)")
        old.execSQL("INSERT INTO library_visits (id,start,end,checkpoint,elapsedStart,boot) VALUES (10,1000,6000,6000,1000,1)")
        old.execSQL("INSERT INTO focus_windows (id,sessionId,start,end,checkpoint,elapsedStart,boot,mode,packages,calls,cursor,tracker,seeded) VALUES (11,7,1000,5000,5000,1000,1,'ON','[\"selected.app\"]',0,5000,'',1)")
        old.execSQL("INSERT INTO distraction_periods (id,windowId,start,end,reason,packageName) VALUES (12,11,2000,3000,'APP','selected.app')")
        helper.close()
        val db=StudyDatabase.open(context)
        try {
            assertEquals(8,db.openHelper.writableDatabase.version)
            assertEquals("Keep me",db.dao().sessions().first().single().note)
            assertEquals(4000L,db.dao().segments().first().single().let { it.end!!-it.start })
            assertEquals("Preserve task",db.dao().tasks().first().single().title)
            assertTrue(db.dao().tasks().first().single().completed)
            assertTrue(db.dao().state()!!.wifiInside);assertTrue(db.dao().state()!!.soundWasVibrate)
            assertEquals(10L,db.focusDao().visits().first().single().id)
            assertEquals("[\"selected.app\"]",db.focusDao().windows().first().single().packages)
            assertEquals("selected.app",db.focusDao().distractions().first().single().packageName)
            assertTrue(db.breakDao().records().first().isEmpty());assertNull(db.breakDao().tracking())
        } finally { db.close();context.deleteDatabase("studynest.db") }
    }
    @Test fun stableSoundTicksDoNotRewriteAutomationOrCancelWorkRepeatedly()=runBlocking {
        var writes=0;var cancels=0;var schedules=0
        val db=Room.inMemoryDatabaseBuilder(context,StudyDatabase::class.java)
            .setQueryCallback({sql,_ -> if((sql.startsWith("INSERT",true)||sql.startsWith("UPDATE",true))&&sql.contains("automation")) writes++ },java.util.concurrent.Executor { it.run() }).build()
        var time=1_800_000_000_000L;var mode=2
        val config=Config(wifi=true,tracking=false,vibrate=true,overrideMode="RESTORE")
        val engine=SessionEngine(db,object:StudyClock {
            override fun now()=time;override fun elapsed()=time;override fun boot()=1
        },{config},object:SoundControl {
            override fun current()=mode;override suspend fun set(value:Int):Boolean { mode=value;return true }
        },{},cancelReminder={cancels++},scheduleOverride={schedules++})
        try {
            engine.signal("Wi-Fi",true)
            engine.tick() // First observation clears any persisted fallback once.
            writes=0;val previous=cancels
            repeat(6) { time+=15000;engine.tick() }
            assertEquals(0,writes);assertEquals(previous,cancels)
            mode=2;engine.tick();assertEquals(1,schedules)
            repeat(6) { time+=15000;engine.tick() };assertEquals(1,schedules)
            time+=300000;engine.tick();assertEquals(1,mode)
        } finally { db.close() }
    }
}
