package com.example.mpesatracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SampleDataAndTimeRangeTest {

    private val nairobi = TimeZone.getTimeZone("Africa/Nairobi")

    @Test
    fun everySampleMessageParses() {
        val messages = SampleData.messages(tz = nairobi)
        assertEquals(SampleData.size, messages.size)
        val parsed = messages.mapNotNull { MpesaSmsParser.parse(it.body, it.receivedAt, nairobi) }
        assertEquals("every sample should parse", messages.size, parsed.size)
        // Codes must be unique, because the database ignores duplicate codes.
        assertEquals(parsed.size, parsed.map { it.code }.toSet().size)
        // The date read from the text should be within a minute of when it was "sent".
        messages.zip(parsed).forEach { (sms, p) ->
            assertTrue(Math.abs(sms.receivedAt - p.timestamp) < 60_000)
        }
    }

    @Test
    fun thisMonthRange() {
        val now = Calendar.getInstance(nairobi).apply { clear(); set(2026, Calendar.NOVEMBER, 13, 11, 27) }.timeInMillis
        val range = TimeRange.thisMonth(now, nairobi)
        val start = Calendar.getInstance(nairobi).apply { clear(); set(2026, Calendar.NOVEMBER, 1, 0, 0, 0) }.timeInMillis
        val nextMonth = Calendar.getInstance(nairobi).apply { clear(); set(2026, Calendar.DECEMBER, 1, 0, 0, 0) }.timeInMillis
        assertEquals(start, range.start)
        assertEquals(nextMonth - 1, range.end)
    }
}
