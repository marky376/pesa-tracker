package com.example.mpesatracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mpesatracker.data.TransactionEntity
import com.example.mpesatracker.data.TransactionRepository
import com.example.mpesatracker.domain.Category
import com.example.mpesatracker.domain.Period
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The search box text and the chosen filters. */
data class HistoryFilter(
    val query: String = "",
    val category: Category? = null,   // null = all categories
    val period: Period = Period.ALL,
)

class HistoryViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    /** Re-runs the database search every time the filter changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> = _filter
        .flatMapLatest { f -> repository.search(f.query.trim(), f.category, f.period.range()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(query: String) = _filter.update { it.copy(query = query) }

    fun setCategory(category: Category?) = _filter.update { it.copy(category = category) }

    fun setPeriod(period: Period) = _filter.update { it.copy(period = period) }

    /** The user corrects the automatic category. */
    fun changeCategory(transaction: TransactionEntity, category: Category) {
        viewModelScope.launch { repository.update(transaction.copy(category = category)) }
    }

    fun delete(transaction: TransactionEntity) {
        viewModelScope.launch { repository.delete(transaction) }
    }
}
