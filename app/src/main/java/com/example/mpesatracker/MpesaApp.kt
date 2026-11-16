package com.example.mpesatracker

import android.app.Application
import com.example.mpesatracker.data.AppDatabase
import com.example.mpesatracker.data.TransactionRepository
import com.example.mpesatracker.sms.SmsReader

/**
 * Runs once when the app starts and holds the objects the whole app shares.
 * This is simple manual dependency injection: ViewModels get these from here.
 */
class MpesaApp : Application() {
    val repository: TransactionRepository by lazy {
        TransactionRepository(AppDatabase.getInstance(this).transactionDao())
    }
    val smsReader: SmsReader by lazy { SmsReader(this) }
}
