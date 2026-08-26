package com.example.studio.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types


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
    val perspWarpEnabled: Boolean = false,
    val perspWarpSplitY: Float = 0.5f,
    val perspWarpWidth: Float = 1.0f,
    val perspWarpHeight: Float = 1.0f,
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
    val letterSpacing: Float? = 0f,
    val lineSpacing: Float? = 1.0f,
    val fontFamilyName: String? = "Sans-Serif",
    val fontIsBold: Boolean? = false,
    val fontIsItalic: Boolean? = false,
    val fontAlign: String? = "Center",
    val fontPath: String? = null,
    val richTextSpansJson: String? = "",
    val isAspectLocked: Boolean? = true,
    val parentGroupId: String? = null,
    val bezierNodeTypes: List<String>? = emptyList(),
    val perspWarpPointsStr: String? = "",
    val meshWarpPointsStr: String? = "",
    val warpRepeatMode: String? = "Off",
    val warpRepeatX: Float? = 1.0f,
    val warpRepeatY: Float? = 1.0f,
    val warpPhaseX: Float? = 0.0f,
    val warpPhaseY: Float? = 0.0f,
    val warpInterpolation: Boolean? = true,
    val warpTarget: String? = "Layer",
    val warpMeshDivisionX: Int? = 3,
    val warpMeshDivisionY: Int? = 3,
    val cropLeft: Float? = 0f,
    val cropTop: Float? = 0f,
    val cropRight: Float? = 1.0f,
    val cropBottom: Float? = 1.0f,
    val adjBrightness: Float? = 0f,
    val adjContrast: Float? = 1f,
    val adjSaturation: Float? = 1f,
    val adjColorTintValue: Long? = 0L,
    val adjTintColorIntensity: Float? = 0f,
    val threeDStateJson: String? = null
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
            perspWarpEnabled = perspWarpEnabled,
            perspWarpSplitY = perspWarpSplitY,
            perspWarpWidth = perspWarpWidth,
            perspWarpHeight = perspWarpHeight,
            opacity = opacity,
            blendMode = try { 
                ZenithBlendMode.valueOf(blendModeName.uppercase()) 
            } catch (e: Exception) { 
                ZenithBlendMode.values().firstOrNull { it.displayName.equals(blendModeName, ignoreCase = true) } ?: ZenithBlendMode.NORMAL 
            },
            isVisible = isVisible,
            isAlphaLocked = isAlphaLocked,
            isClippingMask = isClippingMask,
            isAspectLocked = isAspectLocked ?: true,
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
            letterSpacing = letterSpacing ?: 0f,
            lineSpacing = lineSpacing ?: 1.0f,
            fontFamilyName = fontFamilyName ?: "Sans-Serif",
            fontIsBold = fontIsBold ?: false,
            fontIsItalic = fontIsItalic ?: false,
            fontAlign = fontAlign ?: "Center",
            fontPath = fontPath,
            richTextSpansJson = richTextSpansJson ?: "",
            parentGroupId = parentGroupId,
            bezierNodeTypes = bezierNodeTypes ?: emptyList(),
            perspWarpPointsStr = perspWarpPointsStr ?: "",
            meshWarpPointsStr = meshWarpPointsStr ?: "",
            warpRepeatMode = warpRepeatMode ?: "Off",
            warpRepeatX = warpRepeatX ?: 1.0f,
            warpRepeatY = warpRepeatY ?: 1.0f,
            warpPhaseX = warpPhaseX ?: 0.0f,
            warpPhaseY = warpPhaseY ?: 0.0f,
            warpInterpolation = warpInterpolation ?: true,
            warpTarget = warpTarget ?: "Layer",
            warpMeshDivisionX = warpMeshDivisionX ?: 3,
            warpMeshDivisionY = warpMeshDivisionY ?: 3,
            cropLeft = cropLeft ?: 0f,
            cropTop = cropTop ?: 0f,
            cropRight = cropRight ?: 1.0f,
            cropBottom = cropBottom ?: 1.0f,
            adjBrightness = adjBrightness ?: 0f,
            adjContrast = adjContrast ?: 1f,
            adjSaturation = adjSaturation ?: 1f,
            adjColorTint = androidx.compose.ui.graphics.Color((adjColorTintValue ?: 0L).toULong()),
            adjTintColorIntensity = adjTintColorIntensity ?: 0f,
            threeDStateJson = threeDStateJson
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
                perspWarpEnabled = layer.perspWarpEnabled,
                perspWarpSplitY = layer.perspWarpSplitY.sanitize(0.5f),
                perspWarpWidth = layer.perspWarpWidth.sanitize(1.0f),
                perspWarpHeight = layer.perspWarpHeight.sanitize(1.0f),
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
                letterSpacing = layer.letterSpacing.sanitize(0f),
                lineSpacing = layer.lineSpacing.sanitize(1.0f),
                fontFamilyName = layer.fontFamilyName,
                fontIsBold = layer.fontIsBold,
                fontIsItalic = layer.fontIsItalic,
                fontAlign = layer.fontAlign,
                fontPath = layer.fontPath,
                richTextSpansJson = layer.richTextSpansJson,
                isAspectLocked = layer.isAspectLocked,
                parentGroupId = layer.parentGroupId,
                bezierNodeTypes = layer.bezierNodeTypes,
                perspWarpPointsStr = layer.perspWarpPointsStr,
                meshWarpPointsStr = layer.meshWarpPointsStr,
                warpRepeatMode = layer.warpRepeatMode,
                warpRepeatX = layer.warpRepeatX.sanitize(1.0f),
                warpRepeatY = layer.warpRepeatY.sanitize(1.0f),
                warpPhaseX = layer.warpPhaseX.sanitize(),
                warpPhaseY = layer.warpPhaseY.sanitize(),
                warpInterpolation = layer.warpInterpolation,
                warpTarget = layer.warpTarget,
                warpMeshDivisionX = layer.warpMeshDivisionX,
                warpMeshDivisionY = layer.warpMeshDivisionY,
                cropLeft = layer.cropLeft.sanitize(0f),
                cropTop = layer.cropTop.sanitize(0f),
                cropRight = layer.cropRight.sanitize(1.0f),
                cropBottom = layer.cropBottom.sanitize(1.0f),
                adjBrightness = layer.adjBrightness.sanitize(0f),
                adjContrast = layer.adjContrast.sanitize(1f),
                adjSaturation = layer.adjSaturation.sanitize(1f),
                adjColorTintValue = layer.adjColorTint.value.toLong(),
                adjTintColorIntensity = layer.adjTintColorIntensity.sanitize(0f),
                threeDStateJson = layer.threeDStateJson
            )
        }
    }
}


