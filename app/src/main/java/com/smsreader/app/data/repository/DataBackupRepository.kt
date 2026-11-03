package com.smsreader.app.data.repository

import androidx.room3.withWriteTransaction
import com.smsreader.app.data.local.AppDatabase
import com.smsreader.app.data.local.entity.AccountEntity
import com.smsreader.app.data.local.entity.BudgetEntity
import com.smsreader.app.data.local.entity.CategoryEntity
import com.smsreader.app.data.local.entity.SmsReviewItemEntity
import com.smsreader.app.data.local.entity.SmsReviewStatus
import com.smsreader.app.data.local.entity.TransactionEntity
import com.smsreader.app.domain.model.AccountType
import com.smsreader.app.domain.model.BudgetPeriod
import com.smsreader.app.domain.model.TransactionType
import com.smsreader.app.util.Dates
import com.smsreader.app.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

class DataBackupRepository(private val database: AppDatabase) {
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val smsReviewDao = database.smsReviewDao()

    suspend fun exportTransactionsToCsv(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val transactions = transactionDao.getAll()
            val accountsMap = accountDao.getAll().associateBy { it.id }
            val categoriesMap = categoryDao.getAll().associateBy { it.id }

            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("ID,Date,Merchant,Description,Amount,Currency,Type,Account,Main Category,Subcategory,Notes")
                writer.newLine()

                transactions.forEach { tx ->
                    val accountName = accountsMap[tx.accountId]?.name.orEmpty()
                    val l1Name = categoriesMap[tx.l1CategoryId]?.name.orEmpty()
                    val l2Name = tx.l2CategoryId?.let { categoriesMap[it]?.name }.orEmpty()
                    val amountFormatted = Money.format(tx.amountMinor, tx.currency)
                    val dateFormatted = Dates.format(tx.transactionDate)

                    val row = listOf(
                        tx.id.toString(),
                        escapeCsv(dateFormatted),
                        escapeCsv(tx.merchantName.orEmpty()),
                        escapeCsv(tx.description),
                        escapeCsv(amountFormatted),
                        escapeCsv(tx.currency),
                        escapeCsv(tx.transactionType.name),
                        escapeCsv(accountName),
                        escapeCsv(l1Name),
                        escapeCsv(l2Name),
                        escapeCsv(tx.notes.orEmpty())
                    ).joinToString(",")

                    writer.write(row)
                    writer.newLine()
                }
                writer.flush()
            }
            transactions.size
        }
    }

    suspend fun exportBackupToJson(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val accounts = accountDao.getAll()
            val categories = categoryDao.getAll()
            val transactions = transactionDao.getAll()
            val budgets = budgetDao.getAll()
            val reviewItems = smsReviewDao.getAll()

            val rootJson = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("appName", "SMSReader")
                put("exportedAt", System.currentTimeMillis())

                put("accounts", JSONArray().apply {
                    accounts.forEach { acc ->
                        put(JSONObject().apply {
                            put("id", acc.id)
                            put("name", acc.name)
                            put("accountType", acc.accountType.name)
                            put("bank", acc.bank ?: JSONObject.NULL)
                            put("cardLast4", acc.cardLast4 ?: JSONObject.NULL)
                            put("currency", acc.currency)
                            put("openingBalanceMinor", acc.openingBalanceMinor)
                            put("createdAt", acc.createdAt)
                        })
                    }
                })

                put("categories", JSONArray().apply {
                    categories.forEach { cat ->
                        put(JSONObject().apply {
                            put("id", cat.id)
                            put("name", cat.name)
                            put("level", cat.level)
                            put("parentId", cat.parentId ?: JSONObject.NULL)
                            put("createdAt", cat.createdAt)
                        })
                    }
                })

                put("transactions", JSONArray().apply {
                    transactions.forEach { tx ->
                        put(JSONObject().apply {
                            put("id", tx.id)
                            put("accountId", tx.accountId)
                            put("transactionDate", tx.transactionDate)
                            put("description", tx.description)
                            put("merchantName", tx.merchantName ?: JSONObject.NULL)
                            put("amountMinor", tx.amountMinor)
                            put("currency", tx.currency)
                            put("transactionType", tx.transactionType.name)
                            put("l1CategoryId", tx.l1CategoryId)
                            put("l2CategoryId", tx.l2CategoryId ?: JSONObject.NULL)
                            put("statementDate", tx.statementDate ?: JSONObject.NULL)
                            put("sourceFile", tx.sourceFile ?: JSONObject.NULL)
                            put("sourceSmsId", tx.sourceSmsId ?: JSONObject.NULL)
                            put("notes", tx.notes ?: JSONObject.NULL)
                            put("createdAt", tx.createdAt)
                        })
                    }
                })

                put("budgets", JSONArray().apply {
                    budgets.forEach { b ->
                        put(JSONObject().apply {
                            put("id", b.id)
                            put("l1CategoryId", b.l1CategoryId)
                            put("amountLimitMinor", b.amountLimitMinor)
                            put("period", b.period.name)
                            put("isActive", b.isActive)
                        })
                    }
                })

                put("smsReviewItems", JSONArray().apply {
                    reviewItems.forEach { r ->
                        put(JSONObject().apply {
                            put("id", r.id)
                            put("sourceSmsId", r.sourceSmsId)
                            put("sender", r.sender ?: JSONObject.NULL)
                            put("body", r.body)
                            put("receivedAt", r.receivedAt)
                            put("detectedAmountMinor", r.detectedAmountMinor)
                            put("detectedCurrency", r.detectedCurrency)
                            put("detectedMerchant", r.detectedMerchant ?: JSONObject.NULL)
                            put("suggestedType", r.suggestedType?.name ?: JSONObject.NULL)
                            put("accountHintLast4", r.accountHintLast4 ?: JSONObject.NULL)
                            put("status", r.status.name)
                            put("createdAt", r.createdAt)
                        })
                    }
                })
            }

            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(rootJson.toString(2))
                writer.flush()
            }

            accounts.size + categories.size + transactions.size + budgets.size + reviewItems.size
        }
    }

    suspend fun importBackupFromJson(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(jsonString)

            val version = root.optInt("version", 0)
            require(version in 1..BACKUP_VERSION) { "Unsupported backup version: $version" }

            val accountsJson = root.optJSONArray("accounts") ?: JSONArray()
            val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
            val transactionsJson = root.optJSONArray("transactions") ?: JSONArray()
            val budgetsJson = root.optJSONArray("budgets") ?: JSONArray()
            val reviewItemsJson = root.optJSONArray("smsReviewItems") ?: JSONArray()

            val accounts = (0 until accountsJson.length()).map { i ->
                val obj = accountsJson.getJSONObject(i)
                AccountEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    accountType = AccountType.valueOf(obj.getString("accountType")),
                    bank = if (obj.isNull("bank")) null else obj.getString("bank"),
                    cardLast4 = if (obj.isNull("cardLast4")) null else obj.getString("cardLast4"),
                    currency = obj.optString("currency", "INR"),
                    openingBalanceMinor = obj.optLong("openingBalanceMinor", 0L),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }

            val categories = (0 until categoriesJson.length()).map { i ->
                val obj = categoriesJson.getJSONObject(i)
                CategoryEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    level = obj.getInt("level"),
                    parentId = if (obj.isNull("parentId")) null else obj.getLong("parentId"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }

            val transactions = (0 until transactionsJson.length()).map { i ->
                val obj = transactionsJson.getJSONObject(i)
                TransactionEntity(
                    id = obj.getLong("id"),
                    accountId = obj.getLong("accountId"),
                    transactionDate = obj.getLong("transactionDate"),
                    description = obj.getString("description"),
                    merchantName = if (obj.isNull("merchantName")) null else obj.getString("merchantName"),
                    amountMinor = obj.getLong("amountMinor"),
                    currency = obj.optString("currency", "INR"),
                    transactionType = TransactionType.valueOf(obj.getString("transactionType")),
                    l1CategoryId = obj.getLong("l1CategoryId"),
                    l2CategoryId = if (obj.isNull("l2CategoryId")) null else obj.getLong("l2CategoryId"),
                    statementDate = if (obj.isNull("statementDate")) null else obj.optLong("statementDate"),
                    sourceFile = if (obj.isNull("sourceFile")) null else obj.optString("sourceFile"),
                    sourceSmsId = if (obj.isNull("sourceSmsId")) null else obj.optLong("sourceSmsId"),
                    notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }

            val budgets = (0 until budgetsJson.length()).map { i ->
                val obj = budgetsJson.getJSONObject(i)
                BudgetEntity(
                    id = obj.getLong("id"),
                    l1CategoryId = obj.getLong("l1CategoryId"),
                    amountLimitMinor = obj.getLong("amountLimitMinor"),
                    period = BudgetPeriod.valueOf(obj.getString("period")),
                    isActive = obj.optBoolean("isActive", true)
                )
            }

            val reviewItems = (0 until reviewItemsJson.length()).map { i ->
                val obj = reviewItemsJson.getJSONObject(i)
                SmsReviewItemEntity(
                    id = obj.getLong("id"),
                    sourceSmsId = obj.getLong("sourceSmsId"),
                    sender = if (obj.isNull("sender")) null else obj.getString("sender"),
                    body = obj.getString("body"),
                    receivedAt = obj.getLong("receivedAt"),
                    detectedAmountMinor = obj.getLong("detectedAmountMinor"),
                    detectedCurrency = obj.optString("detectedCurrency", "INR"),
                    detectedMerchant = if (obj.isNull("detectedMerchant")) null else obj.getString("detectedMerchant"),
                    suggestedType = if (obj.isNull("suggestedType")) null else TransactionType.valueOf(obj.getString("suggestedType")),
                    accountHintLast4 = if (obj.isNull("accountHintLast4")) null else obj.getString("accountHintLast4"),
                    status = SmsReviewStatus.valueOf(obj.getString("status")),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }

            database.withWriteTransaction {
                categoryDao.upsertAll(categories.sortedBy { it.level })
                accountDao.upsertAll(accounts)
                smsReviewDao.upsertAll(reviewItems)
                transactionDao.upsertAll(transactions)
                budgetDao.upsertAll(budgets)
            }

            accounts.size + categories.size + transactions.size + budgets.size + reviewItems.size
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    companion object {
        private const val BACKUP_VERSION = 1
    }
}
