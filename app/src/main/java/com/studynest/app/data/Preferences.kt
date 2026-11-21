package com.studynest.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Config(
    val setup: Boolean = false,
    val wifi: Boolean = false,
    val geo: Boolean = false,
    val ssid: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius: Float = 150f,
    val tracking: Boolean = true,
    val vibrate: Boolean = true,
    val goalMinutes: Int = 300,
    val streakMinutes: Int = 120,
    val theme: String = "System",
    val quoteOffset: Int = 0,
    val geoVersion: Long = 0,
    val ssids: List<String> = emptyList(),
    val overrideMode: String = "NOTIFY",
    val appMonitoring: Boolean = false,
    val monitoredApps: Set<String> = emptySet(),
    val monitorCallAudio: Boolean = false,
    val minimumSessionMinutes: Int = 10
) : java.io.Serializable {
    fun networks(): List<String> = (if (ssids.isEmpty()) listOf(ssid) else ssids)
        .map { it.removeSurrounding("\"").trim() }.filter { it.isNotBlank() }.distinct()
    fun matchesSsid(value: String?) = value != null && value in networks()
    fun withNetworks(values: List<String>): Config {
        val cleaned = values.map { it.removeSurrounding("\"").trim() }.filter { it.isNotBlank() }.distinct()
        return copy(ssid = cleaned.firstOrNull().orEmpty(), ssids = cleaned)
    }
}

class Preferences(context: Context) {
    private val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    init {
        if (!p.contains("ssids")) {
            val legacy = p.getString("ssid", "").orEmpty()
            check(p.edit().putString("ssids", org.json.JSONArray(if (legacy.isBlank()) emptyList<String>() else listOf(legacy)).toString()).commit())
        }
    }

    private fun savedNetworks(): List<String> {
        val array = org.json.JSONArray(p.getString("ssids", "[]"))
        return List(array.length()) { array.getString(it) }
    }

    private fun read() =
        Config(
            p.getBoolean("setup", false),
            p.getBoolean("wifi", false),
            p.getBoolean("geo", false),
            p.getString("ssid", "")!!,
            p.getString("lat", null)?.toDoubleOrNull(),
            p.getString("lon", null)?.toDoubleOrNull(),
            p.getFloat("radius", 150f),
            p.getBoolean("tracking", true),
            p.getBoolean("vibrate", true),
            p.getInt("goal", 300),
            p.getInt("streak", 120),
            p.getString("theme", "System")!!,
            p.getInt("quote", 0),
            p.getLong("geoVersion", 0),
            savedNetworks(),
            p.getString("overrideMode", "NOTIFY")!!,
            p.getBoolean("appMonitoring", false),
            p.getStringSet("monitoredApps", emptySet())!!.toSet(),
            p.getBoolean("monitorCallAudio", false),
            p.getInt("minimumSessionMinutes", 10).coerceIn(1, 1440))

    private val mutable = MutableStateFlow(read())
    val flow = mutable.asStateFlow()
    val value
        get() = mutable.value

    @Synchronized
    fun save(c: Config) {
        require(c.minimumSessionMinutes in 1..1440) { "Minimum session duration must be 1–1440 minutes" }
        check(
            p.edit()
                .putBoolean("setup", c.setup)
                .putBoolean("wifi", c.wifi)
                .putBoolean("geo", c.geo)
                .putString("ssid", c.networks().firstOrNull().orEmpty())
                .putString("ssids", org.json.JSONArray(c.networks()).toString())
                .putString("overrideMode", c.overrideMode)
                .putBoolean("appMonitoring", c.appMonitoring)
                .putStringSet("monitoredApps", c.monitoredApps.toSet())
                .putBoolean("monitorCallAudio", c.monitorCallAudio)
                .putInt("minimumSessionMinutes", c.minimumSessionMinutes)
                .putString("lat", c.latitude?.toString())
                .putString("lon", c.longitude?.toString())
                .putFloat("radius", c.radius)
                .putBoolean("tracking", c.tracking)
                .putBoolean("vibrate", c.vibrate)
                .putInt("goal", c.goalMinutes)
                .putInt("streak", c.streakMinutes)
                .putString("theme", c.theme)
                .putInt("quote", c.quoteOffset)
                .putLong("geoVersion", c.geoVersion)
                .commit()) {
                "Settings could not be saved"
            }
        mutable.value = c
    }
}

