package com.studynest.app.automation

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.media.AudioManager
import android.net.*
import android.net.wifi.*
import android.os.IBinder
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import androidx.room.withTransaction
import com.studynest.app.domain.StudyMath
import com.studynest.app.domain.SessionEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.android.gms.common.*
import com.google.android.gms.location.*
import com.studynest.app.MainActivity
import com.studynest.app.R
import com.studynest.app.StudyNestApp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

fun Context.allowed(p: String) =
    ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

object Notifications {
    const val SERVICE_CHANNEL = "study_v2_no_badge"
    const val SOUND_CHANNEL = "sound_v1_no_badge"
    fun create(c: Context) {
        val manager = c.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel("study") != null) {
            manager.cancel(1)
            manager.deleteNotificationChannel("study")
        }
        manager.createNotificationChannel(NotificationChannel(SERVICE_CHANNEL,
            "Study timer & Wi-Fi detection", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        manager.createNotificationChannel(NotificationChannel(SOUND_CHANNEL,
            "One-time sound reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setShowBadge(false); setSound(null, null); enableVibration(false)
            })
    }
    fun reminder(c: Context) {
        if (!c.notificationsAllowed()) {
            (c.applicationContext as StudyNestApp).report("Sound override detected. Notifications are disabled, so the one-time reminder cannot be shown.")
            return
        }
        val manager = c.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled() || manager.getNotificationChannel(SOUND_CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) {
            (c.applicationContext as StudyNestApp).report("Sound override detected, but Android has disabled sound-reminder notifications. Open StudyNest notification settings to enable them.")
            return
        }
        val open = PendingIntent.getActivity(c, 10, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, SOUND_CHANNEL)
            .setSmallIcon(R.drawable.ic_study_notification).setContentTitle("StudyNest")
            .setContentText("Your phone is still in sound mode.").setContentIntent(open)
            .setAutoCancel(true).setOnlyAlertOnce(true).setSilent(true).setNumber(0)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE).setTimeoutAfter(300000).build()
        c.getSystemService(NotificationManager::class.java).notify(2, n)
    }
    /** Separate one-time event; reuse the existing silent, badge-free reminder channel. */
    fun breakReminder(c: Context) {
        val manager = c.getSystemService(NotificationManager::class.java)
        if (!c.notificationsAllowed() || !manager.areNotificationsEnabled() ||
            manager.getNotificationChannel(SOUND_CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) {
            (c.applicationContext as? StudyNestApp)?.report("Break reached 45 minutes, but Android has disabled reminder notifications.")
            return
        }
        val open = PendingIntent.getActivity(c, 11, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(3, NotificationCompat.Builder(c, SOUND_CHANNEL)
            .setSmallIcon(R.drawable.ic_study_notification).setContentTitle("Break Reminder")
            .setContentText("You've been on a break for 45 minutes. Time to get back to studying.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("You've been on a break for 45 minutes. Time to get back to studying."))
            .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true).setSilent(true)
            .setNumber(0).setBadgeIconType(NotificationCompat.BADGE_ICON_NONE).build())
    }
    fun cancelReminder(c: Context) {
        c.getSystemService(NotificationManager::class.java).cancel(2)
        (c.applicationContext as? StudyNestApp)?.cancelSoundDeadline()
        WorkManager.getInstance(c).cancelUniqueWork("sound-override")
    }

    private val refreshLock = Mutex()

    suspend fun current(app: StudyNestApp): Notification = app.db.withTransaction {
        val active = app.db.dao().active()
        val now = System.currentTimeMillis()
        val anchor = android.os.SystemClock.elapsedRealtime()
        val dao = app.db.focusDao()
        val visit = dao.openVisit()
        val from = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val timing = NotificationTiming.snapshot(active, visit,
            dao.visitsBetween(from, now), dao.windowsBetween(from, now), dao.periodsBetween(from, now), now, anchor,
            sessions = app.db.dao().sessionsBetween(from,now), segments = app.db.dao().segmentsBetween(from,now),
            segmentRange = app.engine::range)
        // Rebase only at existing reconciliation/state boundaries. Android advances
        // RemoteViews chronometers without app work, database reads or usage queries.
        val text = "Sitting: ${StudyMath.clock(timing.sittingMs)} • Actual: ${timing.studyMs?.let { StudyMath.clock(it) } ?: "Unavailable"}"
        notification(app, text, active != null, active?.paused == true, timing = timing)
    }

    suspend fun refresh(app: StudyNestApp) = refreshLock.withLock {
        if (!app.monitoring.value || !app.notificationsAllowed()) return@withLock
        val notification = current(app)
        // Check again after database suspension so a stopped service cannot be resurrected.
        if (app.monitoring.value) app.getSystemService(NotificationManager::class.java).notify(1, notification)
    }

    fun notification(c: Context, text: String, active: Boolean, paused: Boolean, elapsedMs: Long = 0L, timing: NotificationTiming? = null): Notification {
        val open =
            PendingIntent.getActivity(
                c,
                0,
                Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b =
            NotificationCompat.Builder(c, SERVICE_CHANNEL)
                .setSmallIcon(R.drawable.ic_study_notification)
                .setContentTitle(if (timing == null) "StudyNest" else "StudyNest • ${timing.state}")
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(true)
                .setUsesChronometer(timing == null && active && !paused)
                .setShowWhen(timing == null && active && !paused)
                .setWhen(if (active && !paused) System.currentTimeMillis() - elapsedMs.coerceAtLeast(0) else 0)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE).setNumber(0).setOnlyAlertOnce(true)
                .setSilent(true)
        if (timing != null) {
            b.setSubText(timing.state)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(timing.views(c, false))
                .setCustomBigContentView(timing.views(c, true))
                .addExtras(timing.extras())
        }
        if (active) {
            fun action(name: String, id: Int) =
                PendingIntent.getForegroundService(
                    c,
                    id,
                    Intent(c, StudyService::class.java).setAction(name),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            b.addAction(0, if (paused) "Resume" else "Pause", action("PAUSE", 1))
                .addAction(0, "Finish", action("FINISH", 2))
        }
        return b.build()
    }
}

object Automation {
    fun start(c: Context, action: String? = null) {
        try {
            ContextCompat.startForegroundService(
                c, Intent(c, StudyService::class.java).setAction(action))
        } catch (e: Exception) {
            (c.applicationContext as StudyNestApp).report(
                "Android prevented monitoring. Open StudyNest to restart it. ${e.javaClass.simpleName}")
        }
    }

    suspend fun apply(c: Context) {
        val app = c.applicationContext as StudyNestApp
        app.engine.reconfigure()
        if (!app.preferences.value.wifi) app.automationStatus.value = "Wi-Fi off"
        registerGeofence(c)
        if (app.engine.needsService()) start(c, "REFRESH")
        else c.stopService(Intent(c, StudyService::class.java))
    }

    private fun pending(c: Context) =
        PendingIntent.getBroadcast(
            c,
            30,
            Intent(c, GeofenceReceiver::class.java),
            geofencePendingFlags())

    @SuppressLint("MissingPermission")
    suspend fun registerGeofence(c: Context) {
        val app = c.applicationContext as StudyNestApp
        val cfg = app.preferences.value
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(c) !=
            ConnectionResult.SUCCESS) {
            app.geofenceStatus.value =
                if (cfg.geo)
                    "Location automation needs Google Play services. Manual and Wi-Fi modes still work."
                else "Location off"
            return
        }
        try {
            val client = LocationServices.getGeofencingClient(c)
            client.removeGeofences(pending(c)).await()
            if (!cfg.geo) {
                app.geofenceStatus.value = "Location off"
                return
            }
            if (!c.backgroundLocationAllowed() ||
                !c.locationEnabled()) {
                app.geofenceStatus.value =
                    "Location inactive: enable Location and grant Precise / Allow all the time."
                app.events.signal("Location", false)
                return
            }
            val lat = cfg.latitude
            val lon = cfg.longitude
            if (lat == null || lon == null) {
                app.geofenceStatus.value = "Save your library location first."
                return
            }
            val fence =
                Geofence.Builder()
                    .setRequestId("library-${cfg.geoVersion}")
                    .setCircularRegion(lat, lon, cfg.radius)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setLoiteringDelay(30000)
                    .setNotificationResponsiveness(60000)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
            client
                .addGeofences(
                    GeofencingRequest.Builder()
                        .setInitialTrigger(
                            GeofencingRequest.INITIAL_TRIGGER_DWELL or
                                GeofencingRequest.INITIAL_TRIGGER_EXIT)
                        .addGeofence(fence)
                        .build(),
                    pending(c))
                .await()
            app.geofenceStatus.value =
                "Library geofence registered • ${cfg.radius.toInt()} m radius"
        } catch (e: Exception) {
            app.geofenceStatus.value =
                "Location setup failed: ${e.message}. Check permissions and retry."
            app.report(app.geofenceStatus.value)
            app.events.signal("Location", false)
        }
    }

    fun scheduleOverride(c: Context, deadline: Long) {
        (c.applicationContext as? StudyNestApp)?.scheduleSoundDeadline(deadline)
        WorkManager.getInstance(c).enqueueUniqueWork("sound-override", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ExitWorker>().setInitialDelay((deadline - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS).build())
    }

    fun scheduleExit(c: Context, afterMs: Long) {
        WorkManager.getInstance(c).enqueueUniqueWork("timer-exit", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ExitWorker>().setInitialDelay(afterMs.coerceAtLeast(0), TimeUnit.MILLISECONDS).build())
        WorkManager.getInstance(c)
            .enqueueUniqueWork(
                "settle-exit",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ExitWorker>()
                    .setInitialDelay(SessionEngine.WIFI_SOUND_GRACE_MS, TimeUnit.MILLISECONDS)
                    .build())
    }

    fun cleanSsid(s: String?) =
        s?.removeSurrounding("\"")?.takeIf { it.isNotBlank() && it != UNKNOWN_WIFI_SSID }

    @SuppressLint("MissingPermission")
    fun currentSsid(c: Context): String? {
        if (!c.allowed(Manifest.permission.ACCESS_FINE_LOCATION) || !c.locationEnabled())
            return null
        return try {
            @Suppress("DEPRECATION")
            val wifi = c.applicationContext.getSystemService(WifiManager::class.java)
            if (wifi?.isWifiEnabled != true) return null
            val info = wifi.connectionInfo
            cleanSsid(info?.ssid)
        } catch (e: SecurityException) {
            null
        }
    }
}

class ExitWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result =
        try {
            val app = applicationContext as StudyNestApp
            if (app.preferences.value.wifi) app.events.signal("Wi-Fi", app.preferences.value.matchesSsid(Automation.currentSsid(applicationContext)))
            app.events.recover()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
}

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        val app = c.applicationContext as StudyNestApp
        if (!app.preferences.value.geo) return
        val pending = goAsync()
        app.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                if (event.hasError()) {
                    app.report(
                        "Geofence unavailable (${event.errorCode}). Check Location and retry in Settings.")
                    app.geofenceStatus.value = "Geofence unavailable. Retry in Settings."
                    app.events.signal("Location", false)
                } else if (app.preferences.value.geo &&
                    event.triggeringGeofences.orEmpty().any {
                        it.requestId == "library-${app.preferences.value.geoVersion}"
                    }) {
                    val inside =
                        event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL ||
                            event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                    app.events.signal("Location", inside)
                    Automation.start(c)
                }
            } catch (e: Exception) {
                app.report("Location event failed: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
            return
        val app = c.applicationContext as StudyNestApp
        val pending = goAsync()
        app.scope.launch {
            try {
                app.engine.recover()
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) app.engine.reconfigure()
                if (app.engine.needsService()) Automation.start(c)
                withTimeout(7000) { Automation.registerGeofence(c) }
            } catch (e: Exception) {
                app.report("Open StudyNest to complete monitoring recovery: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }
}

class StudyService : Service() {
    private val app
        get() = application as StudyNestApp

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var wifiEpoch = 0
    private val networks = mutableMapOf<Network, Boolean?>()
    private var lastReady: Boolean? = null
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scope.launch {
                runCatching {
                    app.usage.poll(if (intent.action == Intent.ACTION_SCREEN_OFF || !UsageAccess.interactive(app))
                        com.studynest.app.domain.UsageTimeline.LOCK else com.studynest.app.domain.UsageTimeline.UNLOCK)
                    Notifications.refresh(app)
                }.onFailure { app.report("App monitoring needs attention: ${it.message}") }
            }
        }
    }
    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) scope.launch {
                try { app.engine.tick() } catch (e: Exception) { app.report("Sound monitoring: ${e.message}") }
            }
        }
    }
    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!app.preferences.value.wifi) return
            when (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)) {
                WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> {
                    wifiEpoch++ // Ignore capabilities queued before Wi-Fi was switched off.
                    networks.clear()
                    app.automationStatus.value = "Wi-Fi is turned off"
                    emitWifi(false)
                }
                WifiManager.WIFI_STATE_ENABLED -> watchWifi()
            }
        }
    }
    private val manager
        get() = getSystemService(ConnectivityManager::class.java)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!promote()) return
        ContextCompat.registerReceiver(this, ringerReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION), ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(this, wifiReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION), ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_EXPORTED) // SCREEN/USER_PRESENT are protected OS broadcasts.
        scope.launch {
            while (isActive) {
                try {
                    if (app.preferences.value.appMonitoring &&
                        (UsageAccess.interactive(app) || app.preferences.value.monitorCallAudio) &&
                        app.db.dao().active()?.paused == false) {
                        app.engine.syncFocus()
                        Notifications.refresh(app)
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { app.report("App monitoring needs attention: ${e.message}") }
                delay(UsageMonitor.POLL_MS)
            }
        }
        watchWifi()
        scope.launch {
            try {
                if (app.preferences.value.wifi) app.events.signal("Wi-Fi", app.preferences.value.matchesSsid(Automation.currentSsid(this@StudyService)))
                app.engine.recover()
                app.events.recover()
                // The first onStartCommand may still be creating a manual session.
                // Let it decide when to stop after its action has been applied.
                Notifications.refresh(app)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { app.report("Monitoring recovery needs attention: ${e.message}") }
            var timerDay = java.time.LocalDate.now() to java.time.ZoneId.systemDefault()
            while (isActive) {
                delay(30000)
                try {
                    val currentDay = java.time.LocalDate.now() to java.time.ZoneId.systemDefault()
                    val dayChanged = currentDay != timerDay
                    timerDay = currentDay
                    val ready =
                        locationEnabled() && backgroundLocationAllowed()
                    val readinessChanged = ready != lastReady
                    if (app.preferences.value.wifi && !ready && lastReady != false) {
                        app.automationStatus.value =
                            "Wi-Fi inactive: enable Location and precise / always permission."
                        app.events.signal("Wi-Fi", false)
                        watchWifi() // Unregister callbacks that can no longer carry SSIDs.
                    }
                    if (app.preferences.value.geo && !ready && lastReady != false) {
                        app.geofenceStatus.value =
                            "Location inactive: check permissions and device Location."
                        app.events.signal("Location", false)
                    } else if (ready && app.preferences.value.geo && lastReady == false)
                        Automation.registerGeofence(this@StudyService)
                    if (ready && lastReady == false) { promote(); watchWifi() }
                    lastReady = ready
                    // Radio changes arrive through NetworkCallback; do not poll the SSID.
                    val state = app.db.dao().state()
                    if ((app.preferences.value.wifi && state?.wifiInside == true ||
                        app.preferences.value.geo && state?.geoInside == true) && app.db.dao().active()?.paused != false)
                        app.engine.syncFocus() // Checkpoint presence even during a manual break.
                    // Checkpoint running timers and retry pending sound changes. Idle
                    // Wi-Fi monitoring should not rewrite Room or WorkManager every tick.
                    if (app.db.dao().active()?.paused == false || state?.normalSoundPending == true ||
                        state?.exitAt != null || state?.wifiExitAt != null || readinessChanged || dayChanged) {
                        app.engine.tick()
                        updateNotificationAndStop()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    app.report("Monitoring needs attention: ${e.message}")
                }
            }
        }
    }

    private fun promote(): Boolean {
        val location = (app.preferences.value.wifi || app.preferences.value.geo) &&
            backgroundLocationAllowed() && locationEnabled()
        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                if (location) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        } else if (Build.VERSION.SDK_INT >= 29 && location)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        try {
            val notification = Notifications.notification(this, "Preparing study monitoring", false, false)
            if (Build.VERSION.SDK_INT >= 29) startForeground(1, notification, type)
            else startForeground(1, notification)
            app.monitoring.value = true
            return true
        } catch (e: Exception) {
            app.report("Could not start monitoring: ${e.message}")
            app.monitoring.value = false
            stopSelf()
            return false
        }
    }

    private suspend fun updateNotificationAndStop() {
        Notifications.refresh(app)
        val state = app.db.dao().state()
        val canMonitor = backgroundLocationAllowed() && locationEnabled()
        val hasSessionOrDeadline = app.db.dao().active() != null || state?.exitAt != null ||
            state?.wifiExitAt != null || state?.normalSoundPending == true
        if (!app.engine.needsService() || (!canMonitor && !hasSessionOrDeadline)) {
            app.monitoring.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun emitWifi(inside: Boolean) {
        val enabled = applicationContext.getSystemService(WifiManager::class.java).isWifiEnabled
        app.events.dispatch("Wi-Fi", inside && enabled)
    }

    private fun emitKnownWifi() {
        val inside = wifiPresence(networks.values)
        if (inside != null) emitWifi(inside)
    }

    @SuppressLint("MissingPermission")
    private fun watchWifi() {
        val epoch = ++wifiEpoch
        callback?.let { runCatching { manager.unregisterNetworkCallback(it) } }
        callback = null
        networks.clear()
        if (!app.preferences.value.wifi) {
            app.automationStatus.value = "Wi-Fi off"
            return
        }
        if (!backgroundLocationAllowed() ||
            !locationEnabled()) {
            app.automationStatus.value =
                "Wi-Fi inactive: grant Precise / Always location and enable Location."
            emitWifi(false)
            return
        }
        val cb = wifiCallback(
                capabilities = { n, c ->
                    // transportInfo is API 29; location-bearing callbacks are API 31.
                    // Older phones use connectionInfo only in response to radio events.
                    val ssid = if (Build.VERSION.SDK_INT >= 31)
                        Automation.cleanSsid((c.transportInfo as? WifiInfo)?.ssid)
                            ?: Automation.currentSsid(this@StudyService)
                    else Automation.currentSsid(this@StudyService)
                    val wake = EventWake(this@StudyService, 10000)
                    scope.launch {
                        if (epoch != wifiEpoch) return@launch
                        // Redacted names are not proof of departure. onLost handles loss.
                        networks[n] = if (ssid == null) networks[n]
                            else app.preferences.value.matchesSsid(ssid)
                        app.automationStatus.value =
                            if (ssid == null) "Wi-Fi name unavailable. Check Location access."
                            else if (networks[n] == true) "Connected to library • $ssid"
                            else "Waiting for a saved library Wi-Fi"
                        emitKnownWifi()
                    }.invokeOnCompletion { wake.close() }
                },
                lost = { n ->
                    val wake = EventWake(this@StudyService, 10000)
                    scope.launch {
                        if (epoch != wifiEpoch) return@launch
                        networks.remove(n)
                        emitKnownWifi()
                        app.automationStatus.value = when (wifiPresence(networks.values)) {
                            true -> "Connected to a saved library Wi-Fi"
                            false -> "Waiting for a saved library Wi-Fi"
                            null -> "Wi-Fi name unavailable. Check Location access."
                        }
                    }.invokeOnCompletion { wake.close() }
                })
        try {
            manager.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build(),
                cb)
            callback = cb
            emitWifi(app.preferences.value.matchesSsid(Automation.currentSsid(this)))
            app.automationStatus.value = "Wi-Fi monitoring active"
        } catch (e: Exception) {
            app.report("Wi-Fi monitoring unavailable: ${e.message}")
            emitWifi(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            try {
                when (intent?.action) {
                    "START" -> {
                        if (app.db.dao().active() == null) {
                            app.engine.manualStart()
                            if (app.preferences.value.tracking) {
                                watchWifi()
                                Automation.registerGeofence(this@StudyService)
                            }
                        }
                    }
                    "AUTO_START" -> {
                        app.preferences.save(app.preferences.value.copy(tracking = true))
                        app.engine.startAutomatic()
                        watchWifi()
                        Automation.registerGeofence(this@StudyService)
                    }
                    "AUTO_STOP" -> {
                        app.preferences.save(app.preferences.value.copy(tracking = false))
                        app.engine.recover()
                    }
                    "PAUSE" -> app.engine.pauseResume()
                    "FINISH" -> app.engine.finish()
                    "REFRESH" -> {
                        promote()
                        watchWifi()
                    }
                }
                app.usage.poll()
                updateNotificationAndStop()
            } catch (e: Exception) {
                app.report("Session action failed: ${e.message}")
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        app.monitoring.value = false
        runCatching { unregisterReceiver(ringerReceiver) }
        runCatching { unregisterReceiver(wifiReceiver) }
        runCatching { unregisterReceiver(screenReceiver) }
        app.runTask { app.usage.poll() }
        callback?.let { runCatching { manager.unregisterNetworkCallback(it) } }
        scope.cancel()
        super.onDestroy()
    }
}
