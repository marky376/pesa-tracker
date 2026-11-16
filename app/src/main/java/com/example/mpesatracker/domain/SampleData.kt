package com.example.mpesatracker.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Made-up M-Pesa messages for demos on an emulator, which has no real SMS.
 * They go through the same parser as real messages, so loading them also
 * shows the parser working. Dates are relative to "now" so the dashboard
 * always has data for the current month. The codes start with "SMP" so they
 * never clash with real transaction codes.
 */
object SampleData {

    private const val HOUR_MS = 60L * 60 * 1000

    /** Each entry: how many hours ago, and a function that builds the SMS text. */
    private val templates: List<Pair<Double, (code: String, on: String) -> String>> = listOf(
        3.0 to { c, on -> "$c Confirmed. Ksh350.00 paid to JAVA HOUSE KIMATHI STREET. $on.New M-PESA balance is Ksh4,120.50. Transaction cost, Ksh0.00." },
        14.0 to { c, on -> "$c confirmed.You bought Ksh100.00 of airtime $on.New M-PESA balance is Ksh4,470.50. Transaction cost, Ksh0.00." },
        29.0 to { c, on -> "$c Confirmed. Ksh480.00 paid to NAIVAS SUPERMARKET THIKA ROAD. $on.New M-PESA balance is Ksh4,570.50. Transaction cost, Ksh0.00." },
        43.0 to { c, on -> "$c Confirmed. Ksh150.00 sent to PETER OTIENO 0711222333 $on. New M-PESA balance is Ksh5,050.50. Transaction cost, Ksh7.00." },
        60.0 to { c, on -> "$c Confirmed. Ksh1,000.00 sent to KPLC PREPAID for account 54321098765 $on New M-PESA balance is Ksh5,207.50. Transaction cost, Ksh0.00." },
        74.0 to { c, on -> "$c Confirmed. Ksh620.00 paid to LITTLE CAB LTD. $on.New M-PESA balance is Ksh6,207.50. Transaction cost, Ksh0.00." },
        94.0 to { c, on -> "$c Confirmed. Ksh120.00 paid to SUPER METRO SACCO. $on.New M-PESA balance is Ksh6,827.50. Transaction cost, Ksh0.00." },
        110.0 to { c, on -> "$c Confirmed.$on" + "Withdraw Ksh2,000.00 from 012345 - JOE AGENT Kenyatta Ave New M-PESA balance is Ksh6,947.50. Transaction cost, Ksh29.00." },
        128.0 to { c, on -> "$c Confirmed. Ksh1,200.00 sent to ZUKU for account ZK998877 $on New M-PESA balance is Ksh8,976.50. Transaction cost, Ksh0.00." },
        144.0 to { c, on -> "$c confirmed.You bought Ksh50.00 of airtime for 254712345678 $on.New M-PESA balance is Ksh10,176.50. Transaction cost, Ksh0.00." },
        163.0 to { c, on -> "$c Confirmed. Ksh180.00 paid to MAMA NJERI KIBANDA. $on.New M-PESA balance is Ksh10,226.50. Transaction cost, Ksh0.00." },
        180.0 to { c, on -> "$c Confirmed. Ksh850.00 sent to NAIROBI WATER for account 1234567 $on New M-PESA balance is Ksh10,406.50. Transaction cost, Ksh0.00." },
        221.0 to { c, on -> "$c Confirmed. Ksh950.00 paid to CHICKEN INN MOI AVENUE. $on.New M-PESA balance is Ksh11,256.50. Transaction cost, Ksh0.00." },
        242.0 to { c, on -> "$c Confirmed. Ksh500.00 sent to JOHN KAMAU 0733444555 $on. New M-PESA balance is Ksh12,206.50. Transaction cost, Ksh7.00." },
        265.0 to { c, on -> "$c Confirmed.You have received Ksh2,500.00 from PETER OTIENO 0711222333 $on  New M-PESA balance is Ksh12,713.50." },
        298.0 to { c, on -> "$c Confirmed.You have received Ksh10,000.00 from MARY ACHIENG 0700111222 $on  New M-PESA balance is Ksh10,213.50." },
        // Two from last month, so the History date filters have something to show.
        840.0 to { c, on -> "$c Confirmed. Ksh400.00 paid to JAVA HOUSE KIMATHI STREET. $on.New M-PESA balance is Ksh213.50. Transaction cost, Ksh0.00." },
        960.0 to { c, on -> "$c Confirmed. Ksh1,000.00 sent to KPLC PREPAID for account 54321098765 $on New M-PESA balance is Ksh613.50. Transaction cost, Ksh0.00." },
    )

    val size: Int get() = templates.size

    fun messages(now: Long = System.currentTimeMillis(), tz: TimeZone = TimeZone.getDefault()): List<RawSms> {
        val dateFormat = SimpleDateFormat("d/M/yy", Locale.ENGLISH).apply { timeZone = tz }
        val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH).apply { timeZone = tz }
        return templates.mapIndexed { index, (hoursAgo, build) ->
            val time = now - (hoursAgo * HOUR_MS).toLong()
            val code = String.format(Locale.ROOT, "SMP%07d", index + 1)
            val on = "on ${dateFormat.format(Date(time))} at ${timeFormat.format(Date(time))}"
            RawSms(body = build(code, on), receivedAt = time)
        }
    }
}
