package com.example.mpesatracker.domain

// This "domain" package is plain Kotlin with no Android imports, so the parser
// and categoriser can be unit tested on a normal computer without a phone.

/** Spending categories shown on the dashboard. INCOME is only for money received. */
enum class Category(val label: String) {
    FOOD("Food"),
    TRANSPORT("Transport"),
    AIRTIME("Airtime"),
    BILLS("Bills"),
    OTHER("Other"),
    INCOME("Money in");

    companion object {
        /** The categories a user can pick for money going out. */
        val expenseCategories: List<Category> = entries.filter { it != INCOME }
    }
}

/** The kind of M-Pesa transaction, worked out from the wording of the SMS. */
enum class TransactionType(val label: String, val isIncome: Boolean = false) {
    SEND_MONEY("Send money"),
    PAYBILL("Paybill"),
    BUY_GOODS("Buy goods (Till)"),
    AIRTIME("Airtime"),
    WITHDRAW("Cash withdrawal"),
    RECEIVED("Money received", isIncome = true),
    MANUAL("Manual entry"),
}

/** Where a transaction came from. */
enum class Source(val label: String) {
    SMS("M-Pesa SMS"),
    MANUAL("Added by hand"),
    SAMPLE("Sample data"),
}

/** One SMS as read from the phone's inbox: its text and when it arrived. */
data class RawSms(val body: String, val receivedAt: Long)

/** The useful fields pulled out of one M-Pesa confirmation SMS. */
data class ParsedMpesa(
    val code: String,            // e.g. "QK12ABC3DE", unique per transaction
    val type: TransactionType,
    val amount: Double,
    val counterparty: String,    // who was paid, or who sent the money
    val account: String?,        // paybill account number, if any
    val timestamp: Long,         // milliseconds since 1970
    val fee: Double,             // Safaricom's transaction cost
    val balance: Double?,        // M-Pesa balance after the transaction
)
