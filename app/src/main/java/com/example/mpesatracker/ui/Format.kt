package com.example.mpesatracker.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Small helpers that turn numbers and dates into text for the screens.

fun formatKsh(amount: Double): String = String.format(Locale.US, "Ksh %,.2f", amount)

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(millis))

fun formatDay(millis: Long): String =
    SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault()).format(Date(millis))

fun formatMonth(millis: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(millis))

/**
 * The Material date picker works in UTC midnight values. These two functions
 * convert between that and a normal local time, keeping the calendar day.
 */
object PickerDates {
    fun localToPicker(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** Picked day at the current time of day, so entries keep a sensible order. */
    fun pickerToLocal(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return Calendar.getInstance().apply {
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
}
