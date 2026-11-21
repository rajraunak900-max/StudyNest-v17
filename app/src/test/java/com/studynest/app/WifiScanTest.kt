package com.studynest.app

import com.studynest.app.automation.*
import com.studynest.app.data.Config
import org.junit.Test
import org.junit.Assert.*

class WifiScanTest {
    private class Backend : WifiScanBackend {
        var permitted = true; var location = true; var enabled = true; var success = true
        var failure = false; var calls = 0
        var networks = listOf(NearbyNetwork("Library", -50), NearbyNetwork("Library", -80), NearbyNetwork("Campus", -60), NearbyNetwork("", -30))
        override fun permission() = permitted
        override fun locationOn() = location
        override fun wifiOn() = enabled
        override fun results(): List<NearbyNetwork> { if (failure) throw SecurityException(); return networks }
        override fun requestScan(): Boolean { calls++; return success }
    }
    @Test fun freshResultsDeduplicateAndSelectExactName() {
        val backend = Backend(); val logic = WifiScanLogic(backend)
        logic.refresh(); assertTrue(logic.state.value.busy)
        logic.completed(true)
        assertEquals(listOf("Library", "Campus"), logic.state.value.networks.map { it.ssid })
        val c = Config().withNetworks(listOf(logic.state.value.networks[0].ssid))
        assertTrue(c.matchesSsid("Library")); assertFalse(c.matchesSsid("library"))
    }
    @Test fun deniedPermissionDoesNotRequestScan() {
        val b = Backend(); b.permitted = false; val logic = WifiScanLogic(b); logic.refresh()
        assertEquals(0,b.calls); assertTrue(logic.state.value.message.contains("precise"))
    }
    @Test fun wifiOffAndLocationOffExplainFailure() {
        val b = Backend(); b.enabled = false; val logic = WifiScanLogic(b); logic.refresh()
        assertTrue(logic.state.value.message.contains("Turn on Wi-Fi")); assertEquals(0,b.calls)
        b.location = false; logic.refresh(); assertTrue(logic.state.value.message.contains("device Location"))
    }
    @Test fun throttledScanShowsCachedResultsClearly() {
        val b = Backend(); b.success = false; val logic = WifiScanLogic(b); logic.refresh()
        assertFalse(logic.state.value.busy); assertEquals(2,logic.state.value.networks.size)
        assertTrue(logic.state.value.message.contains("throttled"))
    }
    @Test fun permissionRevokedDuringScanIsSafe() {
        val b = Backend(); val logic = WifiScanLogic(b); logic.refresh(); b.failure = true; logic.completed(true)
        assertFalse(logic.state.value.busy); assertTrue(logic.state.value.networks.isEmpty())
        assertTrue(logic.state.value.message.contains("revoked"))
    }
    @Test fun emptyAndTimedOutScansHaveUsefulMessages() {
        val b = Backend(); b.networks = emptyList(); val logic = WifiScanLogic(b); logic.completed(true)
        assertTrue(logic.state.value.message.contains("No named networks")); logic.refresh(); logic.completed(false)
        assertFalse(logic.state.value.busy); assertTrue(logic.state.value.message.contains("cached"))
    }
}
