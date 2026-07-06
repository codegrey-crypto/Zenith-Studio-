package com.example.studio.ui

import android.graphics.Bitmap
import com.example.studio.model.ZenithBlendMode
import kotlin.math.*

object BlendPipeline {

    // Helper to clamp a float to [0.0, 1.0]
    private fun clamp(v: Float): Float = v.coerceIn(0f, 1f)

    // RGB to HSL helper
    // Returns FloatArray(3) with H in [0, 360], S in [0, 1], L in [0, 1]
    private fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        var h = 0f
        var s = 0f
        val l = (max + min) / 2f

        if (max != min) {
            val d = max - min
            s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
            h = when (max) {
                r -> (g - b) / d + (if (g < b) 6f else 0f)
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
            h *= 60f
        }
        return floatArrayOf(h, s, l)
    }

    // HSL to RGB helper
    private fun hslToRgb(h: Float, s: Float, l: Float): FloatArray {
        if (s == 0f) {
            return floatArrayOf(l, l, l) // achromatic
        }

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        val hRad = h / 360f
        val r = hueToRgb(p, q, hRad + 1f / 3f)
        val g = hueToRgb(p, q, hRad)
        val b = hueToRgb(p, q, hRad - 1f / 3f)

        return floatArrayOf(clamp(r), clamp(g), clamp(b))
    }

    private fun hueToRgb(p: Float, q: Float, t: Float): Float {
        var tr = t
        if (tr < 0f) tr += 1f
        if (tr > 1f) tr -= 1f
        if (tr < 1f / 6f) return p + (q - p) * 6f * tr
        if (tr < 1f / 2f) return q
        if (tr < 2f / 3f) return p + (q - p) * (2f / 3f - tr) * 6f
        return p
    }

    // Main blend mode math solver for a single normalized channel
    // a = Source (incoming, Normalized Float)
    // b = Destination (backdrop, Normalized Float)
    fun blendChannel(a: Float, b: Float, mode: ZenithBlendMode): Float {
        return when (mode) {
            ZenithBlendMode.NORMAL -> a
            ZenithBlendMode.DARKEN -> min(a, b)
            ZenithBlendMode.MULTIPLY -> a * b
            ZenithBlendMode.COLOR_BURN -> {
                if (a <= 0f) 0f else clamp(1f - (1f - b) / a)
            }
            ZenithBlendMode.LIGHTEN -> max(a, b)
            ZenithBlendMode.SCREEN -> 1f - (1f - a) * (1f - b)
            ZenithBlendMode.LINEAR_DODGE, ZenithBlendMode.ADD -> min(a + b, 1f)
            ZenithBlendMode.COLOR_DODGE -> {
                if (a >= 1f) 1f else clamp(b / (1f - a))
            }
            ZenithBlendMode.OVERLAY -> {
                if (b < 0.5f) {
                    2f * a * b
                } else {
                    1f - 2f * (1f - a) * (1f - b)
                }
            }
            ZenithBlendMode.DIFFERENCE -> abs(a - b)
            ZenithBlendMode.EXCLUSION -> a + b - 2f * a * b
            else -> a // HSL modes or unhandled default to Normal
        }
    }

