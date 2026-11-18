package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.repository.BellRepository

object BellAlarmScheduler {
    private const val REQUEST_CODE_NEXT_BELL = 1001

    const val EXTRA_SCHEDULE_IDS = "extra_schedule_ids"
    const val EXTRA_SCHEDULE_TITLE = "extra_schedule_title"
    const val EXTRA_BELL_TYPE = "extra_bell_type"
    const val EXTRA_SOUND_ID = "extra_sound_id"
    const val EXTRA_RING_DURATION_SEC = "extra_ring_duration_sec"
    const val EXTRA_TRIGGER_MILLIS = "extra_trigger_millis"
    const val EXTRA_DAY_BIT = "extra_day_bit"

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    suspend fun rescheduleNextAlarm(
        context: Context,
        repository: BellRepository,
        afterMillis: Long = System.currentTimeMillis(),
    ): NextBellGroup? {
        val prefs = repository.getPreferencesSync()
        val schedules = repository.getAllSchedulesSync()
        val nextGroup = NextBellCalculator.findNextBellGroup(
            schedules = schedules,
            nowMillis = afterMillis,
            masterEnabled = prefs.masterEnabled,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelExistingAlarm(context, alarmManager)

        if (nextGroup == null) return null

        val primary = nextGroup.primaryEvent
        val intent = Intent(context, BellAlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_IDS, nextGroup.allEventsAtTime.map { it.scheduleId }.toLongArray())
            putExtra(EXTRA_SCHEDULE_TITLE, primary.scheduleTitle)
            putExtra(EXTRA_BELL_TYPE, primary.bellType.name)
            putExtra(EXTRA_SOUND_ID, primary.soundId)
            putExtra(EXTRA_RING_DURATION_SEC, primary.ringDurationSeconds)
            putExtra(EXTRA_TRIGGER_MILLIS, nextGroup.triggerAtMillis)
            putExtra(EXTRA_DAY_BIT, primary.dayBit)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_NEXT_BELL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (canScheduleExactAlarms(context)) {
            val info = AlarmManager.AlarmClockInfo(nextGroup.triggerAtMillis, showIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextGroup.triggerAtMillis,
                pendingIntent,
            )
        }

        return nextGroup
    }

    private fun cancelExistingAlarm(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, BellAlarmReceiver::class.java)
        val existing = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_NEXT_BELL,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }
}
