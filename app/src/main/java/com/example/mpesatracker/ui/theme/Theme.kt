package com.example.mpesatracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mpesatracker.domain.Category

val MpesaGreen = Color(0xFF2E7D32)
val IncomeGreen = Color(0xFF2E7D32)

private val LightColors = lightColorScheme(
    primary = MpesaGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0B3D0F),
    secondary = Color(0xFF558B2F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFAED581),
)

@Composable
fun MpesaTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** The colour each category uses in the chart and lists. */
val Category.color: Color
    get() = when (this) {
        Category.FOOD -> Color(0xFFEF6C00)
        Category.TRANSPORT -> Color(0xFF1565C0)
        Category.AIRTIME -> Color(0xFF8E24AA)
        Category.BILLS -> Color(0xFFC62828)
        Category.OTHER -> Color(0xFF757575)
        Category.INCOME -> IncomeGreen
    }