    // High performance per-pixel blending processing
    fun blendBitmaps(
        source: Bitmap,
        destination: Bitmap,
        mode: ZenithBlendMode,
        alpha: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        
        // Ensure same bounds
        if (destination.width != width || destination.height != height) {
            return source
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        val resPixels = IntArray(width * height)

        source.getPixels(srcPixels, 0, width, 0, 0, width, height)
        destination.getPixels(dstPixels, 0, width, 0, 0, width, height)

        for (i in 0 until (width * height)) {
            val srcColor = srcPixels[i]
            val dstColor = dstPixels[i]

            val sa = (srcColor ushr 24 and 0xFF) / 255f
            val sr = (srcColor ushr 16 and 0xFF) / 255f
            val sg = (srcColor ushr 8 and 0xFF) / 255f
            val sb = (srcColor and 0xFF) / 255f

            val da = (dstColor ushr 24 and 0xFF) / 255f
            val dr = (dstColor ushr 16 and 0xFF) / 255f
            val dg = (dstColor ushr 8 and 0xFF) / 255f
            val db = (dstColor and 0xFF) / 255f

            // Effective layer alpha opacity intersected with pixel alpha
            val effectiveAlpha = sa * alpha

            if (effectiveAlpha <= 0f) {
                resPixels[i] = dstColor
                continue
            }

            try {
                var r = 0f
                var g = 0f
                var b = 0f

                when (mode) {
                    ZenithBlendMode.HUE, ZenithBlendMode.SATURATION, ZenithBlendMode.COLOR, ZenithBlendMode.LUMINOSITY -> {
                        // Transform both to HSL
                        val srcHsl = rgbToHsl(sr, sg, sb)
                        val dstHsl = rgbToHsl(dr, dg, db)

                        val finalHsl = FloatArray(3)
                        when (mode) {
                            ZenithBlendMode.HUE -> {
                                finalHsl[0] = srcHsl[0]
                                finalHsl[1] = dstHsl[1]
                                finalHsl[2] = dstHsl[2]
                            }
                            ZenithBlendMode.SATURATION -> {
                                finalHsl[0] = dstHsl[0]
                                finalHsl[1] = srcHsl[1]
                                finalHsl[2] = dstHsl[2]
                            }
                            ZenithBlendMode.COLOR -> {
                                finalHsl[0] = srcHsl[0]
                                finalHsl[1] = srcHsl[1]
                                finalHsl[2] = dstHsl[2]
                            }
                            ZenithBlendMode.LUMINOSITY -> {
                                finalHsl[0] = dstHsl[0]
                                finalHsl[1] = dstHsl[1]
                                finalHsl[2] = srcHsl[2]
                            }
                            else -> {
                                finalHsl[0] = srcHsl[0]
                                finalHsl[1] = srcHsl[1]
                                finalHsl[2] = srcHsl[2]
                            }
                        }

                        val rgb = hslToRgb(finalHsl[0], finalHsl[1], finalHsl[2])
                        r = rgb[0]
                        g = rgb[1]
                        b = rgb[2]
                    }
                    else -> {
                        r = blendChannel(sr, dr, mode)
                        g = blendChannel(sg, dg, mode)
                        b = blendChannel(sb, db, mode)
                    }
                }

                // Alpha Compositing Intersect:
                // C_final = (C * Alpha) + (B * (1 - Alpha))
                val finalR = r * effectiveAlpha + dr * (1f - effectiveAlpha)
                val finalG = g * effectiveAlpha + dg * (1f - effectiveAlpha)
                val finalB = b * effectiveAlpha + db * (1f - effectiveAlpha)

                // Output alpha
                val finalA = clamp(effectiveAlpha + da * (1f - effectiveAlpha))

                val aInt = (finalA * 255f + 0.5f).toInt().coerceIn(0, 255)
                val rInt = (finalR * 255f + 0.5f).toInt().coerceIn(0, 255)
                val gInt = (finalG * 255f + 0.5f).toInt().coerceIn(0, 255)
                val bInt = (finalB * 255f + 0.5f).toInt().coerceIn(0, 255)

                resPixels[i] = (aInt shl 24) or (rInt shl 16) or (gInt shl 8) or bInt

            } catch (e: Exception) {
                // Graceful fallback to NORMAL blending
                val finalR = sr * effectiveAlpha + dr * (1f - effectiveAlpha)
                val finalG = sg * effectiveAlpha + dg * (1f - effectiveAlpha)
                val finalB = sb * effectiveAlpha + db * (1f - effectiveAlpha)
                val finalA = clamp(effectiveAlpha + da * (1f - effectiveAlpha))

                val aInt = (finalA * 255f + 0.5f).toInt().coerceIn(0, 255)
                val rInt = (finalR * 255f + 0.5f).toInt().coerceIn(0, 255)
                val gInt = (finalG * 255f + 0.5f).toInt().coerceIn(0, 255)
                val bInt = (finalB * 255f + 0.5f).toInt().coerceIn(0, 255)

                resPixels[i] = (aInt shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
            }
        }

        result.setPixels(resPixels, 0, width, 0, 0, width, height)
        return result
    }
}
