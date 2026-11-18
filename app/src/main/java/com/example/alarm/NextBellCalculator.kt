package com.example.alarm

import com.example.data.model.BellScheduleEntity
import com.example.data.model.DayOfWeekBitmask
import java.util.Calendar

enum class BellType(val label: String) {
    START("시작종"),
    END("종료종"),
}

data class BellEvent(
    val scheduleId: Long,
    val scheduleTitle: String,
    val bellType: BellType,
    val triggerAtMillis: Long,
    val soundId: String,
    val ringDurationSeconds: Int,
    val isWeeklyRepeat: Boolean,
    val dayBit: Int,
    val hour: Int,
    val minute: Int,
)

data class NextBellGroup(
    val triggerAtMillis: Long,
    val primaryEvent: BellEvent,
    val allEventsAtTime: List<BellEvent>,
)

object NextBellCalculator {

    fun findNextBellGroup(
        schedules: List<BellScheduleEntity>,
        nowMillis: Long,
        masterEnabled: Boolean,
    ): NextBellGroup? {
        if (!masterEnabled) return null

        val activeSchedules = schedules.filter { it.isEnabled && it.daysOfWeekMask != 0 }
        if (activeSchedules.isEmpty()) return null

        val candidates = mutableListOf<BellEvent>()

        for (schedule in activeSchedules) {
            for (dayItem in DayOfWeekBitmask.orderedDays) {
                if (!DayOfWeekBitmask.hasDay(schedule.daysOfWeekMask, dayItem.bit)) continue

                if (schedule.startBellEnabled) {
                    val triggerMs = nextOccurrenceMillis(
                        nowMillis = nowMillis,
                        targetCalendarDay = dayItem.calendarDay,
                        hour = schedule.startHour,
                        minute = schedule.startMinute,
                    )
                    candidates.add(
                        BellEvent(
                            scheduleId = schedule.id,
                            scheduleTitle = schedule.title,
                            bellType = BellType.START,
                            triggerAtMillis = triggerMs,
                            soundId = schedule.startBellSoundId,
                            ringDurationSeconds = schedule.ringDurationSeconds,
                            isWeeklyRepeat = schedule.isWeeklyRepeat,
                            dayBit = dayItem.bit,
                            hour = schedule.startHour,
                            minute = schedule.startMinute,
                        )
                    )
                }

                if (schedule.endBellEnabled) {
                    val triggerMs = nextOccurrenceMillis(
                        nowMillis = nowMillis,
                        targetCalendarDay = dayItem.calendarDay,
                        hour = schedule.endHour,
                        minute = schedule.endMinute,
                    )
                    candidates.add(
                        BellEvent(
                            scheduleId = schedule.id,
                            scheduleTitle = schedule.title,
                            bellType = BellType.END,
                            triggerAtMillis = triggerMs,
                            soundId = schedule.endBellSoundId,
                            ringDurationSeconds = schedule.ringDurationSeconds,
                            isWeeklyRepeat = schedule.isWeeklyRepeat,
                            dayBit = dayItem.bit,
                            hour = schedule.endHour,
                            minute = schedule.endMinute,
                        )
                    )
                }
            }
        }

        if (candidates.isEmpty()) return null

        val earliestTime = candidates.minOf { it.triggerAtMillis }
        val eventsAtEarliest = candidates.filter { it.triggerAtMillis == earliestTime }
        // Prioritize START bell if both an end bell and start bell occur at the exact same minute
        val primary = eventsAtEarliest.firstOrNull { it.bellType == BellType.START }
            ?: eventsAtEarliest.first()

        return NextBellGroup(
            triggerAtMillis = earliestTime,
            primaryEvent = primary,
            allEventsAtTime = eventsAtEarliest,
        )
    }

    fun nextOccurrenceMillis(
        nowMillis: Long,
        targetCalendarDay: Int,
        hour: Int,
        minute: Int,
    ): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysAhead = (targetCalendarDay - currentDay + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)

        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
        }
        return cal.timeInMillis
    }
}
