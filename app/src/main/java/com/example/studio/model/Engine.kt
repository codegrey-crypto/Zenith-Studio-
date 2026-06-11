package com.example.studio.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import java.util.UUID

enum class LayerType {
    VECTOR_RECT,
    VECTOR_CIRCLE,
    VECTOR_STAR,
    VECTOR_TRIANGLE,
    VECTOR_PENTAGON,
    VECTOR_HEXAGON,
    VECTOR_OVAL,
    VECTOR_LINE,
    VECTOR_BEZIER,
    TEXT,
    FREEHAND_DRAWING,
    IMAGE_CARD,
    GROUP
}

enum class ZenithBlendMode(val displayName: String) {
    NORMAL("Normal"),
    ADD("Add"),
    MULTIPLY("Multiply"),
    SCREEN("Screen"),
    OVERLAY("Overlay"),
    LINEAR_DODGE("Linear Dodge (Add)"),
    DARKEN("Darken"),
    LIGHTEN("Lighten"),
    COLOR_BURN("Color Burn"),
    COLOR_DODGE("Color Dodge"),
    DIFFERENCE("Difference"),
    EXCLUSION("Exclusion"),
    HUE("Hue"),
    SATURATION("Saturation"),
    COLOR("Color"),
    LUMINOSITY("Luminosity");

    fun toComposeBlendMode(): BlendMode {
        return when (this) {
            NORMAL -> BlendMode.SrcOver
            ADD -> BlendMode.Plus
            MULTIPLY -> BlendMode.Multiply
            SCREEN -> BlendMode.Screen
            OVERLAY -> BlendMode.Overlay
            LINEAR_DODGE -> BlendMode.Plus
            DARKEN -> BlendMode.Darken
            LIGHTEN -> BlendMode.Lighten
            COLOR_BURN -> BlendMode.ColorBurn
            COLOR_DODGE -> BlendMode.ColorDodge
            DIFFERENCE -> BlendMode.Difference
            EXCLUSION -> BlendMode.Exclusion
            HUE -> BlendMode.Hue
            SATURATION -> BlendMode.Saturation
            COLOR -> BlendMode.Color
            LUMINOSITY -> BlendMode.Luminosity
        }
    }
}

data class EffectParameter(
    val name: String,
    val value: Float,
    val rangeMin: Float,
    val rangeMax: Float,
    val unit: String = ""
)

sealed class StudioEffect {
    abstract val id: String
    abstract val name: String
    abstract val parameters: Map<String, EffectParameter>
    abstract val isEnabled: Boolean
    abstract fun updateParameter(paramName: String, newValue: Float): StudioEffect
    abstract fun toggleEnabled(): StudioEffect

    data class GaussianBlur(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Gaussian Blur",
        override val parameters: Map<String, EffectParameter> = mapOf(
            "Radius" to EffectParameter("Radius", 12f, 0f, 40f, "px"),
            "Intensity" to EffectParameter("Intensity", 0.8f, 0f, 1f)
        ),
        override val isEnabled: Boolean = true
    ) : StudioEffect() {
        override fun updateParameter(paramName: String, newValue: Float): StudioEffect {
            val updated = parameters.toMutableMap()
            val old = updated[paramName] ?: return this
            updated[paramName] = old.copy(value = newValue)
            return this.copy(parameters = updated)
        }
        override fun toggleEnabled(): StudioEffect {
            return this.copy(isEnabled = !isEnabled)
        }
    }

    data class InnerGlow(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Inner Glow",
        override val parameters: Map<String, EffectParameter> = mapOf(
            "Choke" to EffectParameter("Choke", 15f, 1f, 50f, "px"),
            "Opacity" to EffectParameter("Opacity", 0.65f, 0f, 1f)
        ),
        val glowColor: Color = Color(0xFFFF9800),
        override val isEnabled: Boolean = true
    ) : StudioEffect() {
        override fun updateParameter(paramName: String, newValue: Float): StudioEffect {
            val updated = parameters.toMutableMap()
            val old = updated[paramName] ?: return this
            updated[paramName] = old.copy(value = newValue)
            return this.copy(parameters = updated)
        }
        override fun toggleEnabled(): StudioEffect {
            return this.copy(isEnabled = !isEnabled)
        }
    }

