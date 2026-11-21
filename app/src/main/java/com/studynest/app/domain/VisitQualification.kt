package com.studynest.app.domain

import com.studynest.app.data.LibraryVisit
import java.time.LocalDate
import java.time.ZoneId

/** Classification is persisted once on closure; neither midnight nor later settings changes alter it. */
object VisitQualification {
    fun finish(visit: LibraryVisit, cutoff: Long): LibraryVisit {
        if (visit.end != null) return visit
        val end = cutoff.coerceAtLeast(visit.start)
        return visit.copy(end = end, qualified = visit.minimumMinutes?.let {
            LibraryPresence.duration(visit.copy(end = end), end) >= it.toLong() * 60_000
        })
    }

    fun forDay(visits: List<LibraryVisit>, day: LocalDate, zone: ZoneId): List<LibraryVisit> {
        val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val until = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return visits.filter { it.qualified == true && it.end != null && LibraryPresence.ranges(it, it.end).any { r -> r.start < until && r.end > from } }
            .distinctBy { it.id }.sortedBy { it.start }
    }
}
