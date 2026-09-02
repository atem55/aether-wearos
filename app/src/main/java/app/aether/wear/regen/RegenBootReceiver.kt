package app.aether.wear.regen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RegenBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        RegenController.sync(context)
    }
}
