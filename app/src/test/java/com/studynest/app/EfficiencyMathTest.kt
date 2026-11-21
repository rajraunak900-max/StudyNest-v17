package com.studynest.app

import com.studynest.app.domain.EfficiencyMath
import org.junit.Assert.*
import org.junit.Test

class EfficiencyMathTest {
    @Test fun manualStudyUsesSittingTimeIncludingDistraction() {
        assertEquals(66.666666, EfficiencyMath.calculate(60 * 60_000L, 90 * 60_000L)!!, 0.00001)
    }

    @Test fun mixedLibraryAndManualStudyUsesUnionSittingTotal() {
        // The caller supplies the deduplicated Sitting Time union (3 hours), not 4 additive hours.
        assertEquals(75.0, EfficiencyMath.calculate(135 * 60_000L, 180 * 60_000L)!!, 0.00001)
    }

    @Test fun unknownCoverageOrNoSittingMakesEfficiencyUnavailable() {
        assertNull(EfficiencyMath.calculate(30 * 60_000L, 60 * 60_000L, unknownMs = 1))
        assertNull(EfficiencyMath.calculate(0, 0))
    }

    @Test fun efficiencyDoesNotExceedOneHundredPercent() {
        assertEquals(100.0, EfficiencyMath.calculate(90, 60)!!, 0.0)
    }
}
