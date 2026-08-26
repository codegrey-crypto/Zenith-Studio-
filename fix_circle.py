import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

pattern = re.compile(
r"""(                                                val blurRadius = getBlurRadius\(layer\)\n                                                if \(blurRadius > 0f\) \{\n                                                    for \(o in 1\.\.4\) \{\n                                                        drawCircle\(\n                                                            color = layer\.baseColor\.copy\(alpha = layerOpacity \* 0\.15f\),\n                                                            radius = radius \+ o \* blurRadius \* 0\.4f,\n                                                            center = center,\n                                                            blendMode = composeBlendMode\n                                                        \)\n                                                    \}\n                                                \}\n\s+drawCircle\(\n                                                    color = layer\.baseColor\.copy\(alpha = layerOpacity\),\n                                                    radius = radius,\n                                                    center = center,\n                                                    blendMode = composeBlendMode\n                                                \))"""
)

repl = r"""                                                val blurRadius = getBlurRadius(layer)
                                                if (blurRadius > 0f) {
                                                    drawIntoCanvas { canvas ->
                                                        val paint = androidx.compose.ui.graphics.Paint().apply {
                                                            color = layer.baseColor.copy(alpha = layerOpacity)
                                                            blendMode = composeBlendMode
                                                        }
                                                        paint.asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                                        canvas.drawCircle(center, radius, paint)
                                                    }
                                                } else {
                                                    drawCircle(
                                                        color = layer.baseColor.copy(alpha = layerOpacity),
                                                        radius = radius,
                                                        center = center,
                                                        blendMode = composeBlendMode
                                                    )
                                                }"""

content = pattern.sub(repl, content)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)

