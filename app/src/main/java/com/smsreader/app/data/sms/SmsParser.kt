package com.smsreader.app.data.sms

import com.smsreader.app.domain.model.TransactionType

interface SmsParser {
    fun parse(sms: DeviceSms): SmsParseResult
}

sealed interface SmsParseResult {
    data class Candidate(
        val amountMinor: Long,
        val currency: String = "INR",
        val merchant: String?,
        val accountLast4: String?,
        val suggestedType: TransactionType?
    ) : SmsParseResult

    data object NotTransaction : SmsParseResult
}
