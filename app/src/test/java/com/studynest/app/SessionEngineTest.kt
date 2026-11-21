package com.studynest.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
@RoboConfig(sdk = [33], application = android.app.Application::class)
class SessionEngineTest {
    private lateinit var db: StudyDatabase
    private lateinit var engine: SessionEngine
    private var config = Config(wifi = true, geo = true)
    private val clock =
        object : StudyClock {
            var wall = 1790000000000L
            var monotonic = 1000000L
            var boots = 3

            override fun now() = wall

            override fun elapsed() = monotonic

            override fun boot() = boots

            fun advance(ms: Long) {
                wall += ms
                monotonic += ms
            }
        }
    private val sound =
        object : SoundControl {
            var mode = 2
            var permission = true

            override fun current() = mode

            override suspend fun set(mode: Int): Boolean {
                if (!permission) return false
                this.mode = mode
                return true
            }
        }

    private fun makeEngine() = SessionEngine(db, clock, { config }, sound, {})

    private suspend fun duration() =
        db.dao().segments().first().sumOf { engine.range(it).let { r -> r.end - r.start } }

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(), StudyDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        engine = makeEngine()
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun pauseResumeExcludesBreaks() = runBlocking {
        engine.manualStart()
        clock.advance(60000)
        engine.pauseResume()
        clock.advance(120000)
        engine.pauseResume()
        clock.advance(60000)
        engine.finish()
        assertEquals(120000L, duration())
        assertNull(db.dao().active())
        assertEquals(2, sound.mode)
    }

