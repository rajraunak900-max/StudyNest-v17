package com.studynest.app.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.studynest.app.automation.*

/** Shared by onboarding and Settings. Status is read on resume and permission result. */
@Composable
fun PermissionPanel(detection: Boolean, vibrate: Boolean, showIntro: Boolean = true) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var refresh by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf<String?>(null) }
    var continueToBackground by rememberSaveable { mutableStateOf(false) }
    var explainBackground by rememberSaveable { mutableStateOf(false) }
    var backgroundRequested by rememberSaveable { mutableStateOf(false) }
    var backgroundDenied by rememberSaveable { mutableStateOf(false) }
    fun open(destination: AccessDestination) { hint = AccessNavigation.open(context, destination) }
    fun denied(permission: String, granted: Boolean) {
        refresh++
        if (!granted && (context as? Activity)?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } == true)
            open(AccessDestination.APP)
    }
    val location = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val granted = it[Manifest.permission.ACCESS_FINE_LOCATION] == true
        denied(Manifest.permission.ACCESS_FINE_LOCATION, granted)
        if (granted && continueToBackground && Build.VERSION.SDK_INT >= 29 && !context.backgroundLocationAllowed())
            explainBackground = true
        continueToBackground = false
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (Build.VERSION.SDK_INT >= 33) denied(Manifest.permission.POST_NOTIFICATIONS, it)
    }
    val background = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
        backgroundDenied = !it
    }
    if (explainBackground) AlertDialog(onDismissRequest = { explainBackground = false },
        title = { Text("Library detection with the screen off") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Precise location is allowed. This second step enables library detection while StudyNest is closed or your phone is locked.")
                val option = if (Build.VERSION.SDK_INT >= 30) context.packageManager.backgroundPermissionOptionLabel.toString().takeIf { it.isNotBlank() } ?: "Allow all the time" else "Allow all the time"
                Text("On the Android page, choose $option and keep precise location enabled. This is optional for manual study.")
                Text("If Android does not open the Location page, use App info → Permissions → Location → $option.")
                TextButton(onClick = { explainBackground = false; open(AccessDestination.APP) }) { Text("Open app settings") }
            }
        },
        confirmButton = { TextButton(onClick = {
            explainBackground = false
            if (Build.VERSION.SDK_INT >= 29) {
                val activity = context as? Activity
                if (backgroundRequested && activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                    open(AccessDestination.APP)
                else {
                    backgroundRequested = true
                    // Foreground permission has already been granted. The public
                    // runtime request lets Android open its own background flow;
                    // API 30+ requires the separate Location settings approval.
                    background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }) { Text("Continue to Location access") } },
        dismissButton = { TextButton(onClick = { explainBackground = false }) { Text("Set up later") } })
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Explicit reads keyed by refresh; no polling or separate verify button.
    val state = remember(refresh) {
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(UsageAccess.allowed(context), context.allowed(Manifest.permission.ACCESS_FINE_LOCATION),
            context.backgroundLocationAllowed(), context.locationEnabled(), manager.isNotificationPolicyAccessGranted,
            context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
            context.notificationsAllowed() && manager.getNotificationChannel(Notifications.SERVICE_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)
    }
    Column(Modifier.fillMaxWidth().testTag("permissions-list"), verticalArrangement = Arrangement.spacedBy(StudyLayout.sectionGap)) {
        if (showIntro) Text("Manage the permissions StudyNest uses for automatic study tracking and library features.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${state.filterIndexed { i, _ -> i != 2 || Build.VERSION.SDK_INT >= 29 }.count { it }} of ${if (Build.VERSION.SDK_INT >= 29) 7 else 6} access settings configured", style = MaterialTheme.typography.labelLarge)
        if (showIntro) Text("Manual timing, tasks and history work without library access. You can set up any feature later.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionLabel("Study tracking")
        AccessCard("usage", Icons.Outlined.PhoneAndroid, "Usage Access",
            "Required only to measure selected distracting apps. StudyNest reads app timing, never app content.",
            state[0], "Select StudyNest and allow usage access if your phone opens an app list.") { open(AccessDestination.USAGE) }
        if (!state[0]) RestrictedAccessHelp { open(AccessDestination.APP) }
        SectionLabel("Library automation")
        AccessCard("location", Icons.Outlined.LocationOn, "Precise location",
            if (detection) "Required for your library detection. Android also protects Wi-Fi names with location access."
            else "Required only for library Wi-Fi or location detection; optional for manual study.",
            state[1], "Choose precise location. If app settings opens, select Permissions → Location.") {
            if (state[1]) open(AccessDestination.APP)
            else {
                continueToBackground = detection && Build.VERSION.SDK_INT >= 29
                location.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }
        if (Build.VERSION.SDK_INT >= 29) AccessCard("background", Icons.Outlined.MyLocation, "Background location",
            "Required for library detection while StudyNest is closed or the screen is locked.", state[2],
            "Step 2: choose Allow all the time after precise location. Grant Access guides both steps separately; manual study works if you skip.") {
            if (!state[1]) {
                continueToBackground = true
                location.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            } else if (!state[2]) explainBackground = true
            else open(AccessDestination.APP)
        }
        else Text("Background detection uses the same location permission on this Android version.", style = MaterialTheme.typography.bodySmall)
        AccessCard("device-location", Icons.Outlined.Explore, "Device Location",
            "Required for Android to provide library Wi-Fi names and location events.", state[3], "Turn on your phone's Location setting.") {
            open(AccessDestination.LOCATION)
        }
        SectionLabel("Sound automation")
        AccessCard("sound", Icons.Outlined.Vibration, "Do Not Disturb access",
            if (vibrate) "Required for automatic ringer changes. Media volume stays unchanged."
            else "Optional while automatic vibration is off. Enables library ringer changes.",
            state[4], "Select StudyNest in this access list and allow access. Other phone rules can still silence notifications.") {
            open(AccessDestination.SOUND)
        }
        SectionLabel("Background reliability")
        AccessCard("battery", Icons.Outlined.BatteryChargingFull, "Battery optimization",
            "Recommended for library monitoring with the screen off. This is optional and can use more battery.",
            state[5], "Select All apps, find StudyNest and choose Don't optimize. Your phone may also have a sleeping-app list.",
            granted = "Exemption enabled ✓", missing = "Recommended") { open(AccessDestination.BATTERY) }
        SectionLabel("Notifications")
        AccessCard("notifications", Icons.Outlined.Notifications, "Notifications",
            "Recommended for the live study notification and its Pause/Finish controls. Timers still work when skipped.",
            state[6], "Allow StudyNest notifications and its study timer channel.") {
            if (Build.VERSION.SDK_INT >= 33 && !context.allowed(Manifest.permission.POST_NOTIFICATIONS))
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            else open(AccessDestination.NOTIFICATIONS)
        }
        if (backgroundDenied && !state[2]) Text("Background location is still off. You can set it up later, or open StudyNest app settings → Permissions → Location → Allow all the time.",
            style = MaterialTheme.typography.bodySmall)
        hint?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AccessCard(tag: String, icon: ImageVector, title: String, description: String,
    configured: Boolean, instruction: String, granted: String = "Granted ✓", missing: String = "Action required for this feature",
    action: () -> Unit) {
    Panel(Modifier.testTag("access-$tag")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                SectionLabel(title)
                Text(if (configured) granted else missing, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(description, style = MaterialTheme.typography.bodyMedium)
        if (!configured) Text(instruction, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = StudyLayout.controlHeight)
            .testTag("grant-$tag")) { Text(if (configured) "Manage" else if (tag == "battery" || tag == "device-location" || tag == "sound") "Open Settings" else "Grant Access") }
    }
}
