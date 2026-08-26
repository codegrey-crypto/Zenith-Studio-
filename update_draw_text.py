with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

target = """    val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        android.text.StaticLayout.Builder.obtain(textFormatted, 0, textFormatted.length, textPaint, widthToUse)
            .setAlignment(align)
            .setLineSpacing(0f, maxOf(0.1f, layer.lineSpacing))
            .setIncludePad(false)
            .build()
    } else {
        @Suppress("DEPRECATION")
        android.text.StaticLayout(textFormatted, textPaint, widthToUse, align, maxOf(0.1f, layer.lineSpacing), 0f, false)
    }"""

replacement = """    val cacheKey = com.example.studio.ui.TextLayoutCacheKey(
        text = rawText,
        textSpansJson = layer.richTextSpansJson,
        fontPath = layer.fontPath,
        fontFamilyName = layer.fontFamilyName,
        isBold = layer.fontIsBold,
        isItalic = layer.fontIsItalic,
        color = activeColor.toArgb(),
        fontSize = layer.fontSize,
        letterSpacing = layer.letterSpacing,
        blurRadius = blurRadius,
        drawStyleHash = if (drawStyle is androidx.compose.ui.graphics.drawscope.Stroke) drawStyle.width.hashCode() else 0,
        align = layer.fontAlign,
        width = widthToUse,
        lineSpacing = layer.lineSpacing,
        strokeThickness = 0f // No stroke for the fill pass
    )
    
    val staticLayout = com.example.studio.ui.TextLayoutCache.get(cacheKey) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.text.StaticLayout.Builder.obtain(textFormatted, 0, textFormatted.length, textPaint, widthToUse)
                .setAlignment(align)
                .setLineSpacing(0f, maxOf(0.1f, layer.lineSpacing))
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.text.StaticLayout(textFormatted, textPaint, widthToUse, align, maxOf(0.1f, layer.lineSpacing), 0f, false)
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    print("Success 1")
else:
    print("Target 1 not found")

target2 = """        val strokeLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.text.StaticLayout.Builder.obtain(textFormatted, 0, textFormatted.length, strokeTextPaint, widthToUse)
                .setAlignment(align)
                .setLineSpacing(0f, maxOf(0.1f, layer.lineSpacing))
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.text.StaticLayout(textFormatted, strokeTextPaint, widthToUse, align, maxOf(0.1f, layer.lineSpacing), 0f, false)
        }"""

replacement2 = """        val strokeCacheKey = cacheKey.copy(strokeThickness = layer.strokeThickness)
        val strokeLayout = com.example.studio.ui.TextLayoutCache.get(strokeCacheKey) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder.obtain(textFormatted, 0, textFormatted.length, strokeTextPaint, widthToUse)
                    .setAlignment(align)
                    .setLineSpacing(0f, maxOf(0.1f, layer.lineSpacing))
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.text.StaticLayout(textFormatted, strokeTextPaint, widthToUse, align, maxOf(0.1f, layer.lineSpacing), 0f, false)
            }
        }"""

if target2 in content:
    content = content.replace(target2, replacement2)
    print("Success 2")
else:
    print("Target 2 not found")

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
