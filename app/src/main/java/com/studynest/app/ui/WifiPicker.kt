package com.studynest.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.studynest.app.automation.*

@Composable
fun WifiPicker(saved: List<String>, dismiss: () -> Unit, select: (String) -> Unit) {
    val context = LocalContext.current
    var navigationHint by remember { mutableStateOf<String?>(null) }
    fun open(destination: AccessDestination) { navigationHint = AccessNavigation.open(context, destination) }
    val discovery = remember { WifiDiscovery(context) }
    val state by discovery.logic.state.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { discovery.refresh() }
    DisposableEffect(discovery) { discovery.start(); onDispose { discovery.stop() } }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Choose Wi-Fi Network") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.message)
                navigationHint?.let { Text(it) }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(state.networks, key = { it.ssid }) { network ->
                        Column(Modifier.fillMaxWidth().clickable { select(network.ssid) }.padding(vertical = 12.dp)) {
                            Text(network.ssid, style = MaterialTheme.typography.titleSmall)
                            Text(if (network.ssid in saved) "Already saved" else "Tap to select", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (!context.allowed(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    TextButton(onClick = { permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }) { Text("Grant precise location") }
                }
                TextButton(onClick = { open(AccessDestination.APP) }) { Text("App permissions") }
                TextButton(onClick = { open(AccessDestination.WIFI) }) { Text("Wi-Fi settings") }
                if (!context.locationEnabled()) TextButton(onClick = { open(AccessDestination.LOCATION) }) { Text("Enable device Location") }
            }
        },
        confirmButton = { TextButton(enabled = !state.busy, onClick = { discovery.refresh() }) { Text("Refresh / Scan again") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Close") } }
    )
}
