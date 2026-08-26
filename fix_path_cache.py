with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

target = """                    com.example.studio.model.LayerType.TEXT -> {
                        val rawText = if (layer.textContent.isEmpty()) "DOUBLE TAP TO EDIT" else layer.textContent
                        val textFormatted = com.aistudio.zenithstudio.rpxwtq.RichTextSpanHelper.buildSpannableText(rawText, layer)
                        val typeface = com.example.studio.ui.TypefaceCache.get(
                            layer.fontPath,
                            layer.fontFamilyName,
                            layer.fontIsBold,
                            layer.fontIsItalic
                        )
                        val textPaint = android.text.TextPaint().apply {
                            textSize = layer.fontSize
                            letterSpacing = layer.letterSpacing
                            isAntiAlias = true
                            this.typeface = typeface
                            style = android.graphics.Paint.Style.FILL
                        }
                        val align = when (layer.fontAlign.lowercase()) {
                            "left" -> android.text.Layout.Alignment.ALIGN_NORMAL
                            "right" -> android.text.Layout.Alignment.ALIGN_OPPOSITE
                            else -> android.text.Layout.Alignment.ALIGN_CENTER
                        }
                        val widthToUse = maxOf(1, layer.width.toInt())
                        val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            android.text.StaticLayout.Builder.obtain(textFormatted, 0, textFormatted.length, textPaint, widthToUse)
                                .setAlignment(align)
                                .setLineSpacing(0f, maxOf(0.1f, layer.lineSpacing))
                                .setIncludePad(false)
                                .build()
                        } else {
                            @Suppress("DEPRECATION")
                            android.text.StaticLayout(textFormatted, textPaint, widthToUse, align, maxOf(0.1f, layer.lineSpacing), 0f, false)
                        }"""

replacement = """                    com.example.studio.model.LayerType.TEXT -> {
                        val rawText = if (layer.textContent.isEmpty()) "DOUBLE TAP TO EDIT" else layer.textContent
                        val alignStr = layer.fontAlign
                        val widthToUse = maxOf(1, layer.width.toInt())
                        
                        val cacheKey = com.example.studio.ui.TextLayoutCacheKey(
                            text = rawText,
                            textSpansJson = layer.richTextSpansJson,
                            fontPath = layer.fontPath,
                            fontFamilyName = layer.fontFamilyName,
                            isBold = layer.fontIsBold,
                            isItalic = layer.fontIsItalic,
                            color = android.graphics.Color.WHITE,
                            fontSize = layer.fontSize,
                            letterSpacing = layer.letterSpacing,
                            blurRadius = 0f,
                            drawStyleHash = 0,
                            align = alignStr,
                            width = widthToUse,
                            lineSpacing = layer.lineSpacing,
                            strokeThickness = 0f
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
                                textSize = layer.fontSize
                                letterSpacing = layer.letterSpacing
                                isAntiAlias = true
                                this.typeface = typeface
                                style = android.graphics.Paint.Style.FILL
                            }
                            val align = when (alignStr.lowercase()) {
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
                        val textPaint = staticLayout.paint as android.text.TextPaint
                        val textFormatted = staticLayout.text
                        """

if target in content:
    content = content.replace(target, replacement)
    print("Success")
    with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
        f.write(content)
else:
    print("Target not found")