data class ArtboardDto(
    val id: String,
    val name: String,
    val width: Float,
    val height: Float,
    val offsetX: Float,
    val offsetY: Float,
    val layers: List<LayerDto>
)


data class WorkspaceStateDto(
    val artboards: List<ArtboardDto>,
    val selectedArtboardId: String,
    val zenithFilters: Map<String, List<EffectDto>>? = null
    
)

object LayerSerializer {
    private val moshi: Moshi = Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, LayerDto::class.java)
    private val adapter = moshi.adapter<List<LayerDto>>(listType)
    private val workspaceStateAdapter = moshi.adapter(WorkspaceStateDto::class.java)

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

    fun serializeWorkspace(artboards: List<com.example.studio.ui.ArtboardData>, selectedId: String): String {
        val dtos = artboards.map { art ->
            ArtboardDto(
                id = art.id,
                name = art.name,
                width = art.width,
                height = art.height,
                offsetX = art.offsetX,
                offsetY = art.offsetY,
                layers = art.layers.map { LayerDto.fromLayer(it) }
            )
        }
        val zenithMap = mutableMapOf<String, List<EffectDto>>()
        com.aistudio.zenithstudio.rpxwtq.EffectStackManager.filtersByLayer.forEach { (layerId, filters) ->
            val serializedFilters = filters.map { filter ->
                val baseTemplateId = filter.id.substringBefore("_copy_").substringBefore("_dup_")
                EffectDto(
                    typeName = "Zenith_$baseTemplateId",
                    id = filter.id,
                    name = filter.name,
                    paramsMap = filter.parameters.associate { it.name to it.currentValue },
                    isEnabled = filter.isEnabled
                )
            }
            zenithMap[layerId] = serializedFilters
        }
        val workspace = WorkspaceStateDto(
            artboards = dtos,
            selectedArtboardId = selectedId,
            zenithFilters = zenithMap
            
        )
        return workspaceStateAdapter.toJson(workspace) ?: "{}"
    }

    fun deserializeWorkspace(json: String, canvasW: Float, canvasH: Float): Pair<List<com.example.studio.ui.ArtboardData>, String> {
        if (json.isBlank()) {
            return Pair(
                listOf(
                    com.example.studio.ui.ArtboardData(
                        id = "default",
                        name = "Artboard 1",
                        width = canvasW,
                        height = canvasH,
                        layers = emptyList()
                    )
                ),
                "default"
            )
        }
        return try {
            val trimmed = json.trimStart()
            if (trimmed.startsWith("{")) {
                val workspace = workspaceStateAdapter.fromJson(json)
                if (workspace != null && workspace.artboards.isNotEmpty()) {
com.aistudio.zenithstudio.rpxwtq.EffectStackManager.filtersByLayer.clear()
workspace.zenithFilters?.forEach { (layerId, dtos) ->
val list = androidx.compose.runtime.mutableStateListOf<com.aistudio.zenithstudio.rpxwtq.ZenithFilter>()
dtos.forEach { dto ->
val baseTemplateId = dto.typeName.removePrefix("Zenith_")
val template = com.aistudio.zenithstudio.rpxwtq.ZenithFilterFactory.getFilterTemplate(baseTemplateId)
if (template != null) {
var filter = template.duplicate(dto.id)
if (dto.isEnabled == false) {
filter = filter.toggleEnabled()
}
dto.paramsMap.forEach { (pName, pValue) ->
filter = filter.copyWithParameter(pName, pValue)
}
list.add(filter)
}
}
com.aistudio.zenithstudio.rpxwtq.EffectStackManager.filtersByLayer[layerId] = list
}
com.aistudio.zenithstudio.rpxwtq.EffectStackManager.changeCounter.value++

                    val arts = workspace.artboards.map { dto ->
                        com.example.studio.ui.ArtboardData(
                            id = dto.id,
                            name = dto.name,
                            width = dto.width,
                            height = dto.height,
                            offsetX = dto.offsetX,
                            offsetY = dto.offsetY,
                            layers = dto.layers.map { it.toLayer() }
                        )
                    }
                    return Pair(arts, workspace.selectedArtboardId)
                }
            }
            // Fallback for legacy format
            val legacyLayers = deserialize(json)
            val defaultArt = com.example.studio.ui.ArtboardData(
                id = "default",
                name = "Artboard 1",
                width = canvasW,
                height = canvasH,
                layers = legacyLayers
            )
            Pair(listOf(defaultArt), "default")
        } catch (e: Exception) {
            e.printStackTrace()
            val defaultArt = com.example.studio.ui.ArtboardData(
                id = "default",
                name = "Artboard 1",
                width = canvasW,
                height = canvasH,
                layers = emptyList()
            )
            Pair(listOf(defaultArt), "default")
        }
    }
}
