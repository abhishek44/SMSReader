package com.smsreader.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsreader.app.data.local.dao.BudgetProgressRow
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.data.repository.FinanceRepository
import com.smsreader.app.domain.model.BudgetPeriod
import com.smsreader.app.domain.model.DateRanges
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class BudgetUiState(
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val categories: List<CategoryEntity> = emptyList(),
    val rows: List<BudgetProgressRow> = emptyList(),
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(private val repository: FinanceRepository) : ViewModel() {
    private val period = MutableStateFlow(BudgetPeriod.MONTHLY)
    private val message = MutableStateFlow<String?>(null)

    private val rows = period.flatMapLatest { p ->
        val range = if (p == BudgetPeriod.MONTHLY) DateRanges.thisMonth() else DateRanges.thisYear()
        repository.observeBudgetProgress(p, range)
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        period,
        repository.observeMainCategories(),
        rows,
        message
    ) { p, categories, budgetRows, msg -> BudgetUiState(p, categories, budgetRows, msg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun setPeriod(value: BudgetPeriod) { period.value = value }

    fun saveBudget(categoryId: Long, amountMinor: Long) {
        viewModelScope.launch {
            runCatching { repository.saveBudget(categoryId, period.value, amountMinor) }
                .onSuccess { message.value = "Budget saved" }
                .onFailure { message.value = it.message ?: "Unable to save budget" }
        }
    }

    fun clearMessage() { message.value = null }
}
