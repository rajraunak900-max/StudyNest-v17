package com.studynest.app.domain

import com.studynest.app.data.LibraryVisit
import org.json.JSONArray

/** Logical visit span and accumulated presence are deliberately independent. */
object LibraryPresence {
    fun ranges(visit: LibraryVisit, now: Long): List<TimeRange> {
        val until = minOf(visit.end ?: now, visit.awaySince ?: Long.MAX_VALUE)
        var kept = if (until > visit.start) listOf(TimeRange(visit.start, until)) else emptyList()
        // Most/legacy visits have no gaps; no Android JSON allocation is needed
        // for their pure time projection (also usable by ordinary JVM callers).
        if (visit.awayRanges == "[]") return kept
        val gaps = JSONArray(visit.awayRanges)
        for (i in 0 until gaps.length()) {
            val gap = gaps.getJSONArray(i); val from = gap.getLong(0); val end = gap.getLong(1)
            kept = kept.flatMap { r ->
                if (end <= r.start || from >= r.end) listOf(r)
                else listOf(TimeRange(r.start, minOf(r.end, from)), TimeRange(maxOf(r.start, end), r.end))
                    .filter { it.end > it.start }
            }
        }
        return kept
    }
    fun duration(visit: LibraryVisit, now: Long): Long = ranges(visit, now).sumOf { it.end-it.start }
    fun resume(visit: LibraryVisit, now: Long): LibraryVisit {
        val from = visit.awaySince ?: return visit
        val gaps = JSONArray(visit.awayRanges)
        if (now > from) gaps.put(JSONArray().put(from).put(now))
        return visit.copy(awaySince = null, awayRanges = gaps.toString())
    }
}
