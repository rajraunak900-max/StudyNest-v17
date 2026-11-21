package com.studynest.app.ui

import com.studynest.app.data.LibraryVisit
import com.studynest.app.domain.LibraryPresence
import java.time.LocalDate
import java.time.ZoneId

/** Read-only presentation of existing open rows; final qualification is still persisted on departure. */
internal fun activeLibrarySessions(visits:List<LibraryVisit>,day:LocalDate,zone:ZoneId,now:Long):List<LibraryVisit> {
    val from=day.atStartOfDay(zone).toInstant().toEpochMilli()
    val until=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return visits.filter { visit ->
        visit.end==null && visit.minimumMinutes?.let { LibraryPresence.duration(visit,now)>=it.toLong()*60_000 }==true &&
            LibraryPresence.ranges(visit,now).any { it.start<until && it.end>from }
    }.distinctBy{it.id}.sortedBy{it.start}
}
