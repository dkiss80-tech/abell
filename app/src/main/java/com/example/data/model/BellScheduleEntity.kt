package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "bell_schedules")
data class BellScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val breakMinutes: Int = 10,
    val daysOfWeekMask: Int = DayOfWeekBitmask.WEEKDAYS,
    val isWeeklyRepeat: Boolean = true,
    val isEnabled: Boolean = true,
    val startBellEnabled: Boolean = true,
    val endBellEnabled: Boolean = true,
    val startBellSoundId: String = BuiltInBellSounds.BUILTIN_BRIGHT_START,
    val endBellSoundId: String = BuiltInBellSounds.BUILTIN_WARM_END,
    val ringDurationSeconds: Int = 8,
) {
    val startTotalMinutes: Int
        get() = startHour * 60 + startMinute

    val endTotalMinutes: Int
        get() = endHour * 60 + endMinute

    val classDurationMinutes: Int
        get() {
            val diff = (endTotalMinutes - startTotalMinutes + 1440) % 1440
            return if (diff == 0) 50 else diff
        }

    fun formattedStartTime(): String =
        String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)

    fun formattedEndTime(): String =
        String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)

    fun nextChainedStartHourMinute(): Pair<Int, Int> {
        val nextTotal = (endTotalMinutes + breakMinutes.coerceAtLeast(0)) % 1440
        return (nextTotal / 60) to (nextTotal % 60)
    }

    fun formattedNextChainedStartTime(): String {
        val (h, m) = nextChainedStartHourMinute()
        return String.format(Locale.getDefault(), "%02d:%02d", h, m)
    }
}
