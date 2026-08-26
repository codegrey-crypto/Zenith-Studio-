with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

target = """    val rawText = if (layer.textContent.isEmpty()) "DOUBLE TAP TO EDIT" else layer.textContent
    val textFormatted = com.aistudio.zenithstudio.rpxwtq.RichTextSpanHelper.buildSpannableText(rawText, layer)
    val typeface = com.example.studio.ui.TypefaceCache.get("""

replacement = """    val rawText = if (layer.textContent.isEmpty()) "DOUBLE TAP TO EDIT" else layer.textContent
    val typeface = com.example.studio.ui.TypefaceCache.get("""

if target in content:
    content = content.replace(target, replacement)
    print("Success")

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
