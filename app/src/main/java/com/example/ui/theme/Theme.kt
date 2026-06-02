package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = IndustrialAmber,
    secondary = EnergeticYellow,
    tertiary = MatteBlue,
    background = DarkOnyx,
    surface = SlatePanel,
    surfaceVariant = MidSlate,
    outline = HighslateOutline,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme for the design studio
    dynamicColor: Boolean = false, // Use our professional palette instead of dynamic colors to preserve visual branding
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
