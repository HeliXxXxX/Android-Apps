package com.helix.budget.util

import java.util.Calendar

/** Helpers for "today" boundaries used by spent-today queries. */
object TimeRange {

    /** Epoch millis at 00:00 of the current local day. */
    fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** Epoch millis at 00:00 of tomorrow (exclusive end of today). */
    fun startOfTomorrow(): Long = startOfToday() + 86_400_000L

    /** Start-of-day millis for an arbitrary timestamp (used to group history). */
    fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
