package com.example.mpesatracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Unit tests for [MpesaSmsParser], using message formats copied from real
 * Safaricom confirmations (names, numbers and codes changed).
 * Run them in Android Studio with right-click > Run, or ./gradlew test
 */
class MpesaSmsParserTest {

    private val nairobi = TimeZone.getTimeZone("Africa/Nairobi")
    private val fallback = 1_000L

    private fun parse(sms: String) = MpesaSmsParser.parse(sms, fallback, nairobi)

    private fun millis(year: Int, month: Int, day: Int, hour24: Int, minute: Int): Long =
        Calendar.getInstance(nairobi).apply {
            clear()
            set(year, month - 1, day, hour24, minute, 0)
        }.timeInMillis

    @Test
    fun sendMoney() {
        val t = parse(
            "QK12ABC3DE Confirmed. Ksh1,500.00 sent to JOHN DOE 0712345678 on 12/11/24 at 7:45 PM. " +
                "New M-PESA balance is Ksh3,250.50. Transaction cost, Ksh23.00. " +
                "Amount you can transact within the day is 498,500.00."
        )
        assertNotNull(t)
        t!!
        assertEquals("QK12ABC3DE", t.code)
        assertEquals(TransactionType.SEND_MONEY, t.type)
        assertEquals(1500.0, t.amount, 0.001)
        assertEquals("JOHN DOE", t.counterparty)
        assertEquals(23.0, t.fee, 0.001)
        assertEquals(3250.50, t.balance!!, 0.001)
        assertEquals(millis(2024, 11, 12, 19, 45), t.timestamp)
    }

    @Test
    fun sendMoneyWithMaskedPhoneNumber() {
        val t = parse(
            "TKD4H7J2LM Confirmed. Ksh200.00 sent to MARY WANJIRU 0722***456 on 3/1/25 at 10:05 AM. " +
                "New M-PESA balance is Ksh800.00. Transaction cost, Ksh0.00."
        )!!
        assertEquals("MARY WANJIRU", t.counterparty)
        assertEquals(millis(2025, 1, 3, 10, 5), t.timestamp)
    }

    @Test
    fun paybill() {
        val t = parse(
            "QK34XYZ9FG Confirmed. Ksh2,000.00 sent to KPLC PREPAID for account 12345678901 on 5/11/24 at 10:15 AM " +
                "New M-PESA balance is Ksh1,250.50. Transaction cost, Ksh0.00."
        )!!
        assertEquals(TransactionType.PAYBILL, t.type)
        assertEquals(2000.0, t.amount, 0.001)
        assertEquals("KPLC PREPAID", t.counterparty)
        assertEquals("12345678901", t.account)
        assertEquals(0.0, t.fee, 0.001)
    }

    @Test
    fun buyGoodsTill() {
        val t = parse(
            "QK56LMN7HJ Confirmed. Ksh350.00 paid to JAVA HOUSE KIMATHI STREET. on 8/11/24 at 1:05 PM." +
                "New M-PESA balance is Ksh900.50. Transaction cost, Ksh0.00."
        )!!
        assertEquals(TransactionType.BUY_GOODS, t.type)
        assertEquals(350.0, t.amount, 0.001)
        assertEquals("JAVA HOUSE KIMATHI STREET", t.counterparty)
        assertEquals(millis(2024, 11, 8, 13, 5), t.timestamp)
    }

    @Test
    fun moneyReceived() {
        val t = parse(
            "QK78PQR1ST Confirmed.You have received Ksh5,000.00 from JANE WANJIKU 0722000111 on 1/11/24 at 9:00 AM  " +
                "New M-PESA balance is Ksh6,250.50. Separate personal and business funds through Pochi la Biashara."
        )!!
        assertEquals(TransactionType.RECEIVED, t.type)
        assertEquals(5000.0, t.amount, 0.001)
        assertEquals("JANE WANJIKU", t.counterparty)
        assertEquals(millis(2024, 11, 1, 9, 0), t.timestamp)
    }

    @Test
    fun airtimeForSelf() {
        val t = parse(
            "QK90UVW2XY confirmed.You bought Ksh100.00 of airtime on 3/11/24 at 8:20 AM." +
                "New M-PESA balance is Ksh800.50. Transaction cost, Ksh0.00."
        )!!
        assertEquals(TransactionType.AIRTIME, t.type)
        assertEquals(100.0, t.amount, 0.001)
        assertEquals("Safaricom airtime", t.counterparty)
    }

    @Test
    fun airtimeForAnotherNumber() {
        val t = parse(
            "QL11AIR2TM confirmed.You bought Ksh50.00 of airtime for 254712345678 on 3/11/24 at 8:20 AM." +
                "New M-PESA balance is Ksh750.50. Transaction cost, Ksh0.00."
        )!!
        assertEquals(TransactionType.AIRTIME, t.type)
        assertEquals("Airtime for 254712345678", t.counterparty)
    }

    @Test
    fun cashWithdrawal() {
        val t = parse(
            "QK11ABC4ZZ Confirmed.on 4/11/24 at 6:30 PMWithdraw Ksh2,000.00 from 012345 - JOE AGENT Kenyatta Ave " +
                "New M-PESA balance is Ksh1,000.00. Transaction cost, Ksh29.00."
        )!!
        assertEquals(TransactionType.WITHDRAW, t.type)
        assertEquals(2000.0, t.amount, 0.001)
        assertEquals("JOE AGENT Kenyatta Ave", t.counterparty)
        assertEquals(29.0, t.fee, 0.001)
        assertEquals(millis(2024, 11, 4, 18, 30), t.timestamp)
    }

    @Test
    fun largeAmountWithCommas() {
        val t = parse(
            "QM22BIG3AM Confirmed. Ksh12,345.67 sent to MOI UNIVERSITY for account ADM1234 on 20/9/24 at 11:59 AM " +
                "New M-PESA balance is Ksh1,000.00. Transaction cost, Ksh108.00."
        )!!
        assertEquals(12345.67, t.amount, 0.001)
        assertEquals("ADM1234", t.account)
        assertEquals(108.0, t.fee, 0.001)
    }

    @Test
    fun fourDigitYearAndLowercaseTime() {
        val t = parse(
            "QN33YEAR4X Confirmed. Ksh75.00 paid to MAMA MBOGA. on 15/2/2025 at 6:10 pm.New M-PESA balance is Ksh10.00."
        )!!
        assertEquals(millis(2025, 2, 15, 18, 10), t.timestamp)
    }

    @Test
    fun unreadableDateFallsBackToWhenTheSmsArrived() {
        val t = parse("QP44BADDT2 Confirmed. Ksh60.00 sent to ANN 0712345678 on 99/99/24 at 7:00 AM.")!!
        assertEquals(fallback, t.timestamp)
    }

    @Test
    fun ignoresMessagesThatAreNotConfirmations() {
        assertNull(parse("Failed. You do not have enough money in your M-PESA account to send Ksh5,000.00."))
        assertNull(parse("Dear customer, get 1GB for Ksh50 today only. Dial *544#"))
        assertNull(parse("QR55FULIZA Confirmed. Fuliza M-PESA amount is Ksh100.00. Access fee charged Ksh1.00."))
        assertNull(parse(""))
    }
}
