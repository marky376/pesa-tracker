package com.example.mpesatracker.data

import com.example.mpesatracker.domain.Categorizer
import com.example.mpesatracker.domain.Category
import com.example.mpesatracker.domain.MpesaSmsParser
import com.example.mpesatracker.domain.ParsedMpesa
import com.example.mpesatracker.domain.RawSms
import com.example.mpesatracker.domain.Source
import com.example.mpesatracker.domain.TimeRange
import com.example.mpesatracker.domain.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * The single place the ViewModels go for data. It hides the DAO and holds
 * the "business logic" of importing: parse each SMS, pick a category, save.
 */
class TransactionRepository(private val dao: TransactionDao) {

    fun recent(limit: Int): Flow<List<TransactionEntity>> = dao.observeRecent(limit)

    fun totalSpent(range: TimeRange): Flow<Double> = dao.observeTotalSpent(range.start, range.end)

    fun totalReceived(range: TimeRange): Flow<Double> = dao.observeTotalReceived(range.start, range.end)

    fun totalFees(range: TimeRange): Flow<Double> = dao.observeTotalFees(range.start, range.end)

    fun spendingByCategory(range: TimeRange): Flow<List<CategoryTotal>> =
        dao.observeSpendingByCategory(range.start, range.end)

    fun search(query: String, category: Category?, range: TimeRange): Flow<List<TransactionEntity>> =
        dao.search(query, category?.name, range.start, range.end)

    /**
     * Parses the messages and saves the M-Pesa ones.
     * @return how many new transactions were added (duplicates are skipped).
     */
    suspend fun importSms(messages: List<RawSms>, source: Source): Int {
        val entities = messages
            .mapNotNull { MpesaSmsParser.parse(it.body, it.receivedAt) }
            .map { it.toEntity(source) }
        if (entities.isEmpty()) return 0
        return dao.insertAll(entities).count { it != -1L }
    }

    suspend fun addManual(
        amount: Double,
        description: String,
        isIncome: Boolean,
        category: Category,
        timestamp: Long,
    ) {
        dao.insert(
            TransactionEntity(
                mpesaCode = null,
                type = if (isIncome) TransactionType.RECEIVED else TransactionType.MANUAL,
                category = if (isIncome) Category.INCOME else category,
                amount = amount,
                counterparty = description,
                timestamp = timestamp,
                source = Source.MANUAL,
            )
        )
    }

    suspend fun update(transaction: TransactionEntity) = dao.update(transaction)

    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)

    suspend fun deleteAll() = dao.deleteAll()

    private fun ParsedMpesa.toEntity(source: Source) = TransactionEntity(
        mpesaCode = code,
        type = type,
        category = Categorizer.categorize(type, counterparty, account),
        amount = amount,
        fee = fee,
        counterparty = counterparty,
        account = account,
        timestamp = timestamp,
        source = source,
    )
}
