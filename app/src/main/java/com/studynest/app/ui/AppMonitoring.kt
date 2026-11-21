package com.studynest.app.ui

import android.content.Intent
import android.provider.Settings
import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.studynest.app.StudyNestApp
import com.studynest.app.automation.*
import com.studynest.app.data.Config
import kotlinx.coroutines.*

/** Drafts belong to this screen. Only an explicit save updates monitoring. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMonitoringScreen(app: StudyNestApp, saved: Config, back: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var generation by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var system by rememberSaveable { mutableStateOf(false) }
    var help by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(help) {
        // Keep the existing expansion, but bring this final item's explanation
        // into the viewport instead of opening invisibly below the bottom bar.
        if (help) listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
    }
    var draft by rememberSaveable { mutableStateOf(saved) }
    var saving by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val dirty = draft.appMonitoring != saved.appMonitoring ||
        draft.monitoredApps != saved.monitoredApps || draft.monitorCallAudio != saved.monitorCallAudio
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            generation++
            app.runTask { app.engine.syncFocus() }
        } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(generation) {
        withContext(Dispatchers.IO) { runCatching { AppCatalog.installed(context) } }
            .onSuccess { apps = it; loaded = true; failure = null }
            .onFailure { failure = "Apps could not be loaded. Tap Refresh to try again." }
    }
    val access = remember(generation) { UsageAccess.allowed(context) }
    val duplicateLabels = remember(apps) { apps.groupBy { it.label }.filterValues { it.size > 1 }.keys }
    val visible = remember(apps, query, system, draft.monitoredApps, saved.monitoredApps) {
        AppCatalog.search(apps.filter {
            !it.system || system || it.pkg in draft.monitoredApps || it.pkg in saved.monitoredApps
        }, query)
    }

    Scaffold(modifier = Modifier.imePadding(), contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text("App Monitoring") }, windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = { IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, "Back to settings") } })
        }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            Surface {
                Column(Modifier.fillMaxWidth()) {
                    HorizontalDivider()
                    Column(Modifier.padding(horizontal = StudyLayout.pagePadding, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${draft.monitoredApps.size} ${if (draft.monitoredApps.size == 1) "app" else "apps"} selected" + if (dirty) " • Unsaved changes" else "",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(enabled = dirty && !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            onClick = {
                                val pending = draft
                                saving = true
                                // The authorized save finishes even if this screen is left.
                                val operation = app.scope.async {
                                    runCatching { app.saveMonitoring(pending.appMonitoring, pending.monitoredApps, pending.monitorCallAudio) }
                                }
                                scope.launch {
                                    val result = operation.await()
                                    saving = false
                                    snackbar.showSnackbar(if (result.isSuccess) "App monitoring preferences saved."
                                        else "Could not save preferences. Please try again.")
                                }
                            }) { Text(if (saving) "Saving…" else "Save Changes") }
                    }
                }
            }
        }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().testTag("monitoring-list"), state = listState,
            contentPadding = PaddingValues(horizontal = StudyLayout.pagePadding, vertical = StudyLayout.sectionGap),
            verticalArrangement = Arrangement.spacedBy(StudyLayout.itemGap)) {
            item {
                Text("Select apps that should count as distractions during your study sessions.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Panel {
                    MonitoringControl("App Monitoring", "Pause actual study time for selected apps.",
                        draft.appMonitoring, !saving, "monitoring-master") { draft = draft.copy(appMonitoring = it) }
                    HorizontalDivider()
                    MonitoringControl("Pause during calls", "When supported by your phone.",
                        draft.monitorCallAudio, !saving, "monitoring-calls") { draft = draft.copy(monitorCallAudio = it) }
                }
            }
            item {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (access) Icons.Outlined.CheckCircle else Icons.Outlined.Security, null,
                            tint = if (access) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                        Column(Modifier.weight(1f)) {
                            SectionLabel("Usage Access")
                            Text(if (access) "Granted ✓" else "Usage Access required", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (!access) Text("Allow access to measure time spent in your selected apps.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(modifier = Modifier.fillMaxWidth().heightIn(min = StudyLayout.controlHeight), onClick = {
                        AccessNavigation.open(context, AccessDestination.USAGE)?.let { app.report(it) }
                    }) { Text(if (access) "Manage access" else "Grant Access") }
                    if (!access) RestrictedAccessHelp {
                        AccessNavigation.open(context, AccessDestination.APP)?.let { app.report(it) }
                    }
                }
            }
            item {
                OutlinedTextField(query, { query = it }, placeholder = { Text("Search apps") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") } },
                    modifier = Modifier.fillMaxWidth().testTag("app-search").semantics { contentDescription = "Search apps" },
                    shape = RoundedCornerShape(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).toggleable(system, role = Role.Checkbox, onValueChange = { system = it }),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(system, null); Text("Show system apps", style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { generation++ }) { Icon(Icons.Outlined.Refresh, "Refresh apps") }
                }
            }
            item { SectionLabel("Distraction apps") }
            if (!loaded && failure == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            failure?.let { message -> item { Text(message) } }
            if (loaded && visible.isEmpty()) item { Text("No matching apps.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(visible, key = { "app:${it.pkg}" }) { item ->
                val selected = item.pkg in draft.monitoredApps
                val icon = remember(item.pkg, generation) { runCatching { context.packageManager.getApplicationIcon(item.pkg) }.getOrNull() }
                Surface(shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(Modifier.fillMaxWidth().testTag("app-${item.pkg}")
                        .toggleable(selected, enabled = !saving, role = Role.Switch, onValueChange = { on ->
                            draft = draft.copy(monitoredApps = if (on) draft.monitoredApps + item.pkg else draft.monitoredApps - item.pkg)
                        }).heightIn(min = 72.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (icon != null) AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                            update = { it.setImageDrawable(icon) }, modifier = Modifier.size(40.dp))
                        else Icon(Icons.Outlined.Apps, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.titleSmall)
                            Text(if (selected) "Selected" else "Not selected", style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.label in duplicateLabels) Text(item.pkg, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(selected, null, enabled = !saving)
                    }
                }
            }
            val missing = draft.monitoredApps - apps.map { it.pkg }.toSet()
            if (loaded && missing.isNotEmpty()) item {
                Panel {
                    SectionLabel("Unavailable apps")
                    Text("Selections are kept if an app is reinstalled.", style = MaterialTheme.typography.bodySmall)
                    missing.sorted().forEach { pkg ->
                        Column {
                            Text(pkg, style = MaterialTheme.typography.bodySmall)
                            TextButton(enabled = !saving, onClick = { draft = draft.copy(monitoredApps = draft.monitoredApps - pkg) }) { Text("Remove selection") }
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { help = !help }, modifier = Modifier.fillMaxWidth().semantics {
                    stateDescription = if (help) "Expanded" else "Collapsed"
                }) {
                    Icon(Icons.Outlined.Info, null); Spacer(Modifier.width(8.dp))
                    Text("How App Monitoring works", Modifier.weight(1f))
                    Icon(if (help) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                }
                if (help) Panel {
                    Text("Sitting Time continues while selected apps pause Actual Study.")
                    Text("Time in selected distracting apps is recorded as Distraction, not Actual Study. Usage Access is required to detect these apps; measurements stay on this phone.")
                    Text("Turning monitoring off keeps your app selections saved and subtracts no app time.")
                    Text("Changes apply only when you save. Leaving this page discards unsaved edits.")
                    Text("Updates normally appear within about 15 seconds. Android may delay them when the phone is sleeping.")
                    Text("Calls are detected only when your phone exposes call audio. No phone or call-log permission is requested.")
                    Text("StudyNest measures app timing on this phone. It cannot read messages, passwords, photos or content inside apps.")
                }
            }
        }
    }
}

@Composable
private fun MonitoringControl(title: String, description: String, checked: Boolean, enabled: Boolean,
    tag: String, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, change, enabled = enabled,
            modifier = Modifier.testTag(tag).semantics { contentDescription = title })
    }
}
