import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

def replacer(match):
    return """                val tileMode = if (blurMode == android.graphics.BlurMaskFilter.Blur.OUTER) {
                    android.graphics.Shader.TileMode.CLAMP
                } else {
                    android.graphics.Shader.TileMode.DECAL
                }
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(blurRadius, blurRadius, tileMode)
                val layerPaint = android.graphics.Paint().apply {
                    try {
                        val setMethod = this.javaClass.getMethod("setRenderEffect", android.graphics.RenderEffect::class.java)
                        setMethod.invoke(this, blurEffect)
                    } catch (e: Exception) {}
                }"""

pattern = re.compile(r'                val tileMode = if \(blurMode.*?                val layerPaint = android.graphics.Paint\(\).apply \{\n                    setRenderEffect\(blurEffect\)\n                \}', re.DOTALL)
new_content = pattern.sub(replacer, content)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(new_content)

