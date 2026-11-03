package com.smsreader.app.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.smsreader.app.data.local.dao.AccountDao
import com.smsreader.app.data.local.dao.BudgetDao
import com.smsreader.app.data.local.dao.CategoryDao
import com.smsreader.app.data.local.dao.SmsReviewDao
import com.smsreader.app.data.local.dao.SmsSyncStateDao
import com.smsreader.app.data.local.dao.TransactionDao
import com.smsreader.app.data.local.entity.AccountEntity
import com.smsreader.app.data.local.entity.BudgetEntity
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.data.local.entity.SmsReviewItemEntity
import com.smsreader.app.data.local.entity.SmsSyncStateEntity
import com.smsreader.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        SmsReviewItemEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SmsSyncStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun smsReviewDao(): SmsReviewDao
    abstract fun smsSyncStateDao(): SmsSyncStateDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
}
