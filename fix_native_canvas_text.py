with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

target = """        com.example.studio.model.LayerType.TEXT -> {
            val textPaint = android.text.TextPaint().apply {
                color = android.graphics.Color.argb(
                    (layer.baseColor.alpha * 255).toInt().coerceIn(0, 255),
                    (layer.baseColor.red * 255).toInt().coerceIn(0, 255),
                    (layer.baseColor.green * 255).toInt().coerceIn(0, 255),
                    (layer.baseColor.blue * 255).toInt().coerceIn(0, 255)
                )
                textSize = layer.fontSize
                letterSpacing = layer.letterSpacing
                isAntiAlias = true
            }
            val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder.obtain(layer.textContent, 0, layer.textContent.length, textPaint, maxOf(1, layer.width.toInt()))
                    .setLineSpacing(0f, maxOf(0.1f, layer.lineSpacing))
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.text.StaticLayout(layer.textContent, textPaint, maxOf(1, layer.width.toInt()), android.text.Layout.Alignment.ALIGN_NORMAL, maxOf(0.1f, layer.lineSpacing), 0f, false)
            }
            staticLayout.draw(canvas)
        }"""

replacement = """        com.example.studio.model.LayerType.TEXT -> {
            val rawText = if (layer.textContent.isEmpty()) "DOUBLE TAP TO EDIT" else layer.textContent
            val widthToUse = maxOf(1, layer.width.toInt())
            
            val cacheKey = com.example.studio.ui.TextLayoutCacheKey(
                text = rawText,
                textSpansJson = layer.richTextSpansJson,
                fontPath = layer.fontPath,
                fontFamilyName = layer.fontFamilyName,
                isBold = layer.fontIsBold,
                isItalic = layer.fontIsItalic,
                color = android.graphics.Color.argb(
                    (layer.baseColor.alpha * 255).toInt().coerceIn(0, 255),
                    (layer.baseColor.red * 255).toInt().coerceIn(0, 255),
                    (layer.baseColor.green * 255).toInt().coerceIn(0, 255),
                    (layer.baseColor.blue * 255).toInt().coerceIn(0, 255)
                ),
                fontSize = layer.fontSize,
                letterSpacing = layer.letterSpacing,
                blurRadius = 0f,
                drawStyleHash = 0,
                align = layer.fontAlign,
                width = widthToUse,
                lineSpacing = layer.lineSpacing,
                strokeThickness = layer.strokeThickness
            )
            
            val staticLayout = com.example.studio.ui.TextLayoutCache.get(cacheKey) {
                val textFormatted = com.aistudio.zenithstudio.rpxwtq.RichTextSpanHelper.buildSpannableText(rawText, layer)
                val typeface = com.example.studio.ui.TypefaceCache.get(
                    layer.fontPath,
                    layer.fontFamilyName,
                    layer.fontIsBold,
                    layer.fontIsItalic
                )
                val textPaint = android.text.TextPaint().apply {
                    color = android.graphics.Color.argb(
                        (layer.baseColor.alpha * 255).toInt().coerceIn(0, 255),
                        (layer.baseColor.red * 255).toInt().coerceIn(0, 255),
                        (layer.baseColor.green * 255).toInt().coerceIn(0, 255),
                        (layer.baseColor.blue * 255).toInt().coerceIn(0, 255)
                    )
                    textSize = layer.fontSize
                    letterSpacing = layer.letterSpacing
                    isAntiAlias = true
                    this.typeface = typeface
                }
                
                val align = when (layer.fontAlign.lowercase()) {
                    "left" -> android.text.Layout.Alignment.ALIGN_NORMAL
                    "right" -> android.text.Layout.Alignment.ALIGN_OPPOSITE
                    else -> android.text.Layout.Alignment.ALIGN_CENTER
                }
                
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
            }
            staticLayout.draw(canvas)
        }"""

if target in content:
    content = content.replace(target, replacement)
    print("Success")
    with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
        f.write(content)
else:
    print("Target not found")
