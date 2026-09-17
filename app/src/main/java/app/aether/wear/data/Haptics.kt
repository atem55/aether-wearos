package app.aether.wear.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.CombinedVibration
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.aether.wear.R
import app.aether.wear.presentation.MainActivity

object Haptics {
    private const val TICK_CHANNEL = "regen_tick"
    private const val TICK_NOTIF_ID = 42
    private val timings = longArrayOf(0, 400, 80, 400)
    private val amplitudes = intArrayOf(0, 255, 0, 255)

    fun regen(context: Context, label: String? = null, view: View? = null) {
        val app = context.applicationContext
        val lock = wakeLock(app)
        try {
            pulseView(view)
            pulseVibrator(app)
            announce(app, label)
        } finally {
            if (lock.isHeld) lock.release()
        }
    }

    fun pulseView(view: View?) {
        if (view == null) return
        runCatching {
            view.isHapticFeedbackEnabled = true
            view.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
            )
            view.performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
            )
        }
    }

    private fun pulseVibrator(app: Context) {
        val effect = if (Build.VERSION.SDK_INT >= 26) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        val vibrator = vibrator(app) ?: return

        if (Build.VERSION.SDK_INT >= 33) {
            val manager = app.getSystemService(VibratorManager::class.java)
            val usages = intArrayOf(
                VibrationAttributes.USAGE_NOTIFICATION,
                VibrationAttributes.USAGE_ALARM,
                VibrationAttributes.USAGE_TOUCH,
            )
            if (manager != null) {
                for (usage in usages) {
                    runCatching {
                        manager.vibrate(
                            CombinedVibration.createParallel(effect),
                            VibrationAttributes.Builder().setUsage(usage).build(),
                        )
                    }
                }
            }
        }

        runCatching {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        runCatching {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    private fun announce(app: Context, label: String?) {
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            manager.createNotificationChannel(
                NotificationChannel(TICK_CHANNEL, "Regen ticks", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Vibrates when a power pool regenerates"
                    enableVibration(true)
                    vibrationPattern = timings
                    setSound(null, null)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                },
            )
        }
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
            .setVibrate(timings)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(tap)
            .build()
        runCatching {
            NotificationManagerCompat.from(app).cancel(TICK_NOTIF_ID)
            NotificationManagerCompat.from(app).notify(TICK_NOTIF_ID, notification)
        }
    }

    private fun wakeLock(app: Context): PowerManager.WakeLock {
        val lock = app.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "poweratti:regen")
        lock.setReferenceCounted(false)
        runCatching { lock.acquire(3_000L) }
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
