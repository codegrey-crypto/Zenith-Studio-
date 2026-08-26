package com.example.studio.ui

import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache

data class TextLayoutCacheKey(
    val text: String,
    val textSpansJson: String?,
    val fontPath: String?,
    val fontFamilyName: String?,
    val isBold: Boolean,
    val isItalic: Boolean,
    val color: Int,
    val fontSize: Float,
    val letterSpacing: Float,
    val blurRadius: Float,
    val drawStyleHash: Int,
    val align: String,
    val width: Int,
    val lineSpacing: Float,
    val strokeThickness: Float
)

object TextLayoutCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 32 // Use 1/32nd of available memory for this cache
    
    // Size is not strictly memory size, but an object count
    private val layoutCache = LruCache<TextLayoutCacheKey, StaticLayout>(100)
    private val pathCache = LruCache<TextLayoutCacheKey, android.graphics.Path>(100)
    
    fun get(key: TextLayoutCacheKey, builder: () -> StaticLayout): StaticLayout {
        var layout = layoutCache.get(key)
        if (layout == null) {
            layout = builder()
            layoutCache.put(key, layout)
        }
        return layout
    }

    fun getPath(key: TextLayoutCacheKey, builder: () -> android.graphics.Path): android.graphics.Path {
        var path = pathCache.get(key)
        if (path == null) {
            path = builder()
            pathCache.put(key, path)
        }
        return path
    }
    
    fun clear() {
        layoutCache.evictAll()
        pathCache.evictAll()
    }
}
