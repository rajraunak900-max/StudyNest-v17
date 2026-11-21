package com.studynest.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** A detected visit is independent of studying, manual pauses and app usage. */
@Entity(tableName = "library_visits")
data class LibraryVisit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val start: Long, val end: Long? = null, val checkpoint: Long = start,
    val elapsedStart: Long, val boot: Int,
    // Null denotes pre-1.6.5 history: retain its original presentation rules.
    val minimumMinutes: Int? = null,
    val qualified: Boolean? = null,
    // Only confirmed future departures populate these fields. Legacy rows keep
    // their original continuous accounting; no historical gaps are inferred.
    @ColumnInfo(defaultValue = "'[]'") val awayRanges: String = "[]",
    val awaySince: Long? = null
)

/** Snapshot selections at each boundary; later edits never rewrite earlier time. */
@Entity(tableName = "focus_windows", indices = [Index("sessionId")])
data class FocusWindow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long, val start: Long, val end: Long? = null,
    val checkpoint: Long = start, val elapsedStart: Long, val boot: Int,
    val mode: String, val packages: String = "[]", val calls: Boolean = false,
    val cursor: Long = start, val tracker: String = "", val seeded: Boolean = false
)

/** Only selected package/category intervals are saved, never app content. */
@Entity(tableName = "distraction_periods", indices = [Index("windowId")])
data class DistractionPeriod(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val windowId: Long, val start: Long, val end: Long,
    val reason: String, val packageName: String? = null
)

@Dao
interface FocusDao {
    @Query("SELECT * FROM library_visits ORDER BY start") fun visits(): Flow<List<LibraryVisit>>
    @Query("SELECT * FROM focus_windows ORDER BY start") fun windows(): Flow<List<FocusWindow>>
    @Query("SELECT * FROM distraction_periods ORDER BY start") fun distractions(): Flow<List<DistractionPeriod>>
    @Query("SELECT * FROM library_visits WHERE end IS NULL LIMIT 1") suspend fun openVisit(): LibraryVisit?
    @Query("SELECT * FROM focus_windows WHERE end IS NULL LIMIT 1") suspend fun openWindow(): FocusWindow?
    @Query("SELECT * FROM library_visits WHERE start < :until AND COALESCE(end, :until) > :from") suspend fun visitsBetween(from: Long, until: Long): List<LibraryVisit>
    @Query("SELECT * FROM focus_windows WHERE start < :until AND COALESCE(end, :until) > :from") suspend fun windowsBetween(from: Long, until: Long): List<FocusWindow>
    @Query("SELECT * FROM distraction_periods WHERE start < :until AND end > :from") suspend fun periodsBetween(from: Long, until: Long): List<DistractionPeriod>
    @Query("SELECT * FROM focus_windows WHERE mode = 'ON' AND (cursor < COALESCE(end, :until) OR (seeded=0 AND end IS NULL)) ORDER BY start") suspend fun pending(until: Long): List<FocusWindow>
    @Insert suspend fun insertVisit(visit: LibraryVisit): Long
    @Update suspend fun updateVisit(visit: LibraryVisit)
    @Insert suspend fun insertWindow(window: FocusWindow): Long
    @Update suspend fun updateWindow(window: FocusWindow)
    @Insert suspend fun insertPeriods(periods: List<DistractionPeriod>)
    @Query("UPDATE focus_windows SET cursor=:cursor, tracker=:tracker, seeded=1 WHERE id=:id")
    suspend fun progress(id: Long, cursor: Long, tracker: String)
}

