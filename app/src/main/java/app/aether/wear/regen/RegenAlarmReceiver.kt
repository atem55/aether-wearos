package app.aether.wear.regen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import app.aether.wear.data.Haptics
import app.aether.wear.data.PoolRepository
import kotlinx.coroutines.runBlocking

class RegenAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val pending = goAsync()
        val lock = app.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "poweratti:tick")
        lock.setReferenceCounted(false)
        runCatching { lock.acquire(8_000L) }
        try {
            val result = runBlocking { PoolRepository(app).applyTick(System.currentTimeMillis()) }
            if (result.buzz) {
                val name = result.saved.pools.firstOrNull { it.id == result.saved.activeRegenId }?.name
                Haptics.regen(app, name)
            }
            if (result.keepRunning) RegenScheduler.schedule(app, result.nextRegenAt)
            else RegenScheduler.cancel(app)
            runCatching { RegenController.sync(app) }
        } finally {
            if (lock.isHeld) runCatching { lock.release() }
            pending.finish()
        }
    }
}
