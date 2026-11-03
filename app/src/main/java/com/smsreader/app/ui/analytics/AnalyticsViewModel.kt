package com.smsreader.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsreader.app.data.local.entity.AccountEntity
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.data.repository.AnalyticsSnapshot
import com.smsreader.app.data.repository.FinanceRepository
import com.smsreader.app.domain.model.AnalyticsPeriod
import com.smsreader.app.domain.model.DateRanges
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn


data class AnalyticsFilter(
    val period: AnalyticsPeriod = AnalyticsPeriod.THIS_MONTH,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val customStart: Long = System.currentTimeMillis(),
    val customEnd: Long = System.currentTimeMillis()
)

data class AnalyticsUiState(
    val filter: AnalyticsFilter = AnalyticsFilter(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val snapshot: AnalyticsSnapshot = AnalyticsSnapshot()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(private val repository: FinanceRepository) : ViewModel() {
    private val filter = MutableStateFlow(AnalyticsFilter())

    private val snapshot = filter.flatMapLatest { f ->
        val range = when (f.period) {
            AnalyticsPeriod.THIS_MONTH -> DateRanges.thisMonth()
            AnalyticsPeriod.THIS_YEAR -> DateRanges.thisYear()
            AnalyticsPeriod.CUSTOM -> DateRanges.custom(f.customStart, f.customEnd)
        }
        repository.observeAnalytics(range, f.accountId, f.categoryId)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        filter,
        repository.observeAccounts(),
        repository.observeCategories(),
        snapshot
    ) { f, accounts, categories, data ->
        AnalyticsUiState(f, accounts, categories, data)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun setPeriod(period: AnalyticsPeriod) { filter.value = filter.value.copy(period = period) }
    fun setAccount(id: Long?) { filter.value = filter.value.copy(accountId = id) }
    fun setCategory(id: Long?) { filter.value = filter.value.copy(categoryId = id) }
    fun setCustomDates(start: Long, end: Long) {
        filter.value = filter.value.copy(period = AnalyticsPeriod.CUSTOM, customStart = start, customEnd = end)
    }
}
