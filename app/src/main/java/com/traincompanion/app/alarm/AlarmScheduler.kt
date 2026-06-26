package com.traincompanion.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.traincompanion.app.data.ScheduledAlarm

/**
 * Wraps AlarmManager for the destination-arrival alarms (PRD 3.5 / 4.5). Falls back to an
 * inexact alarm if the user hasn't granted the "Alarms & reminders" special permission on
 * API 31+ rather than crashing.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    fun schedule(alarm: ScheduledAlarm) {
        if (!alarm.enabled || alarm.triggerAtMillis <= System.currentTimeMillis()) return
        val pendingIntent = buildPendingIntent(alarm)
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(alarm: ScheduledAlarm) {
        alarmManager.cancel(buildPendingIntent(alarm))
    }

    private fun buildPendingIntent(alarm: ScheduledAlarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_LABEL, alarm.label)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
