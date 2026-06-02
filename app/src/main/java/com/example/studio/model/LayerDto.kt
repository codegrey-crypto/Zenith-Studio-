package com.example.studio.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@JsonClass(generateAdapter = true)
data class EffectDto(
    val typeName: String,
    val id: String,
    val name: String,
    val paramsMap: Map<String, Float>,
    val isEnabled: Boolean? = true
) {
    fun toEffect(): StudioEffect {
        val enabled = isEnabled ?: true
        return when (typeName) {
            "GaussianBlur" -> {
                val radiusVal = paramsMap["Radius"] ?: 12f
                val intensityVal = paramsMap["Intensity"] ?: 0.8f
                var effect = StudioEffect.GaussianBlur(id = id, name = name, isEnabled = enabled)
                effect = effect.updateParameter("Radius", radiusVal) as StudioEffect.GaussianBlur
                effect = effect.updateParameter("Intensity", intensityVal) as StudioEffect.GaussianBlur
                effect
            }
            "InnerGlow" -> {
                val chokeVal = paramsMap["Choke"] ?: 15f
                val opacityVal = paramsMap["Opacity"] ?: 0.65f
                var effect = StudioEffect.InnerGlow(id = id, name = name, isEnabled = enabled)
                effect = effect.updateParameter("Choke", chokeVal) as StudioEffect.InnerGlow
                effect = effect.updateParameter("Opacity", opacityVal) as StudioEffect.InnerGlow
                effect
            }
            "ColorBalance" -> {
                val hueShiftVal = paramsMap["HueShift"] ?: 0f
                val brightnessVal = paramsMap["Brightness"] ?: 1.0f
                val saturationVal = paramsMap["Saturation"] ?: 1.0f
                var effect = StudioEffect.ColorBalance(id = id, name = name, isEnabled = enabled)
                effect = effect.updateParameter("HueShift", hueShiftVal) as StudioEffect.ColorBalance
                effect = effect.updateParameter("Brightness", brightnessVal) as StudioEffect.ColorBalance
                effect = effect.updateParameter("Saturation", saturationVal) as StudioEffect.ColorBalance
                effect
            }
            "Invert" -> {
                StudioEffect.Invert(id = id, name = name, isEnabled = enabled)
            }
            "Threshold" -> {
                val thresholdVal = paramsMap["Threshold"] ?: 0.5f
                var effect = StudioEffect.Threshold(id = id, name = name, isEnabled = enabled)
                effect = effect.updateParameter("Threshold", thresholdVal) as StudioEffect.Threshold
                effect
            }
            "Posterize" -> {
                val levelsVal = paramsMap["Levels"] ?: 4f
                var effect = StudioEffect.Posterize(id = id, name = name, isEnabled = enabled)
                effect = effect.updateParameter("Levels", levelsVal) as StudioEffect.Posterize
                effect
            }
            else -> {
                if (typeName.startsWith("PSEffect_")) {
                    val effType = typeName.substringAfter("PSEffect_")
                    var effect = PhotoshopEffectTemplates.create(id = id, effectType = effType) as StudioEffect.PhotoshopEffect
                    effect = effect.copy(isEnabled = enabled)
                    paramsMap.forEach { (pKey, pVal) ->
                        effect = effect.updateParameter(pKey, pVal) as StudioEffect.PhotoshopEffect
                    }
                    effect
                } else {
                    StudioEffect.GaussianBlur(isEnabled = enabled)
                }
            }
        }
    }

    companion object {
        fun fromEffect(effect: StudioEffect): EffectDto {
            val params = effect.parameters.mapValues {
                val v = it.value.value
                if (v.isNaN() || v.isInfinite()) 0f else v
            }
            return EffectDto(
                typeName = when (effect) {
                    is StudioEffect.GaussianBlur -> "GaussianBlur"
                    is StudioEffect.InnerGlow -> "InnerGlow"
                    is StudioEffect.ColorBalance -> "ColorBalance"
                    is StudioEffect.Invert -> "Invert"
                    is StudioEffect.Threshold -> "Threshold"
                    is StudioEffect.Posterize -> "Posterize"
                    is StudioEffect.PhotoshopEffect -> "PSEffect_${effect.effectType}"
                },
                id = effect.id,
                name = effect.name,
                paramsMap = params,
                isEnabled = effect.isEnabled
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class LayerDto(
    val id: String,
    val name: String,
    val typeName: String,
    val positionX: Float,
    val positionY: Float,
    val width: Float,
    val height: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float,
    val opacity: Float,
    val blendModeName: String,
    val isVisible: Boolean,
    val isAlphaLocked: Boolean,
    val isClippingMask: Boolean = false,
    val skewX: Float = 0f,
    val skewY: Float = 0f,
    val perspX: Float = 0f,
    val perspY: Float = 0f,
    val effects: List<EffectDto>,
    val textContent: String,
    val baseColorValue: Long,
    val brushPointsFlat: List<Float>,
    val imageResourceId: Int?,
    val imageUri: String? = null,
    val cornerRadius: Float = 0f,
    val polygonEdges: Int = 5,
    val starInnerRadiusRatio: Float = 0.4f,
    val strokeThickness: Float = -1f,
    val pivotX: Float = 0.5f,
    val pivotY: Float = 0.5f,
    val fontSize: Float? = 36f,
    val fontFamilyName: String? = "Sans-Serif",
    val fontIsBold: Boolean? = false,
    val fontIsItalic: Boolean? = false,
    val fontAlign: String? = "Center",
    val fontPath: String? = null
) {
    fun toLayer(): StudioLayer {
        val pts = mutableListOf<Offset>()
        for (i in 0 until brushPointsFlat.size step 2) {
            if (i + 1 < brushPointsFlat.size) {
                val x = brushPointsFlat[i]
                val y = brushPointsFlat[i + 1]
                if (x == 999994f && y == 999994f) {
                    pts.add(Offset.Unspecified)
                } else {
                    pts.add(Offset(x, y))
                }
            }
        }
        return StudioLayer(
            id = id,
            name = name,
            type = try { LayerType.valueOf(typeName) } catch (e: Exception) { LayerType.VECTOR_RECT },
            positionX = positionX,
            positionY = positionY,
            width = width,
            height = height,
            scaleX = scaleX,
            scaleY = scaleY,
            rotation = rotation,
            skewX = skewX,
            skewY = skewY,
            perspX = perspX,
            perspY = perspY,
            opacity = opacity,
            blendMode = try { ZenithBlendMode.valueOf(blendModeName) } catch (e: Exception) { ZenithBlendMode.NORMAL },
            isVisible = isVisible,
            isAlphaLocked = isAlphaLocked,
            isClippingMask = isClippingMask,
            effects = effects.map { it.toEffect() },
            textContent = textContent,
            baseColor = Color(baseColorValue.toULong()),
            brushPoints = pts,
            imageResourceId = imageResourceId,
            imageUri = imageUri,
            cornerRadius = cornerRadius,
            polygonEdges = polygonEdges,
            starInnerRadiusRatio = starInnerRadiusRatio,
            strokeThickness = strokeThickness,
            pivotX = pivotX,
            pivotY = pivotY,
            fontSize = fontSize ?: 36f,
            fontFamilyName = fontFamilyName ?: "Sans-Serif",
            fontIsBold = fontIsBold ?: false,
            fontIsItalic = fontIsItalic ?: false,
            fontAlign = fontAlign ?: "Center",
            fontPath = fontPath
        )
    }

    companion object {
        private fun Float.sanitize(default: Float = 0f): Float = if (this.isNaN() || this.isInfinite()) default else this

        fun fromLayer(layer: StudioLayer): LayerDto {
            val flatPts = mutableListOf<Float>()
            for (p in layer.brushPoints) {
                if (p.x.isNaN() || p.y.isNaN()) {
                    flatPts.add(999994f)
                    flatPts.add(999994f)
                } else {
                    flatPts.add(p.x)
                    flatPts.add(p.y)
                }
            }
            return LayerDto(
                id = layer.id,
                name = layer.name,
                typeName = layer.type.name,
                positionX = layer.positionX.sanitize(),
                positionY = layer.positionY.sanitize(),
                width = layer.width.sanitize(100f),
                height = layer.height.sanitize(100f),
                scaleX = layer.scaleX.sanitize(1f),
                scaleY = layer.scaleY.sanitize(1f),
                rotation = layer.rotation.sanitize(),
                opacity = layer.opacity.sanitize(1f),
                blendModeName = layer.blendMode.name,
                isVisible = layer.isVisible,
                isAlphaLocked = layer.isAlphaLocked,
                isClippingMask = layer.isClippingMask,
                skewX = layer.skewX.sanitize(),
                skewY = layer.skewY.sanitize(),
                perspX = layer.perspX.sanitize(),
                perspY = layer.perspY.sanitize(),
                effects = layer.effects.map { EffectDto.fromEffect(it) },
                textContent = layer.textContent,
                baseColorValue = layer.baseColor.value.toLong(),
                brushPointsFlat = flatPts,
                imageResourceId = layer.imageResourceId,
                imageUri = layer.imageUri,
                cornerRadius = layer.cornerRadius.sanitize(),
                polygonEdges = layer.polygonEdges,
                starInnerRadiusRatio = layer.starInnerRadiusRatio.sanitize(0.4f),
                strokeThickness = layer.strokeThickness.sanitize(-1f),
                pivotX = layer.pivotX.sanitize(0.5f),
                pivotY = layer.pivotY.sanitize(0.5f),
                fontSize = layer.fontSize.sanitize(36f),
                fontFamilyName = layer.fontFamilyName,
                fontIsBold = layer.fontIsBold,
                fontIsItalic = layer.fontIsItalic,
                fontAlign = layer.fontAlign,
                fontPath = layer.fontPath
            )
        }
    }
}

object LayerSerializer {
    private val moshi: Moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, LayerDto::class.java)
    private val adapter = moshi.adapter<List<LayerDto>>(listType)

    fun serialize(layers: List<StudioLayer>): String {
        val dtos = layers.map { LayerDto.fromLayer(it) }
        return adapter.toJson(dtos) ?: "[]"
    }

    fun deserialize(json: String): List<StudioLayer> {
        if (json.isBlank()) return emptyList()
        return try {
            val dtos = adapter.fromJson(json)
            dtos?.map { it.toLayer() } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
