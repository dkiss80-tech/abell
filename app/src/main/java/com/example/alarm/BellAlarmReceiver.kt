package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.data.model.BuiltInBellSounds
import com.example.data.model.DayOfWeekBitmask
import com.example.data.repository.BellRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BellAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AcademySmartBell:AlarmWakeLock",
        ).apply { acquire(15_000L) }

        val title = intent.getStringExtra(BellAlarmScheduler.EXTRA_SCHEDULE_TITLE) ?: "수업"
        val bellTypeStr = intent.getStringExtra(BellAlarmScheduler.EXTRA_BELL_TYPE) ?: BellType.START.name
        val soundId = intent.getStringExtra(BellAlarmScheduler.EXTRA_SOUND_ID)
            ?: BuiltInBellSounds.BUILTIN_BRIGHT_START
        val durationSec = intent.getIntExtra(BellAlarmScheduler.EXTRA_RING_DURATION_SEC, 8)
        val triggerMillis = intent.getLongExtra(
            BellAlarmScheduler.EXTRA_TRIGGER_MILLIS,
            System.currentTimeMillis(),
        )
        val dayBit = intent.getIntExtra(BellAlarmScheduler.EXTRA_DAY_BIT, 0)
        val scheduleIds = intent.getLongArrayExtra(BellAlarmScheduler.EXTRA_SCHEDULE_IDS) ?: longArrayOf()

        val serviceIntent = Intent(context, BellPlaybackService::class.java).apply {
            action = BellPlaybackService.ACTION_START_BELL
            putExtra(BellPlaybackService.EXTRA_TITLE, title)
            putExtra(BellPlaybackService.EXTRA_BELL_TYPE, bellTypeStr)
            putExtra(BellPlaybackService.EXTRA_SOUND_ID, soundId)
            putExtra(BellPlaybackService.EXTRA_DURATION_SEC, durationSec)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = BellRepository(context.applicationContext)
                val firedBellType = runCatching { BellType.valueOf(bellTypeStr) }.getOrDefault(BellType.START)

                // If one-time schedule (!isWeeklyRepeat) finished its final bell for this day, clear that day bit
                if (dayBit != 0) {
                    for (id in scheduleIds) {
                        val schedule = repo.getScheduleByIdSync(id) ?: continue
                        if (!schedule.isWeeklyRepeat) {
                            val isFinalBellForDay =
                                firedBellType == BellType.END || !schedule.endBellEnabled
                            if (isFinalBellForDay) {
                                val newMask = DayOfWeekBitmask.clearDay(schedule.daysOfWeekMask, dayBit)
                                repo.updateScheduleDaysAndEnabled(
                                    id = schedule.id,
                                    mask = newMask,
                                    enabled = newMask != 0,
                                )
                            }
                        }
                    }
                }

                // Chain next exact alarm strictly after this trigger timestamp
                val nextAfter = maxOf(System.currentTimeMillis(), triggerMillis + 1_500L)
                BellAlarmScheduler.rescheduleNextAlarm(
                    context = context.applicationContext,
                    repository = repo,
                    afterMillis = nextAfter,
                )
            } finally {
                if (wakeLock.isHeld) {
                    runCatching { wakeLock.release() }
                }
                pendingResult.finish()
            }
        }
    }
}
