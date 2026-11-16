package com.example.mpesatracker.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mpesatracker.domain.Category
import com.example.mpesatracker.ui.AppViewModelProvider
import com.example.mpesatracker.ui.PickerDates
import com.example.mpesatracker.ui.components.ChipRow
import com.example.mpesatracker.ui.formatDay

/** Manual entry form, for cash spending or M-Pesa messages that were deleted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onSaved: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state = viewModel.uiState
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ChipRow {
            FilterChip(
                selected = !state.isIncome,
                onClick = { viewModel.setIncome(false) },
                label = { Text("Money out") },
            )
            FilterChip(
                selected = state.isIncome,
                onClick = { viewModel.setIncome(true) },
                label = { Text("Money in") },
            )
        }

        OutlinedTextField(
            value = state.amountText,
            onValueChange = viewModel::setAmount,
            label = { Text("Amount (Ksh)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = state.amountError != null,
            supportingText = { state.amountError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::setDescription,
            label = { Text(if (state.isIncome) "Received from" else "Paid to / what for") },
            singleLine = true,
            isError = state.descriptionError != null,
            supportingText = { state.descriptionError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )

        if (!state.isIncome) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Picked from what you type. Tap to change it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChipRow {
                    Category.expenseCategories.forEach { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text(category.label) },
                        )
                    }
                }
            }
        }

        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Date: ${formatDay(state.dateMillis)}")
        }

        Button(
            onClick = { viewModel.save(onSaved) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save transaction")
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = PickerDates.localToPicker(state.dateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.setDateFromPicker(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
