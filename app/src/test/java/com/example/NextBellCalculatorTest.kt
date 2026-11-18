package com.example

import com.example.alarm.BellType
import com.example.alarm.NextBellCalculator
import com.example.data.model.BellScheduleEntity
import com.example.data.model.DayOfWeekBitmask
import com.example.ui.MainViewModel
import com.example.ui.ScheduleDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class NextBellCalculatorTest {

    @Test
    fun `80 minute class and break chaining computes exact next start time`() {
        val draft = ScheduleDraft(
            title = "1교시",
            startHour = 14,
            startMinute = 0,
            classDurationMinutes = 80,
            breakMinutes = 10,
        )
        assertEquals("15:20", draft.formattedEndTime())
        assertEquals("15:30", draft.formattedNextStartTime())
        assertEquals("2교시", MainViewModel.incrementPeriodTitle(draft.title))
    }

    @Test
    fun `next bell calculator picks start bell before end bell on matching weekday`() {
        val mondayMorning = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 16, 13, 30, 0) // Monday 13:30
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val schedule = BellScheduleEntity(
            id = 1L,
            title = "1교시",
            startHour = 14,
            startMinute = 0,
            endHour = 15,
            endMinute = 20,
            breakMinutes = 10,
            daysOfWeekMask = DayOfWeekBitmask.MON or DayOfWeekBitmask.WED,
            isWeeklyRepeat = true,
            isEnabled = true,
        )

        val next = NextBellCalculator.findNextBellGroup(
            schedules = listOf(schedule),
            nowMillis = mondayMorning,
            masterEnabled = true,
        )

        assertNotNull(next)
        assertEquals(BellType.START, next!!.primaryEvent.bellType)
        assertEquals(14, next.primaryEvent.hour)
        assertEquals(0, next.primaryEvent.minute)
    }

    @Test
    fun `master switch off suppresses all alarms`() {
        val schedule = BellScheduleEntity(
            id = 1L,
            title = "1교시",
            startHour = 14,
            startMinute = 0,
            endHour = 14,
            endMinute = 50,
            daysOfWeekMask = DayOfWeekBitmask.ALL_DAYS,
            isEnabled = true,
        )

        val next = NextBellCalculator.findNextBellGroup(
            schedules = listOf(schedule),
            nowMillis = System.currentTimeMillis(),
            masterEnabled = false,
        )
        assertNull(next)
    }

    @Test
    fun `weekly repeat advances to next week when today time has passed`() {
        val mondayLate = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 16, 16, 0, 0) // Monday 16:00
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val schedule = BellScheduleEntity(
            id = 2L,
            title = "월요일 특강",
            startHour = 14,
            startMinute = 0,
            endHour = 15,
            endMinute = 20,
            daysOfWeekMask = DayOfWeekBitmask.MON,
            isWeeklyRepeat = true,
            isEnabled = true,
        )

        val next = NextBellCalculator.findNextBellGroup(
            schedules = listOf(schedule),
            nowMillis = mondayLate,
            masterEnabled = true,
        )

        assertNotNull(next)
        val cal = Calendar.getInstance().apply { timeInMillis = next!!.triggerAtMillis }
        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(23, cal.get(Calendar.DAY_OF_MONTH))
    }
}
