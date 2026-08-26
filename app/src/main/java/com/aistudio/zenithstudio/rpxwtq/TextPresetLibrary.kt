package com.aistudio.zenithstudio.rpxwtq

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.studio.model.EffectParameter
import com.example.studio.model.StudioEffect
import com.example.studio.model.StudioLayer
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TextEffectConfig(
    val effectType: String,
    val name: String,
    val isEnabled: Boolean = true,
    val parameterValues: Map<String, Float> = emptyMap()
)

data class TextPreset(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val isCustom: Boolean = false,
    val fontFamilyName: String = "Roboto",
    val fontPath: String? = null,
    val fontSize: Float = 64f,
    val letterSpacing: Float = 0.05f,
    val lineSpacing: Float = 1.0f,
    val fontIsBold: Boolean = true,
    val fontIsItalic: Boolean = false,
    val fontAlign: String = "Center",
    val baseColorHex: String = "#FFFFFFFF",
    val opacity: Float = 1.0f,
    val strokeThickness: Float = 0f,
    val effects: List<TextEffectConfig> = emptyList(),
    val tags: List<String> = emptyList()
)

object TextPresetManager {
    private const val PREFS_NAME = "zenith_text_presets_store"
    private const val KEY_CUSTOM_PRESETS = "custom_saved_text_presets_json"

    val presetChangeCounter = mutableStateOf(0)

    val categories = listOf(
        "All",
        "Custom Saved",
        "3D & Metallic",
        "Neon & Glow",
        "Retro & Vintage",
        "Gradient & Modern",
        "Minimal & Elegant",
        "Comic & Pop",
        "Glitch & Sci-Fi"
    )

