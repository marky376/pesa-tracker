package com.example.mpesatracker.sms

import android.content.Context
import android.provider.Telephony
import com.example.mpesatracker.domain.RawSms

/**
 * Reads M-Pesa messages from the phone's SMS inbox through Android's
 * content provider. Needs the READ_SMS permission, which the Dashboard
 * asks for before calling this. Throws SecurityException without it.
 */
class SmsReader(private val context: Context) {

    fun readMpesaMessages(): List<RawSms> {
        val messages = mutableListOf<RawSms>()
        // Column names in the SMS inbox table.
        val projection = arrayOf("body", "date")
        // Safaricom sends from "MPESA"; the body check catches renamed senders.
        val selection = "address = ? OR body LIKE ?"
        val args = arrayOf("MPESA", "%M-PESA%")

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            selection,
            args,
            "date DESC",
        )?.use { cursor ->
            val bodyColumn = cursor.getColumnIndexOrThrow("body")
            val dateColumn = cursor.getColumnIndexOrThrow("date")
            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyColumn) ?: continue
                messages += RawSms(body = body, receivedAt = cursor.getLong(dateColumn))
            }
        }
        return messages
    }
}
