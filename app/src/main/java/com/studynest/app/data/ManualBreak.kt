package com.studynest.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Only an explicit Pause inside a detected visit creates a record. */
@Entity(tableName = "manual_breaks", indices = [Index("sessionId"), Index("visitId")])
data class ManualBreak(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long, val visitId: Long, val start: Long, val end: Long? = null,
    val checkpoint: Long = start, val elapsedStart: Long, val boot: Int,
    @ColumnInfo(defaultValue = "0") val reminderAttempted: Boolean = false
)

/** Distinguishes unrecorded old history from a measured zero-break day. */
@Entity(tableName = "break_tracking")
data class BreakTracking(@PrimaryKey val id: Int = 1, val startedAt: Long)

@Dao
interface BreakDao {
    @Query("SELECT * FROM manual_breaks ORDER BY start") fun records(): Flow<List<ManualBreak>>
    @Query("SELECT * FROM manual_breaks WHERE end IS NULL LIMIT 1") suspend fun active(): ManualBreak?
    @Query("SELECT * FROM break_tracking WHERE id=1") fun coverage(): Flow<BreakTracking?>
    @Query("SELECT * FROM break_tracking WHERE id=1") suspend fun tracking(): BreakTracking?
    @Insert suspend fun insert(record: ManualBreak): Long
    @Update suspend fun update(record: ManualBreak)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun begin(tracking: BreakTracking)
}
