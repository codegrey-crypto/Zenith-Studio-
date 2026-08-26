import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

# Replace any generic drawPath blur loop again just in case (for STAR etc)
pattern2 = re.compile(
r"""( +)if \(blurRadius > 0f\) \{\n\1    for \(o in 1\.\.4\) \{\n\1        drawPath\(\n\1            path = path,\n\1            color = layer\.baseColor\.copy\(alpha = layerOpacity \* 0\.12f\),\n\1            style = Stroke\(width = o \* blurRadius\),\n\1            blendMode = composeBlendMode\n\1        \)\n\1    \}\n\1\}\n\s+drawPath\(\n\1    path = path,\n\1    color = layer\.baseColor\.copy\(alpha = layerOpacity\),\n\1    blendMode = composeBlendMode\n\1\)"""
)

repl2 = r"""\1if (blurRadius > 0f) {
\1    drawIntoCanvas { canvas ->
\1        val paint = androidx.compose.ui.graphics.Paint().apply {
\1            color = layer.baseColor.copy(alpha = layerOpacity)
\1            blendMode = composeBlendMode
\1        }
\1        paint.asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
\1        canvas.drawPath(path, paint)
\1    }
\1} else {
\1    drawPath(
\1        path = path,
\1        color = layer.baseColor.copy(alpha = layerOpacity),
\1        blendMode = composeBlendMode
\1    )
\1}"""

content = pattern2.sub(repl2, content)

# Write back
with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
