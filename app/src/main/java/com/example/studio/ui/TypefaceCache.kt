package com.example.studio.ui

import android.graphics.Typeface
import android.util.LruCache
import java.io.File

object TypefaceCache {
    private const val MAX_CACHE_SIZE = 50

    private val lruCache = object : LruCache<String, Typeface>(MAX_CACHE_SIZE) {
        override fun sizeOf(key: String, value: Typeface): Int = 1
    }

    @Synchronized
    fun get(fontPath: String?, fontFamilyName: String?, isBold: Boolean, isItalic: Boolean): Typeface {
        val style = if (isBold && isItalic) {
            Typeface.BOLD_ITALIC
        } else if (isBold) {
            Typeface.BOLD
        } else if (isItalic) {
            Typeface.ITALIC
        } else {
            Typeface.NORMAL
        }

        val cacheKey = "p:$fontPath|f:$fontFamilyName|s:$style"

        val cached = lruCache.get(cacheKey)
        if (cached != null) {
            return cached
        }

        val typeface = try {
            val effectivePath = when {
                !fontPath.isNullOrEmpty() && File(fontPath).exists() -> fontPath
                !fontFamilyName.isNullOrEmpty() && File(fontFamilyName).exists() -> fontFamilyName
                else -> null
            }

            if (!effectivePath.isNullOrEmpty()) {
                val baseTf = Typeface.createFromFile(effectivePath)
                Typeface.create(baseTf, style)
            } else {
                val famLower = fontFamilyName?.lowercase() ?: ""
                val family = when {
                    famLower.contains("mono") -> Typeface.MONOSPACE
                    famLower.contains("serif") && !famLower.contains("sans") -> Typeface.SERIF
                    famLower.contains("sans") -> Typeface.SANS_SERIF
                    famLower.contains("cursive") || famLower.contains("pacifico") || famLower.contains("dancing") || famLower.contains("caveat") || famLower.contains("script") -> Typeface.create("cursive", style)
                    famLower.contains("casual") || famLower.contains("indie") -> Typeface.create("casual", style)
                    !fontFamilyName.isNullOrEmpty() -> Typeface.create(fontFamilyName, style)
                    else -> Typeface.DEFAULT
                }
                Typeface.create(family, style)
            }
        } catch (e: Exception) {
            try {
                Typeface.defaultFromStyle(style)
            } catch (ex: Exception) {
                Typeface.DEFAULT
            }
        }

        lruCache.put(cacheKey, typeface)
        return typeface
    }

    @Synchronized
    fun clear() {
        lruCache.evictAll()
    }
}
