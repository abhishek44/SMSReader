package com.smsreader.app.data.repository

import androidx.room3.withWriteTransaction
import com.smsreader.app.data.local.AppDatabase
import com.smsreader.app.data.local.dao.AnalyticsSummaryRow
import com.smsreader.app.data.local.dao.BudgetProgressRow
import com.smsreader.app.data.local.dao.CategorySpendRow
import com.smsreader.app.data.local.dao.ConfirmedReviewItemRow
import com.smsreader.app.data.local.dao.SubcategorySpendRow
import com.smsreader.app.data.local.entity.AccountEntity
import com.smsreader.app.data.local.entity.BudgetEntity
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.data.local.entity.SmsReviewItemEntity
import com.smsreader.app.data.local.entity.SmsReviewStatus
import com.smsreader.app.data.local.entity.SmsSyncStateEntity
import com.smsreader.app.data.local.entity.TransactionEntity
import com.smsreader.app.data.sms.SmsDataSource
import com.smsreader.app.data.sms.SmsParseResult
import com.smsreader.app.data.sms.SmsParser
import com.smsreader.app.domain.model.AccountType
import com.smsreader.app.domain.model.BudgetPeriod
import com.smsreader.app.domain.model.DateRange
import com.smsreader.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId


data class SmsScanResult(
    val initialImport: Boolean,
    val messagesScanned: Int,
    val candidatesDetected: Int,
    val newlyAdded: Int
)

data class ConfirmTransactionInput(
    val reviewItemId: Long,
    val accountId: Long,
    val l1CategoryId: Long,
    val l2CategoryId: Long?,
    val amountMinor: Long,
    val currency: String,
    val merchantName: String?,
    val description: String,
    val transactionDate: Long,
    val transactionType: TransactionType,
    val notes: String? = null
)

