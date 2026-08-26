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
                    setRenderEffect(blurEffect)
                }
                
                val pad = blurRadius * 3f
                val bounds = android.graphics.RectF(-pad, -pad, layer.width + pad, layer.height + pad)
                
                if (offsetX != 0f || offsetY != 0f) {
                    originalCanvas.save()
                    originalCanvas.translate(offsetX, offsetY)
                }
                originalCanvas.saveLayer(bounds, layerPaint)
                needsRestore = true"""

pattern = re.compile(r'                val tileMode = if \(blurMode.*?needsRestore = true', re.DOTALL)
new_content = pattern.sub(replacer, content)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(new_content)

