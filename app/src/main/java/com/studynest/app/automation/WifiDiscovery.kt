package com.studynest.app.automation

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow

/** Only used while the user has the picker open. No connection or background scans. */
data class NearbyNetwork(val ssid: String, val strength: Int)
data class WifiScanState(val networks: List<NearbyNetwork> = emptyList(), val message: String = "", val busy: Boolean = false)
interface WifiScanBackend {
    fun permission(): Boolean
    fun locationOn(): Boolean
    fun wifiOn(): Boolean
    fun results(): List<NearbyNetwork>
    fun requestScan(): Boolean
}
class WifiScanLogic(private val backend: WifiScanBackend) {
    val state = MutableStateFlow(WifiScanState())
    private fun blocked(): String? = when {
        !backend.permission() -> "Allow precise location to discover Wi-Fi names. Approximate location is not enough."
        !backend.locationOn() -> "Turn on device Location to discover Wi-Fi networks."
        !backend.wifiOn() -> "Turn on Wi-Fi, then tap Refresh. StudyNest will not connect to a network."
        else -> null
    }
    private fun read() = backend.results().filter { it.ssid.isNotBlank() && it.ssid != UNKNOWN_WIFI_SSID }
        .sortedByDescending { it.strength }.distinctBy { it.ssid }
    fun refresh() {
        try {
            val problem = blocked()
            if (problem != null) { state.value = WifiScanState(message = problem); return }
            val cached = read()
            val started = backend.requestScan()
            state.value = WifiScanState(cached, if (started) "Scanning… Any displayed results may be from an earlier scan."
                else "Android could not start a scan. It may be throttled; wait and Refresh. Displaying cached results if available.", started)
        } catch (_: SecurityException) {
            state.value = WifiScanState(message = "Wi-Fi access was denied. Allow precise location in app permissions, then Refresh.")
        } catch (e: Exception) {
            state.value = WifiScanState(message = "Wi-Fi scan unavailable. Check Wi-Fi and try again. ${e.javaClass.simpleName}")
        }
    }
    fun completed(updated: Boolean) {
        try {
            val problem = blocked()
            if (problem != null) { state.value = WifiScanState(message = problem); return }
            val results = read()
            state.value = WifiScanState(results, when {
                !updated -> "No fresh scan received. Results may be cached; wait before refreshing."
                results.isEmpty() -> "No named networks detected. Move closer to the router and Refresh."
                else -> "Choose a network to monitor. This will not connect your phone."
            })
        } catch (_: SecurityException) { state.value = WifiScanState(message = "Location permission was revoked. Grant precise location and Refresh.") }
        catch (e: Exception) { state.value = WifiScanState(message = "Could not read networks. Try Refresh. ${e.javaClass.simpleName}") }
    }
}

@SuppressLint("MissingPermission")
class WifiDiscovery(private val context: Context) {
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    val logic = WifiScanLogic(object : WifiScanBackend {
        override fun permission() = context.allowed(Manifest.permission.ACCESS_FINE_LOCATION)
        override fun locationOn() = context.locationEnabled()
        override fun wifiOn() = wifi?.isWifiEnabled == true
        @Suppress("DEPRECATION") override fun results() = wifi?.scanResults.orEmpty().map { NearbyNetwork(it.SSID, it.level) }
        @Suppress("DEPRECATION") override fun requestScan() = wifi?.startScan() == true
    })
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { logic.completed(false) }
    private var registered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return
            handler.removeCallbacks(timeout)
            logic.completed(intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false))
        }
    }
    fun start() {
        try {
            androidx.core.content.ContextCompat.registerReceiver(context, receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
            registered = true
            refresh()
        } catch (e: Exception) { logic.state.value = WifiScanState(message = "Wi-Fi discovery unavailable: ${e.javaClass.simpleName}. Reopen this picker to retry.") }
    }
    fun refresh() {
        handler.removeCallbacks(timeout)
        logic.refresh()
        if (logic.state.value.busy) handler.postDelayed(timeout, 15000)
    }
    fun stop() {
        handler.removeCallbacks(timeout)
        if (registered) runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }
}
