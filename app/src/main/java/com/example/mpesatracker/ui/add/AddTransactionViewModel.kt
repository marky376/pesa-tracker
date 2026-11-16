package com.example.mpesatracker.ui.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mpesatracker.data.TransactionRepository
import com.example.mpesatracker.domain.Categorizer
import com.example.mpesatracker.domain.Category
import com.example.mpesatracker.domain.TransactionType
import com.example.mpesatracker.ui.PickerDates
import kotlinx.coroutines.launch

/** What the user has typed into the form so far. */
data class AddTransactionUiState(
    val isIncome: Boolean = false,
    val amountText: String = "",
    val description: String = "",
    val category: Category = Category.OTHER,
    val categoryPickedByUser: Boolean = false,
    val dateMillis: Long = System.currentTimeMillis(),
    val amountError: String? = null,
    val descriptionError: String? = null,
    val isSaving: Boolean = false,
)

/** Form logic for adding a cash or M-Pesa transaction by hand. */
class AddTransactionViewModel(private val repository: TransactionRepository) : ViewModel() {

    var uiState by mutableStateOf(AddTransactionUiState())
        private set

    fun setIncome(isIncome: Boolean) {
        uiState = uiState.copy(isIncome = isIncome)
    }

    /** Accepts only numbers with up to 2 decimal places, e.g. "250" or "99.50". */
    fun setAmount(text: String) {
        if (text.matches(Regex("""\d{0,9}(\.\d{0,2})?"""))) {
            uiState = uiState.copy(amountText = text, amountError = null)
        }
    }

    /** As the user types, the same keyword rules as SMS import suggest a category. */
    fun setDescription(text: String) {
        val suggested = Categorizer.categorize(TransactionType.MANUAL, text)
        uiState = uiState.copy(
            description = text,
            descriptionError = null,
            category = if (uiState.categoryPickedByUser) uiState.category else suggested,
        )
    }

    fun setCategory(category: Category) {
        uiState = uiState.copy(category = category, categoryPickedByUser = true)
    }

    fun setDateFromPicker(pickerMillis: Long) {
        uiState = uiState.copy(dateMillis = PickerDates.pickerToLocal(pickerMillis))
    }

    /** Checks the form, saves it, clears it, then calls [onSaved]. */
    fun save(onSaved: () -> Unit) {
        val amount = uiState.amountText.toDoubleOrNull()?.takeIf { it > 0.0 }
        val description = uiState.description.trim()
        if (amount == null || description.isEmpty()) {
            uiState = uiState.copy(
                amountError = if (amount == null) "Enter an amount above 0" else null,
                descriptionError = if (description.isEmpty()) "Enter who you paid or what it was for" else null,
            )
            return
        }
        uiState = uiState.copy(isSaving = true)
        viewModelScope.launch {
            repository.addManual(
                amount = amount,
                description = description,
                isIncome = uiState.isIncome,
                category = uiState.category,
                timestamp = uiState.dateMillis,
            )
            uiState = AddTransactionUiState()
            onSaved()
        }
    }
}
