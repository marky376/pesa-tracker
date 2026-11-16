package com.example.mpesatracker.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mpesatracker.data.TransactionEntity
import com.example.mpesatracker.domain.Category
import com.example.mpesatracker.domain.Period
import com.example.mpesatracker.ui.AppViewModelProvider
import com.example.mpesatracker.ui.components.ChipRow
import com.example.mpesatracker.ui.components.TransactionRow
import com.example.mpesatracker.ui.formatDateTime
import com.example.mpesatracker.ui.formatKsh

@Composable
fun HistoryScreen(
    showMessage: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    // The transaction whose details dialog is open, if any.
    var selected by remember { mutableStateOf<TransactionEntity?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = filter.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            placeholder = { Text("Search by name or M-Pesa code") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )

        ChipRow(Modifier.padding(top = 8.dp)) {
            Period.entries.forEach { period ->
                FilterChip(
                    selected = filter.period == period,
                    onClick = { viewModel.setPeriod(period) },
                    label = { Text(period.label) },
                )
            }
        }
        ChipRow {
            FilterChip(
                selected = filter.category == null,
                onClick = { viewModel.setCategory(null) },
                label = { Text("All") },
            )
            Category.entries.forEach { category ->
                FilterChip(
                    selected = filter.category == category,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category.label) },
                )
            }
        }

        val spent = transactions.filter { !it.type.isIncome }.sumOf { it.amount }
        Text(
            "${transactions.size} transactions · ${formatKsh(spent)} spent",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        if (transactions.isEmpty()) {
            Text(
                "No transactions match these filters.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionRow(transaction, onClick = { selected = transaction })
                HorizontalDivider()
            }
        }
    }

    selected?.let { transaction ->
        TransactionDetailDialog(
            transaction = transaction,
            onDismiss = { selected = null },
            onCategoryChange = { category ->
                viewModel.changeCategory(transaction, category)
                selected = transaction.copy(category = category)
            },
            onDelete = {
                viewModel.delete(transaction)
                selected = null
                showMessage("Transaction deleted")
            },
        )
    }
}

/** Shows every detail of one transaction and lets the user fix its category. */
@Composable
private fun TransactionDetailDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(transaction.counterparty) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DetailLine("Amount", formatKsh(transaction.amount))
                DetailLine("Type", transaction.type.label)
                DetailLine("Date", formatDateTime(transaction.timestamp))
                transaction.mpesaCode?.let { DetailLine("M-Pesa code", it) }
                transaction.account?.let { DetailLine("Account", it) }
                if (transaction.fee > 0) DetailLine("Transaction cost", formatKsh(transaction.fee))
                DetailLine("Source", transaction.source.label)

                if (!transaction.type.isIncome) {
                    Text(
                        "Category (tap to change)",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    ChipRow {
                        Category.expenseCategories.forEach { category ->
                            FilterChip(
                                selected = transaction.category == category,
                                onClick = { onCategoryChange(category) },
                                label = { Text(category.label) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row {
        Text("$label: ", fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}
