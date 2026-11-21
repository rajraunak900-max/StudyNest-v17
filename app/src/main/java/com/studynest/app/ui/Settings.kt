package com.studynest.app.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.studynest.app.StudyNestApp
import com.studynest.app.automation.*
import com.studynest.app.data.Config

internal fun validLocation(c: Config) =
    (!c.wifi || c.networks().isNotEmpty()) &&
        (!c.geo ||
            c.latitude?.let { it in -90.0..90.0 } == true &&
                c.longitude?.let { it in -180.0..180.0 } == true)

@Composable
fun Toggle(title: String, detail: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, change, modifier = Modifier.semantics { contentDescription = title })
        }
}

@android.annotation.SuppressLint("MissingPermission")
@Composable
fun LocationFields(c: Config, onboarding: Boolean = false, change: (Config) -> Unit) {
    val context = LocalContext.current
    var lat by rememberSaveable { mutableStateOf(c.latitude?.toString() ?: "") }
    var lon by rememberSaveable { mutableStateOf(c.longitude?.toString() ?: "") }
    var locating by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }
    val cancellation = remember { CancellationTokenSource() }
    DisposableEffect(Unit) { onDispose { cancellation.cancel() } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (c.wifi) {
            var picker by remember { mutableStateOf(false) }
            var editing by remember { mutableIntStateOf(-1) }
            c.networks().forEachIndexed { index, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f))
                    TextButton(onClick = { editing = index; picker = true }) { Text("Change") }
                    TextButton(onClick = { change(c.withNetworks(c.networks().filterIndexed { i, _ -> i != index })) }) { Text("Remove") }
                }
            }
            OutlinedButton(onClick = { editing = -1; picker = true }, modifier = Modifier.fillMaxWidth()) { Text("Choose Wi-Fi Network") }
            OutlinedButton(onClick = {
                Automation.currentSsid(context)?.let { change(c.withNetworks(c.networks() + it)) }
                    ?: run { hint = "Connect to Wi-Fi and allow precise location first." }
            }) { Text("Use connected Wi-Fi") }
            if (picker) WifiPicker(c.networks(), { picker = false }) { name ->
                val names = c.networks().toMutableList()
                if (editing in names.indices) names[editing] = name else names.add(name)
                change(c.withNetworks(names)); picker = false
            }
            Text((if (onboarding) "Networks are saved when you enter StudyNest. " else "Save settings after selecting networks. ") + "Any saved network counts as your library. No passwords are stored and StudyNest never connects for you.", style = MaterialTheme.typography.bodySmall)
        }
        if (c.geo) {
            OutlinedTextField(
                lat,
                {
                    lat = it
                    change(c.copy(latitude = it.toDoubleOrNull()))
                },
                label = { Text("Library latitude") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true)
            OutlinedTextField(
                lon,
                {
                    lon = it
                    change(c.copy(longitude = it.toDoubleOrNull()))
                },
                label = { Text("Library longitude") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true)
            OutlinedButton(
                enabled = !locating,
                onClick = {
                    if (!context.allowed(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        !context.locationEnabled())
                        hint = "Enable Location and precise location permission first."
                    else {
                        locating = true
                        try {
                            val request =
                                CurrentLocationRequest.Builder()
                                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                                    .setMaxUpdateAgeMillis(30000)
                                    .setDurationMillis(15000)
                                    .build()
                            LocationServices.getFusedLocationProviderClient(context)
                                .getCurrentLocation(request, cancellation.token)
                                .addOnSuccessListener { location ->
                                    locating = false
                                    if (location == null)
                                        hint =
                                            "No location fix. Try near a window, or enter coordinates."
                                    else {
                                        lat = location.latitude.toString()
                                        lon = location.longitude.toString()
                                        change(
                                            c.copy(
                                                latitude = location.latitude,
                                                longitude = location.longitude))
                                        hint =
                                            "Location captured • accuracy about ${location.accuracy.toInt()} m"
                                    }
                                }
                                .addOnFailureListener {
                                    locating = false
                                    hint = "Location could not be read: ${it.message}"
                                }
                        } catch (e: SecurityException) {
                            locating = false
                            hint = "Precise location permission is required."
                        }
                    }
                }) {
                    Text(if (locating) "Finding location…" else "Use my current location")
                }
            Text("Library radius: ${c.radius.toInt()} metres")
            Slider(c.radius, { change(c.copy(radius = it)) }, valueRange = 100f..1000f, steps = 17)
            Text(
                "Use 150–300 m for typical libraries. Entry requires a brief dwell; Android may deliver events several minutes late.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.bodySmall)
    }
}

