package app.aether.wear.regen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import app.aether.wear.R
import app.aether.wear.data.Haptics
import app.aether.wear.data.PoolRepository
import app.aether.wear.data.isTicking
import app.aether.wear.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RegenService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repo: PoolRepository
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = PoolRepository(applicationContext)
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        if (loop?.isActive != true) {
            loop = scope.launch { runLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        RegenScheduler.cancel(applicationContext)
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runLoop() {
        while (scope.isActive) {
            val now = System.currentTimeMillis()
            val result = repo.applyTick(now)
            if (result.gained) Haptics.regen(applicationContext)
            if (!result.keepRunning) {
                RegenScheduler.cancel(applicationContext)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            RegenScheduler.schedule(applicationContext, result.nextRegenAt)
            val wait = result.nextRegenAt
                ?.minus(System.currentTimeMillis())
                ?.coerceIn(250L, 15_000L)
                ?: 1_000L
            delay(wait)
        }
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Regen", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps power regen running while you are away from the app"
                setSound(null, null)
                enableVibration(false)
            },
        )
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_regen)
            .setContentTitle("Poweratti")
            .setContentText("Regen running")
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(tap)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        OngoingActivity.Builder(applicationContext, NOTIF_ID, builder)
            .setStaticIcon(R.drawable.ic_regen)
            .setTouchIntent(tap)
            .setStatus(Status.forPart(Status.TextPart("Regen")))
            .build()
            .apply(applicationContext)
        val notification: Notification = builder.build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "regen"
        const val NOTIF_ID = 41
    }
}
