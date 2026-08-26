with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

target = """    val textFormatted = com.aistudio.zenithstudio.rpxwtq.RichTextSpanHelper.buildSpannableText(rawText, layer)
    
    val typeface = com.example.studio.ui.TypefaceCache.get(
        layer.fontPath,
        layer.fontFamilyName,
        layer.fontIsBold,
        layer.fontIsItalic
    )"""

replacement = """    
    val typeface = com.example.studio.ui.TypefaceCache.get(
        layer.fontPath,
        layer.fontFamilyName,
        layer.fontIsBold,
        layer.fontIsItalic
    )"""

if target in content:
    content = content.replace(target, replacement)
    print("Removed textFormatted early generation")

target2 = """    val staticLayout = com.example.studio.ui.TextLayoutCache.get(cacheKey) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.text.StaticLayout.Builder.obtain(textFormatted, 0, textFormatted.length, textPaint, widthToUse)"""

replacement2 = """    val staticLayout = com.example.studio.ui.TextLayoutCache.get(cacheKey) {
        val textFormatted = com.aistudio.zenithstudio.rpxwtq.RichTextSpanHelper.buildSpannableText(rawText, layer)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.text.StaticLayout.Builder.obtain(textFormatted, 0, textFormatted.length, textPaint, widthToUse)"""

if target2 in content:
    content = content.replace(target2, replacement2)
    print("Added textFormatted lazy generation 1")

target3 = """        val strokeLayout = com.example.studio.ui.TextLayoutCache.get(strokeCacheKey) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder.obtain(textFormatted, 0, textFormatted.length, strokeTextPaint, widthToUse)"""

replacement3 = """        val strokeLayout = com.example.studio.ui.TextLayoutCache.get(strokeCacheKey) {
            val textFormatted = com.aistudio.zenithstudio.rpxwtq.RichTextSpanHelper.buildSpannableText(rawText, layer)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder.obtain(textFormatted, 0, textFormatted.length, strokeTextPaint, widthToUse)"""

if target3 in content:
    content = content.replace(target3, replacement3)
    print("Added textFormatted lazy generation 2")

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
