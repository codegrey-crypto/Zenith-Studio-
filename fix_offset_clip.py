import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

def replacer(match):
    return """                try {
                    tempCanvas.save()
                    tempCanvas.translate(pad.toFloat(), pad.toFloat())
                    
                    // Recursive call in software canvas mode
                    drawGeometryWithBlurAndOffset(finalColor, finalOpacity, fillStyle, blurRadius, blurMode, 0f, 0f, isOverlay, blendModeOverride, isShadowOrEffect, gradientShader, cornerRadiusOverride)
                    
                    tempCanvas.restore()
                } finally {
                    drawContext.canvas = originalCanvas
                }
                
                originalCanvas.drawImage(tempBmp.asImageBitmap(), androidx.compose.ui.geometry.Offset(-pad.toFloat() + offsetX, -pad.toFloat() + offsetY), androidx.compose.ui.graphics.Paint())"""

pattern = re.compile(r'                try \{\n                    tempCanvas\.save\(\)\n                    tempCanvas\.translate\(pad\.toFloat\(\) \+ offsetX, pad\.toFloat\(\) \+ offsetY\)\n                    \n                    // Recursive call in software canvas mode\n                    drawGeometryWithBlurAndOffset\(finalColor, finalOpacity, fillStyle, blurRadius, blurMode, 0f, 0f, isOverlay, blendModeOverride, isShadowOrEffect, gradientShader, cornerRadiusOverride\)\n                    \n                    tempCanvas\.restore\(\)\n                \} finally \{\n                    drawContext\.canvas = originalCanvas\n                \}\n                \n                originalCanvas\.drawImage\(tempBmp\.asImageBitmap\(\), androidx\.compose\.ui\.geometry\.Offset\(-pad\.toFloat\(\), -pad\.toFloat\(\)\), androidx\.compose\.ui\.graphics\.Paint\(\)\)')
new_content = pattern.sub(replacer, content)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(new_content)

