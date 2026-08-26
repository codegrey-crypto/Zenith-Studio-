import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

# Let's target the exact chunks for `for (o in 1..4)` 
# Since we know line numbers and content exactly, we can use simple string replaces.

replacements = [
    # VECTOR_TRIANGLE, VECTOR_POLYGON, VECTOR_STAR, VECTOR_HUD... (has 4 identical)
    (
        r"""                                                val blurRadius = getBlurRadius(layer)
                                                if (blurRadius > 0f) {
                                                    for (o in 1..4) {
                                                        drawPath(
                                                            path = path,
                                                            color = layer.baseColor.copy(alpha = layerOpacity * 0.12f),
                                                            style = Stroke(width = o * blurRadius),
                                                            blendMode = composeBlendMode
                                                        )
                                                    }
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = layer.baseColor.copy(alpha = layerOpacity),
                                                    blendMode = composeBlendMode
                                                )""",
        r"""                                                val blurRadius = getBlurRadius(layer)
                                                if (blurRadius > 0f) {
                                                    drawIntoCanvas { canvas ->
                                                        val paint = androidx.compose.ui.graphics.Paint().apply {
                                                            color = layer.baseColor.copy(alpha = layerOpacity)
                                                            blendMode = composeBlendMode
                                                        }
                                                        paint.asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                                        canvas.drawPath(path, paint)
                                                    }
                                                } else {
                                                    drawPath(
                                                        path = path,
                                                        color = layer.baseColor.copy(alpha = layerOpacity),
                                                        blendMode = composeBlendMode
                                                    )
                                                }"""
    ),
    # VECTOR_OVAL
    (
        r"""                                                val blurRadius = getBlurRadius(layer)
                                                if (blurRadius > 0f) {
                                                    for (o in 1..4) {
                                                        drawOval(
                                                            color = layer.baseColor.copy(alpha = layerOpacity * 0.15f),
                                                            topLeft = Offset(-o * blurRadius * 0.4f, -o * blurRadius * 0.4f),
                                                            size = Size(layer.width + o * blurRadius * 0.8f, layer.height + o * blurRadius * 0.8f),
                                                            blendMode = composeBlendMode
                                                        )
                                                    }
                                                }
                                                drawOval(
                                                    color = layer.baseColor.copy(alpha = layerOpacity),
                                                    topLeft = Offset.Zero,
                                                    size = Size(layer.width, layer.height),
                                                    blendMode = composeBlendMode
                                                )""",
        r"""                                                val blurRadius = getBlurRadius(layer)
                                                if (blurRadius > 0f) {
                                                    drawIntoCanvas { canvas ->
                                                        val paint = androidx.compose.ui.graphics.Paint().apply {
                                                            color = layer.baseColor.copy(alpha = layerOpacity)
                                                            blendMode = composeBlendMode
                                                        }
                                                        paint.asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                                        canvas.drawOval(0f, 0f, layer.width, layer.height, paint)
                                                    }
                                                } else {
                                                    drawOval(
                                                        color = layer.baseColor.copy(alpha = layerOpacity),
                                                        topLeft = Offset.Zero,
                                                        size = Size(layer.width, layer.height),
                                                        blendMode = composeBlendMode
                                                    )
                                                }"""
    ),
    # VECTOR_LINE
    (
        r"""                                                val blurRadius = getBlurRadius(layer)
                                                if (blurRadius > 0f) {
                                                    for (o in 1..4) {
                                                        drawLine(
                                                            color = layer.baseColor.copy(alpha = layerOpacity * 0.18f),
                                                            start = Offset.Zero,
                                                            end = Offset(layer.width, layer.height),
                                                            strokeWidth = 6f + o * blurRadius * 0.5f,
                                                            blendMode = composeBlendMode
                                                        )
                                                    }
                                                }
                                                drawLine(
                                                    color = layer.baseColor.copy(alpha = layerOpacity),
                                                    start = Offset.Zero,
                                                    end = Offset(layer.width, layer.height),
                                                    strokeWidth = 6f,
                                                    blendMode = composeBlendMode
                                                )""",
        r"""                                                val blurRadius = getBlurRadius(layer)
                                                if (blurRadius > 0f) {
                                                    drawIntoCanvas { canvas ->
                                                        val paint = androidx.compose.ui.graphics.Paint().apply {
                                                            color = layer.baseColor.copy(alpha = layerOpacity)
                                                            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                                                            strokeWidth = 6f
                                                            blendMode = composeBlendMode
                                                        }
                                                        paint.asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                                        canvas.drawLine(androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(layer.width, layer.height), paint)
                                                    }
                                                } else {
                                                    drawLine(
                                                        color = layer.baseColor.copy(alpha = layerOpacity),
                                                        start = Offset.Zero,
                                                        end = Offset(layer.width, layer.height),
                                                        strokeWidth = 6f,
                                                        blendMode = composeBlendMode
                                                    )
                                                }"""
    ),
    # VECTOR_BEZIER
    (
        r"""                                                if (blurRadius > 0f) {
                                                    for (o in 1..4) {
                                                        val blurStyle = if (layer.strokeThickness <= 0f) {
                                                            androidx.compose.ui.graphics.drawscope.Fill
                                                        } else {
                                                            Stroke(width = layer.strokeThickness + o * blurRadius * 0.5f)
                                                        }
                                                        drawPath(
                                                            path = path,
                                                            color = layer.baseColor.copy(alpha = layerOpacity * 0.15f),
                                                            style = blurStyle,
                                                            blendMode = composeBlendMode
                                                        )
                                                    }
                                                }

                                                drawPath(
                                                    path = path,
                                                    color = layer.baseColor.copy(alpha = layerOpacity),
                                                    style = shapeStyle,
                                                    blendMode = composeBlendMode
                                                )""",
        r"""                                                if (blurRadius > 0f) {
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
    ),
    # VECTOR_CIRCLE
    (
        r"""                                                val blurRadius = getBlurRadius(layer)
                                                if (blurRadius > 0f) {
                                                    for (o in 1..4) {
                                                        drawCircle(
                                                            color = layer.baseColor.copy(alpha = layerOpacity * 0.15f),
                                                            radius = radius + o * blurRadius * 0.4f,
                                                            center = center,
                                                            blendMode = composeBlendMode
                                                        )
                                                    }
                                                }

                                                drawCircle(
                                                    color = layer.baseColor.copy(alpha = layerOpacity),
                                                    radius = radius,
                                                    center = center,
                                                    blendMode = composeBlendMode
                                                )""",
        r"""                                                val blurRadius = getBlurRadius(layer)
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
    )
]

for old, new in replacements:
    content = content.replace(old, new)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)

