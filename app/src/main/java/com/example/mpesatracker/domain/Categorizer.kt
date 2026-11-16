package com.example.mpesatracker.domain

/**
 * Puts a transaction into a [Category] using simple keyword rules.
 *
 * The rules look for whole words in the name of whoever was paid (and the
 * paybill account). The first category with a matching keyword wins. If no
 * keyword matches we fall back on the transaction type, e.g. paybills are
 * usually bills. The user can always change the category in the History screen.
 */
object Categorizer {

    // Checked in this order. Keywords are matched as whole words, ignoring case.
    private val rules: List<Pair<Category, List<String>>> = listOf(
        Category.AIRTIME to listOf("AIRTIME", "BUNDLES", "DATA BUNDLE", "SAFARICOM DATA"),
        Category.TRANSPORT to listOf(
            "UBER", "BOLT", "LITTLE CAB", "FARAS", "MATATU", "SUPER METRO", "EMBASSAVA",
            "SHELL", "RUBIS", "TOTALENERGIES", "OLA ENERGY", "PETROL", "FUEL", "PARKING",
            "EASY COACH", "ENA COACH", "MASH", "SGR", "KENYA RAILWAYS", "BODA", "TAXI", "NTSA",
        ),
        Category.FOOD to listOf(
            "JAVA", "KFC", "CHICKEN INN", "PIZZA", "ARTCAFFE", "CAFE", "RESTAURANT", "HOTEL",
            "KIBANDA", "BUTCHERY", "BAKERY", "FOOD", "FOODS", "EATERY", "GRILL", "NYAMA",
            "NAIVAS", "QUICKMART", "CARREFOUR", "CHANDARANA", "MAGUNAS", "CLEANSHELF",
            "SUPERMARKET", "GROCERY", "GROCERIES", "MAMA MBOGA", "GLOVO",
        ),
        Category.BILLS to listOf(
            "KPLC", "KENYA POWER", "WATER", "ZUKU", "SAFARICOM HOME", "FAIBA", "DSTV", "GOTV",
            "STARTIMES", "SHOWMAX", "NETFLIX", "RENT", "NHIF", "SHA", "KRA", "SCHOOL",
            "UNIVERSITY", "COLLEGE", "FEES", "HELB", "INSURANCE",
        ),
    )

    // Pre-build one regular expression per keyword, e.g. \bKPLC\b
    private val compiled: List<Pair<Category, List<Regex>>> = rules.map { (category, words) ->
        category to words.map { Regex("""\b${Regex.escape(it)}\b""", RegexOption.IGNORE_CASE) }
    }

    fun categorize(type: TransactionType, counterparty: String, account: String? = null): Category {
        // Some types decide the category on their own.
        if (type.isIncome) return Category.INCOME
        if (type == TransactionType.AIRTIME) return Category.AIRTIME

        val text = listOfNotNull(counterparty, account).joinToString(" ")
        for ((category, patterns) in compiled) {
            if (patterns.any { it.containsMatchIn(text) }) return category
        }
        return when (type) {
            TransactionType.PAYBILL -> Category.BILLS
            else -> Category.OTHER
        }
    }
}
