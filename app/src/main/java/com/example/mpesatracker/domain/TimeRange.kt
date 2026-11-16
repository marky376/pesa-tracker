package com.example.mpesatracker.domain

import java.util.Calendar
import java.util.TimeZone

/** A span of time from [start] to [end], both in milliseconds and inclusive. */
data class TimeRange(val start: Long, val end: Long) {
    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000

        val ALL = TimeRange(0L, Long.MAX_VALUE)

        /** From the first moment of this calendar month to its last moment. */
        fun thisMonth(now: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()): TimeRange =
            monthContaining(now, tz, monthOffset = 0)

        fun lastMonth(now: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()): TimeRange =
            monthContaining(now, tz, monthOffset = -1)

        fun lastDays(days: Int, now: Long = System.currentTimeMillis()): TimeRange =
            TimeRange(now - days * DAY_MS, Long.MAX_VALUE)

        private fun monthContaining(now: Long, tz: TimeZone, monthOffset: Int): TimeRange {
            val cal = Calendar.getInstance(tz).apply {
                timeInMillis = now
                add(Calendar.MONTH, monthOffset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            return TimeRange(start, cal.timeInMillis - 1)
        }
    }
}

/** The date filters offered on the History screen. */
enum class Period(val label: String) {
    THIS_MONTH("This month"),
    LAST_30_DAYS("Last 30 days"),
    LAST_MONTH("Last month"),
    ALL("All time");

    fun range(now: Long = System.currentTimeMillis()): TimeRange = when (this) {
        THIS_MONTH -> TimeRange.thisMonth(now)
        LAST_30_DAYS -> TimeRange.lastDays(30, now)
        LAST_MONTH -> TimeRange.lastMonth(now)
        ALL -> TimeRange.ALL
    }
}
