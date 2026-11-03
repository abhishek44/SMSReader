package com.smsreader.app.data.sms

data class DeviceSms(
    val id: Long,
    val sender: String?,
    val body: String,
    val receivedAt: Long
)

interface SmsDataSource {
    suspend fun getMessages(startInclusive: Long, endExclusive: Long): List<DeviceSms>
}

class SmsPermissionDeniedException(
    message: String = "READ_SMS permission is required",
    cause: Throwable? = null
) : SecurityException(message, cause)
