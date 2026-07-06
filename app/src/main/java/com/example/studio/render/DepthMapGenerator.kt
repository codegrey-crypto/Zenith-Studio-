package com.example.studio.render

import android.graphics.Bitmap

object DepthMapGenerator {
    /**
     * Generates a pseudo-depth map from the bitmap's alpha channel or luminance.
     * Values are normalized to 0.0 .. 1.0 (where 1.0 represents the closest depth).
     */
    fun generateDepthMap(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val size = width * height
        val depthMap = FloatArray(size)
        val pixels = IntArray(size)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in 0 until size) {
            val color = pixels[i]
            val alpha = (color ushr 24) and 0xFF
            val r = (color ushr 16) and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = color and 0xFF
            
            // Standard relative luminance formula
            val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            
            if (alpha == 0) {
                depthMap[i] = 0f
            } else {
                // Combine alpha transparency and inverted luminance for depth cues
                depthMap[i] = (alpha / 255f) * (1f - luminance)
            }
        }
        return depthMap
    }
}
