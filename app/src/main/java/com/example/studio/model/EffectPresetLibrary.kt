package com.example.studio.model
import com.aistudio.zenithstudio.rpxwtq.ZenithFilterFactory
import com.aistudio.zenithstudio.rpxwtq.EffectStackManager

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.studio.model.StudioLayer
import com.example.studio.model.ZenithBlendMode
import com.example.studio.ui.ParametricLayerCache
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FilterConfig(
    val templateId: String,
    val isEnabled: Boolean = true,
    val parameterValues: Map<String, Float> = emptyMap()
)

data class LayerAdjustmentsConfig(
    val opacity: Float = 1.0f,
    val blendModeName: String = "Normal",
    val adjBrightness: Float = 0f,
    val adjContrast: Float = 1f,
    val adjSaturation: Float = 1f,
    val adjColorTintHex: String? = null,
    val adjTintColorIntensity: Float = 0f
)

data class EffectPreset(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val isCustom: Boolean = false,
    val filters: List<FilterConfig> = emptyList(),
    val adjustments: LayerAdjustmentsConfig = LayerAdjustmentsConfig(),
    val tags: List<String> = emptyList()
)

object EffectPresetManager {
    private const val PREFS_NAME = "zenith_effect_presets_store"
    private const val KEY_CUSTOM_PRESETS = "custom_saved_presets_json"

    val presetChangeCounter = mutableStateOf(0)

    val categories = listOf(
        "All",
        "Custom Saved",
        "Cinematic",
        "Retro & Vintage",
        "Cyberpunk & Neon",
        "HDR & Clarity",
        "Artistic",
        "Distortion & Glass",
        "Monochrome"
    )

