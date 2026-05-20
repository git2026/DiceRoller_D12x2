package com.padm.d12x2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Esquema de cores da aplicação
private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    background = LightGray,
    surface = LightGray,
    onBackground = DarkGray,
    onSurface = DarkGray
)

// Tema principal da aplicação
@Composable
fun D12DiceRollerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