    @Test
    fun duplicateSignalsAndBothSourcesCreateOneSession() = runBlocking {
        coroutineScope { repeat(8) { launch { engine.signal("Wi-Fi", true) } } }
        engine.signal("Location", true)
        assertEquals(1, db.dao().sessions().first().size)
        engine.signal("Wi-Fi", false)
        clock.advance(120000)
        engine.tick()
        assertNotNull(db.dao().active())
        engine.signal("Location", false)
        clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS)
        engine.tick()
        assertTrue(db.dao().active()!!.pausedByDeparture)
        assertEquals(2, sound.mode)
    }

    @Test
    fun reconnectWithinGraceKeepsOneSession() = runBlocking {
        engine.signal("Wi-Fi", true)
        clock.advance(60000)
        engine.signal("Wi-Fi", false)
        clock.advance(45000)
        engine.signal("Wi-Fi", true)
        engine.tick()
        assertEquals(1, db.dao().sessions().first().size)
        assertNotNull(db.dao().active())
    }

    @Test
    fun confirmedExitCutsAtDisconnect() = runBlocking {
        engine.signal("Wi-Fi", true)
        clock.advance(60000)
        engine.signal("Wi-Fi", false)
        clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS)
        engine.tick()
        assertEquals(60000L, duration())
    }

    @Test
    fun autoSessionCannotBeDuplicatedByManualStart() = runBlocking {
        engine.signal("Wi-Fi", true)
        engine.manualStart()
        assertEquals(1, db.dao().sessions().first().size)
        assertEquals("Wi-Fi", db.dao().active()?.trigger)
    }

    @Test
    fun restorationRetriesAfterPermissionReturns() = runBlocking {
        config = config.copy(wifi = false, geo = false)
        engine.manualStart()
        sound.permission = false
        engine.finish()
        assertEquals(1, sound.mode)
        assertTrue(db.dao().state()!!.soundOwned)
        sound.permission = true
        engine.tick()
        assertEquals(2, sound.mode)
        assertFalse(db.dao().state()!!.soundOwned)
    }

    @Test
    fun manualSessionSurvivesLocationExit() = runBlocking {
        config = config.copy(tracking = false)
        engine.manualStart()
        engine.signal("Wi-Fi", true)
        engine.signal("Wi-Fi", false)
        clock.advance(120000)
        engine.tick()
        assertEquals("Manual", db.dao().active()?.trigger)
        assertEquals(1, db.dao().sessions().first().size)
    }

    @Test
    fun manualFinishSuppressesUntilReentry() = runBlocking {
        engine.signal("Wi-Fi", true)
        engine.finish()
        engine.signal("Wi-Fi", true)
        assertNull(db.dao().active())
        engine.signal("Wi-Fi", false)
        clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS)
        engine.tick()
        engine.signal("Wi-Fi", true)
        assertEquals(2, db.dao().sessions().first().size)
    }

    @Test
    fun processRestartKeepsManualTimer() = runBlocking {
        engine.manualStart()
        clock.advance(60000)
        engine = makeEngine()
        engine.recover(true)
        clock.advance(60000)
        engine.finish()
        assertEquals(120000L, duration())
    }

    @Test
    fun rebootExcludesPoweredOffTime() = runBlocking {
        engine.manualStart()
        clock.advance(60000)
        engine.tick()
        clock.wall += 3600000
        clock.boots++
        clock.monotonic = 30000
        engine = makeEngine()
        engine.recover(true)
        assertNull(db.dao().active())
        assertEquals(60000L, duration())
    }

    @Test
    fun clockChangeDoesNotInflateDuration() = runBlocking {
        engine.manualStart()
        clock.advance(60000)
        clock.wall += 3600000
        engine.finish()
        assertEquals(60000L, duration())
    }

    @Test
    fun openingAppAfterSleepDoesNotTruncateLiveSession() = runBlocking {
        engine.signal("Wi-Fi", true)
        clock.advance(300000)
        engine.recover()
        assertNotNull(db.dao().active())
        engine.finish()
        assertEquals(300000L, duration())
    }

    @Test
    fun interruptedAutoMonitoringClosesAtCheckpoint() = runBlocking {
        engine.signal("Wi-Fi", true)
        clock.advance(60000)
        engine.tick()
        clock.advance(300000)
        engine = makeEngine()
        engine.recover(true)
        assertNull(db.dao().active())
        assertEquals(60000L, duration())
    }

    @Test
    fun respectsUserSoundOverride() = runBlocking {
        engine.manualStart()
        engine.signal("Wi-Fi", true)
        assertEquals(1, sound.mode)
        sound.mode = 0
        engine.tick()
        engine.finish()
        assertEquals(0, sound.mode)
    }

    @Test
    fun vibrationWithoutAutomaticTracking() = runBlocking {
        config = config.copy(tracking = false)
        engine.signal("Wi-Fi", true)
        assertEquals(1, sound.mode)
        assertNull(db.dao().active())
        engine.signal("Wi-Fi", false)
        clock.advance(120000)
        engine.tick()
        assertEquals(2, sound.mode)
    }

    @Test
    fun explicitSoundRetryClearsOverrideWithoutRestartingSession() = runBlocking {
        engine.signal("Wi-Fi", true)
        val id = db.dao().active()!!.id
        sound.mode = 2
        engine.tick()
        assertTrue(db.dao().state()!!.soundOverridden)
        engine.retrySound()
        assertEquals(1, sound.mode)
        assertFalse(db.dao().state()!!.soundOverridden)
        assertEquals(id, db.dao().active()!!.id)
        assertEquals(1, db.dao().sessions().first().size)
    }

    @Test
    fun retryWhileAlreadyVibratingPreservesRestorationMode() = runBlocking {
        config = config.copy(wifi = false, geo = false)
        engine.manualStart()
        engine.retrySound()
        engine.finish()
        assertEquals(2, sound.mode)
    }

    @Test
    fun soundRetryWithoutStudyDoesNotChangeRinger() = runBlocking {
        engine.retrySound()
        assertEquals(2, sound.mode)
        assertNull(db.dao().active())
    }

    @Test
    fun soundRetryRecoversFromPermissionFailure() = runBlocking {
        config = config.copy(wifi = false, geo = false)
        sound.permission = false
        engine.manualStart()
        assertEquals(2, sound.mode)
        sound.permission = true
        engine.retrySound()
        assertEquals(1, sound.mode)
        engine.finish()
        assertEquals(2, sound.mode)
    }

    @Test
    fun automaticCanBeRearmedWithoutLeavingLibrary() = runBlocking {
        engine.signal("Wi-Fi", true)
        engine.finish()
        assertNull(db.dao().active())
        engine.startAutomatic()
        assertEquals("Wi-Fi", db.dao().active()?.trigger)
        engine.startAutomatic()
        assertEquals(2, db.dao().sessions().first().size)
    }

    @Test
    fun stoppingAutomaticTrackingSavesSessionAndKeepsLibraryVibration() = runBlocking {
        engine.signal("Wi-Fi", true)
        clock.advance(60000)
        config = config.copy(tracking = false)
        engine.recover()
        assertNull(db.dao().active())
        assertEquals(60000L, duration())
        assertFalse(db.dao().sessions().first().single().endedExplicitly)
        assertEquals(1, sound.mode)
        engine.signal("Wi-Fi", true)
        assertNull(db.dao().active())
    }

    @Test
    fun armingAutomaticAdoptsManualSessionWithoutDuplication() = runBlocking {
        engine.manualStart()
        engine.signal("Wi-Fi", true)
        engine.startAutomatic()
        assertEquals(SessionEngine.MANUAL_AUTO_TRIGGER, db.dao().active()?.trigger)
        assertEquals(1, db.dao().sessions().first().size)
    }

    @Test
    fun departurePausesManualAndRequiresExplicitResume() = runBlocking {
        config = config.copy(tracking = false)
        engine.manualStart()
        engine.signal("Wi-Fi", true)
        clock.advance(60000)
        engine.signal("Wi-Fi", false)
        clock.advance(120000)
        engine.tick()
        assertTrue(db.dao().active()!!.pausedByDeparture)
        assertEquals(60000L, duration())
        assertEquals(2, sound.mode)
        engine.signal("Wi-Fi", true)
        clock.advance(60000)
        assertTrue(db.dao().active()!!.paused)
        assertEquals(60000L, duration())
        engine.pauseResume()
        clock.advance(60000)
        assertEquals(120000L, duration())
    }

    @Test
    fun automaticDeparturePauseResumesWithoutCountingAbsence() = runBlocking {
        engine.signal("Wi-Fi", true)
        val id = db.dao().active()!!.id
        clock.advance(60000)
        engine.signal("Wi-Fi", false)
        clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS)
        engine.tick()
        assertTrue(db.dao().active()!!.pausedByDeparture)
        engine = makeEngine()
        engine.recover(true)
        clock.advance(3600000)
        engine.signal("Wi-Fi", true)
        clock.advance(60000)
        assertFalse(db.dao().active()!!.paused)
        assertEquals(id, db.dao().active()!!.id)
        assertEquals(120000L, duration())
    }

    @Test
    fun userPausedAutomaticDoesNotResumeOnReturn() = runBlocking {
        engine.signal("Wi-Fi", true)
        engine.pauseResume()
        engine.signal("Wi-Fi", false)
        clock.advance(SessionEngine.WIFI_EXIT_GRACE_MS)
        engine.tick()
        engine.signal("Wi-Fi", true)
        // 1.6.9: an expired user-paused automatic visit now starts fresh on return.
        assertFalse(db.dao().active()!!.paused)
        assertFalse(db.dao().active()!!.pausedByDeparture)
    }

    @Test
    fun versionOneMigrationPreservesStudyData() = runBlocking { validateMigration(1) }

    @Test
    fun versionTwoMigrationPreservesStudyData() = runBlocking { validateMigration(2) }

    @Test fun versionThreeMigrationPreservesStudyData() = runBlocking { validateMigration(3) }

    @Test fun versionFourMigrationPreservesStudyData() = runBlocking { validateMigration(4) }

    private suspend fun validateMigration(version: Int) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema =
            org.json
                .JSONObject(
                    java.io.File("schemas/com.studynest.app.data.StudyDatabase/$version.json").readText())
                .getJSONObject("database")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) old.execSQL(
                    indices
                        .getJSONObject(j)
                        .getString("createSql")
                        .replace("\${TABLE_NAME}", table))
            }
            old.execSQL(
                "INSERT INTO sessions (id, startedAt, endedAt, trigger, paused, note) VALUES (1, 1000, 61000, 'Manual', 0, '')")
            old.execSQL(
                "INSERT INTO tasks (id, title, dueDate, priority, completed) VALUES (1, 'Keep my task', NULL, 1, 0)")
            old.execSQL("INSERT INTO segments (id, sessionId, start, end, elapsedStart, boot, checkpoint) VALUES (1, 1, 1000, 61000, 0, 1, 61000)")
            old.execSQL("INSERT INTO automation (id, wifiInside, geoInside, exitAt, suppressed, previousRinger, soundOwned, soundOverridden) VALUES (1, 1, 0, NULL, 0, NULL, 0, 0)")
            old.version = version
        }
        val migrated =
            Room.databaseBuilder(context, StudyDatabase::class.java, name)
                .addMigrations(StudyDatabase.MIGRATION_1_2, StudyDatabase.MIGRATION_2_3, StudyDatabase.MIGRATION_3_4, StudyDatabase.MIGRATION_4_5, StudyDatabase.MIGRATION_5_6, StudyDatabase.MIGRATION_6_7, StudyDatabase.MIGRATION_7_8)
                .build()
        try {
            assertEquals(61000L, migrated.dao().sessions().first().single().endedAt)
            assertEquals(60000L, migrated.dao().segmentsForSession(1).single().let { it.end!! - it.start })
            assertTrue(migrated.dao().state()!!.wifiInside)
            assertFalse(migrated.dao().sessions().first().single().pausedByDeparture)
            assertEquals("Keep my task", migrated.dao().tasks().first().single().title)
            assertEquals(if (version < 3) 0 else 1, migrated.dao().tasks().first().single().priority)
            assertFalse(migrated.dao().sessions().first().single().endedExplicitly)
            assertNull(migrated.dao().sessions().first().single().measurementStart)
            assertTrue(migrated.focusDao().visits().first().isEmpty())
            assertTrue(migrated.breakDao().records().first().isEmpty())
            assertNull(migrated.breakDao().tracking())
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun taskAndSessionSurviveDatabaseReopen() = runBlocking {
        db.close()
        val c = ApplicationProvider.getApplicationContext<Context>()
        c.deleteDatabase("persistence-test")
        db =
            Room.databaseBuilder(c, StudyDatabase::class.java, "persistence-test")
                .allowMainThreadQueries()
                .build()
        engine = makeEngine()
        engine.manualStart()
        clock.advance(60000)
        engine.finish()
        db.dao().saveTask(StudyTask(title = "IELTS Writing", dueDate = "2026-09-30", priority = 2))
        db.close()
        db =
            Room.databaseBuilder(c, StudyDatabase::class.java, "persistence-test")
                .allowMainThreadQueries()
                .build()
        assertEquals("IELTS Writing", db.dao().tasks().first().single().title)
        assertEquals(1, db.dao().sessions().first().size)
        assertEquals(60000L, db.dao().segments().first().single().let { it.end!! - it.start })
        c.deleteDatabase("persistence-test")
        Unit
    }
}

