package app.aether.wear.regen

import android.content.Context
import android.content.Intent
import android.os.Build

object RegenController {
    fun sync(context: Context) {
        val app = context.applicationContext
        val intent = Intent(app, RegenService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }
}
