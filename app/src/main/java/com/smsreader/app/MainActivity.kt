package com.smsreader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smsreader.app.ui.SmsReaderApp
import com.smsreader.app.ui.theme.SmsReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmsReaderTheme {
                SmsReaderApp()
            }
        }
    }
}