    fun getBuiltInPresets(): List<TextPreset> {
        return listOf(
            TextPreset(
                id = "builtin_3d_gold_chrome",
                name = "3D Gold Chrome",
                description = "Lustrous 3D metallic gold text with rich bevel highlights and deep drop shadow.",
                category = "3D & Metallic",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 72f,
                letterSpacing = 0.08f,
                fontIsBold = true,
                baseColorHex = "#FFFFD700",
                strokeThickness = 1.5f,
                tags = listOf("Gold", "3D", "Chrome", "Bevel", "Metallic"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 14f,
                            "DropShadow_Size" to 18f,
                            "DropShadow_Angle" to 120f,
                            "DropShadow_Opacity" to 0.75f,
                            "DropShadow_Color_R" to 0.05f,
                            "DropShadow_Color_G" to 0.05f,
                            "DropShadow_Color_B" to 0.05f,
                            "InnerStroke_Enabled" to 1f,
                            "InnerStroke_Size" to 2f,
                            "InnerStroke_Opacity" to 0.9f,
                            "InnerStroke_Color_R" to 1.0f,
                            "InnerStroke_Color_G" to 0.95f,
                            "InnerStroke_Color_B" to 0.6f
                        )
                    ),
                    TextEffectConfig(
                        effectType = "BevelEmboss",
                        name = "Bevel & Emboss",
                        parameterValues = mapOf(
                            "Size" to 8f,
                            "Depth" to 150f,
                            "Angle" to 120f,
                            "Altitude" to 35f
                        )
                    )
                )
            ),
            TextPreset(
                id = "builtin_neon_cyan_glow",
                name = "Electric Cyan Glow",
                description = "Vibrant cyberpunk electric cyan text with intense outer aura and dark drop shadow.",
                category = "Neon & Glow",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 68f,
                letterSpacing = 0.12f,
                fontIsBold = true,
                baseColorHex = "#FF00FFFF",
                tags = listOf("Neon", "Cyan", "Glow", "Cyberpunk", "Electric"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 8f,
                            "DropShadow_Size" to 22f,
                            "DropShadow_Angle" to 90f,
                            "DropShadow_Opacity" to 0.85f,
                            "DropShadow_Color_R" to 0f,
                            "DropShadow_Color_G" to 0.8f,
                            "DropShadow_Color_B" to 1.0f,
                            "OuterBorder_Enabled" to 1f,
                            "OuterBorder_Size" to 3f,
                            "OuterBorder_Opacity" to 0.95f,
                            "OuterBorder_Color_R" to 0.2f,
                            "OuterBorder_Color_G" to 1.0f,
                            "OuterBorder_Color_B" to 1.0f
                        )
                    ),
                    TextEffectConfig(
                        effectType = "OuterGlow",
                        name = "Outer Glow",
                        parameterValues = mapOf(
                            "Size" to 25f,
                            "Opacity" to 0.9f
                        )
                    )
                )
            ),
            TextPreset(
                id = "builtin_retro_synthwave_80s",
                name = "Synthwave 80s Pink",
                description = "Hot magenta pink text with retro purple border stroke and angled drop shadow.",
                category = "Retro & Vintage",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 70f,
                letterSpacing = 0.10f,
                fontIsBold = true,
                fontIsItalic = true,
                baseColorHex = "#FFFF007F",
                strokeThickness = 3f,
                tags = listOf("Retro", "80s", "Synthwave", "Pink", "Magenta"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 12f,
                            "DropShadow_Size" to 8f,
                            "DropShadow_Angle" to 135f,
                            "DropShadow_Opacity" to 0.85f,
                            "DropShadow_Color_R" to 0.35f,
                            "DropShadow_Color_G" to 0f,
                            "DropShadow_Color_B" to 0.55f,
                            "OuterStroke_Enabled" to 1f,
                            "OuterStroke_Size" to 4f,
                            "OuterStroke_Opacity" to 1f,
                            "OuterStroke_Color_R" to 0.6f,
                            "OuterStroke_Color_G" to 0f,
                            "OuterStroke_Color_B" to 0.9f
                        )
                    )
                )
            ),
            TextPreset(
                id = "builtin_comic_pop_out",
                name = "Comic Pop Yellow",
                description = "Bold comic book yellow text with thick black outer stroke and sharp offset shadow.",
                category = "Comic & Pop",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 76f,
                letterSpacing = 0.04f,
                fontIsBold = true,
                baseColorHex = "#FFFFEB3B",
                strokeThickness = 5f,
                tags = listOf("Comic", "Pop Art", "Yellow", "Outline", "Bold"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 10f,
                            "DropShadow_Size" to 2f,
                            "DropShadow_Angle" to 135f,
                            "DropShadow_Opacity" to 0.95f,
                            "DropShadow_Color_R" to 0f,
                            "DropShadow_Color_G" to 0f,
                            "DropShadow_Color_B" to 0f,
                            "OuterStroke_Enabled" to 1f,
                            "OuterStroke_Size" to 6f,
                            "OuterStroke_Opacity" to 1f,
                            "OuterStroke_Color_R" to 0f,
                            "OuterStroke_Color_G" to 0f,
                            "OuterStroke_Color_B" to 0f
                        )
                    )
                )
            ),
            TextPreset(
                id = "builtin_sunset_gradient_overlay",
                name = "Sunset Amber Gradient",
                description = "Rich sunset orange-amber gradient with warm glowing border and soft drop shadow.",
                category = "Gradient & Modern",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 68f,
                letterSpacing = 0.06f,
                fontIsBold = true,
                baseColorHex = "#FFFF6F00",
                tags = listOf("Sunset", "Gradient", "Amber", "Warm", "Modern"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 10f,
                            "DropShadow_Size" to 15f,
                            "DropShadow_Angle" to 90f,
                            "DropShadow_Opacity" to 0.65f,
                            "DropShadow_Color_R" to 0.4f,
                            "DropShadow_Color_G" to 0.1f,
                            "DropShadow_Color_B" to 0f,
                            "OuterBorder_Enabled" to 1f,
                            "OuterBorder_Size" to 2f,
                            "OuterBorder_Opacity" to 0.8f,
                            "OuterBorder_Color_R" to 1.0f,
                            "OuterBorder_Color_G" to 0.8f,
                            "OuterBorder_Color_B" to 0.2f
                        )
                    ),
                    TextEffectConfig(
                        effectType = "GradientOverlay",
                        name = "Gradient Overlay",
                        parameterValues = mapOf(
                            "Angle" to 90f,
                            "Scale" to 100f,
                            "Opacity" to 0.9f
                        )
                    )
                )
            ),
            TextPreset(
                id = "builtin_frosted_glassmorphic",
                name = "Frosted Glassmorphic",
                description = "Modern translucent text with crisp white outline stroke and soft inner shadow.",
                category = "Minimal & Elegant",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 64f,
                letterSpacing = 0.15f,
                fontIsBold = true,
                baseColorHex = "#80FFFFFF",
                strokeThickness = 2f,
                tags = listOf("Glass", "Minimal", "Frosted", "White", "Clean"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 6f,
                            "DropShadow_Size" to 12f,
                            "DropShadow_Angle" to 120f,
                            "DropShadow_Opacity" to 0.4f,
                            "DropShadow_Color_R" to 0f,
                            "DropShadow_Color_G" to 0f,
                            "DropShadow_Color_B" to 0f,
                            "InnerShadow_Enabled" to 1f,
                            "InnerShadow_Distance" to 3f,
                            "InnerShadow_Size" to 6f,
                            "InnerShadow_Angle" to 120f,
                            "InnerShadow_Opacity" to 0.5f,
                            "InnerShadow_Color_R" to 1f,
                            "InnerShadow_Color_G" to 1f,
                            "InnerShadow_Color_B" to 1f
                        )
                    )
                )
            ),
            TextPreset(
                id = "builtin_cyberpunk_glitch",
                name = "Cyber Acid Glitch",
                description = "High-octane neon acid green text with offset purple ghost shadow and wide tracking.",
                category = "Glitch & Sci-Fi",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 72f,
                letterSpacing = 0.20f,
                fontIsBold = true,
                baseColorHex = "#FF76FF03",
                tags = listOf("Glitch", "Acid", "Sci-Fi", "Green", "Cyberpunk"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 15f,
                            "DropShadow_Size" to 4f,
                            "DropShadow_Angle" to 45f,
                            "DropShadow_Opacity" to 0.9f,
                            "DropShadow_Color_R" to 0.7f,
                            "DropShadow_Color_G" to 0f,
                            "DropShadow_Color_B" to 1.0f,
                            "OuterStroke_Enabled" to 1f,
                            "OuterStroke_Size" to 3f,
                            "OuterStroke_Opacity" to 1f,
                            "OuterStroke_Color_R" to 0.1f,
                            "OuterStroke_Color_G" to 0.1f,
                            "OuterStroke_Color_B" to 0.1f
                        )
                    )
                )
            ),
            TextPreset(
                id = "builtin_royal_gold_outline",
                name = "Royal Gold Outline",
                description = "Deep purple text enclosed in a regal gold dual stroke and soft ambient shadow.",
                category = "3D & Metallic",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 68f,
                letterSpacing = 0.12f,
                fontIsBold = true,
                baseColorHex = "#FF3A1C71",
                strokeThickness = 2.5f,
                tags = listOf("Royal", "Gold", "Purple", "Luxury", "Outline"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 10f,
                            "DropShadow_Size" to 14f,
                            "DropShadow_Angle" to 120f,
                            "DropShadow_Opacity" to 0.6f,
                            "DropShadow_Color_R" to 0f,
                            "DropShadow_Color_G" to 0f,
                            "DropShadow_Color_B" to 0f,
                            "OuterBorder_Enabled" to 1f,
                            "OuterBorder_Size" to 3.5f,
                            "OuterBorder_Opacity" to 1f,
                            "OuterBorder_Color_R" to 1.0f,
                            "OuterBorder_Color_G" to 0.84f,
                            "OuterBorder_Color_B" to 0f
                        )
                    )
                )
            ),
            TextPreset(
                id = "builtin_fire_amber_embers",
                name = "Fire Amber Embers",
                description = "Fiery intense crimson-amber text with burning orange glow and heavy drop shadow.",
                category = "Gradient & Modern",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 72f,
                letterSpacing = 0.08f,
                fontIsBold = true,
                baseColorHex = "#FFD50000",
                tags = listOf("Fire", "Amber", "Flame", "Red", "Glow"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 12f,
                            "DropShadow_Size" to 20f,
                            "DropShadow_Angle" to 90f,
                            "DropShadow_Opacity" to 0.85f,
                            "DropShadow_Color_R" to 1.0f,
                            "DropShadow_Color_G" to 0.4f,
                            "DropShadow_Color_B" to 0f,
                            "OuterStroke_Enabled" to 1f,
                            "OuterStroke_Size" to 3f,
                            "OuterStroke_Opacity" to 1f,
                            "OuterStroke_Color_R" to 1.0f,
                            "OuterStroke_Color_G" to 0.7f,
                            "OuterStroke_Color_B" to 0f
                        )
                    )
                )
            ),
            TextPreset(
                id = "builtin_minimal_dark_mono",
                name = "Minimal Dark Mono",
                description = "Ultra sleek dark slate typography with sharp white stroke outline and subtle elevation.",
                category = "Minimal & Elegant",
                isCustom = false,
                fontFamilyName = "Roboto",
                fontSize = 64f,
                letterSpacing = 0.18f,
                fontIsBold = true,
                baseColorHex = "#FF18181B",
                strokeThickness = 2f,
                tags = listOf("Minimal", "Dark", "Slate", "Clean", "Monochrome"),
                effects = listOf(
                    TextEffectConfig(
                        effectType = "BordersAndShadows",
                        name = "Borders & Shadows",
                        parameterValues = mapOf(
                            "DropShadow_Enabled" to 1f,
                            "DropShadow_Distance" to 8f,
                            "DropShadow_Size" to 10f,
                            "DropShadow_Angle" to 120f,
                            "DropShadow_Opacity" to 0.5f,
                            "DropShadow_Color_R" to 0f,
                            "DropShadow_Color_G" to 0f,
                            "DropShadow_Color_B" to 0f,
                            "OuterBorder_Enabled" to 1f,
                            "OuterBorder_Size" to 2f,
                            "OuterBorder_Opacity" to 0.9f,
                            "OuterBorder_Color_R" to 0.9f,
                            "OuterBorder_Color_G" to 0.9f,
                            "OuterBorder_Color_B" to 0.9f
                        )
                    )
                )
            )
        )
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCustomPresets(context: Context): List<TextPreset> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<TextPreset>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(parsePresetJsonObject(obj))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getAllPresets(context: Context): List<TextPreset> {
        return getBuiltInPresets() + getCustomPresets(context)
    }

    fun saveCustomPreset(
        context: Context,
        name: String,
        description: String,
        category: String,
        currentLayer: StudioLayer?
    ) {
        if (currentLayer == null) {
            Toast.makeText(context, "No active text layer selected", Toast.LENGTH_SHORT).show()
            return
        }

        val titleToUse = name.ifBlank { "Custom Text Preset" }
        val descToUse = description.ifBlank { "Saved from layer text configuration" }
        val catToUse = category.ifBlank { "Custom Saved" }

        val effectsList = mutableListOf<TextEffectConfig>()
        for (effect in currentLayer.effects) {
            if (!effect.isEnabled) continue
            if (effect is StudioEffect.PhotoshopEffect) {
                val paramMap = effect.parameters.mapValues { it.value.value }
                effectsList.add(
                    TextEffectConfig(
                        effectType = effect.effectType,
                        name = effect.name,
                        isEnabled = true,
                        parameterValues = paramMap
                    )
                )
            }
        }

        val baseColorHex = String.format(
            "#%08X",
            currentLayer.baseColor.toArgb()
        )

        val newPreset = TextPreset(
            id = "custom_text_" + UUID.randomUUID().toString(),
            name = titleToUse,
            description = descToUse,
            category = catToUse,
            isCustom = true,
            fontFamilyName = currentLayer.fontFamilyName,
            fontPath = currentLayer.fontPath,
            fontSize = currentLayer.fontSize,
            letterSpacing = currentLayer.letterSpacing,
            lineSpacing = currentLayer.lineSpacing,
            fontIsBold = currentLayer.fontIsBold,
            fontIsItalic = currentLayer.fontIsItalic,
            fontAlign = currentLayer.fontAlign,
            baseColorHex = baseColorHex,
            opacity = currentLayer.opacity,
            strokeThickness = currentLayer.strokeThickness,
            effects = effectsList,
            tags = listOf("Custom", "User Saved")
        )

        val currentCustom = getCustomPresets(context).toMutableList()
        currentCustom.add(0, newPreset)

        val jsonArray = JSONArray()
        for (p in currentCustom) {
            jsonArray.put(toJsonObject(p))
        }

        getPrefs(context).edit().putString(KEY_CUSTOM_PRESETS, jsonArray.toString()).apply()
        presetChangeCounter.value += 1
        Toast.makeText(context, "Saved text preset '$titleToUse'", Toast.LENGTH_SHORT).show()
    }

    fun deleteCustomPreset(context: Context, presetId: String) {
        val currentCustom = getCustomPresets(context).filter { it.id != presetId }
        val jsonArray = JSONArray()
        for (p in currentCustom) {
            jsonArray.put(toJsonObject(p))
        }
        getPrefs(context).edit().putString(KEY_CUSTOM_PRESETS, jsonArray.toString()).apply()
        presetChangeCounter.value += 1
        Toast.makeText(context, "Deleted custom text preset", Toast.LENGTH_SHORT).show()
    }

    fun applyPresetToLayer(
        context: Context,
        layerId: String,
        preset: TextPreset,
        currentLayer: StudioLayer?,
        onUpdateLayer: ((StudioLayer) -> Unit)?
    ) {
        if (currentLayer == null || onUpdateLayer == null) {
            Toast.makeText(context, "Unable to update layer", Toast.LENGTH_SHORT).show()
            return
        }

        val parsedColor = try {
            Color(AndroidColor.parseColor(preset.baseColorHex))
        } catch (e: Exception) {
            Color.White
        }

        val updatedEffects = currentLayer.effects.toMutableList()
        updatedEffects.removeAll { it is StudioEffect.PhotoshopEffect && it.effectType in listOf("BordersAndShadows", "DropShadow", "InnerShadow", "OuterGlow", "BevelEmboss", "GradientOverlay") }

        for (effConfig in preset.effects) {
            val newParamMap = mutableMapOf<String, EffectParameter>()
            for ((key, valFloat) in effConfig.parameterValues) {
                newParamMap[key] = EffectParameter(
                    name = key,
                    value = valFloat,
                    rangeMin = if (key.contains("Angle")) -180f else 0f,
                    rangeMax = when {
                        key.contains("Size") || key.contains("Distance") -> 100f
                        key.contains("Angle") -> 360f
                        else -> 1f
                    }
                )
            }

            val psEffect = StudioEffect.PhotoshopEffect(
                id = UUID.randomUUID().toString(),
                name = effConfig.name,
                category = "Layer Styles (fx)",
                effectType = effConfig.effectType,
                parameters = newParamMap,
                isEnabled = effConfig.isEnabled
            )
            updatedEffects.add(psEffect)
        }

        val updatedLayer = currentLayer.copy(
            fontFamilyName = preset.fontFamilyName,
            fontPath = preset.fontPath,
            fontSize = preset.fontSize,
            letterSpacing = preset.letterSpacing,
            lineSpacing = preset.lineSpacing,
            fontIsBold = preset.fontIsBold,
            fontIsItalic = preset.fontIsItalic,
            fontAlign = preset.fontAlign,
            baseColor = parsedColor,
            opacity = preset.opacity,
            strokeThickness = preset.strokeThickness,
            effects = updatedEffects
        )

        onUpdateLayer(updatedLayer)
        Toast.makeText(context, "Applied text preset '${preset.name}'", Toast.LENGTH_SHORT).show()
    }

    fun exportPresetJson(preset: TextPreset): String {
        return toJsonObject(preset).toString(2)
    }

    fun importPresetJson(context: Context, jsonStr: String): TextPreset? {
        return try {
            val obj = JSONObject(jsonStr)
            val preset = parsePresetJsonObject(obj).copy(
                id = "custom_text_" + UUID.randomUUID().toString(),
                isCustom = true
            )
            val currentCustom = getCustomPresets(context).toMutableList()
            currentCustom.add(0, preset)

            val jsonArray = JSONArray()
            for (p in currentCustom) {
                jsonArray.put(toJsonObject(p))
            }
            getPrefs(context).edit().putString(KEY_CUSTOM_PRESETS, jsonArray.toString()).apply()
            presetChangeCounter.value += 1
            Toast.makeText(context, "Imported text preset '${preset.name}'", Toast.LENGTH_SHORT).show()
            preset
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Invalid text preset JSON string", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun toJsonObject(p: TextPreset): JSONObject {
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("name", p.name)
        obj.put("description", p.description)
        obj.put("category", p.category)
        obj.put("isCustom", p.isCustom)
        obj.put("fontFamilyName", p.fontFamilyName)
        obj.put("fontPath", p.fontPath ?: "")
        obj.put("fontSize", p.fontSize.toDouble())
        obj.put("letterSpacing", p.letterSpacing.toDouble())
        obj.put("lineSpacing", p.lineSpacing.toDouble())
        obj.put("fontIsBold", p.fontIsBold)
        obj.put("fontIsItalic", p.fontIsItalic)
        obj.put("fontAlign", p.fontAlign)
        obj.put("baseColorHex", p.baseColorHex)
        obj.put("opacity", p.opacity.toDouble())
        obj.put("strokeThickness", p.strokeThickness.toDouble())

        val effArr = JSONArray()
        for (eff in p.effects) {
            val eObj = JSONObject()
            eObj.put("effectType", eff.effectType)
            eObj.put("name", eff.name)
            eObj.put("isEnabled", eff.isEnabled)
            val pObj = JSONObject()
            for ((k, v) in eff.parameterValues) {
                pObj.put(k, v.toDouble())
            }
            eObj.put("parameterValues", pObj)
            effArr.put(eObj)
        }
        obj.put("effects", effArr)

        val tagArr = JSONArray()
        for (t in p.tags) tagArr.put(t)
        obj.put("tags", tagArr)

        return obj
    }

    private fun parsePresetJsonObject(obj: JSONObject): TextPreset {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val name = obj.optString("name", "Text Preset")
        val desc = obj.optString("description", "")
        val cat = obj.optString("category", "Custom Saved")
        val isCustom = obj.optBoolean("isCustom", false)
        val fontFamilyName = obj.optString("fontFamilyName", "Roboto")
        val fontPathStr = obj.optString("fontPath", "")
        val fontPath = if (fontPathStr.isBlank()) null else fontPathStr
        val fontSize = obj.optDouble("fontSize", 64.0).toFloat()
        val letterSpacing = obj.optDouble("letterSpacing", 0.05).toFloat()
        val lineSpacing = obj.optDouble("lineSpacing", 1.0).toFloat()
        val fontIsBold = obj.optBoolean("fontIsBold", true)
        val fontIsItalic = obj.optBoolean("fontIsItalic", false)
        val fontAlign = obj.optString("fontAlign", "Center")
        val baseColorHex = obj.optString("baseColorHex", "#FFFFFFFF")
        val opacity = obj.optDouble("opacity", 1.0).toFloat()
        val strokeThickness = obj.optDouble("strokeThickness", 0.0).toFloat()

        val effList = mutableListOf<TextEffectConfig>()
        val effArr = obj.optJSONArray("effects")
        if (effArr != null) {
            for (i in 0 until effArr.length()) {
                val eObj = effArr.getJSONObject(i)
                val effectType = eObj.optString("effectType", "BordersAndShadows")
                val eName = eObj.optString("name", "Borders & Shadows")
                val isEnabled = eObj.optBoolean("isEnabled", true)
                val paramMap = mutableMapOf<String, Float>()
                val pObj = eObj.optJSONObject("parameterValues")
                if (pObj != null) {
                    val keys = pObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        paramMap[k] = pObj.optDouble(k, 0.0).toFloat()
                    }
                }
                effList.add(TextEffectConfig(effectType, eName, isEnabled, paramMap))
            }
        }

        val tagList = mutableListOf<String>()
        val tagArr = obj.optJSONArray("tags")
        if (tagArr != null) {
            for (i in 0 until tagArr.length()) {
                tagList.add(tagArr.getString(i))
            }
        }

        return TextPreset(
            id = id,
            name = name,
            description = desc,
            category = cat,
            isCustom = isCustom,
            fontFamilyName = fontFamilyName,
            fontPath = fontPath,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            lineSpacing = lineSpacing,
            fontIsBold = fontIsBold,
            fontIsItalic = fontIsItalic,
            fontAlign = fontAlign,
            baseColorHex = baseColorHex,
            opacity = opacity,
            strokeThickness = strokeThickness,
            effects = effList,
            tags = tagList
        )
    }
}