    data class ColorBalance(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Color Balance",
        override val parameters: Map<String, EffectParameter> = mapOf(
            "HueShift" to EffectParameter("Hue Shift", 0f, -180f, 180f, "°"),
            "Brightness" to EffectParameter("Brightness", 1.0f, 0.4f, 2.0f, "x"),
            "Saturation" to EffectParameter("Saturation", 1.0f, 0f, 3.0f, "x")
        ),
        override val isEnabled: Boolean = true
    ) : StudioEffect() {
        override fun updateParameter(paramName: String, newValue: Float): StudioEffect {
            val updated = parameters.toMutableMap()
            val old = updated[paramName] ?: return this
            updated[paramName] = old.copy(value = newValue)
            return this.copy(parameters = updated)
        }
        override fun toggleEnabled(): StudioEffect {
            return this.copy(isEnabled = !isEnabled)
        }
    }

    data class Invert(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Invert Color",
        override val parameters: Map<String, EffectParameter> = emptyMap(),
        override val isEnabled: Boolean = true
    ) : StudioEffect() {
        override fun updateParameter(paramName: String, newValue: Float): StudioEffect = this
        override fun toggleEnabled(): StudioEffect {
            return this.copy(isEnabled = !isEnabled)
        }
    }

    data class Threshold(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Threshold",
        override val parameters: Map<String, EffectParameter> = mapOf(
            "Threshold" to EffectParameter("Threshold", 0.5f, 0.0f, 1.0f)
        ),
        override val isEnabled: Boolean = true
    ) : StudioEffect() {
        override fun updateParameter(paramName: String, newValue: Float): StudioEffect {
            val updated = parameters.toMutableMap()
            val old = updated[paramName] ?: return this
            updated[paramName] = old.copy(value = newValue)
            return this.copy(parameters = updated)
        }
        override fun toggleEnabled(): StudioEffect {
            return this.copy(isEnabled = !isEnabled)
        }
    }

    data class Posterize(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Posterize",
        override val parameters: Map<String, EffectParameter> = mapOf(
            "Levels" to EffectParameter("Levels", 4f, 2f, 16f)
        ),
        override val isEnabled: Boolean = true
    ) : StudioEffect() {
        override fun updateParameter(paramName: String, newValue: Float): StudioEffect {
            val updated = parameters.toMutableMap()
            val old = updated[paramName] ?: return this
            updated[paramName] = old.copy(value = newValue)
            return this.copy(parameters = updated)
        }
        override fun toggleEnabled(): StudioEffect {
            return this.copy(isEnabled = !isEnabled)
        }
    }

    data class PhotoshopEffect(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String,
        override val parameters: Map<String, EffectParameter>,
        val category: String,
        val effectType: String,
        override val isEnabled: Boolean = true
    ) : StudioEffect() {
        override fun updateParameter(paramName: String, newValue: Float): StudioEffect {
            val updated = parameters.toMutableMap()
            val old = updated[paramName] ?: EffectParameter(
                name = paramName,
                value = newValue,
                rangeMin = if (paramName == "Angle") -180f else 0f,
                rangeMax = when {
                    paramName == "CustomStopCount" -> 6f
                    paramName == "Preset" -> 7f
                    paramName == "Angle" -> 180f
                    paramName == "Scale" -> 150f
                    else -> 1f
                }
            )
            updated[paramName] = old.copy(value = newValue)
            return this.copy(parameters = updated)
        }
        override fun toggleEnabled(): StudioEffect {
            return this.copy(isEnabled = !isEnabled)
        }
    }
}

