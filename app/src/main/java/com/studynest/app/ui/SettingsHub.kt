package com.studynest.app.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import com.studynest.app.R
import com.studynest.app.StudyNestApp
import com.studynest.app.automation.*
import com.studynest.app.data.Config

@Composable
fun SettingsScreen(app: StudyNestApp, saved: Config, initialPage: String = "main") {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var refresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val permissionSnapshot = remember(refresh, saved) {
        Triple(context.backgroundLocationAllowed() && context.locationEnabled(),
            context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted,
            context.notificationsAllowed())
    }
    val running by app.monitoring.collectAsStateWithLifecycle()
    val wifiStatus by app.automationStatus.collectAsStateWithLifecycle()
    val geoStatus by app.geofenceStatus.collectAsStateWithLifecycle()
    val soundStatus by app.soundStatus.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var draft by rememberSaveable(page) { mutableStateOf(saved) }
    fun commit(next: Config) {
        val old = app.preferences.value
        val detectionChanged = old.wifi != next.wifi || old.geo != next.geo || old.networks() != next.networks() || old.latitude != next.latitude || old.longitude != next.longitude || old.radius != next.radius
        app.preferences.save(next.copy(geoVersion = if (detectionChanged) System.currentTimeMillis() else old.geoVersion))
        app.runTask { Automation.apply(app); app.engine.recover() }
    }
    fun notificationSettings() { AccessNavigation.open(context, AccessDestination.NOTIFICATIONS)?.let { app.report(it) } }
    fun appSettings() { AccessNavigation.open(context, AccessDestination.APP)?.let { app.report(it) } }
    if (page == "monitoring") {
        BackHandler { page = "main" }
        AppMonitoringScreen(app, saved) { page = "main" }
        return
    }
    if (page == "wifi") {
        BackHandler { page = "main" }
        val currentSsid = remember(refresh, wifiStatus) { Automation.currentSsid(context) }
        WifiSettings(draft, currentSsid, { draft = it }, {
            commit(app.preferences.value.withNetworks(draft.networks()).copy(wifi = draft.wifi)); page = "main"
        }, { page = "permissions" }, { page = "main" })
        return
    }
    if (page == "goals") {
        BackHandler { page = "main" }
        var valid by remember { mutableStateOf(true) }
        Page("Goal & Streak", "Set your daily study goal, streak rules and session preferences. These help you stay consistent.", back = { page = "main" }) {
            GoalFields(draft, { valid = it }) { draft = it }
            Button(enabled = valid, onClick = {
                app.preferences.save(app.preferences.value.copy(goalMinutes = draft.goalMinutes,
                    streakMinutes = draft.streakMinutes, minimumSessionMinutes = draft.minimumSessionMinutes)); page = "main"
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("goals-save")) { Icon(Icons.Outlined.Save, null); Text("Save goals") }
        }
        return
    }
    if (page != "main") {
        BackHandler { page = "main" }
        val title = when(page) { "monitoring" -> "App Monitoring"; "wifi" -> "Library Wi-Fi"; "location" -> "Library location"; "sound" -> "Manual Sound Override"; "goals" -> "Your goals"; "permissions" -> "Permissions & System Access"; "appearance" -> "Appearance"; "battery" -> "Background operation"; else -> "About StudyNest" }
        Page(title, "Settings") {
            TextButton(onClick = { page = "main" }) { Icon(Icons.Outlined.ArrowBack, null); Text("Back to settings") }
            when(page) {
                "wifi", "location" -> {
                    Panel {
                        Toggle(if (page == "wifi") "Enable Wi-Fi detection" else "Enable location detection",
                            "Recognize your library automatically after saving.", if (page == "wifi") draft.wifi else draft.geo) {
                            draft = if (page == "wifi") draft.copy(wifi = it) else draft.copy(geo = it)
                        }
                        LocationFields(draft.copy(wifi = page == "wifi", geo = page == "location")) { next -> draft = next.copy(wifi = draft.wifi, geo = draft.geo) }
                        Button(enabled = if (page == "wifi") draft.networks().isNotEmpty() else validLocation(draft.copy(wifi = false, geo = true)), onClick = {
                            val current = app.preferences.value
                            commit(if (page == "wifi") current.withNetworks(draft.networks()).copy(wifi = draft.wifi) else current.copy(geo = draft.geo, latitude = draft.latitude, longitude = draft.longitude, radius = draft.radius))
                            page = "main"
                        }, modifier = Modifier.fillMaxWidth()) { Text("Save configuration") }
                    }
                    TextButton(onClick = { page = "permissions" }) { Text("Review required permissions") }
                    Text(if(page == "wifi") wifiStatus else geoStatus, style = MaterialTheme.typography.bodySmall)
                }
                "sound" -> {
                    Panel {
                        Text("When you switch Vibrate to Sound at the library:")
                        listOf("RESTORE" to "Auto restore after 5 minutes", "NOTIFY" to "Remind me once").forEach { (value, label) ->
                            Row(Modifier.fillMaxWidth().clickable { app.preferences.save(app.preferences.value.copy(overrideMode = value)); app.runTask { app.engine.tick() } }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(saved.overrideMode == value, { app.preferences.save(app.preferences.value.copy(overrideMode = value)); app.runTask { app.engine.tick() } })
                                Text(label, Modifier.weight(1f))
                            }
                        }
                        Text("Saved immediately. One reminder per manual change; leaving cancels pending restoration.", style = MaterialTheme.typography.bodySmall)
                    }
                    Panel {
                        SectionLabel("Sound status")
                        Text(soundStatus)
                        OutlinedButton(onClick = { app.runTask { app.engine.retrySound() } }) { Text("Apply vibration now") }
                        TextButton(onClick = { page = "permissions" }) { Text("Sound access settings") }
                    }
                }
                "permissions" -> PermissionPanel(saved.wifi || saved.geo, saved.vibrate)
                "appearance" -> Panel {
                    Text("System follows your phone. Light and Dark override its theme.")
                    listOf("System", "Light", "Dark").forEach { theme ->
                        Row(Modifier.fillMaxWidth().clickable { app.preferences.save(app.preferences.value.copy(theme = theme)) }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(saved.theme == theme, { app.preferences.save(app.preferences.value.copy(theme = theme)) })
                            Text(theme)
                        }
                    }
                }
                "battery" -> Panel {
                    val power = context.getSystemService(PowerManager::class.java)
                    Text(if (power.isIgnoringBatteryOptimizations(context.packageName)) "Battery optimization exemption is enabled." else "Android battery optimization is enabled. It can delay background events.")
                    Text("For reliable detection, allow background location and keep StudyNest out of sleeping/deep-sleeping apps in your phone settings. Force-stop blocks automation until you reopen the app.")
                    Text("Detection uses events. Brief, timeout-limited CPU wake protection covers arrival and departure checks; no continuous GPS or screen wake is used.", style = MaterialTheme.typography.bodySmall)
                    Text("Wi-Fi monitoring keeps a visible foreground notification while enabled because Android cannot reliably deliver arbitrary Wi-Fi arrival events to a stopped process. It stops when Wi-Fi monitoring is off and no session or sound deadline needs it.", style = MaterialTheme.typography.bodySmall)
                    Text("Sound restoration may briefly keep the CPU awake for its five-minute deadline. Android or the phone manufacturer can still delay events or stop the service; removing the app from Recents normally leaves monitoring running.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { AccessNavigation.open(context, AccessDestination.BATTERY)?.let { app.report(it) } }) { Text("Battery optimization settings") }
                    TextButton(onClick = { appSettings() }) { Text("App battery & system settings") }
                    TextButton(onClick = { Automation.start(context, "REFRESH") }) { Text("Restart monitoring") }
                }
                else -> Panel {
                    AndroidView(factory = { c -> ImageView(c).apply { setImageResource(R.mipmap.ic_launcher) } }, modifier = Modifier.size(72.dp))
                    Text("StudyNest", style = MaterialTheme.typography.headlineSmall)
                    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown" }
                    Text("Version $version")
                    Text("Study smarter. Stay focused.")
                    SectionLabel("Created by RAUNAK RAJ")
                    Text("Manual and automatic study timers, library Wi-Fi and location detection, and background tracking help you build a consistent study routine.", style = MaterialTheme.typography.bodyMedium)
                    Text("Track Sitting Time across library visits and Manual timer sessions, alongside Actual Study, distractions, breaks and efficiency. Monitor selected apps and calls, review session details, and explore your calendar and week/month reports.", style = MaterialTheme.typography.bodyMedium)
                    Text("Plan tasks, set daily goals and streak rules, choose a minimum session duration, and stay encouraged with motivational quotes. Sound, vibration, break reminders and notifications support your focus.", style = MaterialTheme.typography.bodyMedium)
                    Text("Personalize saved library networks, monitoring preferences and light/dark themes. Optional setup and permission guidance let you configure features at your own pace.", style = MaterialTheme.typography.bodyMedium)
                    Text("Your study records stay on your phone. No accounts, advertising or analytics. Uninstalling or clearing data removes local history.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        return
    }
    Page("Settings", "A quieter place to focus.") {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Text("StudyNest automation", style = MaterialTheme.typography.titleMedium)
            }
            Text(if (running) "Monitoring service running" else "Monitoring service not running", style = MaterialTheme.typography.labelLarge, color = if(running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            StatusLine("Library Wi-Fi", if(!saved.wifi) "Disabled" else if(saved.networks().isEmpty()) "Not configured" else if(!permissionSnapshot.first) "Permission required" else "Configured")
            StatusLine("Location", if(!saved.geo) "Disabled" else if(saved.latitude == null || saved.longitude == null) "Not configured" else if(!permissionSnapshot.first) "Permission required" else "Configured")
            StatusLine("Sound access", if(!saved.vibrate) "Disabled" else if(permissionSnapshot.second) "Granted" else "Permission required")
            StatusLine("Auto tracking", if(saved.tracking) "Enabled" else "Disabled")
            if ((saved.wifi || saved.geo) && !permissionSnapshot.first || saved.vibrate && !permissionSnapshot.second)
                TextButton(onClick = { page = "permissions" }) { Icon(Icons.Outlined.WarningAmber, null); Text("Review required access") }
        }
        SectionLabel("Library automation")
        Panel {
            SettingRow(Icons.Outlined.Wifi, "Library Wi-Fi", if(saved.networks().isEmpty()) "Not configured" else saved.networks().joinToString(", "), { page = "wifi" }, saved.wifi) { commit(saved.copy(wifi = it)) }
            HorizontalDivider()
            SettingRow(Icons.Outlined.LocationOn, "Library location", if(saved.latitude == null) "Not configured" else "Geofence • ${saved.radius.toInt()} m", { page = "location" }, saved.geo) { commit(saved.copy(geo = it)) }
            HorizontalDivider()
            SettingRow(Icons.Outlined.BatteryChargingFull, "Background operation", if(running) "Monitoring enabled • review battery access" else "Review monitoring and battery access", { page = "battery" })
        }
        SectionLabel("Sound & vibration")
        Panel {
            SettingRow(Icons.Outlined.Vibration, "Automatic vibration", "Vibrate at the library", checked = saved.vibrate, toggle = { commit(saved.copy(vibrate = it)) })
            HorizontalDivider()
            SettingRow(Icons.Outlined.VolumeUp, "Manual Sound Override", if(saved.overrideMode == "RESTORE") "Auto restore after 5 minutes" else "Remind me once", { page = "sound" })
        }
        SectionLabel("Study tracking")
        Panel {
            SettingRow(Icons.Outlined.Timer, "Automatic study tracking", "Track when library detection is active", checked = saved.tracking, toggle = { commit(saved.copy(tracking = it)) })
            HorizontalDivider()
            SettingRow(Icons.Outlined.Flag, "Goals & streak", "${saved.goalMinutes / 60}h ${saved.goalMinutes % 60}m daily goal", { page = "goals" })
            Text("Both enabled detection sources must report outside for 90 seconds to pause. Departure-paused automatic timers resume on return; manual timers wait for Resume.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionLabel("Study focus")
        Panel {
            SettingRow(Icons.Outlined.PhoneAndroid, "App Monitoring", if (saved.appMonitoring) "${saved.monitoredApps.size} selected apps • monitoring on" else "Off • select your distraction apps", { page = "monitoring" })
        }
        SectionLabel("Notifications & permissions")
        Panel {
            SettingRow(Icons.Outlined.Notifications, "Notifications", if(permissionSnapshot.third) "Allowed • timer and reminders" else "Permission required", { notificationSettings() })
            HorizontalDivider()
            SettingRow(Icons.Outlined.Security, "Permissions & System Access", "Manage feature access", { page = "permissions" })
        }
        SectionLabel("Appearance & about")
        Panel {
            SettingRow(Icons.Outlined.DarkMode, "Appearance", saved.theme, { page = "appearance" })
            HorizontalDivider()
            SettingRow(Icons.Outlined.Info, "About StudyNest", "Study smarter. Stay focused.", { page = "about" })
        }
    }
}

@Composable private fun StatusLine(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable private fun SettingRow(icon: ImageVector, title: String, summary: String, open: (() -> Unit)? = null, checked: Boolean? = null, toggle: ((Boolean) -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).then(if(open != null) Modifier.clickable(onClick = open) else Modifier), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (checked != null && toggle != null) Switch(checked, toggle)
        if (open != null) Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
