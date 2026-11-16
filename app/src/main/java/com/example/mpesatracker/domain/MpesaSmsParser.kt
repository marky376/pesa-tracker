package com.example.mpesatracker.domain

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Turns the text of a Safaricom M-PESA confirmation SMS into a [ParsedMpesa].
 *
 * Every confirmation starts with a 10 character transaction code followed by
 * "Confirmed". The rest of the wording tells us the type, for example:
 *  - "Ksh1,500.00 sent to JOHN DOE 0712345678 on 12/11/24 at 7:45 PM"  -> send money
 *  - "Ksh2,000.00 sent to KPLC PREPAID for account 1234 on ..."         -> paybill
 *  - "Ksh350.00 paid to JAVA HOUSE. on ..."                             -> buy goods
 *  - "You have received Ksh5,000.00 from JANE ... on ..."               -> received
 *  - "You bought Ksh100.00 of airtime on ..."                           -> airtime
 *  - "Withdraw Ksh2,000.00 from 012345 - AGENT NAME New M-PESA ..."     -> withdrawal
 *
 * Each pattern is a regular expression. Anything we don't recognise
 * (failed transactions, Fuliza notices, adverts) returns null and is skipped.
 */
object MpesaSmsParser {

    // "Ksh1,500.00" or "Ksh 1,500.00" -> captures "1,500.00"
    private const val MONEY = """Ksh\s?([\d,]+(?:\.\d{1,2})?)"""

    private val CODE = Regex("""^\s*([A-Z0-9]{10})\s+[Cc]onfirmed""")
    private val DATE_TIME = Regex("""on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[AaPp][Mm])""")
    private val FEE = Regex("""Transaction cost,?\s*$MONEY""")
    private val BALANCE = Regex("""balance is\s*$MONEY""")

    // The order matters: paybill must be checked before plain "sent to".
    private val RECEIVED = Regex("""You have received\s+$MONEY\s+from\s+(.+?)\s+on\s+\d""")
    private val AIRTIME = Regex("""You bought\s+$MONEY\s+of airtime(?:\s+for\s+(\d+))?""")
    private val WITHDRAW = Regex("""Withdraw\s+$MONEY\s+from\s+(.+?)\s+New M-PESA""")
    private val PAYBILL = Regex("""$MONEY\s+sent to\s+(.+?)\s+for account\s+(.+?)\s+on\s+\d""")
    private val BUY_GOODS = Regex("""$MONEY\s+paid to\s+(.+?)\.?\s+on\s+\d""")
    private val SEND_MONEY = Regex("""$MONEY\s+sent to\s+(.+?)\s+on\s+\d""")

    // A phone number (possibly masked, e.g. 0712***678) at the end of a name.
    private val TRAILING_PHONE = Regex("""\s+\+?[\d*]{6,}$""")
    // An agent number at the start, e.g. "012345 - JOE AGENT".
    private val LEADING_AGENT_NUMBER = Regex("""^\d+\s*-\s*""")

    /**
     * @param body the SMS text
     * @param receivedAt when the phone received it, used if the text has no date
     * @param timeZone time zone used to read the date in the message
     */
    fun parse(
        body: String,
        receivedAt: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): ParsedMpesa? {
        val text = body.replace(Regex("""\s+"""), " ").trim()
        val code = CODE.find(text)?.groupValues?.get(1) ?: return null

        // Work out the type, amount and who the money went to / came from.
        val type: TransactionType
        val amount: Double
        var counterparty: String
        var account: String? = null

        val received = RECEIVED.find(text)
        val airtime = AIRTIME.find(text)
        val withdraw = WITHDRAW.find(text)
        val paybill = PAYBILL.find(text)
        val buyGoods = BUY_GOODS.find(text)
        val send = SEND_MONEY.find(text)

        when {
            received != null -> {
                type = TransactionType.RECEIVED
                amount = money(received.groupValues[1])
                counterparty = cleanName(received.groupValues[2])
            }
            airtime != null -> {
                type = TransactionType.AIRTIME
                amount = money(airtime.groupValues[1])
                val forNumber = airtime.groupValues[2]
                counterparty = if (forNumber.isEmpty()) "Safaricom airtime" else "Airtime for $forNumber"
            }
            withdraw != null -> {
                type = TransactionType.WITHDRAW
                amount = money(withdraw.groupValues[1])
                counterparty = cleanName(withdraw.groupValues[2].replace(LEADING_AGENT_NUMBER, ""))
            }
            paybill != null -> {
                type = TransactionType.PAYBILL
                amount = money(paybill.groupValues[1])
                counterparty = cleanName(paybill.groupValues[2])
                account = paybill.groupValues[3].trim().trimEnd('.')
            }
            buyGoods != null -> {
                type = TransactionType.BUY_GOODS
                amount = money(buyGoods.groupValues[1])
                counterparty = cleanName(buyGoods.groupValues[2])
            }
            send != null -> {
                type = TransactionType.SEND_MONEY
                amount = money(send.groupValues[1])
                counterparty = cleanName(send.groupValues[2])
            }
            else -> return null
        }
        if (counterparty.isEmpty()) counterparty = type.label

        val timestamp = DATE_TIME.find(text)?.let {
            parseDate(it.groupValues[1], it.groupValues[2], timeZone)
        } ?: receivedAt

        return ParsedMpesa(
            code = code,
            type = type,
            amount = amount,
            counterparty = counterparty,
            account = account,
            timestamp = timestamp,
            fee = FEE.find(text)?.let { money(it.groupValues[1]) } ?: 0.0,
            balance = BALANCE.find(text)?.let { money(it.groupValues[1]) },
        )
    }

    /** "1,500.00" -> 1500.0 */
    private fun money(text: String): Double = text.replace(",", "").toDouble()

    private fun cleanName(raw: String): String =
        raw.trim().trimEnd('.').replace(TRAILING_PHONE, "").trim()

    /** Reads "12/11/24" + "7:45 PM" (day/month/year) as a time in [timeZone]. */
    private fun parseDate(date: String, time: String, timeZone: TimeZone): Long? {
        val format = SimpleDateFormat("d/M/yy h:mm a", Locale.ENGLISH).apply {
            this.timeZone = timeZone
            isLenient = false
        }
        // Normalise "7:45PM" and "7:45 pm" to "7:45 PM".
        val normalisedTime = time.uppercase(Locale.ENGLISH).replace(Regex("""\s*(AM|PM)"""), " \$1")
        return try {
            format.parse("$date $normalisedTime")?.time
        } catch (e: ParseException) {
            null
        }
    }
}
