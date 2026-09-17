package app.aether.wear.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CombinedVibration
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.aether.wear.R
import app.aether.wear.presentation.MainActivity

object Haptics {
    private const val TICK_CHANNEL = "regen_tick"
    private const val TICK_NOTIF_ID = 42
    private val pattern = longArrayOf(0, 220, 70, 220)

    fun regen(context: Context, label: String? = null) {
        val app = context.applicationContext
        val lock = wakeLock(app)
        try {
            pulse(app)
            announce(app, label)
        } finally {
            if (lock.isHeld) lock.release()
        }
    }

    private fun pulse(app: Context) {
        val effect = VibrationEffect.createWaveform(pattern, -1)
        val usages = intArrayOf(
            VibrationAttributes.USAGE_NOTIFICATION,
            VibrationAttributes.USAGE_ALARM,
            VibrationAttributes.USAGE_HARDWARE_FEEDBACK,
        )
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = app.getSystemService(VibratorManager::class.java) ?: return
            for (usage in usages) {
                manager.vibrate(
                    CombinedVibration.createParallel(effect),
                    VibrationAttributes.Builder().setUsage(usage).build(),
                )
            }
        } else {
            val vibrator = vibrator(app) ?: return
            vibrator.vibrate(effect)
        }
    }

    private fun announce(app: Context, label: String?) {
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(TICK_CHANNEL, "Regen ticks", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Vibrates when a power pool regenerates"
                enableVibration(true)
                vibrationPattern = pattern
                setSound(null, null)
            },
        )
        val tap = PendingIntent.getActivity(
            app,
            0,
            Intent(app, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (label.isNullOrBlank()) "Power regenerated" else "$label regenerated"
        val notification = NotificationCompat.Builder(app, TICK_CHANNEL)
            .setSmallIcon(R.drawable.ic_regen)
            .setContentTitle("Poweratti")
            .setContentText(text)
            .setVibrate(pattern)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(tap)
            .build()
        try {
            NotificationManagerCompat.from(app).cancel(TICK_NOTIF_ID)
            NotificationManagerCompat.from(app).notify(TICK_NOTIF_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission denied; vibrator path above may still fire.
        }
    }

    private fun wakeLock(app: Context): PowerManager.WakeLock {
        val lock = app.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "poweratti:regen")
        lock.setReferenceCounted(false)
        lock.acquire(2_500L)
        return lock
    }

    private fun vibrator(context: Context): Vibrator? {
        val app = context.applicationContext
        return if (Build.VERSION.SDK_INT >= 31) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Vibrator::class.java)
        }
    }
}
