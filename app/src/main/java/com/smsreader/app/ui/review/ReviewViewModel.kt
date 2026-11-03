package com.smsreader.app.ui.review

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsreader.app.data.local.dao.ConfirmedReviewItemRow
import com.smsreader.app.data.local.entity.AccountEntity
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.data.local.entity.SmsReviewItemEntity
import com.smsreader.app.data.repository.ConfirmTransactionInput
import com.smsreader.app.data.repository.DataBackupRepository
import com.smsreader.app.data.repository.FinanceRepository
import com.smsreader.app.data.repository.SmsReviewRepository
import com.smsreader.app.domain.model.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ReviewUiState(
    val pending: List<SmsReviewItemEntity> = emptyList(),
    val reviewed: List<ConfirmedReviewItemRow> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isSyncing: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ReviewViewModel(
    private val reviewRepository: SmsReviewRepository,
    private val financeRepository: FinanceRepository,
    private val dataBackupRepository: DataBackupRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewUiState())
    private var lastAutomaticSyncRequestAt: Long = 0L
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                reviewRepository.observePending(),
                reviewRepository.observeConfirmed(),
                financeRepository.observeAccounts(),
                financeRepository.observeCategories()
            ) { pending, reviewed, accounts, categories ->
                ReviewData(pending, reviewed, accounts, categories)
            }.collect { data ->
                _uiState.value = _uiState.value.copy(
                    pending = data.pending,
                    reviewed = data.reviewed,
                    accounts = data.accounts,
                    categories = data.categories
                )
            }
        }
    }

private data class ReviewData(
    val pending: List<SmsReviewItemEntity>,
    val reviewed: List<ConfirmedReviewItemRow>,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>
)

    fun onSmsPermissionAvailable(nowMillis: Long = System.currentTimeMillis()) {
        if (_uiState.value.isSyncing) return
        if (nowMillis - lastAutomaticSyncRequestAt < AUTO_SYNC_COOLDOWN_MS) return
        lastAutomaticSyncRequestAt = nowMillis
        sync(forceThreeMonths = false)
    }

    fun refresh() = sync(forceThreeMonths = false)
    fun rescanThreeMonths() = sync(forceThreeMonths = true)

    private fun sync(forceThreeMonths: Boolean) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null, message = null)
            runCatching {
                if (forceThreeMonths) reviewRepository.rescanLastThreeMonths()
                else reviewRepository.syncOnAppOpen()
            }.onSuccess { result ->
                val prefix = if (result.initialImport) "Initial 3-month import complete." else "SMS scan complete."
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    message = "$prefix ${result.newlyAdded} new transaction alerts found."
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = error.message ?: "Unable to scan SMS"
                )
            }
        }
    }

    fun confirm(input: ConfirmTransactionInput) {
        viewModelScope.launch {
            runCatching { reviewRepository.confirm(input) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Transaction saved", error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to save transaction") }
        }
    }

    fun ignore(id: Long) {
        viewModelScope.launch {
            runCatching { reviewRepository.ignore(id) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to ignore message") }
        }
    }

    fun addAccount(name: String, type: AccountType, bank: String?, last4: String?) {
        viewModelScope.launch {
            runCatching { financeRepository.addAccount(name, type, bank, last4) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Account added", error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to add account") }
        }
    }

    fun updateAccount(id: Long, name: String, type: AccountType, bank: String?, last4: String?) {
        viewModelScope.launch {
            runCatching { financeRepository.updateAccount(id, name, type, bank, last4) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Account updated", error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to update account") }
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            runCatching { financeRepository.deleteAccount(id) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Account deleted", error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to delete account") }
        }
    }

    fun addMainCategory(name: String) {
        viewModelScope.launch {
            runCatching { financeRepository.addMainCategory(name) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to add category") }
        }
    }

    fun addSubCategory(name: String, parentId: Long) {
        viewModelScope.launch {
            runCatching { financeRepository.addSubCategory(name, parentId) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to add subcategory") }
        }
    }

    fun updateCategory(id: Long, name: String, parentId: Long?) {
        viewModelScope.launch {
            runCatching { financeRepository.updateCategory(id, name, parentId) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Category updated", error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to update category") }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            runCatching { financeRepository.deleteCategory(id) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Category deleted", error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to delete category") }
        }
    }

    fun exportTransactionsCsv(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(message = "Exporting CSV…", error = null)
            runCatching {
                val outputStream = contentResolver.openOutputStream(uri)
                    ?: error("Unable to open output file")
                dataBackupRepository.exportTransactionsToCsv(outputStream).getOrThrow()
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "Exported $count transactions to CSV", error = null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message ?: "Failed to export CSV")
            }
        }
    }

    fun exportBackupJson(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(message = "Creating backup…", error = null)
            runCatching {
                val outputStream = contentResolver.openOutputStream(uri)
                    ?: error("Unable to open output file")
                dataBackupRepository.exportBackupToJson(outputStream).getOrThrow()
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "Backup complete ($count records saved)", error = null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message ?: "Failed to create backup")
            }
        }
    }

    fun importBackupJson(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(message = "Restoring backup…", error = null)
            runCatching {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: error("Unable to open backup file")
                dataBackupRepository.importBackupFromJson(inputStream).getOrThrow()
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "Backup restored ($count records imported)", error = null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message ?: "Failed to restore backup")
            }
        }
    }

    private companion object {
        const val AUTO_SYNC_COOLDOWN_MS = 5L * 60L * 1000L
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
