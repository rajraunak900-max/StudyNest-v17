package com.studynest.app.domain

import com.studynest.app.data.*
import java.time.*
import java.time.temporal.ChronoUnit

/** Time seated for a study visit: library presence plus manually timed study, counted once. */
object SittingMath {
    fun forRange(visits: List<LibraryVisit>, sessions: List<StudySession>, segments: List<Segment>,
        now: Long, range: TimeRange, segmentRange: (Segment) -> TimeRange): Long {
        val manualIds = sessions.filter { SessionEngine.isManualTrigger(it.trigger) }.mapTo(hashSetOf()) { it.id }
        val parts = buildList {
            visits.flatMap { LibraryPresence.ranges(it, now) }.forEach { add(it) }
            segments.filter { it.sessionId in manualIds }.forEach { add(segmentRange(it)) }
        }
        val clipped = parts.mapNotNull { part ->
            val start = maxOf(part.start, range.start); val end = minOf(part.end, range.end)
            if (end > start) TimeRange(start, end) else null
        }.sortedBy { it.start }
        var total = 0L; var start: Long? = null; var end = 0L
        for (part in clipped) {
            if (start == null) { start = part.start; end = part.end }
            else if (part.start <= end) end = maxOf(end, part.end)
            else { total += end - start; start = part.start; end = part.end }
        }
        if (start != null) total += end - start
        return total
    }
    fun forDay(visits: List<LibraryVisit>, sessions: List<StudySession>, segments: List<Segment>,
        day: LocalDate, zone: ZoneId, now: Long, segmentRange: (Segment) -> TimeRange): Long =
        forRange(visits, sessions, segments, now,
            TimeRange(day.atStartOfDay(zone).toInstant().toEpochMilli(), day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()), segmentRange)
}

/** Bucket once when Room emits data, then recalculate only open-record dates on the existing UI clock. */
class HistoryIndex(visits: List<LibraryVisit>, windows: List<FocusWindow>, periods: List<DistractionPeriod>,
    breaks: List<ManualBreak>, private val zone: ZoneId, private val now: Long) {
    private val visitsByDay = buckets(visits, { it.start }, { it.end ?: now })
    private val windowsByDay = buckets(windows, { it.start }, { it.end ?: now })
    private val periodsByDay = buckets(periods, { it.start }, { it.end })
    private val breaksByDay = buckets(breaks, { it.start }, { it.end ?: now })
    val days = visitsByDay.keys + windowsByDay.keys + breaksByDay.keys
    private val openVisits = visits.filter { it.end == null }
    private val openWindows = windows.filter { it.end == null }
    private val openBreaks = breaks.filter { it.end == null }
    val openStarts = openVisits.map { it.start } + openWindows.map { it.start } + openBreaks.map { it.start }
    val closed = days.filter { day -> openStarts.none { start -> day >= date(start) } }.associateWith { summary(it, now) }
    private fun date(time: Long) = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
    private fun <T> buckets(items: List<T>, start: (T) -> Long, end: (T) -> Long): Map<LocalDate,List<T>> {
        val result = mutableMapOf<LocalDate,MutableList<T>>()
        items.forEach { item ->
            val a=start(item);val b=end(item)
            if (b > a) {
                var day=date(a);val last=date(b-1)
                while(day <= last) {result.getOrPut(day){mutableListOf()}.add(item);day=day.plusDays(1)}
            }
        }
        return result
    }
    fun visitCoverage(day: LocalDate, time: Long): List<TimeRange> {
        val from=day.atStartOfDay(zone).toInstant().toEpochMilli()
        val until=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return (visitsByDay[day].orEmpty()+openVisits.filter {date(it.start)<=day}).distinctBy {it.id}
            .filter {it.minimumMinutes!=null}.flatMap {LibraryPresence.ranges(it,time)}
            .map {TimeRange(maxOf(from,it.start),minOf(until,it.end))}
            .filter {it.end>it.start}
    }
    fun summary(day: LocalDate, time: Long): FocusSummary {
        // An open interval may reach a new date before Room's next ordinary checkpoint.
        val visits=(visitsByDay[day].orEmpty()+openVisits.filter {date(it.start)<=day}).distinctBy {it.id}
        val windows=(windowsByDay[day].orEmpty()+openWindows.filter {date(it.start)<=day}).distinctBy {it.id}
        val breaks=(breaksByDay[day].orEmpty()+openBreaks.filter {date(it.start)<=day}).distinctBy {it.id}
        return FocusMath.summary(visits,windows,periodsByDay[day].orEmpty(),time,
            TimeRange(day.atStartOfDay(zone).toInstant().toEpochMilli(),day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()),breaks=breaks)
    }
    fun live(time: Long): Map<LocalDate,FocusSummary> {
        if(openStarts.isEmpty()) return closed
        val liveDays=mutableSetOf<LocalDate>()
        openStarts.forEach { start ->
            var day=date(start);val last=date(time)
            while(day<=last) {liveDays+=day;day=day.plusDays(1)}
        }
        return closed + liveDays.associateWith {summary(it,time)}
    }
}

