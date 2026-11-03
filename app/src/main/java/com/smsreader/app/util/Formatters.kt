package com.smsreader.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

object Money {
    fun parseToMinor(value: String): Long? {
        val cleaned = value.trim().replace(",", "")
        if (cleaned.isBlank()) return null
        return runCatching {
            BigDecimal(cleaned)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
        }.getOrNull()
    }

    fun editable(minor: Long): String = BigDecimal(minor).movePointLeft(2).setScale(2).toPlainString()

    fun format(minor: Long, currencyCode: String = "INR"): String {
        val locale = if (currencyCode == "INR") Locale.forLanguageTag("en-IN") else Locale.getDefault()
        val formatter = NumberFormat.getCurrencyInstance(locale)
        runCatching { formatter.currency = Currency.getInstance(currencyCode) }
        return formatter.format(BigDecimal(minor).movePointLeft(2))
    }
}

object Dates {
    private val display = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val shortDisplay = DateTimeFormatter.ofPattern("dd MMM")

    fun format(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(display)

    fun formatShort(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(shortDisplay)
}
