package com.taskly.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceUtilsTest {

    @Test
    fun testNullRuleReturnsNull() {
        assertNull(RecurrenceUtils.calculateNextDueDate(null, System.currentTimeMillis()))
        assertNull(RecurrenceUtils.calculateNextDueDate("", System.currentTimeMillis()))
    }

    @Test
    fun testDailyRecurrence() {
        val today = LocalDate.of(2026, 5, 10)
        val todayMillis = DateUtils.localDateToEpochMillis(today)

        val nextMillis = RecurrenceUtils.calculateNextDueDate("DAILY", todayMillis)
        assertNotNull(nextMillis)

        val nextDate = DateUtils.epochMillisToLocalDate(nextMillis!!)
        assertEquals(LocalDate.of(2026, 5, 11), nextDate)
    }

    @Test
    fun testWeekdaysRecurrenceSkipsWeekend() {
        // Friday May 15, 2026 -> next weekday is Monday May 18, 2026
        val friday = LocalDate.of(2026, 5, 15)
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)
        val fridayMillis = DateUtils.localDateToEpochMillis(friday)

        val nextMillis = RecurrenceUtils.calculateNextDueDate("WEEKDAYS", fridayMillis)
        assertNotNull(nextMillis)

        val nextDate = DateUtils.epochMillisToLocalDate(nextMillis!!)
        assertEquals(DayOfWeek.MONDAY, nextDate.dayOfWeek)
        assertEquals(LocalDate.of(2026, 5, 18), nextDate)
    }

    @Test
    fun testWeeklyRecurrence() {
        val date = LocalDate.of(2026, 5, 10)
        val dateMillis = DateUtils.localDateToEpochMillis(date)

        val nextMillis = RecurrenceUtils.calculateNextDueDate("WEEKLY", dateMillis)
        assertNotNull(nextMillis)

        val nextDate = DateUtils.epochMillisToLocalDate(nextMillis!!)
        assertEquals(LocalDate.of(2026, 5, 17), nextDate)
    }

    @Test
    fun testMonthlyRecurrence() {
        val date = LocalDate.of(2026, 5, 10)
        val dateMillis = DateUtils.localDateToEpochMillis(date)

        val nextMillis = RecurrenceUtils.calculateNextDueDate("MONTHLY", dateMillis)
        assertNotNull(nextMillis)

        val nextDate = DateUtils.epochMillisToLocalDate(nextMillis!!)
        assertEquals(LocalDate.of(2026, 6, 10), nextDate)
    }

    @Test
    fun testCustomRecurrenceEvery2Weeks() {
        val date = LocalDate.of(2026, 5, 10)
        val dateMillis = DateUtils.localDateToEpochMillis(date)

        val nextMillis = RecurrenceUtils.calculateNextDueDate("CUSTOM:interval=2;unit=WEEKS", dateMillis)
        assertNotNull(nextMillis)

        val nextDate = DateUtils.epochMillisToLocalDate(nextMillis!!)
        assertEquals(LocalDate.of(2026, 5, 24), nextDate)
    }
}
