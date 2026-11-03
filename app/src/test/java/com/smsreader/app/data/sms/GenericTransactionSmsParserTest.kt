package com.smsreader.app.data.sms

import com.smsreader.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericTransactionSmsParserTest {
    private val parser = GenericTransactionSmsParser()

    @Test
    fun parsesDebitCardSms() {
        val result = parser.parse(
            DeviceSms(
                id = 1,
                sender = "HDFCBK",
                body = "INR 1,250.00 debited from A/c XX1234 at AMAZON on 02-Nov-26. Avl Bal INR 10,000.00",
                receivedAt = 1L
            )
        )
        assertTrue(result is SmsParseResult.Candidate)
        result as SmsParseResult.Candidate
        assertEquals(125000L, result.amountMinor)
        assertEquals(TransactionType.DEBIT, result.suggestedType)
        assertEquals("1234", result.accountLast4)
        assertEquals("AMAZON", result.merchant)
    }

    @Test
    fun ignoresOtpMessage() {
        val result = parser.parse(
            DeviceSms(2, "BANK", "OTP for transaction of INR 500.00 on card XX1234 is 987654", 1L)
        )
        assertTrue(result is SmsParseResult.NotTransaction)
    }

    @Test
    fun ignoresFailedPayment() {
        val result = parser.parse(
            DeviceSms(3, "BANK", "Your payment of INR 500.00 failed. Amount not debited.", 1L)
        )
        assertTrue(result is SmsParseResult.NotTransaction)
    }
}
