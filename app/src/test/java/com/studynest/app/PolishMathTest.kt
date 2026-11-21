package com.studynest.app

import com.studynest.app.domain.*
import com.studynest.app.ui.chartInitialOffset
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class PolishMathTest {
    @Test fun currentDatePositionFitsEarlyMiddleAndEndOfScrollableWeekAndMonth() {
        for(count in listOf(7,28,29,30,31)) for(index in listOf(0,count/2,count-1)) {
            val width=100f;val viewport=300f;val maximum=count*100-300
            val offset=chartInitialOffset(index,width,viewport,maximum)
            assertTrue(index*width-offset>=0);assertTrue((index+1)*width-offset<=viewport)
        }
        assertEquals(0,chartInitialOffset(-1,100f,300f,2800))
        assertEquals(0,chartInitialOffset(6,40f,280f,0))
    }
    @Test fun weekAndMonthTotalsAveragesBestUseElapsedLocalDatesNotSelectedDate() {
        for(today in listOf(LocalDate.of(2026,10,5),LocalDate.of(2026,10,8),LocalDate.of(2026,10,11),
            LocalDate.of(2026,10,1),LocalDate.of(2026,10,15),LocalDate.of(2026,10,31))) for(month in listOf(false,true)) {
            val first=if(month)today.withDayOfMonth(1) else today.minusDays(today.dayOfWeek.value-1L)
            val last=if(month)first.withDayOfMonth(first.lengthOfMonth()) else first.plusDays(6)
            val elapsed=(java.time.temporal.ChronoUnit.DAYS.between(first,today)+1).toInt()
            val data=mapOf(first to HistoryDay(3600000,7200000,4000000,100000),today.plusDays(1) to HistoryDay(999999999,999999999,0,0))
            val result=HistoryMath.period(data,first,last,today)
            assertEquals(3600000L,result.actual);assertEquals(7200000L,result.library)
            assertEquals(3600000L/elapsed,result.average);assertEquals(elapsed,result.elapsedDays)
            assertEquals(first to 3600000L,result.best)
        }
    }
    @Test fun oneUnknownDayMakesExactActualAggregateUnknownWhileLibraryRemainsRecorded() {
        val first=LocalDate.of(2026,9,28);val last=first.plusDays(6)
        val d=mapOf(first to HistoryDay(7200000,8000000,7200000,0),first.plusDays(1) to HistoryDay.from(null,600000),
            last to HistoryDay(4119000,4245000,4245000,126000))
        val result=HistoryMath.period(d,first,last,last)
        assertNull(result.actual);assertNull(result.average);assertNull(result.best)
        assertEquals(12245000L,result.library);assertFalse(result.libraryComplete)
        val reliable=HistoryMath.period(d,last,last,last)
        assertEquals(4119000L,reliable.actual);assertEquals(4119000L,reliable.average)
        assertEquals(last to 4119000L,reliable.best)
    }
    @Test fun knownMultiDaySumAverageAndBestIgnoreFutureAndCountRestDays() {
        val first=LocalDate.of(2026,10,1);val last=first.withDayOfMonth(31);val today=first.plusDays(3)
        val d=mapOf(first to HistoryDay(3600000,4000000,0,0),first.plusDays(1) to HistoryDay(5400000,6000000,0,0),
            today to HistoryDay(1800000,2400000,0,0),today.plusDays(1) to HistoryDay(999999999,999999999,0,0))
        val result=HistoryMath.period(d,first,last,today)
        assertEquals(10800000L,result.actual);assertEquals(12400000L,result.library)
        assertEquals(2700000L,result.average);assertEquals(first.plusDays(1) to 5400000L,result.best)
        assertEquals(4,result.elapsedDays)
    }
}
