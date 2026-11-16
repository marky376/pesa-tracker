package com.example.mpesatracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mpesatracker.ui.MpesaTrackerApp
import com.example.mpesatracker.ui.theme.MpesaTrackerTheme

/** The only Activity. Everything on screen is drawn with Jetpack Compose. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status bar, with white status bar icons over our green top bar.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            MpesaTrackerTheme {
                MpesaTrackerApp()
            }
        }
    }
}
