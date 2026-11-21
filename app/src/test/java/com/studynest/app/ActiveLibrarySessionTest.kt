package com.studynest.app

import com.studynest.app.data.LibraryVisit
import com.studynest.app.domain.VisitQualification
import com.studynest.app.ui.activeLibrarySessions
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ActiveLibrarySessionTest {
    private val day=LocalDate.of(2026,10,3)
    private val zone=ZoneOffset.UTC
    private val start=day.atTime(12,0).toInstant(zone).toEpochMilli()
    private val visit=LibraryVisit(id=7,start=start,elapsedStart=0,boot=1,minimumMinutes=10)
    @Test fun thresholdPresentationUsesExistingRowWithoutMutation() {
        assertTrue(activeLibrarySessions(listOf(visit),day,zone,start+599000).isEmpty())
        assertEquals(listOf(visit),activeLibrarySessions(listOf(visit),day,zone,start+600000))
        assertNull(visit.end);assertNull(visit.qualified)
    }
    @Test fun closureReplacesProgressWithTheSameCompletedIdentity() {
        val closed=VisitQualification.finish(visit,start+3600000)
        assertTrue(activeLibrarySessions(listOf(closed),day,zone,start+3600000).isEmpty())
        assertEquals(listOf(closed),VisitQualification.forDay(listOf(closed),day,zone))
        assertEquals(visit.id,closed.id)
    }
    @Test fun historicalUnrelatedAndFutureDatesDoNotShowCurrentVisit() {
        assertTrue(activeLibrarySessions(listOf(visit),day.minusDays(1),zone,start+3600000).isEmpty())
        assertTrue(activeLibrarySessions(listOf(visit),day.plusDays(1),zone,start+3600000).isEmpty())
    }
    @Test fun midnightProgressReferencesOneUnderlyingVisitOnBothOverlappingDates() {
        val v=visit.copy(start=day.atTime(23,30).toInstant(zone).toEpochMilli())
        val now=day.plusDays(1).atTime(0,30).toInstant(zone).toEpochMilli()
        assertEquals(listOf(v),activeLibrarySessions(listOf(v,v),day,zone,now))
        assertEquals(listOf(v),activeLibrarySessions(listOf(v),day.plusDays(1),zone,now))
    }
    @Test fun capturedCustomThresholdAndLegacyNullArePreserved() {
        assertTrue(activeLibrarySessions(listOf(visit.copy(minimumMinutes=46)),day,zone,start+45*60000).isEmpty())
        assertEquals(1,activeLibrarySessions(listOf(visit.copy(minimumMinutes=46)),day,zone,start+46*60000).size)
        assertTrue(activeLibrarySessions(listOf(visit.copy(minimumMinutes=null)),day,zone,start+3600000).isEmpty())
    }
}
