package app.aether.wear.regen

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.aether.wear.presentation.MainActivity

object RegenScheduler {
    private const val REQUEST_CODE = 41
    private const val SHOW_CODE = 42

    fun schedule(context: Context, atMillis: Long?) {
        val app = context.applicationContext
        val alarms = app.getSystemService(AlarmManager::class.java) ?: return
        val pending = pending(app)
        if (atMillis == null) {
            alarms.cancel(pending)
            return
        }
        val whenAt = atMillis.coerceAtLeast(System.currentTimeMillis() + 250L)
        val show = PendingIntent.getActivity(
            app,
            SHOW_CODE,
            Intent(app, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(whenAt, show), pending)
        }
        val canExact = Build.VERSION.SDK_INT < 31 || runCatching { alarms.canScheduleExactAlarms() }.getOrDefault(true)
        if (canExact) {
            runCatching { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending) }
        } else {
            runCatching { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending) }
        }
    }

    fun cancel(context: Context) {
        context.applicationContext.getSystemService(AlarmManager::class.java)?.cancel(pending(context.applicationContext))
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, RegenAlarmReceiver::class.java).setAction("app.aether.wear.REGEN_TICK")
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
