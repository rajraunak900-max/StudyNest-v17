package com.studynest.app.domain

import com.studynest.app.data.Segment
import com.studynest.app.data.StudySession
import java.time.*

data class TrackingPart(val sessionId: Long, val segmentId: Long, val trigger: String, val range: TimeRange)
data class StudyPeriod(val parts: List<TrackingPart>) {
    val start get() = parts.minOf { it.range.start }
    val end get() = parts.maxOf { it.range.end }
    val key get() = "${parts.first().segmentId}:$start"
    val studyMs get() = parts.sumOf { it.range.end - it.range.start }
    val breakMs get() = (end - start - studyMs).coerceAtLeast(0)
}

/** Presentation groups only; original records remain intact and totals unchanged. */
object StudyPeriods {
    const val SEPARATE_GAP_MS = 30 * 60 * 1000L

    fun forDay(sessions: List<StudySession>, segments: List<Segment>, day: LocalDate,
               zone: ZoneId, range: (Segment) -> TimeRange): List<StudyPeriod> =
        forDay(sessions, segments, day, zone, range, emptyList())

    fun forDay(sessions: List<StudySession>, segments: List<Segment>, day: LocalDate,
               zone: ZoneId, range: (Segment) -> TimeRange, excluded: List<TimeRange>, minimumDurationMs: Long = 60_000L): List<StudyPeriod> {
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val byId = sessions.associateBy { it.id }
        val parts = segments.flatMap { segment ->
            val session = byId[segment.sessionId] ?: return@flatMap emptyList()
            val r = range(segment)
            val clipped = TimeRange(maxOf(start, r.start), minOf(end, r.end))
            var kept = if (clipped.end <= clipped.start) emptyList() else listOf(clipped)
            for (cut in excluded) kept = kept.flatMap { r ->
                if (cut.end <= r.start || cut.start >= r.end) listOf(r)
                else listOf(TimeRange(r.start, minOf(r.end, cut.start)), TimeRange(maxOf(r.start, cut.end), r.end))
                    .filter { it.end > it.start }
            }
            kept.map { TrackingPart(session.id, segment.id, session.trigger, it) }
        }.sortedBy { it.range.start }
        val groups = mutableListOf<MutableList<TrackingPart>>()
        for (part in parts) {
            val previous = groups.lastOrNull()?.lastOrNull()
            val explicitlyFinished = previous != null && previous.sessionId != part.sessionId &&
                byId[previous.sessionId]?.endedExplicitly == true
            if (previous == null || explicitlyFinished || part.range.start - previous.range.end >= SEPARATE_GAP_MS)
                groups.add(mutableListOf(part))
            else groups.last().add(part)
        }
        // Hide short groups from Session Information, not their source records or cumulative totals.
        return groups.map { StudyPeriod(it) }.filter { it.studyMs >= minimumDurationMs.coerceAtLeast(1L) }
    }
}