object PhotoshopEffectTemplates {
    fun create(id: String = java.util.UUID.randomUUID().toString(), effectType: String): StudioEffect {
        return when (effectType) {
            "BrushConfig" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Brush Settings",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Size" to EffectParameter("Size", 24f, 1f, 1000f, "px"),
                    "Opacity" to EffectParameter("Opacity", 1.0f, 0.05f, 1.0f),
                    "Smoothing" to EffectParameter("Smoothing", 1.0f, 0f, 1f),
                    "Preset" to EffectParameter("Preset", 0f, 0f, 10f)
                )
            )
            "GlassMorphism" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Glass Morphism",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "FractalIntensity" to EffectParameter("Fractal Intensity", 5f, 0f, 10f),
                    "FractalType" to EffectParameter("Fractal Type (Shattered)", 0f, 0f, 1f),
                    "RefractionIndex" to EffectParameter("Refraction Index", 3f, -10f, 10f),
                    "EdgeTorsion" to EffectParameter("Edge Torsion", 2f, 0f, 10f),
                    "SurfaceTension" to EffectParameter("Surface Tension", 4f, 0f, 10f),
                    "Radius" to EffectParameter("Glass Thickness (Blur)", 20f, 0f, 100f, "px")
                )
            )
            "ReededGlass" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Reeded Glass",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "LineDensity" to EffectParameter("Line Density", 33f, 5f, 100f),
                    "RefractionStrength" to EffectParameter("Refraction Strength", 8f, 0f, 50f),
                    "BlurMix" to EffectParameter("Blur Mix", 12f, 0f, 100f),
                    "SpecularHighlight" to EffectParameter("Specular Highlight", 5f, 0f, 10f),
                    "Rotation" to EffectParameter("Rotation (Horiz=1)", 0f, 0f, 1f)
                )
            )
            "DropShadow" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Drop Shadow",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Distance" to EffectParameter("Distance", 10f, 0f, 100f, "px"),
                    "Size" to EffectParameter("Size (Blur)", 15f, 0f, 100f, "px"),
                    "Angle" to EffectParameter("Angle", 120f, 0f, 360f, "°"),
                    "Opacity" to EffectParameter("Opacity", 0.5f, 0f, 1f)
                )
            )
            "InnerShadow" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Inner Shadow",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Distance" to EffectParameter("Distance", 5f, 0f, 100f, "px"),
                    "Choke" to EffectParameter("Choke", 0f, 0f, 100f, "%"),
                    "Size" to EffectParameter("Size", 10f, 0f, 120f, "px"),
                    "Angle" to EffectParameter("Angle", 120f, 0f, 360f, "°"),
                    "Opacity" to EffectParameter("Opacity", 0.5f, 0f, 1f)
                )
            )
            "OuterGlow" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Outer Glow",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Size" to EffectParameter("Size", 20f, 0f, 100f, "px"),
                    "Spread" to EffectParameter("Spread", 10f, 0f, 100f, "%"),
                    "Opacity" to EffectParameter("Opacity", 0.75f, 0f, 1f)
                )
            )
            "InnerGlow" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Inner Glow",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Choke" to EffectParameter("Choke", 15f, 1f, 50f, "px"),
                    "Opacity" to EffectParameter("Opacity", 0.65f, 0f, 1f)
                )
            )
            "BevelEmboss" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Bevel & Emboss",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Depth" to EffectParameter("Depth", 100f, 1f, 200f, "%"),
                    "Size" to EffectParameter("Size", 5f, 1f, 50f, "px"),
                    "Soften" to EffectParameter("Soften", 0f, 0f, 16f, "px")
                )
            )
            "Satin" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Satin",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Distance" to EffectParameter("Distance", 11f, 0f, 100f, "px"),
                    "Size" to EffectParameter("Size", 14f, 0f, 100f, "px"),
                    "Opacity" to EffectParameter("Opacity", 0.5f, 0f, 1f)
                )
            )
            "ColorOverlay" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Color Overlay",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "HueShift" to EffectParameter("Hue Shift", 0f, -180f, 180f, "°"),
                    "Opacity" to EffectParameter("Opacity", 1f, 0f, 1f)
                )
            )
            "GradientOverlay" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Gradient Overlay",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Preset" to EffectParameter("Preset", 0f, 0f, 7f),
                    "Scale" to EffectParameter("Scale", 100f, 10f, 150f, "%"),
                    "Angle" to EffectParameter("Angle", 90f, -180f, 180f, "°"),
                    "Opacity" to EffectParameter("Opacity", 1.0f, 0f, 1f),
                    "CustomStart_R" to EffectParameter("Custom Start Red", 1.0f, 0f, 1f),
                    "CustomStart_G" to EffectParameter("Custom Start Green", 0.0f, 0f, 1f),
                    "CustomStart_B" to EffectParameter("Custom Start Blue", 0.0f, 0f, 1f),
                    "CustomEnd_R" to EffectParameter("Custom End Red", 0.0f, 0f, 1f),
                    "CustomEnd_G" to EffectParameter("Custom End Green", 1.0f, 0f, 1f),
                    "CustomEnd_B" to EffectParameter("Custom End Blue", 1.0f, 0f, 1f)
                )
            )
            "PatternOverlay" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Pattern Overlay",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Scale" to EffectParameter("Scale", 100f, 1f, 200f, "%"),
                    "Opacity" to EffectParameter("Opacity", 1f, 0f, 1f)
                )
            )
            "Stroke" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Stroke",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "Size" to EffectParameter("Size", 3f, 1f, 50f, "px"),
                    "Opacity" to EffectParameter("Opacity", 1f, 0f, 1f)
                )
            )
            "BordersAndShadows" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Borders & Shadows",
                category = "Layer Styles (fx)",
                effectType = effectType,
                parameters = mapOf(
                    "DropShadow_Enabled" to EffectParameter("Drop Shadow Enabled", 0f, 0f, 1f),
                    "DropShadow_Distance" to EffectParameter("Drop Shadow Distance", 10f, 0f, 100f, "px"),
                    "DropShadow_Size" to EffectParameter("Drop Shadow Size (Blur)", 15f, 0f, 120f, "px"),
                    "DropShadow_Angle" to EffectParameter("Drop Shadow Angle", 120f, 0f, 360f, "°"),
                    "DropShadow_Opacity" to EffectParameter("Drop Shadow Opacity", 0.5f, 0f, 1f),
                    "DropShadow_Glow" to EffectParameter("Drop Shadow Glow", 0f, 0f, 100f, "%"),
                    "DropShadow_Sharpen" to EffectParameter("Drop Shadow Sharpen", 0f, 0f, 1f),
                    "DropShadow_Zoom" to EffectParameter("Drop Shadow Zoom", 1.0f, 0.5f, 2.0f, "x"),
                    "DropShadow_Hardness" to EffectParameter("Drop Shadow Hardness", 0f, 0f, 1f),
                    "DropShadow_Alpha" to EffectParameter("Drop Shadow Color Alpha", 1.0f, 0f, 1f),
                    "DropShadow_Color_R" to EffectParameter("Drop Shadow Color Red", 0f, 0f, 1f),
                    "DropShadow_Color_G" to EffectParameter("Drop Shadow Color Green", 0f, 0f, 1f),
                    "DropShadow_Color_B" to EffectParameter("Drop Shadow Color Blue", 0f, 0f, 1f),

                    "InnerShadow_Enabled" to EffectParameter("Inner Shadow Enabled", 0f, 0f, 1f),
                    "InnerShadow_Distance" to EffectParameter("Inner Shadow Distance", 5f, 0f, 100f, "px"),
                    "InnerShadow_Size" to EffectParameter("Inner Shadow Size (Blur)", 10f, 0f, 120f, "px"),
                    "InnerShadow_Angle" to EffectParameter("Inner Shadow Angle", 120f, 0f, 360f, "°"),
                    "InnerShadow_Opacity" to EffectParameter("Inner Shadow Opacity", 0.5f, 0f, 1f),
                    "InnerShadow_Hardness" to EffectParameter("Inner Shadow Hardness", 0f, 0f, 1f),
                    "InnerShadow_Choke" to EffectParameter("Inner Shadow Choke", 0f, 0f, 100f, "%"),
                    "InnerShadow_Color_R" to EffectParameter("Inner Shadow Color Red", 0f, 0f, 1f),
                    "InnerShadow_Color_G" to EffectParameter("Inner Shadow Color Green", 0f, 0f, 1f),
                    "InnerShadow_Color_B" to EffectParameter("Inner Shadow Color Blue", 0f, 0f, 1f),

                    "InnerBorder_Enabled" to EffectParameter("Inner Border Enabled", 0f, 0f, 1f),
                    "InnerBorder_Size" to EffectParameter("Inner Border Size", 0f, 0f, 100f, "px"),
                    "InnerBorder_Opacity" to EffectParameter("Inner Border Opacity", 1f, 0f, 1f),
                    "InnerBorder_Color_R" to EffectParameter("Inner Border Color Red", 1f, 0f, 1f),
                    "InnerBorder_Color_G" to EffectParameter("Inner Border Color Green", 1f, 0f, 1f),
                    "InnerBorder_Color_B" to EffectParameter("Inner Border Color Blue", 1f, 0f, 1f),

                    "OuterBorder_Enabled" to EffectParameter("Outer Border Enabled", 0f, 0f, 1f),
                    "OuterBorder_Size" to EffectParameter("Outer Border Size", 0f, 0f, 100f, "px"),
                    "OuterBorder_Opacity" to EffectParameter("Outer Border Opacity", 1f, 0f, 1f),
                    "OuterBorder_Color_R" to EffectParameter("Outer Border Color Red", 1f, 0f, 1f),
                    "OuterBorder_Color_G" to EffectParameter("Outer Border Color Green", 1f, 0f, 1f),
                    "OuterBorder_Color_B" to EffectParameter("Outer Border Color Blue", 1f, 0f, 1f),

                    "InnerStroke_Enabled" to EffectParameter("Inner Stroke Enabled", 0f, 0f, 1f),
                    "InnerStroke_Size" to EffectParameter("Inner Stroke Size", 0f, 0f, 100f, "px"),
                    "InnerStroke_Opacity" to EffectParameter("Inner Stroke Opacity", 1f, 0f, 1f),
                    "InnerStroke_Color_R" to EffectParameter("Inner Stroke Color Red", 1f, 0f, 1f),
                    "InnerStroke_Color_G" to EffectParameter("Inner Stroke Color Green", 1f, 0f, 1f),
                    "InnerStroke_Color_B" to EffectParameter("Inner Stroke Color Blue", 1f, 0f, 1f),

                    "OuterStroke_Enabled" to EffectParameter("Outer Stroke Enabled", 0f, 0f, 1f),
                    "OuterStroke_Size" to EffectParameter("Outer Stroke Size", 0f, 0f, 100f, "px"),
                    "OuterStroke_Opacity" to EffectParameter("Outer Stroke Opacity", 1f, 0f, 1f),
                    "OuterStroke_Color_R" to EffectParameter("Outer Stroke Color Red", 1f, 0f, 1f),
                    "OuterStroke_Color_G" to EffectParameter("Outer Stroke Color Green", 1f, 0f, 1f),
                    "OuterStroke_Color_B" to EffectParameter("Outer Stroke Color Blue", 1f, 0f, 1f),

                    "CenterStroke_Enabled" to EffectParameter("Center Stroke Enabled", 0f, 0f, 1f),
                    "CenterStroke_Size" to EffectParameter("Center Stroke Size", 0f, 0f, 100f, "px"),
                    "CenterStroke_Opacity" to EffectParameter("Center Stroke Opacity", 1f, 0f, 1f),
                    "CenterStroke_Color_R" to EffectParameter("Center Stroke Color Red", 1f, 0f, 1f),
                    "CenterStroke_Color_G" to EffectParameter("Center Stroke Color Green", 1f, 0f, 1f),
                    "CenterStroke_Color_B" to EffectParameter("Center Stroke Color Blue", 1f, 0f, 1f)
                )
            )

            "GaussianBlur" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Gaussian Blur",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Radius" to EffectParameter("Radius", 12f, 0f, 40f, "px"),
                    "Intensity" to EffectParameter("Intensity", 0.8f, 0f, 1f)
                )
            )
            "MotionBlur" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Motion Blur",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Angle" to EffectParameter("Angle", 0f, 0f, 360f, "°"),
                    "Distance" to EffectParameter("Distance", 15f, 1f, 100f, "px")
                )
            )
            "RadialBlur" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Radial Blur",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Amount" to EffectParameter("Amount", 10f, 1f, 100f)
                )
            )
            "LensBlur" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Lens Blur",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Radius" to EffectParameter("Radius", 15f, 1f, 100f),
                    "BladeCurvature" to EffectParameter("Blade Curvature", 3f, 0f, 10f)
                )
            )
            "Pinch" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Pinch",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Amount" to EffectParameter("Amount", 50f, -100f, 100f, "%")
                )
            )
            "Ripple" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Ripple",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Amount" to EffectParameter("Amount", 30f, 1f, 100f, "%"),
                    "WaveSize" to EffectParameter("Wave Size", 1f, 1f, 3f)
                )
            )
            "Spherize" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Spherize",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Amount" to EffectParameter("Amount", 100f, -100f, 100f, "%")
                )
            )
            "Twirl" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Twirl",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Angle" to EffectParameter("Angle", 100f, -360f, 360f, "°")
                )
            )
            "Wave" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Wave",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "WaveAmplitude" to EffectParameter("Wave Amplitude", 20f, 1f, 100f)
                )
            )
            "ZigZag" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "ZigZag",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Amount" to EffectParameter("Amount", 10f, -100f, 100f)
                )
            )
            "AddNoise" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Add Noise",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Amount" to EffectParameter("Amount", 10f, 0f, 100f, "%")
                )
            )
            "Despeckle" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Despeckle",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Radius" to EffectParameter("Radius", 2f, 1f, 10f)
                )
            )
            "DustScratches" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Dust & Scratches",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Radius" to EffectParameter("Radius", 2f, 1f, 100f, "px"),
                    "Threshold" to EffectParameter("Threshold", 0f, 0f, 255f)
                )
            )
            "Median" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Median",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Radius" to EffectParameter("Radius", 3f, 1f, 100f, "px")
                )
            )
            "ColorHalftone" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Color Halftone",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "MaxRadius" to EffectParameter("Max Radius", 8f, 4f, 127f, "px")
                )
            )
            "Crystallize" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Crystallize",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "CellSize" to EffectParameter("Cell Size", 10f, 3f, 300f, "px")
                )
            )
            "Mosaic" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Mosaic",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "CellSize" to EffectParameter("Cell Size", 8f, 2f, 200f, "px")
                )
            )
            "Pointillize" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Pointillize",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "CellSize" to EffectParameter("Cell Size", 10f, 3f, 300f, "px")
                )
            )
            "Clouds" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Clouds",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "DetailLevel" to EffectParameter("Detail Level", 2f, 1f, 5f)
                )
            )
            "LensFlare" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Lens Flare",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Brightness" to EffectParameter("Brightness", 100f, 10f, 300f, "%")
                )
            )
            "LightingEffects" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Lighting Effects",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Intensity" to EffectParameter("Intensity", 50f, 0f, 100f)
                )
            )
            "UnsharpMask" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Unsharp Mask",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Amount" to EffectParameter("Amount", 50f, 1f, 500f, "%"),
                    "Radius" to EffectParameter("Radius", 1.0f, 0.1f, 250f)
                )
            )
            "SmartSharpen" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Smart Sharpen",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Amount" to EffectParameter("Amount", 100f, 1f, 500f),
                    "Radius" to EffectParameter("Radius", 1.0f, 0.1f, 50f)
                )
            )
            "HighPass" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "High Pass",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Radius" to EffectParameter("Radius", 1.0f, 0.1f, 250f)
                )
            )
            "FindEdges" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Find Edges",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Sensitivity" to EffectParameter("Sensitivity", 50f, 1f, 100f)
                )
            )
            "Emboss" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Emboss",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Angle" to EffectParameter("Angle", 135f, -360f, 360f, "°"),
                    "Height" to EffectParameter("Height", 2f, 1f, 10f, "px"),
                    "Amount" to EffectParameter("Amount", 100f, 1f, 500f, "%")
                )
            )
            "OilPaint" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Oil Paint",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Stylization" to EffectParameter("Stylization", 5f, 0f, 10f),
                    "Cleanliness" to EffectParameter("Cleanliness", 5f, 0f, 10f)
                )
            )
            "Solarize" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Solarize",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Threshold" to EffectParameter("Threshold", 0.5f, 0f, 1f)
                )
            )
            "Wind" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Wind",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    "Angle" to EffectParameter("Angle", 0f, -180f, 180f, "°")
                )
            )

            "Artistic" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Artistic Medium",
                category = "Filter Gallery",
                effectType = effectType,
                parameters = mapOf(
                    "ColorIntensity" to EffectParameter("Color Intensity", 5f, 1f, 10f)
                )
            )
            "BrushStrokes" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Brush Strokes",
                category = "Filter Gallery",
                effectType = effectType,
                parameters = mapOf(
                    "InkDensity" to EffectParameter("Ink Density", 5f, 1f, 10f)
                )
            )
            "Sketch" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Sketch Stylizer",
                category = "Filter Gallery",
                effectType = effectType,
                parameters = mapOf(
                    "Detail" to EffectParameter("Detail", 5f, 1f, 10f)
                )
            )
            "Texture" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Texturizer Tool",
                category = "Filter Gallery",
                effectType = effectType,
                parameters = mapOf(
                    "Relief" to EffectParameter("Relief", 4f, 1f, 10f)
                )
            )

            "Liquify" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Liquify Warp Engine",
                category = "Advanced & AI Engines",
                effectType = effectType,
                parameters = mapOf(
                    "BrushSize" to EffectParameter("Brush Size", 100f, 1f, 300f, "px"),
                    "Density" to EffectParameter("Density", 50f, 1f, 100f, "%"),
                    "Pressure" to EffectParameter("Pressure", 50f, 1f, 100f, "%")
                )
            )
            "CameraRaw" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Camera Raw Filter",
                category = "Advanced & AI Engines",
                effectType = effectType,
                parameters = mapOf(
                    "Exposure" to EffectParameter("Exposure", 0f, -5f, 5f, "ev"),
                    "Contrast" to EffectParameter("Contrast", 0f, -100f, 100f, "%"),
                    "Highlights" to EffectParameter("Highlights", 0f, -100f, 100f, "%"),
                    "Shadows" to EffectParameter("Shadows", 0f, -100f, 100f, "%"),
                    "Whites" to EffectParameter("Whites", 0f, -100f, 100f, "%"),
                    "Blacks" to EffectParameter("Blacks", 0f, -100f, 100f, "%"),
                    "Temp" to EffectParameter("Temp", 0f, -100f, 100f, "k"),
                    "Tint" to EffectParameter("Tint", 0f, -100f, 100f, "%"),
                    "Vibrance" to EffectParameter("Vibrance", 0f, -100f, 100f, "%"),
                    "Saturation" to EffectParameter("Saturation", 0f, -100f, 100f, "%"),
                    "Texture" to EffectParameter("Texture", 0f, -100f, 100f, "%"),
                    "Clarity" to EffectParameter("Clarity", 0f, -100f, 100f, "%"),
                    "Dehaze" to EffectParameter("Dehaze", 0f, -100f, 100f, "%")
                )
            )
            "NeuralFilters" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Neural Skin & Face AI",
                category = "Advanced & AI Engines",
                effectType = effectType,
                parameters = mapOf(
                    "SmoothSkin" to EffectParameter("Smooth Skin", 0f, 0f, 100f, "%"),
                    "Age" to EffectParameter("Age Adjust", 0f, -50f, 50f, "yr"),
                    "EyeDirection" to EffectParameter("Eye Angle", 0f, -50f, 50f, "°")
                )
            )
            "PixelStretch" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Pixel Stretch & Warp",
                category = "Advanced & AI Engines",
                effectType = effectType,
                parameters = mapOf(
                    "SliceLine" to EffectParameter("Slice Position", 0.5f, 0f, 1f),
                    "Orientation" to EffectParameter("Horiz(=1) vs Vert(=0)", 0f, 0f, 1f),
                    "WarpBend" to EffectParameter("Warp Curvature Bend", 30f, -250f, 250f, "px"),
                    "WarpFrequency" to EffectParameter("Warp Wave Frequency", 1f, 0f, 5f),
                    "SubjectCutout" to EffectParameter("Subject Cutout Opacity", 1f, 0f, 1f),
                    "CutoutThreshold" to EffectParameter("Cutout Threshold", 230f, 0f, 255f)
                )
            )
            "ColorGrading" -> StudioEffect.PhotoshopEffect(
                id = id,
                name = "Color Grading",
                category = "Core Filters",
                effectType = effectType,
                parameters = mapOf(
                    // Legacy LUT
                    "Preset" to EffectParameter("Preset LUT", 0f, 0f, 6f),
                    "Intensity" to EffectParameter("Intensity", 1.0f, 0.0f, 1.0f),

                    // 1. Basic Tonal Adjustments
                    "Exposure" to EffectParameter("Exposure", 0f, -5f, 5f, "ev"),
                    "Contrast" to EffectParameter("Contrast", 0f, -100f, 100f, "%"),
                    "Highlights" to EffectParameter("Highlights", 0f, -100f, 100f, "%"),
                    "Shadows" to EffectParameter("Shadows", 0f, -100f, 100f, "%"),
                    "Whites" to EffectParameter("Whites", 0f, -100f, 100f, "%"),
                    "Blacks" to EffectParameter("Blacks", 0f, -100f, 100f, "%"),

                    // 2. Color & White Balance
                    "Temperature" to EffectParameter("Temperature", 6500f, 2000f, 12000f, "K"),
                    "Tint" to EffectParameter("Tint", 0f, -100f, 100f),
                    "Vibrance" to EffectParameter("Vibrance", 0f, -100f, 100f, "%"),
                    "Saturation" to EffectParameter("Saturation", 0f, -100f, 100f, "%"),

                    // 3. Detail & Presence
                    "Clarity" to EffectParameter("Clarity", 0f, -100f, 100f, "%"),
                    "Texture" to EffectParameter("Texture", 0f, -100f, 100f, "%"),
                    "Sharpening" to EffectParameter("Sharpening", 0f, 0f, 100f, "%"),
                    "SharpeningRadius" to EffectParameter("Sharpening Radius", 1f, 0.1f, 10f, "px"),
                    "SharpeningMasking" to EffectParameter("Sharpening Masking", 0f, 0f, 100f, "%"),
                    "Dehaze" to EffectParameter("Dehaze", 0f, -100f, 100f, "%"),

                    // 4. HSL / Color Mixer (8 colors x 3 metrics)
                    "HSL_Red_Hue" to EffectParameter("Red Hue", 0f, -100f, 100f),
                    "HSL_Red_Sat" to EffectParameter("Red Saturation", 0f, -100f, 100f),
                    "HSL_Red_Lum" to EffectParameter("Red Luminance", 0f, -100f, 100f),

                    "HSL_Orange_Hue" to EffectParameter("Orange Hue", 0f, -100f, 100f),
                    "HSL_Orange_Sat" to EffectParameter("Orange Saturation", 0f, -100f, 100f),
                    "HSL_Orange_Lum" to EffectParameter("Orange Luminance", 0f, -100f, 100f),

                    "HSL_Yellow_Hue" to EffectParameter("Yellow Hue", 0f, -100f, 100f),
                    "HSL_Yellow_Sat" to EffectParameter("Yellow Saturation", 0f, -100f, 100f),
                    "HSL_Yellow_Lum" to EffectParameter("Yellow Luminance", 0f, -100f, 100f),

                    "HSL_Green_Hue" to EffectParameter("Green Hue", 0f, -100f, 100f),
                    "HSL_Green_Sat" to EffectParameter("Green Saturation", 0f, -100f, 100f),
                    "HSL_Green_Lum" to EffectParameter("Green Luminance", 0f, -100f, 100f),

                    "HSL_Aqua_Hue" to EffectParameter("Aqua Hue", 0f, -100f, 100f),
                    "HSL_Aqua_Sat" to EffectParameter("Aqua Saturation", 0f, -100f, 100f),
                    "HSL_Aqua_Lum" to EffectParameter("Aqua Luminance", 0f, -100f, 100f),

                    "HSL_Blue_Hue" to EffectParameter("Blue Hue", 0f, -100f, 100f),
                    "HSL_Blue_Sat" to EffectParameter("Blue Saturation", 0f, -100f, 100f),
                    "HSL_Blue_Lum" to EffectParameter("Blue Luminance", 0f, -100f, 100f),

                    "HSL_Purple_Hue" to EffectParameter("Purple Hue", 0f, -100f, 100f),
                    "HSL_Purple_Sat" to EffectParameter("Purple Saturation", 0f, -100f, 100f),
                    "HSL_Purple_Lum" to EffectParameter("Purple Luminance", 0f, -100f, 100f),

                    "HSL_Magenta_Hue" to EffectParameter("Magenta Hue", 0f, -100f, 100f),
                    "HSL_Magenta_Sat" to EffectParameter("Magenta Saturation", 0f, -100f, 100f),
                    "HSL_Magenta_Lum" to EffectParameter("Magenta Luminance", 0f, -100f, 100f),

                    // 5. Technical Correction & Utilities
                    "NoiseLuminance" to EffectParameter("Noise Reduction: Lum", 0f, 0f, 100f, "%"),
                    "NoiseColor" to EffectParameter("Noise Reduction: Color", 0f, 0f, 100f, "%"),
                    "Vignetting" to EffectParameter("Vignetting", 0f, -100f, 100f, "%"),
                    "Grain" to EffectParameter("Film Grain", 0f, 0f, 100f, "%")
                )
            )
            else -> StudioEffect.GaussianBlur(id = id)
        }
    }

    val ALL_TYPES_BY_CATEGORY = mapOf(
        "Layer Styles (fx)" to listOf(
            "DropShadow", "InnerShadow", "OuterGlow", "InnerGlow",
            "BevelEmboss", "Satin", "ColorOverlay", "GradientOverlay",
            "PatternOverlay", "Stroke", "GlassMorphism", "ReededGlass"
        ),
        "Core Filters" to listOf(
            "GaussianBlur", "MotionBlur", "RadialBlur", "LensBlur",
            "Pinch", "Ripple", "Spherize", "Twirl", "Wave", "ZigZag",
            "AddNoise", "Despeckle", "DustScratches", "Median",
            "ColorHalftone", "Crystallize", "Mosaic", "Pointillize",
            "Clouds", "LensFlare", "LightingEffects", "UnsharpMask",
            "SmartSharpen", "HighPass", "FindEdges", "Emboss",
            "OilPaint", "Solarize", "Wind", "ColorGrading"
        ),
        "Filter Gallery" to listOf(
            "Artistic", "BrushStrokes", "Sketch", "Texture"
        ),
        "Advanced & AI Engines" to listOf(
            "Liquify", "CameraRaw", "NeuralFilters", "PixelStretch"
        )
    )
}

