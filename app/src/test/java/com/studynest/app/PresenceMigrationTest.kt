package com.studynest.app

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.studynest.app.data.*
import com.studynest.app.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.json.JSONObject
import java.io.File

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33],application=Application::class)
class PresenceMigrationTest {
    @Test fun schemaFivePreservesExistingRecords()=runBlocking{migrate(5)}
    @Test fun schemaSixPreservesExistingRecords()=runBlocking{migrate(6)}
    @Test fun schemaSevenPreservesAllExistingTablesAndSettings()=runBlocking{migrate(7)}
    private suspend fun migrate(version:Int){
        val context=ApplicationProvider.getApplicationContext<Context>();val name="presence-migration-$version"
        context.deleteDatabase(name);val file=context.getDatabasePath(name);file.parentFile!!.mkdirs()
        val schema=JSONObject(File("schemas/com.studynest.app.data.StudyDatabase/$version.json").readText()).getJSONObject("database").getJSONArray("entities")
        val snapshots=mutableMapOf<String,Pair<List<String>,List<List<String?>>>>()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file,null).use { old ->
            for(i in 0 until schema.length()){
                val entity=schema.getJSONObject(i);val table=entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                val indices=entity.getJSONArray("indices")
                for(j in 0 until indices.length())old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
                // Populate every existing column/table with non-default evidence.
                val fields=entity.getJSONArray("fields");val columns=mutableListOf<String>();val values=mutableListOf<Any?>()
                for(j in 0 until fields.length()){
                    val f=fields.getJSONObject(j);val col=f.getString("columnName");columns+=col
                    values+=when {
                        f.getString("affinity")=="TEXT" -> when(col){"packages"->"[\"com.example.selected\"]";"tracker"->"";"mode"->"OFF";else->"preserved-$col"}
                        col=="id" || col=="sessionId" || col=="visitId" || col=="windowId" ->1L
                        col=="start" || col=="startedAt" || col=="elapsedStart" ->1000L
                        col=="end" || col=="endedAt" || col=="checkpoint" || col=="cursor" ->601000L
                        col=="minimumMinutes" ->10L
                        else ->1L
                    }
                }
                old.execSQL("INSERT INTO `$table` (${columns.joinToString(","){"`$it`"}}) VALUES (${columns.joinToString(","){"?"}})",values.toTypedArray())
                old.rawQuery("SELECT * FROM `$table`",null).use { c ->
                    val rows=mutableListOf<List<String?>>()
                    while(c.moveToNext())rows+=c.columnNames.indices.map { if(c.isNull(it))null else c.getString(it) }
                    snapshots[table]=c.columnNames.toList() to rows
                }
            }
            old.version=version
        }
        val prefs=Preferences(context)
        val config=Config(setup=true,wifi=true,geo=false,goalMinutes=420,streakMinutes=120,monitoredApps=setOf("com.example.selected")).withNetworks(listOf("Library A","Library B"))
        prefs.save(config)
        val db=androidx.room.Room.databaseBuilder(context,StudyDatabase::class.java,name)
            .addMigrations(StudyDatabase.MIGRATION_5_6,StudyDatabase.MIGRATION_6_7,StudyDatabase.MIGRATION_7_8).build()
        try {
            val sql=db.openHelper.writableDatabase
            for((table,snapshot) in snapshots){
                sql.query("SELECT ${snapshot.first.joinToString(","){"`$it`"}} FROM `$table`").use { c ->
                    val rows=mutableListOf<List<String?>>()
                    while(c.moveToNext())rows+=c.columnNames.indices.map { if(c.isNull(it))null else c.getString(it) }
                    assertEquals("Original columns in $table",snapshot.second,rows)
                }
            }
            val v=db.focusDao().visits().first().single()
            assertEquals("[]",v.awayRanges);assertNull(v.awaySince)
            assertEquals(600000L,LibraryPresence.duration(v,9999999))
            assertEquals(if(version==7)true else null,v.qualified)
            assertEquals(config,Preferences(context).value)
        }finally{db.close();context.deleteDatabase(name)}
    }
    @Test fun confirmedAwayAndReturnedIntervalsSurviveDatabaseReopen()=runBlocking{
        val context=ApplicationProvider.getApplicationContext<Context>();val name="presence-reopen";context.deleteDatabase(name)
        var db=androidx.room.Room.databaseBuilder(context,StudyDatabase::class.java,name).build()
        val id=db.focusDao().insertVisit(LibraryVisit(start=1000,checkpoint=601000,elapsedStart=1000,boot=1,minimumMinutes=10,awaySince=601000))
        db.close();db=androidx.room.Room.databaseBuilder(context,StudyDatabase::class.java,name).build()
        val pending=db.focusDao().openVisit()!!;assertEquals(id,pending.id)
        assertEquals(600000L,LibraryPresence.duration(pending,1801000))
        db.focusDao().updateVisit(LibraryPresence.resume(pending,1201000))
        db.close();db=androidx.room.Room.databaseBuilder(context,StudyDatabase::class.java,name).build()
        try{
            val returned=db.focusDao().openVisit()!!;assertEquals(id,returned.id)
            assertEquals(660000L,LibraryPresence.duration(returned,1261000))
            val closed=VisitQualification.finish(returned,1261000);assertEquals(true,closed.qualified)
            db.focusDao().updateVisit(closed)
            assertEquals(660000L,FocusMath.summary(db.focusDao().visits().first(),emptyList(),emptyList(),9999999).libraryMs)
        }finally{db.close();context.deleteDatabase(name)}
    }
}
