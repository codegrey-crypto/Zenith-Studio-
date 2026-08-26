import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

pattern = re.compile(
r"""(                                                val blurRadius = getBlurRadius\(layer\)\n                                                val shapeStyle = if \(layer\.strokeThickness <= 0f\) \{\n                                                    androidx\.compose\.ui\.graphics\.drawscope\.Fill\n                                                \} else \{\n)(                                                                                       if \(blurRadius > 0f\) \{\n                                                    drawIntoCanvas \{ canvas ->\n                                                        val paint = androidx\.compose\.ui\.graphics\.Paint\(\)\.apply \{\n                                                            color = layer\.baseColor\.copy\(alpha = layerOpacity\)\n                                                            blendMode = composeBlendMode\n                                                            if \(layer\.strokeThickness > 0f\) \{\n                                                                style = androidx\.compose\.ui\.graphics\.PaintingStyle\.Stroke\n                                                                strokeWidth = layer\.strokeThickness\n                                                            \}\n                                                        \}\n                                                        paint\.asFrameworkPaint\(\)\.maskFilter = android\.graphics\.BlurMaskFilter\(blurRadius, android\.graphics\.BlurMaskFilter\.Blur\.NORMAL\)\n                                                        canvas\.drawPath\(path, paint\)\n                                                    \}\n                                                \} else \{\n                                                    drawPath\(\n                                                        path = path,\n                                                        color = layer\.baseColor\.copy\(alpha = layerOpacity\),\n                                                        style = shapeStyle,\n                                                        blendMode = composeBlendMode\n                                                    \)\n                                                \}                        blendMode = composeBlendMode\n                                                \))"""
)

repl = r"""\1                                                    Stroke(width = layer.strokeThickness)
                                                }
                                                if (blurRadius > 0f) {
                                                    drawIntoCanvas { canvas ->
                                                        val paint = androidx.compose.ui.graphics.Paint().apply {
                                                            color = layer.baseColor.copy(alpha = layerOpacity)
                                                            blendMode = composeBlendMode
                                                            if (layer.strokeThickness > 0f) {
                                                                style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                                                                strokeWidth = layer.strokeThickness
                                                            }
                                                        }
                                                        paint.asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                                        canvas.drawPath(path, paint)
                                                    }
                                                } else {
                                                    drawPath(
                                                        path = path,
                                                        color = layer.baseColor.copy(alpha = layerOpacity),
                                                        style = shapeStyle,
                                                        blendMode = composeBlendMode
                                                    )
                                                }"""

content = pattern.sub(repl, content)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)

