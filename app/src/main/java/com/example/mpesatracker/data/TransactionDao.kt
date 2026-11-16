package com.example.mpesatracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object: the SQL queries the app runs. Room checks these
 * queries when the app is compiled and generates the code that runs them.
 *
 * Functions returning Flow are "live": the screen updates by itself whenever
 * the table changes. Functions marked suspend run off the main thread.
 *
 * Money in has type 'RECEIVED'; everything else counts as spending.
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) FROM transactions " +
            "WHERE type != 'RECEIVED' AND timestamp BETWEEN :from AND :to"
    )
    fun observeTotalSpent(from: Long, to: Long): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) FROM transactions " +
            "WHERE type = 'RECEIVED' AND timestamp BETWEEN :from AND :to"
    )
    fun observeTotalReceived(from: Long, to: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(fee), 0.0) FROM transactions WHERE timestamp BETWEEN :from AND :to")
    fun observeTotalFees(from: Long, to: Long): Flow<Double>

    @Query(
        "SELECT category, SUM(amount) AS total FROM transactions " +
            "WHERE type != 'RECEIVED' AND timestamp BETWEEN :from AND :to " +
            "GROUP BY category ORDER BY total DESC"
    )
    fun observeSpendingByCategory(from: Long, to: Long): Flow<List<CategoryTotal>>

    /**
     * Search used by the History screen. An empty [query] or a null
     * [category] means "don't filter on this".
     */
    @Query(
        "SELECT * FROM transactions " +
            "WHERE (:query = '' OR counterparty LIKE '%' || :query || '%' OR mpesaCode LIKE '%' || :query || '%') " +
            "AND (:category IS NULL OR category = :category) " +
            "AND timestamp BETWEEN :from AND :to " +
            "ORDER BY timestamp DESC"
    )
    fun search(query: String, category: String?, from: Long, to: Long): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    /** IGNORE skips rows whose M-Pesa code is already saved; their id comes back as -1. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
