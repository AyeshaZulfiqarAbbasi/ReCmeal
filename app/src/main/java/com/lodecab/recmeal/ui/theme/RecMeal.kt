package com.lodecab.recmeal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = PrimaryVariant,
    secondary = SecondaryColor,
    onSecondary = Color.White,
    background = BackgroundColor,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    onBackground = TextPrimary,
    error = ErrorColor,
    onError = Color.White
)

@Composable
fun RecMealTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}