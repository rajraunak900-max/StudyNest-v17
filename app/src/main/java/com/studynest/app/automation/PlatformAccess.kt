package com.studynest.app.automation

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi

// WifiManager exposes this constant in the public SDK only from API 30.
internal const val UNKNOWN_WIFI_SSID = "<unknown ssid>"

/** SSID access is location-sensitive, including when Wi-Fi is not used for GPS. */
fun Context.backgroundLocationAllowed(): Boolean =
    allowed(Manifest.permission.ACCESS_FINE_LOCATION) &&
        (Build.VERSION.SDK_INT < 29 || allowed(Manifest.permission.ACCESS_BACKGROUND_LOCATION))

fun Context.locationEnabled(): Boolean {
    val manager = getSystemService(LocationManager::class.java)
    return if (Build.VERSION.SDK_INT >= 28) manager.isLocationEnabled
    else manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

fun Context.notificationsAllowed(): Boolean =
    (Build.VERSION.SDK_INT < 33 || allowed(Manifest.permission.POST_NOTIFICATIONS)) &&
        getSystemService(NotificationManager::class.java).areNotificationsEnabled()

internal fun geofencePendingFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or
    if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0

/** Unknown SSIDs are not departure evidence, including during Wi-Fi handoff. */
internal fun wifiPresence(networks: Collection<Boolean?>): Boolean? = when {
    networks.any { it == true } -> true
    networks.any { it == null } -> null
    else -> false
}

/** Keep the API 31 constructor in a guarded class so API 26 can load the service. */
internal fun wifiCallback(
    capabilities: (Network, NetworkCapabilities) -> Unit,
    lost: (Network) -> Unit
): ConnectivityManager.NetworkCallback =
    if (Build.VERSION.SDK_INT >= 31) LocationWifiCallback(capabilities, lost)
    else object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
            capabilities(network, caps)
        override fun onLost(network: Network) = lost(network)
    }

@RequiresApi(31)
private class LocationWifiCallback(
    private val capabilities: (Network, NetworkCapabilities) -> Unit,
    private val lost: (Network) -> Unit
) : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
    override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
        capabilities(network, caps)
    override fun onLost(network: Network) = lost(network)
}
