package com.studynest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.studynest.app.automation.Automation
import com.studynest.app.ui.StudyNest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StudyNest(application as StudyNestApp) }
    }

    override fun onResume() {
        super.onResume()
        val app = application as StudyNestApp
        app.runTask {
            if (app.preferences.value.wifi) app.engine.signal("Wi-Fi", app.preferences.value.matchesSsid(Automation.currentSsid(this@MainActivity)))
            app.engine.recover()
            if (app.preferences.value.setup) {
                if (app.engine.needsService()) Automation.start(this@MainActivity, "REFRESH")
                Automation.registerGeofence(this@MainActivity)
            }
        }
    }
}
