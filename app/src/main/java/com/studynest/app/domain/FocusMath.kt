package com.studynest.app.domain

import com.studynest.app.data.*

data class FocusSummary(val libraryMs: Long = 0, val studyMs: Long = 0,
    val distractionMs: Long = 0, val unknownMs: Long = 0, val libraryStudyMs: Long = 0,
    val libraryUnknownMs: Long = 0, val manualOutsideMs: Long = 0,
    val breakdown: Map<String, Long> = emptyMap(), val recorded: Boolean = false,
    val libraryDistractionMs: Long = 0, val breakMs: Long = 0,
    val libraryBreakdown: Map<String, Long> = emptyMap()) {
    fun efficiency(sittingMs: Long): Double? = EfficiencyMath.calculate(studyMs, sittingMs, unknownMs)
}

/** Efficiency uses all valid Actual Study against the union-based Sitting Time denominator. */
object EfficiencyMath {
    fun calculate(actualMs: Long, sittingMs: Long, unknownMs: Long = 0L): Double? =
        if (sittingMs > 0 && unknownMs == 0L)
            (100.0 * actualMs / sittingMs).coerceIn(0.0, 100.0)
        else null
}

object FocusMath {
    private fun overlap(a: TimeRange, b: TimeRange): TimeRange? {
        val start = maxOf(a.start, b.start); val end = minOf(a.end, b.end)
        return if (end > start) TimeRange(start, end) else null
    }
    private fun union(items: List<TimeRange>): List<TimeRange> {
        val out = mutableListOf<TimeRange>()
        for (r in items.filter { it.end > it.start }.sortedBy { it.start }) {
            val last = out.lastOrNull()
            if (last != null && r.start <= last.end) out[out.lastIndex] = TimeRange(last.start, maxOf(last.end, r.end))
            else out += r
        }
        return out
    }
    fun summary(visits: List<LibraryVisit>, windows: List<FocusWindow>, periods: List<DistractionPeriod>,
        now: Long, range: TimeRange = TimeRange(0, Long.MAX_VALUE), sessionId: Long? = null, breaks: List<ManualBreak> = emptyList()): FocusSummary {
        val library = union(visits.flatMap { LibraryPresence.ranges(it, now) }.mapNotNull { overlap(it, range) })
        val selectedWindows = windows.filter { sessionId == null || it.sessionId == sessionId }
        var base = 0L; var distraction = 0L; var unknown = 0L
        var libraryBase = 0L; var libraryDistraction = 0L; var libraryUnknown = 0L
        val breakdown = mutableMapOf<String, Long>()
        val libraryBreakdown = mutableMapOf<String, Long>()
        val eligible = mutableListOf<TimeRange>()
        for (w in selectedWindows) {
            val r = overlap(TimeRange(w.start, w.end ?: now), range) ?: continue
            eligible += r
            base += r.end - r.start
            fun inLibrary(parts: List<TimeRange>) = parts.sumOf { part ->
                library.mapNotNull { overlap(part, it) }.sumOf { it.end - it.start }
            }
            libraryBase += inLibrary(listOf(r))
            val saved = periods.filter { it.windowId == w.id }
            val extra = if (w.mode == "ON" && (w.end ?: now) > w.cursor) {
                val selected = org.json.JSONArray(w.packages).let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                val state = UsageState.decode(w.tracker)
                if ((w.end ?: now) - w.cursor > 60000 && (state.interactive || state.call))
                    listOf(UsagePiece(w.cursor, w.end ?: now, "UNKNOWN"))
                else UsageTimeline.reduce(w.cursor, w.end ?: now, state, emptyList(), selected, w.calls).first
            } else emptyList()
            val all = saved.map { UsagePiece(it.start, it.end, it.reason, it.packageName) } + extra
            val unknownRanges = if (w.mode == "UNKNOWN") listOf(r) else
                union(all.filter { it.reason == "UNKNOWN" }.mapNotNull { overlap(TimeRange(it.start, it.end), r) })
            val distracted = union(all.filter { it.reason != "UNKNOWN" }.mapNotNull { overlap(TimeRange(it.start, it.end), r) })
            unknown += unknownRanges.sumOf { it.end - it.start }
            libraryUnknown += inLibrary(unknownRanges)
            distraction += distracted.sumOf { it.end - it.start }
            libraryDistraction += inLibrary(distracted)
            all.filter { it.reason != "UNKNOWN" }.groupBy { it.pkg ?: "Call audio" }.forEach { (name, parts) ->
                val ranges = union(parts.mapNotNull { overlap(TimeRange(it.start, it.end), r) })
                val amount = ranges.sumOf { it.end - it.start }
                breakdown[name] = (breakdown[name] ?: 0) + amount
                val inside = inLibrary(ranges)
                if (inside > 0) libraryBreakdown[name] = (libraryBreakdown[name] ?: 0) + inside
            }
        }
        val libraryMs = library.sumOf { it.end - it.start }
        val study = (base - distraction - unknown).coerceAtLeast(0)
        val libraryStudy = (libraryBase - libraryDistraction - libraryUnknown).coerceAtLeast(0)
        return FocusSummary(libraryMs, study, distraction, unknown, libraryStudy, libraryUnknown,
            (study - libraryStudy).coerceAtLeast(0), breakdown, library.isNotEmpty() || eligible.isNotEmpty(), libraryDistraction,
            union(breaks.filter { sessionId == null || it.sessionId == sessionId }.mapNotNull {
                overlap(TimeRange(it.start, it.end ?: now), range)
            }).sumOf { pause -> library.mapNotNull { overlap(pause, it) }.sumOf { it.end - it.start } }, libraryBreakdown)
    }
}
