package app.aether.wear.regen

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object RegenScheduler {
    private const val REQUEST_CODE = 41

    fun schedule(context: Context, atMillis: Long?) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pending(context)
        if (atMillis == null) {
            alarms.cancel(pending)
            return
        }
        val whenAt = atMillis.coerceAtLeast(System.currentTimeMillis() + 250L)
        val canExact = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
        if (canExact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pending)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context))
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, RegenAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
