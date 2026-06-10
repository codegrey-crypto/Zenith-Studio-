package com.example.studio.ui

import android.graphics.Typeface
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object TypefaceCache {
    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

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

        val cacheKey = if (!fontPath.isNullOrEmpty()) {
            "path|$fontPath|$style"
        } else {
            "name|$fontFamilyName|$style"
        }

        return typefaceCache.getOrPut(cacheKey) {
            try {
                if (!fontPath.isNullOrEmpty() && File(fontPath).exists()) {
                    val baseTf = Typeface.createFromFile(fontPath)
                    Typeface.create(baseTf, style)
                } else {
                    val family = when (fontFamilyName) {
                        "Monospace" -> Typeface.MONOSPACE
                        "Serif" -> Typeface.SERIF
                        "Sans-Serif" -> Typeface.SANS_SERIF
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
        }
    }
}
