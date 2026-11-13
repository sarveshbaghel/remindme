package com.taskly.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateUtilsTest {

    @Test
    fun testTimeToMinutesConversion() {
        val time930 = LocalTime.of(9, 30)
        val minutes = DateUtils.timeToMinutes(time930)
        assertEquals(570, minutes)

        val restoredTime = DateUtils.minutesToTime(minutes)
        assertEquals(time930, restoredTime)
    }

    @Test
    fun testFormatDueTime() {
        val minutes = 14 * 60 + 45 // 14:45 = 2:45 PM
        val formatted = DateUtils.formatDueTime(minutes)
        assertTrue(formatted.contains("2:45") && formatted.contains("PM"))
    }

    @Test
    fun testFormatDueDateRelativeDays() {
        val today = LocalDate.now()
        val todayMillis = DateUtils.localDateToEpochMillis(today)
        assertEquals("Today", DateUtils.formatDueDate(todayMillis))

        val tomorrowMillis = DateUtils.localDateToEpochMillis(today.plusDays(1))
        assertEquals("Tomorrow", DateUtils.formatDueDate(tomorrowMillis))

        val yesterdayMillis = DateUtils.localDateToEpochMillis(today.minusDays(1))
        assertEquals("Yesterday", DateUtils.formatDueDate(yesterdayMillis))
    }

    @Test
    fun testStartAndEndOfDay() {
        val now = System.currentTimeMillis()
        val start = DateUtils.startOfDayEpoch(now)
        val end = DateUtils.endOfDayEpoch(now)

        assertTrue(start <= now)
        assertTrue(end >= now)
        assertTrue(end > start)
    }
}
