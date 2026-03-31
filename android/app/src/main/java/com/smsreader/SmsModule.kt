package com.smsreader

import android.database.Cursor
import android.net.Uri
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray

/**
 * Native module that reads SMS messages from Android's content provider.
 *
 * Exposes a single method to JS:
 *   SmsModule.getMessages(limit: Int): Promise<Array>
 *
 * Each message has: id, address, body, date, type, read
 */
class SmsModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "SmsModule"

    @ReactMethod
    fun getMessages(limit: Int, promise: Promise) {
        try {
            val messages: WritableArray = Arguments.createArray()
            val contentResolver = reactApplicationContext.contentResolver

            val uri: Uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("_id", "address", "body", "date", "type", "read")
            val sortOrder = "date DESC LIMIT $limit"

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow("_id")
                val addressIdx = it.getColumnIndexOrThrow("address")
                val bodyIdx = it.getColumnIndexOrThrow("body")
                val dateIdx = it.getColumnIndexOrThrow("date")
                val typeIdx = it.getColumnIndexOrThrow("type")
                val readIdx = it.getColumnIndexOrThrow("read")

                while (it.moveToNext()) {
                    val message = Arguments.createMap()
                    message.putString("id", it.getString(idIdx))
                    message.putString("address", it.getString(addressIdx))
                    message.putString("body", it.getString(bodyIdx))
                    message.putString("date", it.getString(dateIdx))
                    message.putString("type", it.getString(typeIdx))
                    message.putString("read", it.getString(readIdx))
                    messages.pushMap(message)
                }
            }

            promise.resolve(messages)
        } catch (e: Exception) {
            promise.reject("SMS_READ_ERROR", "Failed to read SMS: ${e.message}", e)
        }
    }
}
