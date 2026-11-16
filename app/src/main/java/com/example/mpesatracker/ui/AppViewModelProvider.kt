package com.example.mpesatracker.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mpesatracker.MpesaApp
import com.example.mpesatracker.ui.add.AddTransactionViewModel
import com.example.mpesatracker.ui.dashboard.DashboardViewModel
import com.example.mpesatracker.ui.history.HistoryViewModel

/** Tells Compose how to build each ViewModel with the shared repository. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { DashboardViewModel(mpesaApp().repository, mpesaApp().smsReader) }
        initializer { HistoryViewModel(mpesaApp().repository) }
        initializer { AddTransactionViewModel(mpesaApp().repository) }
    }
}

private fun CreationExtras.mpesaApp(): MpesaApp = this[APPLICATION_KEY] as MpesaApp
