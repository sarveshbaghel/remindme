package com.taskly.app.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    val zoneId: ZoneId = ZoneId.systemDefault()

    fun todayEpochMillis(): Long {
        return LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun startOfDayEpoch(epochMillis: Long = System.currentTimeMillis()): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun endOfDayEpoch(epochMillis: Long = System.currentTimeMillis()): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return date.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun localDateToEpochMillis(date: LocalDate): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun epochMillisToLocalDate(epochMillis: Long): LocalDate {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    }

    fun timeToMinutes(time: LocalTime): Int {
        return time.hour * 60 + time.minute
    }

    fun minutesToTime(minutes: Int): LocalTime {
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    fun formatDueDate(epochMillis: Long?): String {
        if (epochMillis == null) return "No date"
        val date = epochMillisToLocalDate(epochMillis)
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(today, date)

        return when (daysBetween) {
            0L -> "Today"
            1L -> "Tomorrow"
            -1L -> "Yesterday"
            in 2..6 -> date.format(DateTimeFormatter.ofPattern("EEEE")) // Day of week
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }

    fun formatDueTime(minutes: Int?): String {
        if (minutes == null) return ""
        val time = minutesToTime(minutes)
        return time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    fun formatDateTime(epochMillis: Long): String {
        val dt = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()
        return dt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
    }

    fun combineDateAndTime(dateMillis: Long, minutesOfDay: Int?): Long {
        val date = epochMillisToLocalDate(dateMillis)
        val time = if (minutesOfDay != null) minutesToTime(minutesOfDay) else LocalTime.of(9, 0)
        return date.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun getGreeting(): String {
        return when (LocalTime.now().hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}

