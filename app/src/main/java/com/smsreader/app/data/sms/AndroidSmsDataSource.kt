package com.smsreader.app.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSmsDataSource(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SmsDataSource {

    override suspend fun getMessages(startInclusive: Long, endExclusive: Long): List<DeviceSms> =
        withContext(ioDispatcher) {
            require(startInclusive >= 0L) { "startInclusive must be non-negative" }
            require(endExclusive > startInclusive) { "Invalid SMS date range" }
            verifyPermission()

            val result = mutableListOf<DeviceSms>()
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} < ?"
            val selectionArgs = arrayOf(startInclusive.toString(), endExclusive.toString())

            try {
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${Telephony.Sms.DATE} DESC"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val senderIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                    while (cursor.moveToNext()) {
                        val body = cursor.getString(bodyIndex)?.trim().orEmpty()
                        if (body.isBlank()) continue
                        result += DeviceSms(
                            id = cursor.getLong(idIndex),
                            sender = cursor.getString(senderIndex),
                            body = body,
                            receivedAt = cursor.getLong(dateIndex)
                        )
                    }
                }
            } catch (e: SecurityException) {
                throw SmsPermissionDeniedException(cause = e)
            }
            result
        }

    private fun verifyPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) throw SmsPermissionDeniedException()
    }
}
