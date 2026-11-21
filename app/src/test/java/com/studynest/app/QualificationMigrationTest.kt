package com.studynest.app

import android.app.Application
import android.content.Context
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
class QualificationMigrationTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun oldDatabase(version:Int):SupportSQLiteOpenHelper {
        context.deleteDatabase("studynest.db")
        val schema=JSONObject(java.io.File("schemas/com.studynest.app.data.StudyDatabase/$version.json").readText()).getJSONObject("database")
        return FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name("studynest.db")
            .callback(object:SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db:SupportSQLiteDatabase) {
                    val entities=schema.getJSONArray("entities")
                    for(i in 0 until entities.length()) {
                        val entity=entities.getJSONObject(i);val table=entity.getString("tableName")
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                        val indices=entity.getJSONArray("indices")
                        for(j in 0 until indices.length())db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
                    }
                    val setup=schema.getJSONArray("setupQueries")
                    for(i in 0 until setup.length())db.execSQL(setup.getString(i))
                }
                override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int)=error("Unexpected")
            }).build())
    }
    @Test fun stableSixMigrationPreservesEveryExistingColumnAndRow()=runBlocking {
        val helper=oldDatabase(6);val old=helper.writableDatabase
        old.execSQL("INSERT INTO sessions VALUES (7,1000,61000,'Manual',0,'Preserved',0,1,1000)")
        old.execSQL("INSERT INTO segments VALUES (8,7,1000,61000,1000,1,61000)")
        old.execSQL("INSERT INTO tasks VALUES (9,'Preserve task','2026-10-02',2,1)")
        old.execSQL("INSERT INTO automation VALUES (1,1,0,NULL,0,2,1,0,NULL,0,NULL,0,1)")
        old.execSQL("INSERT INTO library_visits VALUES (10,1000,61000,61000,1000,1)")
        old.execSQL("INSERT INTO focus_windows VALUES (11,7,1000,61000,61000,1000,1,'OFF','[]',0,61000,'',1)")
        old.execSQL("INSERT INTO distraction_periods VALUES (12,11,2000,3000,'APP','selected.app')")
        old.execSQL("INSERT INTO manual_breaks VALUES (13,7,10,3000,4000,4000,3000,1,1)")
        old.execSQL("INSERT INTO break_tracking VALUES (1,1000)")
        val tables=listOf("sessions","segments","tasks","automation","library_visits","focus_windows","distraction_periods","manual_breaks","break_tracking")
        fun snapshot(db:SupportSQLiteDatabase,table:String):Map<String,List<String?>> =db.query("SELECT * FROM $table ORDER BY id").use {cursor ->
            val values=cursor.columnNames.associateWith {mutableListOf<String?>()}
            while(cursor.moveToNext())cursor.columnNames.forEach {name ->val index=cursor.getColumnIndex(name);values[name]!!.add(if(cursor.isNull(index))null else cursor.getString(index))}
            values
        }
        val before=tables.associateWith {snapshot(old,it)};helper.close()
        val db=StudyDatabase.open(context)
        try {
            assertEquals(8,db.openHelper.writableDatabase.version)
            tables.forEach {table ->val after=snapshot(db.openHelper.writableDatabase,table);before[table]!!.forEach {(column,values)->assertEquals("$table.$column",values,after[column])}}
            val legacy=db.focusDao().visits().first().single();assertNull(legacy.minimumMinutes);assertNull(legacy.qualified)
            val periods=StudyPeriods.forDay(db.dao().sessions().first(),db.dao().segments().first(),java.time.LocalDate.ofEpochDay(0),java.time.ZoneOffset.UTC){TimeRange(it.start,it.end!!)}
            assertEquals(1,periods.size)
        }finally{db.close();context.deleteDatabase("studynest.db")}
    }
    @Test fun everySupportedOlderSchemaCanOpenThroughAdditiveMigrations() {
        for(version in 1..5) {
            oldDatabase(version).use {it.writableDatabase}
            val db=StudyDatabase.open(context)
            try {assertEquals(8,db.openHelper.writableDatabase.version)}finally{db.close();context.deleteDatabase("studynest.db")}
        }
    }
    @Test fun qualificationAndArrivalThresholdSurviveDatabaseReopen()=runBlocking {
        context.deleteDatabase("studynest.db")
        var db=StudyDatabase.open(context)
        val closed=VisitQualification.finish(LibraryVisit(start=1000,elapsedStart=0,boot=1,minimumMinutes=45),2701000)
        db.focusDao().insertVisit(closed)
        db.focusDao().insertVisit(LibraryVisit(start=4000000,checkpoint=6760000,elapsedStart=0,boot=1,minimumMinutes=46))
        db.close();db=StudyDatabase.open(context)
        try {
            val records=db.focusDao().visits().first();assertEquals(true,records.first().qualified);assertEquals(45,records.first().minimumMinutes)
            val clock=object:StudyClock {override fun now()=9999999L;override fun elapsed()=0L;override fun boot()=2}
            FocusRecorder(db,clock,{Config(minimumSessionMinutes=30)},{true}).recoverVisit()
            val recovered=db.focusDao().visits().first().last();assertEquals(46,recovered.minimumMinutes);assertEquals(true,recovered.qualified)
            assertEquals(2760000L,recovered.end!!-recovered.start)
        }finally{db.close();context.deleteDatabase("studynest.db")}
    }
}
