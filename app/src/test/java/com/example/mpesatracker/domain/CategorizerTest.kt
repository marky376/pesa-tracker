package com.example.mpesatracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizerTest {

    private fun cat(type: TransactionType, name: String, account: String? = null) =
        Categorizer.categorize(type, name, account)

    @Test
    fun food() {
        assertEquals(Category.FOOD, cat(TransactionType.BUY_GOODS, "JAVA HOUSE KIMATHI STREET"))
        assertEquals(Category.FOOD, cat(TransactionType.BUY_GOODS, "Naivas Supermarket Thika Road"))
    }

    @Test
    fun transport() {
        assertEquals(Category.TRANSPORT, cat(TransactionType.BUY_GOODS, "LITTLE CAB LTD"))
        assertEquals(Category.TRANSPORT, cat(TransactionType.BUY_GOODS, "SHELL WESTLANDS"))
    }

    @Test
    fun bills() {
        assertEquals(Category.BILLS, cat(TransactionType.PAYBILL, "KPLC PREPAID", "1234"))
        assertEquals(Category.BILLS, cat(TransactionType.PAYBILL, "NAIROBI WATER"))
    }

    @Test
    fun typeDecidesAirtimeAndIncome() {
        assertEquals(Category.AIRTIME, cat(TransactionType.AIRTIME, "Safaricom airtime"))
        assertEquals(Category.INCOME, cat(TransactionType.RECEIVED, "JAVA HOUSE"))
    }

    @Test
    fun fallbacks() {
        // Unknown paybill -> bills, unknown person -> other.
        assertEquals(Category.BILLS, cat(TransactionType.PAYBILL, "ACME LTD"))
        assertEquals(Category.OTHER, cat(TransactionType.SEND_MONEY, "PETER OTIENO"))
        assertEquals(Category.OTHER, cat(TransactionType.WITHDRAW, "JOE AGENT"))
    }

    @Test
    fun matchesWholeWordsOnly() {
        // "SHA" is a bills keyword but must not match inside "SHAKES".
        assertEquals(Category.OTHER, cat(TransactionType.SEND_MONEY, "SHAKES MWANGI"))
    }
}
