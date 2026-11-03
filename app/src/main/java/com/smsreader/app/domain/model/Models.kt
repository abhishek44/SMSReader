package com.smsreader.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


enum class AccountType {
    SAVINGS, CURRENT, CREDIT_CARD, CASH, WALLET, INVESTMENT
}

enum class TransactionType {
    DEBIT, CREDIT
}

enum class BudgetPeriod {
    MONTHLY, YEARLY
}

enum class AnalyticsPeriod {
    THIS_MONTH, THIS_YEAR, CUSTOM
}

data class DateRange(
    val startInclusive: Long,
    val endExclusive: Long
)

object DateRanges {
    fun thisMonth(nowMillis: Long = System.currentTimeMillis()): DateRange {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val start = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return DateRange(start, end)
    }

    fun thisYear(nowMillis: Long = System.currentTimeMillis()): DateRange {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val start = LocalDate.of(today.year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(today.year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        return DateRange(start, end)
    }

    fun custom(startDateMillis: Long, endDateMillis: Long): DateRange {
        val zone = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(startDateMillis).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endDateMillis).atZone(zone).toLocalDate()
        val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        require(end > start) { "End date must not be before start date" }
        return DateRange(start, end)
    }
}
