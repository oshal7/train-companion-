package com.traincompanion.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.traincompanion.app.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-arms alarms after a reboot so offline-scheduled arrival alarms survive a device restart. */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = AppContainer.get(appContext)
                val now = System.currentTimeMillis()
                container.alarmsRepository.list()
                    .filter { it.enabled && it.triggerAtMillis > now }
                    .forEach { container.alarmScheduler.schedule(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
