package com.studynest.app.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Android's sideload confirmation belongs to App info, not a browser/help page.
 * There is no public third-party API to inspect or grant this system approval. */
@Composable
fun RestrictedAccessHelp(openAppInfo: () -> Unit) {
    if (Build.VERSION.SDK_INT < 33) return
    HorizontalDivider()
    Text("Android says access was denied?", style = MaterialTheme.typography.titleSmall)
    Text("Apps installed from a file may need an extra Android approval. Open StudyNest's App info, tap ⋮ → Allow restricted settings and follow Android's confirmation. Then return here and tap Grant Access again.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("If the option is missing, try the Usage Access switch once first. Phone or administrator restrictions may still prevent approval.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedButton(onClick = openAppInfo, modifier = Modifier.fillMaxWidth().testTag("restricted-app-info")) {
        Text("Open StudyNest app info")
    }
}
