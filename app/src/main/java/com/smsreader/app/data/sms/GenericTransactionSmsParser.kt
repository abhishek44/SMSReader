package com.smsreader.app.data.sms

import com.smsreader.app.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

class GenericTransactionSmsParser : SmsParser {
    override fun parse(sms: DeviceSms): SmsParseResult {
        val body = sms.body.trim()
        if (body.isBlank()) return SmsParseResult.NotTransaction
        val normalized = body.lowercase()
        if (FAILURE_KEYWORDS.any(normalized::contains)) return SmsParseResult.NotTransaction
        if (NON_TRANSACTION_KEYWORDS.any(normalized::contains)) return SmsParseResult.NotTransaction

        val amountMinor = extractAmountMinor(body) ?: return SmsParseResult.NotTransaction
        if (!looksFinancial(normalized)) return SmsParseResult.NotTransaction

        val type = detectType(normalized)
        return SmsParseResult.Candidate(
            amountMinor = amountMinor,
            merchant = extractMerchant(body, type),
            accountLast4 = extractAccountLast4(body),
            suggestedType = type
        )
    }

    private fun looksFinancial(body: String): Boolean = FINANCIAL_HINTS.any(body::contains)

    private fun detectType(body: String): TransactionType? {
        if (CREDIT_KEYWORDS.any(body::contains)) return TransactionType.CREDIT
        if (DEBIT_KEYWORDS.any(body::contains)) return TransactionType.DEBIT
        return null
    }

    private fun extractAmountMinor(body: String): Long? {
        val match = AMOUNT_REGEX.find(body) ?: return null
        val raw = match.groupValues[1].replace(",", "").trim()
        return runCatching {
            BigDecimal(raw)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
        }.getOrNull()?.takeIf { it > 0L }
    }

    private fun extractAccountLast4(body: String): String? {
        ACCOUNT_LAST4_REGEX.find(body)?.let { return it.groupValues[1] }
        MASKED_LAST4_REGEX.find(body)?.let { return it.groupValues[1] }
        return null
    }

    private fun extractMerchant(body: String, type: TransactionType?): String? {
        val regexes = when (type) {
            TransactionType.CREDIT -> listOf(FROM_REGEX, AT_TO_REGEX)
            TransactionType.DEBIT -> listOf(AT_TO_REGEX, FROM_REGEX)
            null -> listOf(AT_TO_REGEX, FROM_REGEX)
        }
        for (regex in regexes) {
            val value = regex.find(body)?.groupValues?.getOrNull(1)
                ?.trim()?.trim('.', ',', ';', ':', '-')
                ?.take(80)
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private companion object {
        val AMOUNT_REGEX = Regex(
            """(?i)(?:₹|INR|RS\.?|Rs\.?|INR\.)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
        )
        val ACCOUNT_LAST4_REGEX = Regex(
            """(?i)(?:a/c|acct|account|card|ac)\s*(?:no\.?\s*)?(?:ending\s*)?(?:x+|\*+|-)?\s*([0-9]{4})\b"""
        )
        val MASKED_LAST4_REGEX = Regex("""(?i)(?:x{2,}|\*{2,})\s*([0-9]{4})\b""")
        val AT_TO_REGEX = Regex(
            """(?i)\b(?:at|to)\s+([a-z0-9][a-z0-9 &._/'@+-]{1,80}?)(?=\s+(?:on|via|using|ref|txn|transaction|avl|available|from|upi|card)\b|[.;,]|$)"""
        )
        val FROM_REGEX = Regex(
            """(?i)\bfrom\s+([a-z0-9][a-z0-9 &._/'@+-]{1,80}?)(?=\s+(?:on|via|using|ref|txn|transaction|avl|available|to|upi|card)\b|[.;,]|$)"""
        )

        val DEBIT_KEYWORDS = listOf(
            "debited", "debit of", "spent", "paid", "purchase", "purchased", "withdrawn",
            "withdrawal", "sent via", "sent to", "upi debit", "dr by", "dr."
        )
        val CREDIT_KEYWORDS = listOf(
            "credited", "credit of", "received", "deposited", "refund", "refunded", "reversed",
            "reversal", "upi credit", "cr by", "cr."
        )
        val FINANCIAL_HINTS = listOf(
            "debited", "credited", "spent", "paid", "purchase", "withdraw", "received", "deposit",
            "refund", "reversal", "transaction", "txn", "upi", "card", "a/c", "account", "balance",
            "avl bal", "available bal", "imps", "neft", "rtgs", "pos", "atm"
        )
        val FAILURE_KEYWORDS = listOf(
            "declined", "failed", "unsuccessful", "not debited", "transaction cancelled",
            "transaction canceled", "payment failed", "transaction failed"
        )
        val NON_TRANSACTION_KEYWORDS = listOf(
            "one time password", "one-time password", " otp ", "otp is", "otp for",
            "verification code", "do not share otp", "minimum amount due", "payment due date",
            "statement generated"
        )
    }
}
