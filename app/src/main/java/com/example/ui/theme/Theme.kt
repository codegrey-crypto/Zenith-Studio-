package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
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
 * Dynamic Multi-Theme configuration
 * for Zenith Studio Creative Workspace.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = currentThemeStateBySelection.value.isDark,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val theme = currentThemeStateBySelection.value
    val scheme = darkColorScheme(
        primary = theme.primary,
        secondary = theme.secondary,
        tertiary = theme.tertiary,
        background = theme.background,
        surface = theme.surface,
        surfaceVariant = theme.surfaceVariant,
        outline = theme.outline,
        onBackground = theme.onBackground,
        onSurface = theme.onSurface,
        onSurfaceVariant = theme.onSurfaceVariant
    )
    val baseTypography = Typography
    val customTypography = baseTypography.copy(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = theme.fontStyle),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = theme.fontStyle),
        displaySmall = baseTypography.displaySmall.copy(fontFamily = theme.fontStyle),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = theme.fontStyle),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = theme.fontStyle),
        headlineSmall = baseTypography.headlineSmall.copy(fontFamily = theme.fontStyle),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = theme.fontStyle),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = theme.fontStyle),
        titleSmall = baseTypography.titleSmall.copy(fontFamily = theme.fontStyle),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = theme.fontStyle),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = theme.fontStyle),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = theme.fontStyle),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = theme.fontStyle),
        labelMedium = baseTypography.labelMedium.copy(fontFamily = theme.fontStyle),
        labelSmall = baseTypography.labelSmall.copy(fontFamily = theme.fontStyle)
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = customTypography,
        content = content
    )
}

/**
 * Hardware-accelerated glow modification extension using paint shadow layers
 * maps color scheme highlights elegantly.
 */
fun Modifier.neonChartreuseGlow(
    glowRadius: Dp = 8.dp,
    alpha: Float = 0.4f,
    cornerRadius: Dp = 8.dp
): Modifier = if (isLowEndOptimizationEnabled.value) this else this.graphicsLayer {
    clip = false
}.drawBehind {
    val theme = currentThemeStateBySelection.value
    if (theme.isBrutalist) {
        val offsetPx = 5.dp.toPx()
        val paint = Paint().asFrameworkPaint().apply {
            color = theme.outline.toArgb()
            style = android.graphics.Paint.Style.FILL
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                offsetPx,
                offsetPx,
                size.width + offsetPx,
                size.height + offsetPx,
                4.dp.toPx(),
                4.dp.toPx(),
                paint
            )
        }
    } else {
        val glowColor = theme.secondary
        val paint = Paint().asFrameworkPaint().apply {
            color = glowColor.copy(alpha = alpha).toArgb() // Beautiful dynamic glow matching active theme secondary color
            setShadowLayer(
                glowRadius.toPx(),
                0f,
                0f,
                glowColor.toArgb()
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
}

/**
 * Beautiful dedicated Cyber Amber Glow modifier.
 */
fun Modifier.industrialAmberGlow(
    glowRadius: Dp = 8.dp,
    alpha: Float = 0.45f,
    cornerRadius: Dp = 8.dp
): Modifier = if (isLowEndOptimizationEnabled.value) this else this.graphicsLayer {
    clip = false
}.drawBehind {
    val theme = currentThemeStateBySelection.value
    if (theme.isBrutalist) {
        val offsetPx = 5.dp.toPx()
        val paint = Paint().asFrameworkPaint().apply {
            color = theme.outline.toArgb()
            style = android.graphics.Paint.Style.FILL
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                offsetPx,
                offsetPx,
                size.width + offsetPx,
                size.height + offsetPx,
                4.dp.toPx(),
                4.dp.toPx(),
                paint
            )
        }
    } else {
        val glowColor = theme.primary
        val paint = Paint().asFrameworkPaint().apply {
            color = glowColor.copy(alpha = alpha).toArgb() // Dynamic glow matching active theme primary color
            setShadowLayer(
                glowRadius.toPx(),
                0f,
                0f,
                glowColor.toArgb()
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
}

/**
 * Legacy compatibility extension mapping to Electric Violet shade as well.
 */
fun Modifier.electricVioletGlow(
    glowRadius: Dp = 8.dp,
    alpha: Float = 0.4f,
    cornerRadius: Dp = 8.dp
): Modifier = if (isLowEndOptimizationEnabled.value) this else this.graphicsLayer {
    clip = false
 }.drawBehind {
    val theme = currentThemeStateBySelection.value
    if (theme.isBrutalist) {
        val offsetPx = 5.dp.toPx()
        val paint = Paint().asFrameworkPaint().apply {
            color = theme.outline.toArgb()
            style = android.graphics.Paint.Style.FILL
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                offsetPx,
                offsetPx,
                size.width + offsetPx,
                size.height + offsetPx,
                4.dp.toPx(),
                4.dp.toPx(),
                paint
            )
        }
    } else {
        val glowColor = theme.tertiary
        val paint = Paint().asFrameworkPaint().apply {
            color = glowColor.copy(alpha = alpha).toArgb() // Dynamic glow matching active theme tertiary color
            setShadowLayer(
                glowRadius.toPx(),
                0f,
                0f,
                glowColor.toArgb()
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
}

// --- DYNAMIC MULTI-DIMENSIONAL ADVANCED RESHAPING SYSTEM ---

val currentTheme: StudioTheme get() = currentThemeStateBySelection.value

val themeCardShape: Shape
    get() = RoundedCornerShape(currentTheme.cornerRadius.dp)

fun themeCardShape(overrideRadius: Dp): Shape = RoundedCornerShape(if (currentTheme.isBrutalist) 0.dp else overrideRadius)

val themeBorderStroke: BorderStroke
    get() = BorderStroke(currentTheme.borderWidth.dp, HighslateOutline)

fun themeBorderStroke(width: Dp, color: Color = HighslateOutline): BorderStroke = BorderStroke(if (currentTheme.isBrutalist) currentTheme.borderWidth.dp else width, color)

val themePanelSpacing: Dp
    get() = currentTheme.panelSpacing.dp

