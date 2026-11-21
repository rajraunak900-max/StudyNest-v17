package com.studynest.app.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

enum class AccessDestination { APP, NOTIFICATIONS, USAGE, SOUND, BATTERY, LOCATION, WIFI }

/** Public Android actions only. Package-targeted Usage Access is supported by AOSP;
 * OEMs that do not handle it fall back to the specific access list. */
object AccessNavigation {
    fun intents(c: Context, destination: AccessDestination): List<Intent> {
        val app = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${c.packageName}"))
        val primary = when (destination) {
            AccessDestination.APP -> listOf(app)
            AccessDestination.NOTIFICATIONS -> listOf(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName))
            AccessDestination.USAGE -> listOf(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, app.data),
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            AccessDestination.SOUND -> listOf(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            // Direct exemption would require an additional manifest permission.
            AccessDestination.BATTERY -> listOf(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            AccessDestination.LOCATION -> listOf(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            AccessDestination.WIFI -> listOf(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        return if (destination == AccessDestination.APP) primary else primary + app
    }

    fun open(c: Context, destination: AccessDestination): String? {
        val candidates = intents(c, destination)
        for ((index, intent) in candidates.withIndex()) {
            try {
                c.startActivity(intent)
                return if (index == candidates.lastIndex && candidates.size > 1)
                    "Your phone could not open that access page. Open Permissions or search Android Settings for the access name and select StudyNest."
                else when (destination) {
                    AccessDestination.USAGE -> if (index == 1) "Select StudyNest and enable Usage Access." else null
                    AccessDestination.SOUND -> "Select StudyNest and allow modes / Do Not Disturb access."
                    AccessDestination.BATTERY -> "Select All apps → StudyNest → Don't optimize."
                    else -> null
                }
            } catch (_: android.content.ActivityNotFoundException) {
                // Try the nearest supported destination.
            } catch (_: SecurityException) {
                // Some managed/OEM profiles restrict special-access pages.
            }
        }
        return "This phone cannot open that settings page. Search Android Settings for the access name and select StudyNest."
    }
}
