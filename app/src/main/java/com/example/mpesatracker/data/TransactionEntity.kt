package com.example.mpesatracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mpesatracker.domain.Category
import com.example.mpesatracker.domain.Source
import com.example.mpesatracker.domain.TransactionType

/**
 * One row in the "transactions" table.
 *
 * The M-Pesa code has a unique index, so importing the same SMS twice
 * never creates a duplicate. Manual entries have no code (null), and
 * SQLite allows any number of nulls in a unique column.
 */
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["mpesaCode"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mpesaCode: String?,
    val type: TransactionType,
    val category: Category,
    val amount: Double,
    val fee: Double = 0.0,
    val counterparty: String,
    val account: String? = null,
    val timestamp: Long,
    val source: Source,
)

/** Result row for the "spending per category" query. */
data class CategoryTotal(val category: Category, val total: Double)
