package com.example.plancerca.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Terracota,
    secondary = VerdeOliva,
    tertiary = DoradoSuave,
    background = Crema,
    surface = Crema,
    onBackground = GrisCarbon,
    onSurface = GrisCarbon
)

@Composable
fun PlanCercaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}