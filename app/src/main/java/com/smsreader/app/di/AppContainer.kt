package com.smsreader.app.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.smsreader.app.data.local.AppDatabase
import com.smsreader.app.data.repository.DataBackupRepository
import com.smsreader.app.data.repository.FinanceRepository
import com.smsreader.app.data.repository.SmsReviewRepository
import com.smsreader.app.data.sms.AndroidSmsDataSource
import com.smsreader.app.data.sms.GenericTransactionSmsParser
import kotlinx.coroutines.Dispatchers

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = Room.databaseBuilder<AppDatabase>(appContext, "sms_reader.db")
        .setDriver(AndroidSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    val financeRepository = FinanceRepository(database)

    val dataBackupRepository = DataBackupRepository(database)

    val smsReviewRepository = SmsReviewRepository(
        database = database,
        smsDataSource = AndroidSmsDataSource(appContext),
        parser = GenericTransactionSmsParser()
    )
}
