package app.aether.wear.regen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RegenAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        RegenController.sync(context)
    }
}
