import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

# I will find all instances of my broken drawIntoCanvas with BlurMaskFilter and revert them.

# Pattern for VECTOR_TRIANGLE, VECTOR_POLYGON, VECTOR_STAR, VECTOR_HUD_FRAME...
pattern1 = re.compile(
r"""( +)val blurRadius = getBlurRadius\(layer\)\n\1if \(blurRadius > 0f\) \{\n\1    drawIntoCanvas \{ canvas ->\n\1        val paint = androidx\.compose\.ui\.graphics\.Paint\(\)\.apply \{\n\1            color = layer\.baseColor\.copy\(alpha = layerOpacity\)\n\1            blendMode = composeBlendMode\n\1        \}\n\1        paint\.asFrameworkPaint\(\)\.maskFilter = android\.graphics\.BlurMaskFilter\(blurRadius, android\.graphics\.BlurMaskFilter\.Blur\.NORMAL\)\n\1        canvas\.drawPath\(path, paint\)\n\1    \}\n\1\} else \{\n\1    drawPath\(\n\1        path = path,\n\1        color = layer\.baseColor\.copy\(alpha = layerOpacity\),\n\1        blendMode = composeBlendMode\n\1    \)\n\1\}"""
)
repl1 = r"""\1val blurRadius = getBlurRadius(layer)
\1if (blurRadius > 0f) {
\1    for (o in 1..4) {
\1        drawPath(
\1            path = path,
\1            color = layer.baseColor.copy(alpha = layerOpacity * 0.12f),
\1            style = Stroke(width = o * blurRadius),
\1            blendMode = composeBlendMode
\1        )
\1    }
\1}
\1drawPath(
\1    path = path,
\1    color = layer.baseColor.copy(alpha = layerOpacity),
\1    blendMode = composeBlendMode
\1)"""
content = pattern1.sub(repl1, content)

# Pattern for VECTOR_OVAL
pattern_oval = re.compile(
r"""( +)val blurRadius = getBlurRadius\(layer\)\n\1if \(blurRadius > 0f\) \{\n\1    drawIntoCanvas \{ canvas ->\n\1        val paint = androidx\.compose\.ui\.graphics\.Paint\(\)\.apply \{\n\1            color = layer\.baseColor\.copy\(alpha = layerOpacity\)\n\1            blendMode = composeBlendMode\n\1        \}\n\1        paint\.asFrameworkPaint\(\)\.maskFilter = android\.graphics\.BlurMaskFilter\(blurRadius, android\.graphics\.BlurMaskFilter\.Blur\.NORMAL\)\n\1        canvas\.drawOval\(0f, 0f, layer\.width, layer\.height, paint\)\n\1    \}\n\1\} else \{\n\1    drawOval\(\n\1        color = layer\.baseColor\.copy\(alpha = layerOpacity\),\n\1        topLeft = Offset\.Zero,\n\1        size = Size\(layer\.width, layer\.height\),\n\1        blendMode = composeBlendMode\n\1    \)\n\1\}"""
)
repl_oval = r"""\1val blurRadius = getBlurRadius(layer)
\1if (blurRadius > 0f) {
\1    for (o in 1..4) {
\1        drawOval(
\1            color = layer.baseColor.copy(alpha = layerOpacity * 0.15f),
\1            topLeft = Offset(-o * blurRadius * 0.4f, -o * blurRadius * 0.4f),
\1            size = Size(layer.width + o * blurRadius * 0.8f, layer.height + o * blurRadius * 0.8f),
\1            blendMode = composeBlendMode
\1        )
\1    }
\1}
\1drawOval(
\1    color = layer.baseColor.copy(alpha = layerOpacity),
\1    topLeft = Offset.Zero,
\1    size = Size(layer.width, layer.height),
\1    blendMode = composeBlendMode
\1)"""
content = pattern_oval.sub(repl_oval, content)

# Pattern for VECTOR_LINE
pattern_line = re.compile(
r"""( +)val blurRadius = getBlurRadius\(layer\)\n\1if \(blurRadius > 0f\) \{\n\1    drawIntoCanvas \{ canvas ->\n\1        val paint = androidx\.compose\.ui\.graphics\.Paint\(\)\.apply \{\n\1            color = layer\.baseColor\.copy\(alpha = layerOpacity\)\n\1            style = androidx\.compose\.ui\.graphics\.PaintingStyle\.Stroke\n\1            strokeWidth = 6f\n\1            blendMode = composeBlendMode\n\1        \}\n\1        paint\.asFrameworkPaint\(\)\.maskFilter = android\.graphics\.BlurMaskFilter\(blurRadius, android\.graphics\.BlurMaskFilter\.Blur\.NORMAL\)\n\1        canvas\.drawLine\(androidx\.compose\.ui\.geometry\.Offset\.Zero, androidx\.compose\.ui\.geometry\.Offset\(layer\.width, layer\.height\), paint\)\n\1    \}\n\1\} else \{\n\1    drawLine\(\n\1        color = layer\.baseColor\.copy\(alpha = layerOpacity\),\n\1        start = Offset\.Zero,\n\1        end = Offset\(layer\.width, layer\.height\),\n\1        strokeWidth = 6f,\n\1        blendMode = composeBlendMode\n\1    \)\n\1\}"""
)
repl_line = r"""\1val blurRadius = getBlurRadius(layer)
\1if (blurRadius > 0f) {
\1    for (o in 1..4) {
\1        drawLine(
\1            color = layer.baseColor.copy(alpha = layerOpacity * 0.18f),
\1            start = Offset.Zero,
\1            end = Offset(layer.width, layer.height),
\1            strokeWidth = 6f + o * blurRadius * 0.5f,
\1            blendMode = composeBlendMode
\1        )
\1    }
\1}
\1drawLine(
\1    color = layer.baseColor.copy(alpha = layerOpacity),
\1    start = Offset.Zero,
\1    end = Offset(layer.width, layer.height),
\1    strokeWidth = 6f,
\1    blendMode = composeBlendMode
\1)"""
content = pattern_line.sub(repl_line, content)

