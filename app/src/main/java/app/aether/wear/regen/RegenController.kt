package app.aether.wear.regen

import android.content.Context
import android.content.Intent
import android.os.Build
import app.aether.wear.data.PoolRepository
import app.aether.wear.data.tickingPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object RegenController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun sync(context: Context) {
        val app = context.applicationContext
        scope.launch {
            val next = runCatching { PoolRepository(app).current().tickingPool()?.nextRegenAt }.getOrNull()
            if (next != null) RegenScheduler.schedule(app, next)
            else RegenScheduler.cancel(app)
        }
        val intent = Intent(app, RegenService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(intent)
            else app.startService(intent)
        }
    }
}
