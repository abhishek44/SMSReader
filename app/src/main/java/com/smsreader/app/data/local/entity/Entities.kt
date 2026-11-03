package com.smsreader.app.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.smsreader.app.domain.model.AccountType
import com.smsreader.app.domain.model.BudgetPeriod
import com.smsreader.app.domain.model.TransactionType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "account_type") val accountType: AccountType,
    val bank: String? = null,
    @ColumnInfo(name = "card_last4") val cardLast4: String? = null,
    val currency: String = "INR",
    @ColumnInfo(name = "opening_balance_minor") val openingBalanceMinor: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["name", "parent_id"], unique = true),
        Index(value = ["level"]),
        Index(value = ["parent_id"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val level: Int,
    @ColumnInfo(name = "parent_id") val parentId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

enum class SmsReviewStatus { PENDING, CONFIRMED, IGNORED }

@Entity(
    tableName = "sms_review_items",
    indices = [
        Index(value = ["source_sms_id"], unique = true),
        Index(value = ["status"]),
        Index(value = ["received_at"])
    ]
)
data class SmsReviewItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "source_sms_id") val sourceSmsId: Long,
    val sender: String? = null,
    val body: String,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    @ColumnInfo(name = "detected_amount_minor") val detectedAmountMinor: Long,
    @ColumnInfo(name = "detected_currency") val detectedCurrency: String = "INR",
    @ColumnInfo(name = "detected_merchant") val detectedMerchant: String? = null,
    @ColumnInfo(name = "suggested_type") val suggestedType: TransactionType? = null,
    @ColumnInfo(name = "account_hint_last4") val accountHintLast4: String? = null,
    val status: SmsReviewStatus = SmsReviewStatus.PENDING,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["l1_category_id"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["l2_category_id"], onDelete = ForeignKey.NO_ACTION)
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["transaction_date"]),
        Index(value = ["l1_category_id"]),
        Index(value = ["l2_category_id"]),
        Index(value = ["statement_date"]),
        Index(value = ["source_sms_id"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    @ColumnInfo(name = "transaction_date") val transactionDate: Long,
    val description: String,
    @ColumnInfo(name = "merchant_name") val merchantName: String? = null,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String = "INR",
    @ColumnInfo(name = "transaction_type") val transactionType: TransactionType,
    @ColumnInfo(name = "l1_category_id") val l1CategoryId: Long,
    @ColumnInfo(name = "l2_category_id") val l2CategoryId: Long? = null,
    @ColumnInfo(name = "statement_date") val statementDate: Long? = null,
    @ColumnInfo(name = "source_file") val sourceFile: String? = null,
    @ColumnInfo(name = "source_sms_id") val sourceSmsId: Long? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["l1_category_id"], onDelete = ForeignKey.NO_ACTION)
    ],
    indices = [
        Index(value = ["l1_category_id"]),
        Index(value = ["l1_category_id", "period"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "l1_category_id") val l1CategoryId: Long,
    @ColumnInfo(name = "amount_limit_minor") val amountLimitMinor: Long,
    val period: BudgetPeriod,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

@Entity(tableName = "sms_sync_state")
data class SmsSyncStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "initial_import_complete") val initialImportComplete: Boolean = false,
    @ColumnInfo(name = "last_scan_at") val lastScanAt: Long? = null
) {
    companion object { const val SINGLETON_ID = 1 }
}
