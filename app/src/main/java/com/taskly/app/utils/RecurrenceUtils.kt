package com.taskly.app.utils

import java.time.DayOfWeek
import java.time.LocalDate

object RecurrenceUtils {

    /**
     * Calculates the next occurrence epoch millis (start of day) based on recurrence rule.
     */
    fun calculateNextDueDate(rule: String?, currentDueDateMillis: Long?): Long? {
        if (rule.isNullOrBlank() || currentDueDateMillis == null) return null

        val baseDate = DateUtils.epochMillisToLocalDate(currentDueDateMillis)

        val nextDate: LocalDate = when {
            rule.equals("DAILY", ignoreCase = true) -> {
                baseDate.plusDays(1)
            }
            rule.equals("WEEKDAYS", ignoreCase = true) -> {
                var next = baseDate.plusDays(1)
                while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                    next = next.plusDays(1)
                }
                next
            }
            rule.equals("WEEKLY", ignoreCase = true) -> {
                baseDate.plusWeeks(1)
            }
            rule.equals("MONTHLY", ignoreCase = true) -> {
                baseDate.plusMonths(1)
            }
            rule.equals("YEARLY", ignoreCase = true) -> {
                baseDate.plusYears(1)
            }
            rule.startsWith("CUSTOM:", ignoreCase = true) -> {
                parseCustomRule(rule, baseDate)
            }
            else -> baseDate.plusDays(1)
        }

        return DateUtils.localDateToEpochMillis(nextDate)
    }

    private fun parseCustomRule(rule: String, baseDate: LocalDate): LocalDate {
        // e.g. "CUSTOM:interval=2;unit=DAYS" or "CUSTOM:interval=3;unit=WEEKS"
        return try {
            val parts = rule.removePrefix("CUSTOM:").split(";")
            var interval = 1L
            var unit = "DAYS"

            for (part in parts) {
                val kv = part.split("=")
                if (kv.size == 2) {
                    when (kv[0].trim().lowercase()) {
                        "interval" -> interval = kv[1].trim().toLongOrNull() ?: 1L
                        "unit" -> unit = kv[1].trim().uppercase()
                    }
                }
            }

            when (unit) {
                "DAYS", "DAY" -> baseDate.plusDays(interval)
                "WEEKS", "WEEK" -> baseDate.plusWeeks(interval)
                "MONTHS", "MONTH" -> baseDate.plusMonths(interval)
                else -> baseDate.plusDays(interval)
            }
        } catch (e: Exception) {
            baseDate.plusDays(1)
        }
    }
}

