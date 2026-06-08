package com.kenews.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Kenya flag colors as accent
private val KenyaRed = Color(0xFFBB0000)
private val KenyaGreen = Color(0xFF006600)

private val LightColors = lightColorScheme(
    primary = KenyaRed,
    onPrimary = Color.White,
    secondary = KenyaGreen,
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B6B),
    onPrimary = Color.Black,
    secondary = Color(0xFF4CAF50),
    onSecondary = Color.Black,
)

@Composable
fun KeNewsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
