package app.aether.wear.presentation

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import app.aether.wear.regen.RegenController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNoticePermission()
        requestExactAlarms()
        RegenController.sync(this)
        setContent { AetherApp() }
    }

    override fun onStop() {
        RegenController.sync(this)
        super.onStop()
    }

    private fun requestNoticePermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }

    private fun requestExactAlarms() {
        if (Build.VERSION.SDK_INT < 31) return
        val alarms = getSystemService(AlarmManager::class.java) ?: return
        if (alarms.canScheduleExactAlarms()) return
        runCatching {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }
}