class SmsReviewRepository(
    private val database: AppDatabase,
    private val smsDataSource: SmsDataSource,
    private val parser: SmsParser
) {
    private val reviewDao = database.smsReviewDao()
    private val syncDao = database.smsSyncStateDao()
    private val transactionDao = database.transactionDao()
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()

    fun observePending(): Flow<List<SmsReviewItemEntity>> = reviewDao.observeByStatus(SmsReviewStatus.PENDING)
    fun observeConfirmed(): Flow<List<ConfirmedReviewItemRow>> = reviewDao.observeConfirmedRows()

    suspend fun syncOnAppOpen(nowMillis: Long = System.currentTimeMillis()): SmsScanResult {
        val state = syncDao.get() ?: SmsSyncStateEntity()
        val isInitial = !state.initialImportComplete
        val start = if (isInitial) {
            Instant.ofEpochMilli(nowMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .minusMonths(3)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } else {
            val last = state.lastScanAt ?: nowMillis
            (last - ONE_DAY_MS).coerceAtLeast(0L)
        }
        return scanAndPersist(start, nowMillis + 1_000L, isInitial, nowMillis)
    }

    suspend fun rescanLastThreeMonths(nowMillis: Long = System.currentTimeMillis()): SmsScanResult {
        val start = Instant.ofEpochMilli(nowMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .minusMonths(3)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return scanAndPersist(start, nowMillis + 1_000L, initial = false, scannedAt = nowMillis)
    }

    private suspend fun scanAndPersist(
        startInclusive: Long,
        endExclusive: Long,
        initial: Boolean,
        scannedAt: Long
    ): SmsScanResult {
        val messages = smsDataSource.getMessages(startInclusive, endExclusive)
        val items = messages.mapNotNull { sms ->
            when (val parsed = parser.parse(sms)) {
                SmsParseResult.NotTransaction -> null
                is SmsParseResult.Candidate -> SmsReviewItemEntity(
                    sourceSmsId = sms.id,
                    sender = sms.sender,
                    body = sms.body,
                    receivedAt = sms.receivedAt,
                    detectedAmountMinor = parsed.amountMinor,
                    detectedCurrency = parsed.currency,
                    detectedMerchant = parsed.merchant,
                    suggestedType = parsed.suggestedType,
                    accountHintLast4 = parsed.accountLast4
                )
            }
        }
        val inserted = if (items.isEmpty()) emptyList() else reviewDao.insertAll(items)
        val previous = syncDao.get() ?: SmsSyncStateEntity()
        syncDao.upsert(
            previous.copy(
                initialImportComplete = previous.initialImportComplete || initial,
                lastScanAt = scannedAt
            )
        )
        return SmsScanResult(
            initialImport = initial,
            messagesScanned = messages.size,
            candidatesDetected = items.size,
            newlyAdded = inserted.count { it != -1L }
        )
    }

    suspend fun confirm(input: ConfirmTransactionInput): Long {
        require(input.amountMinor > 0L) { "Amount must be greater than zero" }
        require(input.description.isNotBlank()) { "Description is required" }
        val reviewItem = reviewDao.getById(input.reviewItemId)
            ?: error("Review item does not exist")
        check(reviewItem.status == SmsReviewStatus.PENDING) { "Review item is no longer pending" }
        val account = accountDao.getById(input.accountId) ?: error("Selected account does not exist")
        val l1 = categoryDao.getById(input.l1CategoryId) ?: error("Selected category does not exist")
        require(l1.level == 1) { "Main category is invalid" }
        if (input.l2CategoryId != null) {
            val l2 = categoryDao.getById(input.l2CategoryId) ?: error("Selected subcategory does not exist")
            require(l2.level == 2 && l2.parentId == l1.id) { "Subcategory does not belong to main category" }
        }
        return database.withWriteTransaction {
            val id = transactionDao.insert(
                TransactionEntity(
                    accountId = account.id,
                    transactionDate = input.transactionDate,
                    description = input.description.trim().take(500),
                    merchantName = input.merchantName?.trim()?.take(200)?.ifBlank { null },
                    amountMinor = input.amountMinor,
                    currency = input.currency.ifBlank { "INR" },
                    transactionType = input.transactionType,
                    l1CategoryId = input.l1CategoryId,
                    l2CategoryId = input.l2CategoryId,
                    sourceSmsId = reviewItem.sourceSmsId,
                    notes = input.notes?.trim()?.take(500)?.ifBlank { null }
                )
            )
            reviewDao.updateStatus(reviewItem.id, SmsReviewStatus.CONFIRMED)
            id
        }
    }

    suspend fun ignore(reviewItemId: Long) {
        val item = reviewDao.getById(reviewItemId) ?: return
        check(item.status != SmsReviewStatus.CONFIRMED) { "Confirmed items cannot be ignored" }
        reviewDao.updateStatus(reviewItemId, SmsReviewStatus.IGNORED)
    }

    companion object { private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L }
}


data class AnalyticsSnapshot(
    val summary: AnalyticsSummaryRow = AnalyticsSummaryRow(0L, 0L),
    val categorySpend: List<CategorySpendRow> = emptyList(),
    val recent: List<TransactionEntity> = emptyList()
)

class FinanceRepository(private val database: AppDatabase) {
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()

    fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.observeAll()
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    fun observeMainCategories(): Flow<List<CategoryEntity>> = categoryDao.observeLevel1()

    suspend fun addAccount(
        name: String,
        type: AccountType,
        bank: String?,
        last4: String?
    ): Long {
        require(name.isNotBlank()) { "Account name is required" }
        val cleanLast4 = last4?.filter(Char::isDigit)?.takeLast(4)?.ifBlank { null }
        require(cleanLast4 == null || cleanLast4.length == 4) { "Last 4 digits must contain exactly 4 digits" }
        return accountDao.insert(
            AccountEntity(
                name = name.trim().take(100),
                accountType = type,
                bank = bank?.trim()?.take(50)?.ifBlank { null },
                cardLast4 = cleanLast4
            )
        )
    }

    suspend fun updateAccount(
        id: Long,
        name: String,
        type: AccountType,
        bank: String?,
        last4: String?
    ) {
        require(name.isNotBlank()) { "Account name is required" }
        val existing = accountDao.getById(id) ?: error("Account does not exist")
        val cleanLast4 = last4?.filter(Char::isDigit)?.takeLast(4)?.ifBlank { null }
        require(cleanLast4 == null || cleanLast4.length == 4) { "Last 4 digits must contain exactly 4 digits" }
        accountDao.update(
            existing.copy(
                name = name.trim().take(100),
                accountType = type,
                bank = bank?.trim()?.take(50)?.ifBlank { null },
                cardLast4 = cleanLast4
            )
        )
    }

    suspend fun deleteAccount(id: Long) = database.withWriteTransaction {
        transactionDao.deleteByAccountId(id)
        accountDao.deleteById(id)
    }

    suspend fun addMainCategory(name: String): Long = addCategory(name, 1, null)

    suspend fun addSubCategory(name: String, parentId: Long): Long {
        val parent = categoryDao.getById(parentId) ?: error("Parent category does not exist")
        require(parent.level == 1) { "Parent must be a level-1 category" }
        return addCategory(name, 2, parentId)
    }

    suspend fun updateCategory(id: Long, name: String, parentId: Long?) {
        require(name.isNotBlank()) { "Category name is required" }
        val existing = categoryDao.getById(id) ?: error("Category does not exist")
        val cleanedName = name.trim().take(100)
        if (existing.level == 2 && parentId != null) {
            val parent = categoryDao.getById(parentId) ?: error("Parent category does not exist")
            require(parent.level == 1) { "Parent must be a level-1 category" }
        }
        categoryDao.update(
            existing.copy(
                name = cleanedName,
                parentId = if (existing.level == 2) parentId else null
            )
        )
    }

    suspend fun deleteCategory(id: Long) = database.withWriteTransaction {
        val category = categoryDao.getById(id) ?: return@withWriteTransaction
        if (category.level == 1) {
            budgetDao.deleteByCategoryId(id)
            categoryDao.deleteByParentId(id)
            transactionDao.deleteByL1CategoryId(id)
            categoryDao.deleteById(id)
        } else {
            transactionDao.clearL2Category(id)
            categoryDao.deleteById(id)
        }
    }

    private suspend fun addCategory(name: String, level: Int, parentId: Long?): Long {
        require(name.isNotBlank()) { "Category name is required" }
        val cleaned = name.trim().take(100)
        return database.withWriteTransaction {
            categoryDao.findByNameAndParent(cleaned, parentId)?.let { return@withWriteTransaction it.id }
            val insertedId = categoryDao.insert(
                CategoryEntity(name = cleaned, level = level, parentId = parentId)
            )
            if (insertedId != -1L) insertedId
            else categoryDao.findByNameAndParent(cleaned, parentId)?.id
                ?: error("Unable to create category")
        }
    }

    suspend fun ensureDefaultCategories() {
        val defaults = linkedMapOf(
            "Food & Dining" to listOf("Groceries", "Restaurants", "Food Delivery"),
            "Transport" to listOf("Fuel", "Cab", "Public Transport", "Parking"),
            "Shopping" to listOf("Online Shopping", "Clothing", "Electronics"),
            "Bills & Utilities" to listOf("Electricity", "Mobile", "Internet", "Subscriptions"),
            "Health" to listOf("Pharmacy", "Doctor", "Insurance"),
            "Entertainment" to listOf("Movies", "Games", "Events"),
            "Travel" to listOf("Flights", "Hotels"),
            "Education" to listOf("Courses", "Books"),
            "Transfer" to listOf("Account Transfer", "Credit Card Payment"),
            "Income" to listOf("Salary", "Interest", "Refund"),
            "Cash Withdrawal" to emptyList(),
            "Other" to emptyList()
        )
        defaults.forEach { (main, subs) ->
            val mainId = addMainCategory(main)
            subs.forEach { addSubCategory(it, mainId) }
        }
    }

    fun observeAnalytics(
        range: DateRange,
        accountId: Long?,
        categoryId: Long?
    ): Flow<AnalyticsSnapshot> = combine(
        transactionDao.observeSummary(range.startInclusive, range.endExclusive, accountId, categoryId),
        transactionDao.observeCategorySpend(range.startInclusive, range.endExclusive, accountId, categoryId),
        transactionDao.observeRecent(range.startInclusive, range.endExclusive, accountId, categoryId)
    ) { summary, categorySpend, recent ->
        AnalyticsSnapshot(summary, categorySpend, recent)
    }

    fun observeBudgetProgress(period: BudgetPeriod, range: DateRange): Flow<List<BudgetProgressRow>> =
        budgetDao.observeProgress(period, range.startInclusive, range.endExclusive)

    fun observeSubcategorySpend(range: DateRange): Flow<List<SubcategorySpendRow>> =
        budgetDao.observeSubcategorySpend(range.startInclusive, range.endExclusive)

    suspend fun saveBudget(categoryId: Long, period: BudgetPeriod, amountLimitMinor: Long) {
        require(amountLimitMinor > 0L) { "Budget amount must be greater than zero" }
        val category = categoryDao.getById(categoryId) ?: error("Category does not exist")
        require(category.level == 1) { "Budgets can only be assigned to main categories" }
        val existing = budgetDao.find(categoryId, period)
        if (existing == null) {
            budgetDao.insert(BudgetEntity(l1CategoryId = categoryId, amountLimitMinor = amountLimitMinor, period = period))
        } else {
            budgetDao.update(existing.copy(amountLimitMinor = amountLimitMinor, isActive = true))
        }
    }
}
