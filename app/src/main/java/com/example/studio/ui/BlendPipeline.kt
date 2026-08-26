package com.example.studio.ui

import android.graphics.Bitmap
import com.example.studio.model.ZenithBlendMode
import kotlin.math.*

object BlendPipeline {

    // Reusable thread-local buffer to avoid heap allocations in HSL pixel calculations
    private class HslBuffer {
        var h: Float = 0f
        var s: Float = 0f
        var l: Float = 0f
        var r: Float = 0f
        var g: Float = 0f
        var b: Float = 0f
    }

    private val localHslSrc = object : ThreadLocal<HslBuffer>() {
        override fun initialValue() = HslBuffer()
    }

    private val localHslDst = object : ThreadLocal<HslBuffer>() {
        override fun initialValue() = HslBuffer()
    }

    // Helper to clamp a float to [0.0, 1.0]
    private fun clamp(v: Float): Float = v.coerceIn(0f, 1f)

    // GC-free inline RGB to HSL helper
    private fun rgbToHsl(r: Float, g: Float, b: Float, out: HslBuffer) {
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
        out.h = h
        out.s = s
        out.l = l
    }

    // GC-free inline HSL to RGB helper
    private fun hslToRgb(h: Float, s: Float, l: Float, out: HslBuffer) {
        if (s == 0f) {
            out.r = l
            out.g = l
            out.b = l
            return
        }

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        val hRad = h / 360f
        out.r = clamp(hueToRgb(p, q, hRad + 1f / 3f))
        out.g = clamp(hueToRgb(p, q, hRad))
        out.b = clamp(hueToRgb(p, q, hRad - 1f / 3f))
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
            ZenithBlendMode.HARD_LIGHT -> {
                if (a < 0.5f) {
                    2f * a * b
                } else {
                    1f - 2f * (1f - a) * (1f - b)
                }
            }
            ZenithBlendMode.SOFT_LIGHT -> {
                if (a < 0.5f) {
                    b - (1f - 2f * a) * b * (1f - b)
                } else {
                    val db = if (b <= 0.25f) ((16f * b - 12f) * b + 4f) * b else sqrt(b)
                    b + (2f * a - 1f) * (db - b)
                }
            }
            ZenithBlendMode.DIFFERENCE -> abs(a - b)
            ZenithBlendMode.EXCLUSION -> a + b - 2f * a * b
            else -> a // Default to Normal
        }
    }

    // High performance per-pixel blending processing with zero-GC row chunking
    fun blendBitmaps(
        source: Bitmap,
        destination: Bitmap,
        mode: ZenithBlendMode,
        alpha: Float
    ): Bitmap {
        val width = source.width
        val height = source.height

        if (destination.width != width || destination.height != height) {
            return source
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Process in small 2048-pixel chunks to prevent allocating multi-megabyte arrays on heap
        val chunkSize = 2048
        val srcChunk = IntArray(chunkSize)
        val dstChunk = IntArray(chunkSize)
        val resChunk = IntArray(chunkSize)

        val totalPixels = width * height
        var offset = 0

        val srcHsl = localHslSrc.get() ?: HslBuffer()
        val dstHsl = localHslDst.get() ?: HslBuffer()

        while (offset < totalPixels) {
            val count = minOf(chunkSize, totalPixels - offset)
            val y = offset / width
            val x = offset % width

            source.getPixels(srcChunk, 0, width, x, y, minOf(width - x, count), (count + width - 1) / width)
            destination.getPixels(dstChunk, 0, width, x, y, minOf(width - x, count), (count + width - 1) / width)

            for (i in 0 until count) {
                val srcColor = srcChunk[i]
                val dstColor = dstChunk[i]

                val sa = (srcColor ushr 24 and 0xFF) / 255f
                val sr = (srcColor ushr 16 and 0xFF) / 255f
                val sg = (srcColor ushr 8 and 0xFF) / 255f
                val sb = (srcColor and 0xFF) / 255f

                val da = (dstColor ushr 24 and 0xFF) / 255f
                val dr = (dstColor ushr 16 and 0xFF) / 255f
                val dg = (dstColor ushr 8 and 0xFF) / 255f
                val db = (dstColor and 0xFF) / 255f

                val effectiveAlpha = sa * alpha

                if (effectiveAlpha <= 0f) {
                    resChunk[i] = dstColor
                    continue
                }

                var r = 0f
                var g = 0f
                var b = 0f

                when (mode) {
                    ZenithBlendMode.HUE, ZenithBlendMode.SATURATION, ZenithBlendMode.COLOR, ZenithBlendMode.LUMINOSITY -> {
                        rgbToHsl(sr, sg, sb, srcHsl)
                        rgbToHsl(dr, dg, db, dstHsl)

                        var outH = srcHsl.h
                        var outS = srcHsl.s
                        var outL = srcHsl.l

                        when (mode) {
                            ZenithBlendMode.HUE -> {
                                outH = srcHsl.h
                                outS = dstHsl.s
                                outL = dstHsl.l
                            }
                            ZenithBlendMode.SATURATION -> {
                                outH = dstHsl.h
                                outS = srcHsl.s
                                outL = dstHsl.l
                            }
                            ZenithBlendMode.COLOR -> {
                                outH = srcHsl.h
                                outS = srcHsl.s
                                outL = dstHsl.l
                            }
                            ZenithBlendMode.LUMINOSITY -> {
                                outH = dstHsl.h
                                outS = dstHsl.s
                                outL = srcHsl.l
                            }
                            else -> {}
                        }

                        hslToRgb(outH, outS, outL, srcHsl)
                        r = srcHsl.r
                        g = srcHsl.g
                        b = srcHsl.b
                    }
                    else -> {
                        r = blendChannel(sr, dr, mode)
                        g = blendChannel(sg, dg, mode)
                        b = blendChannel(sb, db, mode)
                    }
                }

                val finalA = effectiveAlpha + da * (1f - effectiveAlpha)

                var finalR = 0f
                var finalG = 0f
                var finalB = 0f
                
                if (finalA > 0f) {
                    finalR = (r * effectiveAlpha * da + sr * effectiveAlpha * (1f - da) + dr * da * (1f - effectiveAlpha)) / finalA
                    finalG = (g * effectiveAlpha * da + sg * effectiveAlpha * (1f - da) + dg * da * (1f - effectiveAlpha)) / finalA
                    finalB = (b * effectiveAlpha * da + sb * effectiveAlpha * (1f - da) + db * da * (1f - effectiveAlpha)) / finalA
                }

                val aInt = (finalA * 255f + 0.5f).toInt().coerceIn(0, 255)
                val rInt = (finalR * 255f + 0.5f).toInt().coerceIn(0, 255)
                val gInt = (finalG * 255f + 0.5f).toInt().coerceIn(0, 255)
                val bInt = (finalB * 255f + 0.5f).toInt().coerceIn(0, 255)

                resChunk[i] = (aInt shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
            }

            result.setPixels(resChunk, 0, width, x, y, minOf(width - x, count), (count + width - 1) / width)
            offset += count
        }

        return result
    }
}

