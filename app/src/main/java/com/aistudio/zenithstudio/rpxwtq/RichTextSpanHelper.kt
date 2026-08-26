package com.aistudio.zenithstudio.rpxwtq

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import com.example.studio.model.StudioLayer
import com.example.studio.ui.TypefaceCache
import org.json.JSONArray
import org.json.JSONObject

data class RichTextSpan(
    val start: Int,
    val end: Int,
    val colorHex: String? = null,
    val fontSize: Float? = null,
    val fontFamilyName: String? = null,
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val isUnderline: Boolean? = null,
    val isStrikethrough: Boolean? = null,
    val backgroundColorHex: String? = null
)

class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = typeface
    }
    override fun updateMeasureState(tp: TextPaint) {
        tp.typeface = typeface
    }
}

object RichTextSpanHelper {
    fun parseSpans(json: String?): List<RichTextSpan> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<RichTextSpan>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val start = obj.optInt("start", 0)
                val end = obj.optInt("end", 0)
                if (end > start) {
                    val colorHex = if (obj.has("colorHex")) obj.optString("colorHex", null) else null
                    val fontSize = if (obj.has("fontSize")) obj.optDouble("fontSize", 36.0).toFloat() else null
                    val fontFamilyName = if (obj.has("fontFamilyName")) obj.optString("fontFamilyName", null) else null
                    val isBold = if (obj.has("isBold")) obj.optBoolean("isBold") else null
                    val isItalic = if (obj.has("isItalic")) obj.optBoolean("isItalic") else null
                    val isUnderline = if (obj.has("isUnderline")) obj.optBoolean("isUnderline") else null
                    val isStrikethrough = if (obj.has("isStrikethrough")) obj.optBoolean("isStrikethrough") else null
                    val backgroundColorHex = if (obj.has("backgroundColorHex")) obj.optString("backgroundColorHex", null) else null

                    list.add(
                        RichTextSpan(
                            start = start,
                            end = end,
                            colorHex = if (colorHex == "null" || colorHex.isNullOrBlank()) null else colorHex,
                            fontSize = fontSize,
                            fontFamilyName = if (fontFamilyName == "null" || fontFamilyName.isNullOrBlank()) null else fontFamilyName,
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline,
                            isStrikethrough = isStrikethrough,
                            backgroundColorHex = if (backgroundColorHex == "null" || backgroundColorHex.isNullOrBlank()) null else backgroundColorHex
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toJson(spans: List<RichTextSpan>): String {
        val array = JSONArray()
        for (span in spans) {
            val obj = JSONObject()
            obj.put("start", span.start)
            obj.put("end", span.end)
            if (span.colorHex != null) obj.put("colorHex", span.colorHex)
            if (span.fontSize != null) obj.put("fontSize", span.fontSize.toDouble())
            if (span.fontFamilyName != null) obj.put("fontFamilyName", span.fontFamilyName)
            if (span.isBold != null) obj.put("isBold", span.isBold)
            if (span.isItalic != null) obj.put("isItalic", span.isItalic)
            if (span.isUnderline != null) obj.put("isUnderline", span.isUnderline)
            if (span.isStrikethrough != null) obj.put("isStrikethrough", span.isStrikethrough)
            if (span.backgroundColorHex != null) obj.put("backgroundColorHex", span.backgroundColorHex)
            array.put(obj)
        }
        return array.toString()
    }

    fun buildSpannableText(text: String, layer: StudioLayer): CharSequence {
        val spans = parseSpans(layer.richTextSpansJson)
        return buildSpannableText(text, spans, layer)
    }

    fun buildSpannableText(text: String, spans: List<RichTextSpan>, layer: StudioLayer): CharSequence {
        if (spans.isEmpty() || text.isEmpty()) return text

        val builder = SpannableStringBuilder(text)
        val textLength = text.length

        for (span in spans) {
            val start = span.start.coerceIn(0, textLength)
            val end = span.end.coerceIn(0, textLength)
            if (start >= end) continue

            // Color
            if (!span.colorHex.isNullOrBlank()) {
                try {
                    val colorInt = AndroidColor.parseColor(span.colorHex)
                    builder.setSpan(ForegroundColorSpan(colorInt), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } catch (e: Exception) {}
            }

            // Background Color
            if (!span.backgroundColorHex.isNullOrBlank()) {
                try {
                    val bgColorInt = AndroidColor.parseColor(span.backgroundColorHex)
                    builder.setSpan(BackgroundColorSpan(bgColorInt), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } catch (e: Exception) {}
            }

            // Font Size
            if (span.fontSize != null && span.fontSize > 0f) {
                builder.setSpan(AbsoluteSizeSpan(span.fontSize.toInt(), true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // Typeface & Font Family / Bold / Italic
            val famName = span.fontFamilyName ?: layer.fontFamilyName
            val bold = span.isBold ?: layer.fontIsBold
            val italic = span.isItalic ?: layer.fontIsItalic
            if (span.fontFamilyName != null || span.isBold != null || span.isItalic != null) {
                val fontPathToUse = span.fontFamilyName ?: layer.fontPath
                val tf = TypefaceCache.get(fontPathToUse, famName, bold, italic)
                builder.setSpan(CustomTypefaceSpan(tf), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // Underline
            if (span.isUnderline == true) {
                builder.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // Strikethrough
            if (span.isStrikethrough == true) {
                builder.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        return builder
    }

    fun applyStyleToRange(
        currentSpans: List<RichTextSpan>,
        rangeStart: Int,
        rangeEnd: Int,
        colorHex: String? = null,
        fontSize: Float? = null,
        fontFamilyName: String? = null,
        isBold: Boolean? = null,
        isItalic: Boolean? = null,
        isUnderline: Boolean? = null,
        isStrikethrough: Boolean? = null,
        backgroundColorHex: String? = null,
        clearFormat: Boolean = false
    ): List<RichTextSpan> {
        if (rangeStart >= rangeEnd) return currentSpans

        val result = mutableListOf<RichTextSpan>()

        for (existing in currentSpans) {
            // Check overlap with [rangeStart, rangeEnd]
            if (existing.end <= rangeStart || existing.start >= rangeEnd) {
                // Non-overlapping
                result.add(existing)
            } else {
                // Overlapping span -> split if necessary
                if (existing.start < rangeStart) {
                    result.add(existing.copy(end = rangeStart))
                }
                if (existing.end > rangeEnd) {
                    result.add(existing.copy(start = rangeEnd))
                }
            }
        }

        if (!clearFormat) {
            var mergedColorHex = colorHex
            var mergedFontSize = fontSize
            var mergedFontFamilyName = fontFamilyName
            var mergedIsBold = isBold
            var mergedIsItalic = isItalic
            var mergedIsUnderline = isUnderline
            var mergedIsStrikethrough = isStrikethrough
            var mergedBgColorHex = backgroundColorHex

            for (existing in currentSpans) {
                if (existing.start < rangeEnd && existing.end > rangeStart) {
                    if (mergedColorHex == null) mergedColorHex = existing.colorHex
                    if (mergedFontSize == null) mergedFontSize = existing.fontSize
                    if (mergedFontFamilyName == null) mergedFontFamilyName = existing.fontFamilyName
                    if (mergedIsBold == null) mergedIsBold = existing.isBold
                    if (mergedIsItalic == null) mergedIsItalic = existing.isItalic
                    if (mergedIsUnderline == null) mergedIsUnderline = existing.isUnderline
                    if (mergedIsStrikethrough == null) mergedIsStrikethrough = existing.isStrikethrough
                    if (mergedBgColorHex == null) mergedBgColorHex = existing.backgroundColorHex
                }
            }

            result.add(
                RichTextSpan(
                    start = rangeStart,
                    end = rangeEnd,
                    colorHex = mergedColorHex,
                    fontSize = mergedFontSize,
                    fontFamilyName = mergedFontFamilyName,
                    isBold = mergedIsBold,
                    isItalic = mergedIsItalic,
                    isUnderline = mergedIsUnderline,
                    isStrikethrough = mergedIsStrikethrough,
                    backgroundColorHex = mergedBgColorHex
                )
            )
        }

        return result.sortedBy { it.start }
    }
}
