package com.example.mpesatracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mpesatracker.data.CategoryTotal
import com.example.mpesatracker.data.TransactionEntity
import com.example.mpesatracker.data.TransactionRepository
import com.example.mpesatracker.domain.SampleData
import com.example.mpesatracker.domain.Source
import com.example.mpesatracker.domain.TimeRange
import com.example.mpesatracker.sms.SmsReader
import com.example.mpesatracker.ui.formatMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the Dashboard shows, in one object. */
data class DashboardUiState(
    val monthLabel: String = "",
    val totalSpent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val totalFees: Double = 0.0,
    val byCategory: List<CategoryTotal> = emptyList(),
    val recent: List<TransactionEntity> = emptyList(),
)

/**
 * MVVM: the ViewModel holds the screen's state and survives screen rotation.
 * It combines several live database queries into one [DashboardUiState].
 */
class DashboardViewModel(
    private val repository: TransactionRepository,
    private val smsReader: SmsReader,
) : ViewModel() {

    private val month = TimeRange.thisMonth()

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.totalSpent(month),
        repository.totalReceived(month),
        repository.totalFees(month),
        repository.spendingByCategory(month),
        repository.recent(5),
    ) { spent, received, fees, byCategory, recent ->
        DashboardUiState(
            monthLabel = formatMonth(month.start),
            totalSpent = spent,
            totalReceived = received,
            totalFees = fees,
            byCategory = byCategory,
            recent = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // One-off messages for the snackbar, e.g. "Imported 12 new transactions".
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Reads the SMS inbox (call only after READ_SMS is granted) and saves new transactions. */
    fun importFromSms() {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                // Reading the inbox is slow, so do it on a background thread.
                val sms = withContext(Dispatchers.IO) { smsReader.readMpesaMessages() }
                val added = repository.importSms(sms, Source.SMS)
                _messages.emit(
                    when {
                        sms.isEmpty() -> "No M-Pesa messages found on this phone"
                        added == 0 -> "Up to date: no new M-Pesa transactions"
                        else -> "Imported $added new transactions"
                    }
                )
            } catch (e: SecurityException) {
                _messages.emit("Allow SMS access to import your M-Pesa messages")
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            val added = repository.importSms(SampleData.messages(), Source.SAMPLE)
            _messages.emit(if (added == 0) "Sample data is already loaded" else "Loaded $added sample transactions")
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.deleteAll()
            _messages.emit("All transactions deleted")
        }
    }
}
