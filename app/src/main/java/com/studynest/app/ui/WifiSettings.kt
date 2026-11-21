package com.studynest.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.studynest.app.automation.Automation
import com.studynest.app.data.Config

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WifiSettings(c: Config, currentSsid: String?, change: (Config) -> Unit,
    save: () -> Unit, permissions: () -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    var picker by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    Page("Library Wi-Fi", "Select the networks at your library. Connecting to any saved network lets StudyNest recognize that you are there.", back = back) {
        Panel(Modifier.testTag("wifi-enable-card")) {
            Toggle("Enable Wi-Fi detection", "Detect your library automatically when connected to saved networks.", c.wifi) { change(c.copy(wifi = it)) }
        }
        Panel(Modifier.testTag("saved-wifi-networks")) {
            SettingHeading(Icons.Outlined.Router, "Saved Library Networks", "${c.networks().size} saved networks • No fixed network limit")
            if (c.networks().isEmpty()) Text("Add your library’s Wi-Fi network to get started.")
            c.networks().forEach { ssid ->
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().testTag("wifi-network-$ssid")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingHeading(Icons.Outlined.Wifi, ssid, if (ssid == currentSsid) "Currently connected" else "Saved library network")
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { editing = ssid; picker = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(6.dp)); Text("Change")
                            }
                            TextButton(onClick = { change(c.withNetworks(c.networks().filter { it != ssid })) },
                                modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Icon(Icons.Outlined.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Remove")
                            }
                        }
                    }
                }
            }
            Button(onClick = { editing = null; picker = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Wi-Fi Network")
            }
            OutlinedButton(onClick = {
                val ssid = Automation.currentSsid(context)
                if (ssid != null) {
                    change(c.withNetworks(c.networks() + ssid)); message = if (ssid in c.networks()) "This network is already saved." else "Network added. Save configuration to apply."
                } else message = "Current Wi-Fi is unavailable. Connect to Wi-Fi and review location and Wi-Fi access."
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Wifi, null); Spacer(Modifier.width(8.dp)); Text("Use Current Wi-Fi")
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            InformationStrip("Your privacy is safe\n• No Wi-Fi passwords are stored.\n• StudyNest does not connect to networks on your behalf.\n• Any saved network can count as your library.\n• There is no fixed limit on saved networks.")
        }
        Button(enabled = !c.wifi || c.networks().isNotEmpty(), onClick = save,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("wifi-save")) {
            Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(8.dp)); Text("Save configuration")
        }
        Text("Edits apply only after saving. Back discards unsaved changes.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = permissions) { Text("Review required permissions") }
    }
    if (picker) WifiPicker(c.networks(), { picker = false }) { selected ->
        val old = editing
        change(c.withNetworks(if (old == null) c.networks() + selected else c.networks().map { if (it == old) selected else it }))
        picker = false; editing = null; message = "Save configuration to apply your changes."
    }
}
