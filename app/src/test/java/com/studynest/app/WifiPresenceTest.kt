package com.studynest.app

import com.studynest.app.automation.wifiPresence
import org.junit.Assert.*
import org.junit.Test

class WifiPresenceTest {
    @Test fun lostLibraryWithAnUnreadableHandoffIsNotConfirmedDeparture() {
        assertEquals(true, wifiPresence(listOf(true, null)))
        assertNull(wifiPresence(listOf(null)))
        assertEquals(false, wifiPresence(listOf(false)))
        assertEquals(false, wifiPresence(emptyList()))
    }
    @Test fun knownLibraryWinsOverOtherConnections() {
        assertEquals(true, wifiPresence(listOf(false, true, null)))
        assertNull(wifiPresence(listOf(false, null)))
    }
}