    fun getBuiltInPresets(): List<EffectPreset> {
        return listOf(
            EffectPreset(
                id = "builtin_cyberpunk_neon",
                name = "Cyberpunk Neon",
                description = "High contrast futuristic neon aura with vibrant chromatic shifting and hue split.",
                category = "Cyberpunk & Neon",
                isCustom = false,
                tags = listOf("Neon", "Glow", "Chromatic", "Vibrant"),
                filters = listOf(
                    FilterConfig(
                        templateId = "artistic_neonglow",
                        parameterValues = mapOf("Glow Radius" to 18f)
                    ),
                    FilterConfig(
                        templateId = "color_hue",
                        parameterValues = mapOf("Shift degrees" to 110f)
                    ),
                    FilterConfig(
                        templateId = "color_contrast",
                        parameterValues = mapOf("Amount" to 1.75f)
                    ),
                    FilterConfig(
                        templateId = "color_saturation",
                        parameterValues = mapOf("Factor" to 1.6f)
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjContrast = 1.35f,
                    adjSaturation = 1.5f
                )
            ),
            EffectPreset(
                id = "builtin_vintage_film_1970",
                name = "Vintage 1970s Film",
                description = "Warm analog film aesthetic with gentle sepia tone, soft edge vignette, and organic grain.",
                category = "Retro & Vintage",
                isCustom = false,
                tags = listOf("Vintage", "Film", "Sepia", "Vignette"),
                filters = listOf(
                    FilterConfig(
                        templateId = "color_sepia",
                        parameterValues = mapOf("Intensity" to 0.65f)
                    ),
                    FilterConfig(
                        templateId = "image_toolbox_vignette",
                        parameterValues = mapOf("Radius amount" to 0.7f)
                    ),
                    FilterConfig(
                        templateId = "color_temp",
                        parameterValues = mapOf("Warmth index" to 0.45f)
                    ),
                    FilterConfig(
                        templateId = "color_fast_adjusting",
                        parameterValues = mapOf("Grain" to 15f, "Structure" to 0.2f)
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjContrast = 0.92f,
                    adjSaturation = 0.82f
                )
            ),
            EffectPreset(
                id = "builtin_chroma_key_green",
                name = "Chroma Key (Green Screen)",
                description = "Isolate and remove solid background colors (green, blue, red, or custom) with key tolerance and spill suppression.",
                category = "Artistic",
                isCustom = false,
                tags = listOf("Chroma Key", "Green Screen", "Keyer", "Transparent"),
                filters = listOf(
                    FilterConfig(
                        templateId = "ps_core_filters_chromakey",
                        parameterValues = mapOf(
                            "KeyRed" to 0.0f,
                            "KeyGreen" to 1.0f,
                            "KeyBlue" to 0.0f,
                            "Similarity" to 0.35f,
                            "Smoothness" to 0.15f,
                            "SpillSuppression" to 0.5f
                        )
                    )
                )
            ),
            EffectPreset(
                id = "builtin_cinematic_teal_orange",
                name = "Cinematic Teal & Orange",
                description = "Hollywood blockbuster color grading with deep teal shadows and glowing orange highlights.",
                category = "Cinematic",
                isCustom = false,
                tags = listOf("Blockbuster", "Teal & Orange", "Color Grade"),
                filters = listOf(
                    FilterConfig(
                        templateId = "color_fast_adjusting",
                        parameterValues = mapOf(
                            "Contrast" to 1.3f,
                            "Highlights" to -25f,
                            "Shadows" to 18f,
                            "Temp" to -20f,
                            "Tint" to 12f,
                            "Dehaze" to 22f
                        )
                    ),
                    FilterConfig(
                        templateId = "image_toolbox_vignette",
                        parameterValues = mapOf("Radius amount" to 0.45f)
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjContrast = 1.25f,
                    adjSaturation = 1.2f
                )
            ),
            EffectPreset(
                id = "builtin_hdr_vivid_clarity",
                name = "HDR Vivid Clarity",
                description = "Ultra detailed HDR rendering with boosted micro-contrast, crisp edges, and vivid dynamic range.",
                category = "HDR & Clarity",
                isCustom = false,
                tags = listOf("HDR", "Clarity", "Sharpen", "Detail"),
                filters = listOf(
                    FilterConfig(
                        templateId = "color_fast_adjusting",
                        parameterValues = mapOf(
                            "Dehaze" to 35f,
                            "Structure" to 0.6f,
                            "Exposure" to 0.15f,
                            "Saturation" to 1.35f,
                            "Vibrance" to 25f
                        )
                    ),
                    FilterConfig(
                        templateId = "color_vignette",
                        parameterValues = mapOf("Radius amount" to 0.35f)
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjBrightness = 0.05f,
                    adjContrast = 1.25f,
                    adjSaturation = 1.35f
                )
            ),
            EffectPreset(
                id = "builtin_dramatic_noir",
                name = "Dramatic Film Noir",
                description = "Deep monochrome black & white with aggressive shadow crushing, high contrast, and cinematic vignette.",
                category = "Monochrome",
                isCustom = false,
                tags = listOf("Black & White", "Noir", "High Contrast"),
                filters = listOf(
                    FilterConfig(templateId = "color_grayscale"),
                    FilterConfig(
                        templateId = "color_contrast",
                        parameterValues = mapOf("Amount" to 2.2f)
                    ),
                    FilterConfig(
                        templateId = "image_toolbox_vignette",
                        parameterValues = mapOf("Radius amount" to 0.85f)
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjContrast = 1.5f
                )
            ),
            EffectPreset(
                id = "builtin_glitch_synthwave",
                name = "Synthwave Glitch",
                description = "80s retro arcade distortion with chromatic aberration, hologram scanlines, and digital glitch artifacts.",
                category = "Cyberpunk & Neon",
                isCustom = false,
                tags = listOf("Glitch", "Synthwave", "80s", "Scanlines"),
                filters = listOf(
                    FilterConfig(
                        templateId = "artistic_hologram",
                        parameterValues = mapOf("Fringe Intensity" to 25f)
                    ),
                    FilterConfig(
                        templateId = "color_hue",
                        parameterValues = mapOf("Shift degrees" to -45f)
                    ),
                    FilterConfig(
                        templateId = "color_saturation",
                        parameterValues = mapOf("Factor" to 1.8f)
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjContrast = 1.4f,
                    adjSaturation = 1.6f
                )
            ),
            EffectPreset(
                id = "builtin_shattered_glass_prism",
                name = "Shattered Glass Prism",
                description = "Refractive glassmorphism refraction with shattered prism distortion and spectral light leakage.",
                category = "Distortion & Glass",
                isCustom = false,
                tags = listOf("Glass", "Refraction", "Prism", "Distort"),
                filters = listOf(
                    FilterConfig(
                        templateId = "ps_layer_styles_fx_glassmorphism",
                        parameterValues = mapOf(
                            "FractalIntensity" to 7f,
                            "Radius" to 15f,
                            "RefractionIndex" to 4f
                        )
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjContrast = 1.15f
                )
            ),
            EffectPreset(
                id = "builtin_dreamy_soft_portrait",
                name = "Dreamy Soft Glow",
                description = "Ethereal romantic portrait finish with smooth bloom highlights, soft focus, and warm ambient pastel tone.",
                category = "Artistic",
                isCustom = false,
                tags = listOf("Soft", "Glow", "Bloom", "Dreamy"),
                filters = listOf(
                    FilterConfig(
                        templateId = "image_toolbox_gaussian_blur",
                        parameterValues = mapOf("Radius" to 7f)
                    ),
                    FilterConfig(
                        templateId = "color_fast_adjusting",
                        parameterValues = mapOf(
                            "Exposure" to 0.2f,
                            "Saturation" to 1.1f,
                            "Temp" to 15f,
                            "Highlights" to 20f
                        )
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjBrightness = 0.08f,
                    adjSaturation = 1.1f
                )
            ),
            EffectPreset(
                id = "builtin_halftone_newspaper",
                name = "Halftone Comic Newsprint",
                description = "Classic comic book printing look with CMYK halftone dots, posterized edges, and inked ink outlines.",
                category = "Artistic",
                isCustom = false,
                tags = listOf("Halftone", "Comic", "Pop Art", "Newsprint"),
                filters = listOf(
                    FilterConfig(
                        templateId = "artistic_toon",
                        parameterValues = mapOf("Quantization Levels" to 4f)
                    ),
                    FilterConfig(
                        templateId = "artistic_posteredges",
                        parameterValues = mapOf("Edge Thickness" to 3f)
                    ),
                    FilterConfig(
                        templateId = "color_contrast",
                        parameterValues = mapOf("Amount" to 1.5f)
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjContrast = 1.35f
                )
            ),
            EffectPreset(
                id = "builtin_moody_emerald_forest",
                name = "Moody Emerald Forest",
                description = "Deep cinematic forest green tones with muted warm colors and rich shadow contrast.",
                category = "Cinematic",
                isCustom = false,
                tags = listOf("Moody", "Forest", "Green", "Cinematic"),
                filters = listOf(
                    FilterConfig(
                        templateId = "color_fast_adjusting",
                        parameterValues = mapOf(
                            "Temp" to -25f,
                            "Tint" to -18f,
                            "Saturation" to 0.85f,
                            "Highlights" to -30f,
                            "Shadows" to 22f
                        )
                    ),
                    FilterConfig(
                        templateId = "color_vignette",
                        parameterValues = mapOf("Radius amount" to 0.55f)
                    )
                ),
                adjustments = LayerAdjustmentsConfig(
                    adjContrast = 1.2f
                )
            )
        )
    }

    fun getCustomPresets(context: Context): List<EffectPreset> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()
        val list = mutableListOf<EffectPreset>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(deserializePreset(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getAllPresets(context: Context): List<EffectPreset> {
        return getBuiltInPresets() + getCustomPresets(context)
    }

    fun saveCustomPreset(
        context: Context,
        name: String,
        description: String,
        category: String,
        layerId: String,
        currentLayer: StudioLayer? = null
    ): EffectPreset {
        val activeFilters = EffectStackManager.getFiltersForLayer(layerId)
        val filterConfigs = activeFilters.map { filter ->
            val baseId = if (filter.id.contains("_dup_")) {
                filter.id.substringBefore("_dup_")
            } else filter.id

            FilterConfig(
                templateId = baseId,
                isEnabled = filter.isEnabled,
                parameterValues = filter.parameters.associate { it.name to it.currentValue }
            )
        }

        val adjustments = LayerAdjustmentsConfig(
            opacity = currentLayer?.opacity ?: 1.0f,
            blendModeName = currentLayer?.blendMode?.displayName ?: "Normal",
            adjBrightness = currentLayer?.adjBrightness ?: 0f,
            adjContrast = currentLayer?.adjContrast ?: 1f,
            adjSaturation = currentLayer?.adjSaturation ?: 1f,
            adjTintColorIntensity = currentLayer?.adjTintColorIntensity ?: 0f
        )

        val newPreset = EffectPreset(
            id = "custom_preset_${UUID.randomUUID().toString().take(8)}",
            name = name.ifBlank { "Custom Effect Preset" },
            description = description.ifBlank { "User saved layer adjustment and effect stack preset." },
            category = if (category.isBlank() || category == "All") "Custom Saved" else category,
            isCustom = true,
            filters = filterConfigs,
            adjustments = adjustments,
            tags = listOf("Custom", "User Saved", category)
        )

        val existing = getCustomPresets(context).toMutableList()
        existing.add(0, newPreset)
        saveCustomPresetsToPrefs(context, existing)

        presetChangeCounter.value++
        Toast.makeText(context, "Saved preset: ${newPreset.name}", Toast.LENGTH_SHORT).show()
        return newPreset
    }

    fun deleteCustomPreset(context: Context, presetId: String) {
        val existing = getCustomPresets(context).filter { it.id != presetId }
        saveCustomPresetsToPrefs(context, existing)
        presetChangeCounter.value++
        Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
    }

    fun applyPresetToLayer(
        context: Context,
        layerId: String,
        preset: EffectPreset,
        updateLayerCallback: ((StudioLayer) -> StudioLayer)? = null
    ) {
        EffectStackManager.saveUndoState()

        val layerFilters = EffectStackManager.getFiltersForLayer(layerId)
        layerFilters.clear()

        preset.filters.forEach { filterConfig ->
            val template = ZenithFilterFactory.getFilterTemplate(filterConfig.templateId)
            if (template != null) {
                var newFilter = template.duplicate("${filterConfig.templateId}_dup_${UUID.randomUUID().toString().take(6)}")
                filterConfig.parameterValues.forEach { (paramName, newValue) ->
                    newFilter = newFilter.copyWithParameter(paramName, newValue)
                }
                if (!filterConfig.isEnabled) {
                    newFilter = newFilter.toggleEnabled()
                }
                layerFilters.add(newFilter)
            }
        }

        updateLayerCallback?.let { callback ->
            val updatedLayer = callback.invoke(
                StudioLayer(
                    id = layerId,
                    name = "Active Layer",
                    type = com.example.studio.model.LayerType.IMAGE_CARD,
                    positionX = 0f, positionY = 0f, width = 100f, height = 100f,
                    opacity = preset.adjustments.opacity,
                    adjBrightness = preset.adjustments.adjBrightness,
                    adjContrast = preset.adjustments.adjContrast,
                    adjSaturation = preset.adjustments.adjSaturation,
                    adjTintColorIntensity = preset.adjustments.adjTintColorIntensity
                )
            )
        }

        EffectStackManager.changeCounter.value++
        ParametricLayerCache.invalidate(layerId)

        Toast.makeText(context, "Applied preset: ${preset.name}", Toast.LENGTH_SHORT).show()
    }

    private fun saveCustomPresetsToPrefs(context: Context, presets: List<EffectPreset>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        presets.forEach { preset ->
            jsonArray.put(serializePreset(preset))
        }
        prefs.edit().putString(KEY_CUSTOM_PRESETS, jsonArray.toString()).apply()
    }

    private fun serializePreset(preset: EffectPreset): JSONObject {
        val obj = JSONObject()
        obj.put("id", preset.id)
        obj.put("name", preset.name)
        obj.put("description", preset.description)
        obj.put("category", preset.category)
        obj.put("isCustom", preset.isCustom)

        val tagsArray = JSONArray()
        preset.tags.forEach { tagsArray.put(it) }
        obj.put("tags", tagsArray)

        val filtersArray = JSONArray()
        preset.filters.forEach { f ->
            val fObj = JSONObject()
            fObj.put("templateId", f.templateId)
            fObj.put("isEnabled", f.isEnabled)

            val paramsObj = JSONObject()
            f.parameterValues.forEach { (k, v) ->
                paramsObj.put(k, v.toDouble())
            }
            fObj.put("parameterValues", paramsObj)
            filtersArray.put(fObj)
        }
        obj.put("filters", filtersArray)

        val adjObj = JSONObject()
        adjObj.put("opacity", preset.adjustments.opacity.toDouble())
        adjObj.put("blendModeName", preset.adjustments.blendModeName)
        adjObj.put("adjBrightness", preset.adjustments.adjBrightness.toDouble())
        adjObj.put("adjContrast", preset.adjustments.adjContrast.toDouble())
        adjObj.put("adjSaturation", preset.adjustments.adjSaturation.toDouble())
        adjObj.put("adjTintColorIntensity", preset.adjustments.adjTintColorIntensity.toDouble())
        obj.put("adjustments", adjObj)

        return obj
    }

    private fun deserializePreset(obj: JSONObject): EffectPreset {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val name = obj.optString("name", "Custom Preset")
        val description = obj.optString("description", "")
        val category = obj.optString("category", "Custom Saved")
        val isCustom = obj.optBoolean("isCustom", true)

        val tags = mutableListOf<String>()
        val tagsArray = obj.optJSONArray("tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }

        val filters = mutableListOf<FilterConfig>()
        val filtersArray = obj.optJSONArray("filters")
        if (filtersArray != null) {
            for (i in 0 until filtersArray.length()) {
                val fObj = filtersArray.getJSONObject(i)
                val templateId = fObj.optString("templateId")
                val isEnabled = fObj.optBoolean("isEnabled", true)
                val paramsMap = mutableMapOf<String, Float>()
                val paramsObj = fObj.optJSONObject("parameterValues")
                if (paramsObj != null) {
                    val keys = paramsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        paramsMap[k] = paramsObj.getDouble(k).toFloat()
                    }
                }
                filters.add(FilterConfig(templateId, isEnabled, paramsMap))
            }
        }

        val adjObj = obj.optJSONObject("adjustments")
        val adjustments = if (adjObj != null) {
            LayerAdjustmentsConfig(
                opacity = adjObj.optDouble("opacity", 1.0).toFloat(),
                blendModeName = adjObj.optString("blendModeName", "Normal"),
                adjBrightness = adjObj.optDouble("adjBrightness", 0.0).toFloat(),
                adjContrast = adjObj.optDouble("adjContrast", 1.0).toFloat(),
                adjSaturation = adjObj.optDouble("adjSaturation", 1.0).toFloat(),
                adjTintColorIntensity = adjObj.optDouble("adjTintColorIntensity", 0.0).toFloat()
            )
        } else LayerAdjustmentsConfig()

        return EffectPreset(
            id = id,
            name = name,
            description = description,
            category = category,
            isCustom = isCustom,
            filters = filters,
            adjustments = adjustments,
            tags = tags
        )
    }

    fun exportPresetJson(preset: EffectPreset): String {
        return serializePreset(preset).toString(2)
    }

    fun importPresetJson(context: Context, jsonStr: String): EffectPreset? {
        return try {
            val obj = JSONObject(jsonStr)
            val preset = deserializePreset(obj).copy(
                id = "imported_preset_${UUID.randomUUID().toString().take(6)}",
                isCustom = true
            )
            val existing = getCustomPresets(context).toMutableList()
            existing.add(0, preset)
            saveCustomPresetsToPrefs(context, existing)
            presetChangeCounter.value++
            Toast.makeText(context, "Imported preset: ${preset.name}", Toast.LENGTH_SHORT).show()
            preset
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to parse preset JSON", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
