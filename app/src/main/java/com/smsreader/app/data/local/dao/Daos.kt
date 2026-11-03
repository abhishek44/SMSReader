package com.smsreader.app.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.smsreader.app.data.local.entity.AccountEntity
import com.smsreader.app.data.local.entity.BudgetEntity
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.data.local.entity.SmsReviewItemEntity
import com.smsreader.app.data.local.entity.SmsReviewStatus
import com.smsreader.app.data.local.entity.SmsSyncStateEntity
import com.smsreader.app.data.local.entity.TransactionEntity
import com.smsreader.app.domain.model.BudgetPeriod
import com.smsreader.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE card_last4 = :last4 LIMIT 1")
    suspend fun findByLast4(last4: String): AccountEntity?

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AccountEntity): Long

    @Update
    suspend fun update(entity: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AccountEntity>)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY level, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE level = 1 ORDER BY name COLLATE NOCASE")
    fun observeLevel1(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND ((parent_id IS NULL AND :parentId IS NULL) OR parent_id = :parentId) LIMIT 1")
    suspend fun findByNameAndParent(name: String, parentId: Long?): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY level ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CategoryEntity): Long

    @Update
    suspend fun update(entity: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CategoryEntity>)
}

@Dao
interface SmsReviewDao {
    @Query("SELECT * FROM sms_review_items WHERE status = :status ORDER BY received_at DESC")
    fun observeByStatus(status: SmsReviewStatus): Flow<List<SmsReviewItemEntity>>

    @Query("SELECT * FROM sms_review_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SmsReviewItemEntity?

    @Query("SELECT * FROM sms_review_items")
    suspend fun getAll(): List<SmsReviewItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<SmsReviewItemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SmsReviewItemEntity>)

    @Query("UPDATE sms_review_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SmsReviewStatus)

    @Query(
        """
        SELECT
            r.id AS reviewItemId,
            r.sender AS sender,
            r.body AS body,
            r.received_at AS receivedAt,
            COALESCE(t.id, 0) AS transactionId,
            COALESCE(t.amount_minor, r.detected_amount_minor) AS amountMinor,
            COALESCE(t.currency, r.detected_currency, 'INR') AS currency,
            COALESCE(t.merchant_name, r.detected_merchant) AS merchantName,
            COALESCE(t.description, r.body) AS description,
            COALESCE(t.transaction_date, r.received_at) AS transactionDate,
            COALESCE(t.transaction_type, r.suggested_type, 'DEBIT') AS transactionType,
            COALESCE(a.name, 'Account') AS accountName,
            COALESCE(c1.name, 'Category') AS l1CategoryName,
            c2.name AS l2CategoryName
        FROM sms_review_items r
        LEFT JOIN transactions t ON (
            t.source_sms_id = r.source_sms_id
            OR (t.transaction_date = r.received_at AND t.amount_minor = r.detected_amount_minor)
        )
        LEFT JOIN accounts a ON a.id = t.account_id
        LEFT JOIN categories c1 ON c1.id = t.l1_category_id
        LEFT JOIN categories c2 ON c2.id = t.l2_category_id
        WHERE r.status = 'CONFIRMED'
        ORDER BY COALESCE(t.transaction_date, r.received_at) DESC
        """
    )
    fun observeConfirmedRows(): Flow<List<ConfirmedReviewItemRow>>
}

data class ConfirmedReviewItemRow(
    val reviewItemId: Long,
    val sender: String?,
    val body: String,
    val receivedAt: Long,
    val transactionId: Long,
    val amountMinor: Long,
    val currency: String,
    val merchantName: String?,
    val description: String,
    val transactionDate: Long,
    val transactionType: TransactionType,
    val accountName: String,
    val l1CategoryName: String,
    val l2CategoryName: String?
)

@Dao
interface SmsSyncStateDao {
    @Query("SELECT * FROM sms_sync_state WHERE id = 1 LIMIT 1")
    suspend fun get(): SmsSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SmsSyncStateEntity)
}

data class AnalyticsSummaryRow(
    val totalDebitMinor: Long,
    val totalCreditMinor: Long
)

data class CategorySpendRow(
    val categoryId: Long,
    val categoryName: String,
    val totalMinor: Long
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TransactionEntity): Long

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN transaction_type = 'DEBIT' THEN amount_minor ELSE 0 END), 0) AS totalDebitMinor,
            COALESCE(SUM(CASE WHEN transaction_type = 'CREDIT' THEN amount_minor ELSE 0 END), 0) AS totalCreditMinor
        FROM transactions
        WHERE transaction_date >= :startInclusive
          AND transaction_date < :endExclusive
          AND (:accountId IS NULL OR account_id = :accountId)
          AND (:categoryId IS NULL OR l1_category_id = :categoryId)
        """
    )
    fun observeSummary(
        startInclusive: Long,
        endExclusive: Long,
        accountId: Long?,
        categoryId: Long?
    ): Flow<AnalyticsSummaryRow>

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, COALESCE(SUM(t.amount_minor), 0) AS totalMinor
        FROM transactions t
        INNER JOIN categories c ON c.id = t.l1_category_id
        WHERE t.transaction_type = 'DEBIT'
          AND t.transaction_date >= :startInclusive
          AND t.transaction_date < :endExclusive
          AND (:accountId IS NULL OR t.account_id = :accountId)
          AND (:categoryId IS NULL OR t.l1_category_id = :categoryId)
        GROUP BY c.id, c.name
        ORDER BY totalMinor DESC
        """
    )
    fun observeCategorySpend(
        startInclusive: Long,
        endExclusive: Long,
        accountId: Long?,
        categoryId: Long?
    ): Flow<List<CategorySpendRow>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE transaction_date >= :startInclusive
          AND transaction_date < :endExclusive
          AND (:accountId IS NULL OR account_id = :accountId)
          AND (:categoryId IS NULL OR l1_category_id = :categoryId)
        ORDER BY transaction_date DESC
        LIMIT 100
        """
    )
    fun observeRecent(
        startInclusive: Long,
        endExclusive: Long,
        accountId: Long?,
        categoryId: Long?
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TransactionEntity>)
}

data class BudgetProgressRow(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val limitMinor: Long,
    val spentMinor: Long
)

@Dao
interface BudgetDao {
    @Query(
        """
        SELECT
            b.id AS budgetId,
            b.l1_category_id AS categoryId,
            c.name AS categoryName,
            b.amount_limit_minor AS limitMinor,
            COALESCE(SUM(CASE WHEN t.transaction_type = 'DEBIT' THEN t.amount_minor ELSE 0 END), 0) AS spentMinor
        FROM budgets b
        INNER JOIN categories c ON c.id = b.l1_category_id
        LEFT JOIN transactions t
          ON t.l1_category_id = b.l1_category_id
         AND t.transaction_date >= :startInclusive
         AND t.transaction_date < :endExclusive
        WHERE b.period = :period
          AND b.is_active = 1
        GROUP BY b.id, b.l1_category_id, c.name, b.amount_limit_minor
        ORDER BY c.name COLLATE NOCASE
        """
    )
    fun observeProgress(
        period: BudgetPeriod,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<BudgetProgressRow>>

    @Query("SELECT * FROM budgets WHERE l1_category_id = :categoryId AND period = :period LIMIT 1")
    suspend fun find(categoryId: Long, period: BudgetPeriod): BudgetEntity?

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: BudgetEntity): Long

    @Update
    suspend fun update(entity: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BudgetEntity>)
}