# Pattern for VECTOR_BEZIER (the one that I replaced partially)
pattern_bezier = re.compile(
r"""( +)if \(blurRadius > 0f\) \{\n\1    drawIntoCanvas \{ canvas ->\n\1        val paint = androidx\.compose\.ui\.graphics\.Paint\(\)\.apply \{\n\1            color = layer\.baseColor\.copy\(alpha = layerOpacity\)\n\1            blendMode = composeBlendMode\n\1            if \(layer\.strokeThickness > 0f\) \{\n\1                style = androidx\.compose\.ui\.graphics\.PaintingStyle\.Stroke\n\1                strokeWidth = layer\.strokeThickness\n\1            \}\n\1        \}\n\1        paint\.asFrameworkPaint\(\)\.maskFilter = android\.graphics\.BlurMaskFilter\(blurRadius, android\.graphics\.BlurMaskFilter\.Blur\.NORMAL\)\n\1        canvas\.drawPath\(path, paint\)\n\1    \}\n\1\} else \{\n\1    drawPath\(\n\1        path = path,\n\1        color = layer\.baseColor\.copy\(alpha = layerOpacity\),\n\1        style = shapeStyle,\n\1        blendMode = composeBlendMode\n\1    \)\n\1\}"""
)
repl_bezier = r"""\1if (blurRadius > 0f) {
\1    for (o in 1..4) {
\1        val blurStyle = if (layer.strokeThickness <= 0f) {
\1            androidx.compose.ui.graphics.drawscope.Fill
\1        } else {
\1            Stroke(width = layer.strokeThickness + o * blurRadius * 0.5f)
\1        }
\1        drawPath(
\1            path = path,
\1            color = layer.baseColor.copy(alpha = layerOpacity * 0.15f),
\1            style = blurStyle,
\1            blendMode = composeBlendMode
\1        )
\1    }
\1}
\1drawPath(
\1    path = path,
\1    color = layer.baseColor.copy(alpha = layerOpacity),
\1    style = shapeStyle,
\1    blendMode = composeBlendMode
\1)"""
content = pattern_bezier.sub(repl_bezier, content)

# Pattern for VECTOR_CIRCLE
pattern_circle = re.compile(
r"""( +)val blurRadius = getBlurRadius\(layer\)\n\1if \(blurRadius > 0f\) \{\n\1    drawIntoCanvas \{ canvas ->\n\1        val paint = androidx\.compose\.ui\.graphics\.Paint\(\)\.apply \{\n\1            color = layer\.baseColor\.copy\(alpha = layerOpacity\)\n\1            blendMode = composeBlendMode\n\1        \}\n\1        paint\.asFrameworkPaint\(\)\.maskFilter = android\.graphics\.BlurMaskFilter\(blurRadius, android\.graphics\.BlurMaskFilter\.Blur\.NORMAL\)\n\1        canvas\.drawCircle\(center, radius, paint\)\n\1    \}\n\1\} else \{\n\1    drawCircle\(\n\1        color = layer\.baseColor\.copy\(alpha = layerOpacity\),\n\1        radius = radius,\n\1        center = center,\n\1        blendMode = composeBlendMode\n\1    \)\n\1\}"""
)
repl_circle = r"""\1val blurRadius = getBlurRadius(layer)
\1if (blurRadius > 0f) {
\1    for (o in 1..4) {
\1        drawCircle(
\1            color = layer.baseColor.copy(alpha = layerOpacity * 0.15f),
\1            radius = radius + o * blurRadius * 0.4f,
\1            center = center,
\1            blendMode = composeBlendMode
\1        )
\1    }
\1}
\1drawCircle(
\1    color = layer.baseColor.copy(alpha = layerOpacity),
\1    radius = radius,
\1    center = center,
\1    blendMode = composeBlendMode
\1)"""
content = pattern_circle.sub(repl_circle, content)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