data class HistoryDay(val actual: Long?, val library: Long, val tracked: Long, val distraction: Long, val libraryRecorded: Boolean = true, val sitting: Long = library) {
    companion object {
        fun from(summary: FocusSummary?, tracked: Long) = HistoryDay(
            if (summary?.unknownMs ?: 0L > 0 || (summary == null && tracked > 0)) null else summary?.studyMs ?: 0L,
            summary?.libraryMs ?: 0L,tracked,summary?.libraryDistractionMs ?: 0L, summary != null || tracked == 0L)
    }
}

data class HistoryPeriod(val actual: Long?,val library: Long,val average: Long?,val best: Pair<LocalDate,Long>?,val elapsedDays: Int, val libraryComplete: Boolean = true, val sitting: Long = library)
object HistoryMath {
    fun period(days: Map<LocalDate,HistoryDay>, first: LocalDate,last: LocalDate,today: LocalDate): HistoryPeriod {
        val until=minOf(last,today)
        if (until<first) return HistoryPeriod(0,0,0,null,0)
        val count=(ChronoUnit.DAYS.between(first,until)+1).toInt()
        val records=(0 until count).map {first.plusDays(it.toLong()) to (days[first.plusDays(it.toLong())] ?: HistoryDay(0,0,0,0))}
        val known=records.all {it.second.actual!=null}
        val actual=if(known) records.sumOf {it.second.actual!!} else null
        return HistoryPeriod(actual,records.sumOf {it.second.library},actual?.div(count),
            if(known) records.maxByOrNull {it.second.actual!!}?.takeIf {it.second.actual!!>0}?.let {it.first to it.second.actual!!} else null,count,records.all {it.second.libraryRecorded},records.sumOf {it.second.sitting})
    }
}

/** Closed tracking segments are indexed only on Room changes, not every UI clock update. */
class HistoryTrackingIndex(sessions: List<StudySession>, segments: List<Segment>, private val zone: ZoneId) {
    private val sessionsById=sessions.associateBy {it.id}
    private val open=segments.filter {it.end==null}
    private val byDay=mutableMapOf<LocalDate,MutableList<Segment>>()
    init {
        segments.filter {it.end!=null && it.end>it.start}.forEach {segment ->
            var day=Instant.ofEpochMilli(segment.start).atZone(zone).toLocalDate()
            val last=Instant.ofEpochMilli(segment.end!!-1).atZone(zone).toLocalDate()
            while(day<=last){byDay.getOrPut(day){mutableListOf()}.add(segment);day=day.plusDays(1)}
        }
    }
    fun periods(day:LocalDate,excluded:List<TimeRange>,range:(Segment)->TimeRange):List<StudyPeriod> {
        val from=day.atStartOfDay(zone).toInstant().toEpochMilli();val until=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val selected=byDay[day].orEmpty()+open.filter {val r=range(it);r.start<until && r.end>from}
        return StudyPeriods.forDay(selected.mapNotNull {sessionsById[it.sessionId]}.distinctBy {it.id},selected,day,zone,range,excluded)
    }
}

fun combineFocusDays(days:List<FocusSummary>):FocusSummary {
    fun merge(selector:(FocusSummary)->Map<String,Long>)=days.flatMap {selector(it).entries}
        .groupBy({it.key},{it.value}).mapValues {it.value.sum()}
    return FocusSummary(libraryMs=days.sumOf {it.libraryMs},studyMs=days.sumOf {it.studyMs},
        distractionMs=days.sumOf {it.distractionMs},unknownMs=days.sumOf {it.unknownMs},
        libraryStudyMs=days.sumOf {it.libraryStudyMs},libraryUnknownMs=days.sumOf {it.libraryUnknownMs},
        manualOutsideMs=days.sumOf {it.manualOutsideMs},breakdown=merge {it.breakdown},
        recorded=days.any {it.recorded},libraryDistractionMs=days.sumOf {it.libraryDistractionMs},
        breakMs=days.sumOf {it.breakMs},libraryBreakdown=merge {it.libraryBreakdown})
}

/** Keep the original additive tracked-time math while avoiding a scan of closed segments on each tick. */
class TrackingTotalsIndex(segments:List<Segment>,private val zone:ZoneId,range:(Segment)->TimeRange) {
    private val open=segments.filter {it.end==null}
    val completed=StudyMath.dailyTotals(segments.filter {it.end!=null}.map(range),zone)
    fun current(range:(Segment)->TimeRange):Map<LocalDate,Long> {
        if(open.isEmpty()) return completed
        val live=StudyMath.dailyTotals(open.map(range),zone)
        return completed.toMutableMap().apply {live.forEach {(day,value)->this[day]=(this[day] ?: 0L)+value}}
    }
}
