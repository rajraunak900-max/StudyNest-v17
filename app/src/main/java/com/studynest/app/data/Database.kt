package com.studynest.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val trigger: String,
    val paused: Boolean = false,
    val note: String = "",
    @ColumnInfo(defaultValue = "0") val pausedByDeparture: Boolean = false,
    @ColumnInfo(defaultValue = "0") val endedExplicitly: Boolean = false,
    val measurementStart: Long? = null
)

@Entity(
    tableName = "segments",
    foreignKeys =
        [
            ForeignKey(
                entity = StudySession::class,
                parentColumns = ["id"],
                childColumns = ["sessionId"],
                onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")])
data class Segment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val start: Long,
    val end: Long? = null,
    val elapsedStart: Long,
    val boot: Int,
    val checkpoint: Long = start
)

@Entity(tableName = "tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: String? = null,
    val priority: Int = 0,
    val completed: Boolean = false
)

@Entity(tableName = "automation")
data class AutomationState(
    @PrimaryKey val id: Int = 1,
    val wifiInside: Boolean = false,
    val geoInside: Boolean = false,
    val exitAt: Long? = null,
    val suppressed: Boolean = false,
    val previousRinger: Int? = null,
    val soundOwned: Boolean = false,
    val soundOverridden: Boolean = false,
    val wifiExitAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val normalSoundPending: Boolean = false,
    val overrideAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val overrideNotified: Boolean = false,
    @ColumnInfo(defaultValue = "0") val soundWasVibrate: Boolean = false
)

@Dao
interface StudyDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun sessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM segments ORDER BY start") fun segments(): Flow<List<Segment>>

    @Query("SELECT * FROM tasks ORDER BY completed, priority DESC, dueDate, id DESC")
    fun tasks(): Flow<List<StudyTask>>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL LIMIT 1")
    suspend fun active(): StudySession?

    @Query("SELECT * FROM sessions WHERE startedAt < :until AND COALESCE(endedAt, :until) > :from")
    suspend fun sessionsBetween(from: Long, until: Long): List<StudySession>

    @Query("SELECT * FROM segments WHERE sessionId = :sessionId ORDER BY start")
    suspend fun segmentsForSession(sessionId: Long): List<Segment>

    @Query("SELECT * FROM segments WHERE end IS NULL") suspend fun openSegments(): List<Segment>

    @Query("SELECT * FROM segments WHERE start < :until AND COALESCE(end, :until) > :from")
    suspend fun segmentsBetween(from: Long, until: Long): List<Segment>

    @Query("SELECT * FROM automation WHERE id=1") suspend fun state(): AutomationState?

    @Insert suspend fun insertSession(session: StudySession): Long

    @Update suspend fun updateSession(session: StudySession)

    @Insert suspend fun insertSegment(segment: Segment): Long

    @Update suspend fun updateSegment(segment: Segment)

    @Upsert suspend fun saveState(state: AutomationState)

    @Upsert suspend fun saveTask(task: StudyTask)

    @Delete suspend fun deleteTask(task: StudyTask)
}

@Database(
    entities = [StudySession::class, Segment::class, StudyTask::class, AutomationState::class, LibraryVisit::class, FocusWindow::class, DistractionPeriod::class, ManualBreak::class, BreakTracking::class],
    version = 8,
    exportSchema = true)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun dao(): StudyDao
    abstract fun focusDao(): FocusDao
    abstract fun breakDao(): BreakDao

    companion object {
        val MIGRATION_1_2 =
            object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE sessions ADD COLUMN pausedByDeparture INTEGER NOT NULL DEFAULT 0")
                }
            }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE automation ADD COLUMN wifiExitAt INTEGER")
                db.execSQL("ALTER TABLE automation ADD COLUMN normalSoundPending INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE automation ADD COLUMN overrideAt INTEGER")
                db.execSQL("ALTER TABLE automation ADD COLUMN overrideNotified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE automation ADD COLUMN soundWasVibrate INTEGER NOT NULL DEFAULT 0")
                // Legacy Low/Normal both map to the new Normal; High stays High.
                db.execSQL("UPDATE tasks SET priority = 0 WHERE priority < 2")
            }
        }
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN endedExplicitly INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN measurementStart INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS library_visits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, start INTEGER NOT NULL, end INTEGER, checkpoint INTEGER NOT NULL, elapsedStart INTEGER NOT NULL, boot INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS focus_windows (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, start INTEGER NOT NULL, end INTEGER, checkpoint INTEGER NOT NULL, elapsedStart INTEGER NOT NULL, boot INTEGER NOT NULL, mode TEXT NOT NULL, packages TEXT NOT NULL, calls INTEGER NOT NULL, cursor INTEGER NOT NULL, tracker TEXT NOT NULL, seeded INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_windows_sessionId ON focus_windows (sessionId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS distraction_periods (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, windowId INTEGER NOT NULL, start INTEGER NOT NULL, end INTEGER NOT NULL, reason TEXT NOT NULL, packageName TEXT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_distraction_periods_windowId ON distraction_periods (windowId)")
            }
        }
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS manual_breaks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, visitId INTEGER NOT NULL, start INTEGER NOT NULL, end INTEGER, checkpoint INTEGER NOT NULL, elapsedStart INTEGER NOT NULL, boot INTEGER NOT NULL, reminderAttempted INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_manual_breaks_sessionId ON manual_breaks (sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_manual_breaks_visitId ON manual_breaks (visitId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS break_tracking (id INTEGER NOT NULL, startedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                // Old history contains no explicit pause records. Do not backfill guesses.
            }
        }
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE library_visits ADD COLUMN minimumMinutes INTEGER")
                db.execSQL("ALTER TABLE library_visits ADD COLUMN qualified INTEGER")
                // No guesses about old thresholds, no updates/deletes to existing records.
            }
        }
        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE library_visits ADD COLUMN awayRanges TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE library_visits ADD COLUMN awaySince INTEGER")
                // Additive only: preserve all rows, settings and old qualification.
            }
        }
        fun open(context: Context) =
            Room.databaseBuilder(context, StudyDatabase::class.java, "studynest.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
    }
}

