package com.example.mpesatracker.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mpesatracker.ui.AppViewModelProvider
import com.example.mpesatracker.ui.components.CategoryLegend
import com.example.mpesatracker.ui.components.SpendingDonutChart
import com.example.mpesatracker.ui.components.TransactionRow
import com.example.mpesatracker.ui.formatKsh

@Composable
fun DashboardScreen(
    showMessage: (String) -> Unit,
    onSeeAll: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Show the ViewModel's one-off messages as snackbars.
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { showMessage(it) }
    }

    // Asks Android for the SMS permission; the result arrives in the lambda.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.importFromSms()
        else showMessage("SMS permission denied. You can still add transactions by hand.")
    }

    val onImport: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.importFromSms()
        else permissionLauncher.launch(Manifest.permission.READ_SMS)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SummaryCard(state) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onImport, enabled = !isImporting, modifier = Modifier.weight(1f)) {
                    if (isImporting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Import M-Pesa SMS")
                }
                OutlinedButton(onClick = viewModel::loadSampleData, modifier = Modifier.weight(1f)) {
                    Text("Load sample data")
                }
            }
        }

        item { ChartCard(state) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Recent transactions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSeeAll) { Text("See all") }
            }
        }

        if (state.recent.isEmpty()) {
            item {
                Text(
                    "No transactions yet. Import your M-Pesa messages, load the sample data, " +
                        "or add one with the Add tab.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.recent, key = { it.id }) { transaction ->
                TransactionRow(transaction)
            }
            item {
                TextButton(onClick = { confirmClear = true }) {
                    Text("Delete all transactions", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all transactions?") },
            text = { Text("This removes everything from the app's database on this phone. Your SMS messages are not touched.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAll(); confirmClear = false }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SummaryCard(state: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Spent in ${state.monthLabel}", style = MaterialTheme.typography.labelLarge)
            Text(
                formatKsh(state.totalSpent),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.padding(4.dp))
            Row {
                SmallStat("Money in", formatKsh(state.totalReceived), Modifier.weight(1f))
                SmallStat("M-Pesa charges", formatKsh(state.totalFees), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ChartCard(state: DashboardUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Spending by category", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(4.dp))
            if (state.byCategory.isEmpty()) {
                Text(
                    "Nothing spent this month yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                        SpendingDonutChart(state.byCategory, Modifier.size(150.dp))
                        Text(
                            "${state.byCategory.size}\ncategories",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    CategoryLegend(state.byCategory, Modifier.weight(1f))
                }
            }
        }
    }
}
