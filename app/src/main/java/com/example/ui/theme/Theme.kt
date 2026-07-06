package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.nativeCanvas

/**
 * High-Contrast Professional dark theme configuration
 * for Zenith Studio Creative Workspace.
 */
private val DarkColorScheme = darkColorScheme(
    primary = IndustrialAmber,      // Industrial Accent Amber
    secondary = EnergeticYellow,    // Energetic Accent Gold
    tertiary = MatteBlue,           // Cobalt Sub-accent
    background = DarkOnyx,          // Midnight Obsidian Canvas
    surface = SlatePanel,           // Slate Tool Panels
    surfaceVariant = MidSlate,      // Dark Satin Mid-Slate Surface
    outline = HighslateOutline,     // Subtle Silver-Slate Border Outline
    onBackground = Color.White,     // White text pops on Midnight Obsidian
    onSurface = TextPrimary,        // Clean White text on Slate Panels
    onSurfaceVariant = TextSecondary // Muted Gray info text on Slate Panels
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,       // Force Dark theme for the design studio workspace
    dynamicColor: Boolean = false,   // Preserves professional color system visual rules
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Hardware-accelerated glow modification extension using paint shadow layers
 * maps color scheme highlights elegantly.
 */
fun Modifier.neonChartreuseGlow(
    glowRadius: Dp = 8.dp,
    alpha: Float = 0.5f,
    cornerRadius: Dp = 8.dp
): Modifier = this.graphicsLayer {
    clip = false
}.drawBehind {
    val paint = Paint().asFrameworkPaint().apply {
        color = Color(0xFFFFB300).copy(alpha = alpha).toArgb() // Beautiful Warm Amber glow
        setShadowLayer(
            glowRadius.toPx(),
            0f,
            0f,
            Color(0xFFFFB300).toArgb()
        )
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            cornerRadius.toPx(),
            cornerRadius.toPx(),
            paint
        )
    }
}

/**
 * Beautiful dedicated Cyber Amber Glow modifier.
 */
fun Modifier.industrialAmberGlow(
    glowRadius: Dp = 8.dp,
    alpha: Float = 0.5f,
    cornerRadius: Dp = 8.dp
): Modifier = this.graphicsLayer {
    clip = false
}.drawBehind {
    val paint = Paint().asFrameworkPaint().apply {
        color = Color(0xFFFFB300).copy(alpha = alpha).toArgb()
        setShadowLayer(
            glowRadius.toPx(),
            0f,
            0f,
            Color(0xFFFFB300).toArgb()
        )
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            cornerRadius.toPx(),
            cornerRadius.toPx(),
            paint
        )
    }
}

/**
 * Legacy compatibility extension mapping to Electric Violet shade as well.
 */
fun Modifier.electricVioletGlow(
    glowRadius: Dp = 8.dp,
    alpha: Float = 0.5f,
    cornerRadius: Dp = 8.dp
): Modifier = this.graphicsLayer {
    clip = false
}.drawBehind {
    val paint = Paint().asFrameworkPaint().apply {
        color = Color(0xFFFFB300).copy(alpha = alpha).toArgb() // Harmonized to Cyber Amber visual accent
        setShadowLayer(
            glowRadius.toPx(),
            0f,
            0f,
            Color(0xFFFFB300).toArgb()
        )
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            cornerRadius.toPx(),
            cornerRadius.toPx(),
            paint
        )
    }
}
