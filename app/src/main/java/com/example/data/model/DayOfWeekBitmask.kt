package com.example.data.model

import java.util.Calendar

data class DayItem(
    val bit: Int,
    val shortLabel: String,
    val fullLabel: String,
    val calendarDay: Int,
)

object DayOfWeekBitmask {
    const val MON = 1 shl 0 // 1
    const val TUE = 1 shl 1 // 2
    const val WED = 1 shl 2 // 4
    const val THU = 1 shl 3 // 8
    const val FRI = 1 shl 4 // 16
    const val SAT = 1 shl 5 // 32
    const val SUN = 1 shl 6 // 64

    const val WEEKDAYS = MON or TUE or WED or THU or FRI
    const val WEEKEND = SAT or SUN
    const val ALL_DAYS = WEEKDAYS or WEEKEND

    val orderedDays = listOf(
        DayItem(MON, "월", "월요일", Calendar.MONDAY),
        DayItem(TUE, "화", "화요일", Calendar.TUESDAY),
        DayItem(WED, "수", "수요일", Calendar.WEDNESDAY),
        DayItem(THU, "목", "목요일", Calendar.THURSDAY),
        DayItem(FRI, "금", "금요일", Calendar.FRIDAY),
        DayItem(SAT, "토", "토요일", Calendar.SATURDAY),
        DayItem(SUN, "일", "일요일", Calendar.SUNDAY),
    )

    fun fromCalendarDay(calendarDay: Int): Int = when (calendarDay) {
        Calendar.MONDAY -> MON
        Calendar.TUESDAY -> TUE
        Calendar.WEDNESDAY -> WED
        Calendar.THURSDAY -> THU
        Calendar.FRIDAY -> FRI
        Calendar.SATURDAY -> SAT
        Calendar.SUNDAY -> SUN
        else -> 0
    }

    fun todayBit(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return fromCalendarDay(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun hasDay(mask: Int, dayBit: Int): Boolean = (mask and dayBit) != 0

    fun toggleDay(mask: Int, dayBit: Int): Int = mask xor dayBit

    fun clearDay(mask: Int, dayBit: Int): Int = mask and dayBit.inv()

    fun formatDaysSummary(mask: Int): String = when (mask) {
        0 -> "요일 미선택"
        ALL_DAYS -> "매일"
        WEEKDAYS -> "평일 (월~금)"
        WEEKEND -> "주말 (토·일)"
        else -> orderedDays.filter { (mask and it.bit) != 0 }.joinToString("·") { it.shortLabel }
    }
}