data class StudioLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType,
    val positionX: Float,
    val positionY: Float,
    val width: Float,
    val height: Float,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val rotation: Float = 0f, // in degrees
    val skewX: Float = 0f,
    val skewY: Float = 0f,
    val perspX: Float = 0f,
    val perspY: Float = 0f,
    val opacity: Float = 1.0f,
    val blendMode: ZenithBlendMode = ZenithBlendMode.NORMAL,
    val isVisible: Boolean = true,
    val isAlphaLocked: Boolean = false,
    val isClippingMask: Boolean = false,
    val isAspectLocked: Boolean = true,
    val effects: List<StudioEffect> = emptyList(),
    // Type specific options
    val textContent: String = "",
    val baseColor: Color = Color.White,
    val brushPoints: List<Offset> = emptyList(), // Store hand drawings
    val imageResourceId: Int? = null,             // ID of reference vector
    val imageUri: String? = null,                  // Loaded dynamic photo/bitmap path
    // Shape styling parameters (edit shape tab support)
    val cornerRadius: Float = 0f,
    val polygonEdges: Int = 5,
    val starInnerRadiusRatio: Float = 0.4f,
    val strokeThickness: Float = -1f, // -1f representing FILL, values > 0f represent outline stroke thickness
    val pivotX: Float = 0.5f,
    val pivotY: Float = 0.5f,
    // Text formatting and font parameters
    val fontSize: Float = 36f,
    val fontFamilyName: String = "Sans-Serif",
    val fontIsBold: Boolean = false,
    val fontIsItalic: Boolean = false,
    val fontAlign: String = "Center",
    val fontPath: String? = null,
    val parentGroupId: String? = null
)
