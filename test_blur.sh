#!/bin/bash
cat << 'INNER' > patch_drawblur.py
import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

# Replace drawGeometryWithBlurAndOffset entirely
def replace_func(match):
    return """    fun drawGeometryWithBlurAndOffset(finalColor: androidx.compose.ui.graphics.Color, finalOpacity: Float, fillStyle: androidx.compose.ui.graphics.drawscope.DrawStyle, blurRadius: Float, blurMode: android.graphics.BlurMaskFilter.Blur, offsetX: Float, offsetY: Float, isOverlay: Boolean = true, blendModeOverride: androidx.compose.ui.graphics.BlendMode? = null, isShadowOrEffect: Boolean = false, gradientShader: android.graphics.Shader? = null, cornerRadiusOverride: Float? = null) {
        val finalComposeColor = finalColor.copy(alpha = finalOpacity * layerOpacity)
        val styleToUse = if (layer.strokeThickness > 0f && fillStyle is androidx.compose.ui.graphics.drawscope.Fill && layer.type !in listOf(com.example.studio.model.LayerType.FREEHAND_DRAWING, com.example.studio.model.LayerType.IMAGE_CARD)) {
            androidx.compose.ui.graphics.drawscope.Stroke(width = layer.strokeThickness)
        } else {
            fillStyle
        }

        val originalCanvas = drawContext.canvas.nativeCanvas
        var needsRestore = false

        if (blurRadius > 0.1f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                val tileMode = if (blurMode == android.graphics.BlurMaskFilter.Blur.OUTER) {
                    android.graphics.Shader.TileMode.CLAMP
                } else {
                    android.graphics.Shader.TileMode.DECAL
                }
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(blurRadius, blurRadius, tileMode)
                val chainEffect = if (offsetX != 0f || offsetY != 0f) {
                    android.graphics.RenderEffect.createOffsetEffect(offsetX, offsetY, blurEffect)
                } else {
                    blurEffect
                }
                val layerPaint = android.graphics.Paint().apply {
                    setRenderEffect(chainEffect)
                }
                
                // saveLayer without bounds applies to the whole clipped area, allowing blur to bleed naturally
                val pad = blurRadius * 3f
                val bounds = android.graphics.RectF(-pad + offsetX, -pad + offsetY, layer.width + pad + offsetX, layer.height + pad + offsetY)
                originalCanvas.saveLayer(bounds, layerPaint)
                needsRestore = true
            } catch (t: Throwable) {
                // Ignore, will fallback to BlurMaskFilter
            }
        }

        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = finalComposeColor
            this.style = if (styleToUse is androidx.compose.ui.graphics.drawscope.Stroke) {
                androidx.compose.ui.graphics.PaintingStyle.Stroke
            } else {
                androidx.compose.ui.graphics.PaintingStyle.Fill
            }
            if (styleToUse is androidx.compose.ui.graphics.drawscope.Stroke) {
                this.strokeWidth = styleToUse.width
                this.strokeCap = styleToUse.cap
                this.strokeMiterLimit = styleToUse.miter
                this.strokeJoin = styleToUse.join
                asFrameworkPaint().isAntiAlias = true
            }
            this.blendMode = blendModeOverride ?: composeBlendMode
            if (isShadowOrEffect && layer.type == com.example.studio.model.LayerType.IMAGE_CARD) {
                this.colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(finalColor, androidx.compose.ui.graphics.BlendMode.SrcIn)
            }
            
            if (!needsRestore) {
                // Fallback to mask filter if RenderEffect failed or SDK < 31
                asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(blurRadius, blurMode)
            }
            if (gradientShader != null) {
                asFrameworkPaint().shader = gradientShader
            }
        }

        if (!needsRestore && (offsetX != 0f || offsetY != 0f)) {
            originalCanvas.save()
            originalCanvas.translate(offsetX, offsetY)
        }

        when (layer.type) {
            com.example.studio.model.LayerType.VECTOR_RECT -> {
                val maxR = minOf(layer.width, layer.height) / 2f
                val effectiveR = (cornerRadiusOverride ?: layer.cornerRadius).coerceIn(0f, maxR)
                if (effectiveR > 0f) {
                    drawContext.canvas.drawRoundRect(0f, 0f, layer.width, layer.height, effectiveR, effectiveR, paint)
                } else {
                    drawContext.canvas.drawRect(0f, 0f, layer.width, layer.height, paint)
                }
            }
            com.example.studio.model.LayerType.VECTOR_TRIANGLE -> {
                val cacheKey = getLayerGeometryHash(layer)
                var path = pathCache.get(cacheKey)
                if (path == null) {
                    path = androidx.compose.ui.graphics.Path().apply {
                        val edges = if (layer.polygonEdges in 3..25) layer.polygonEdges else 3
                        val cx = layer.width / 2f
                        val cy = layer.height / 2f
                        val r = minOf(layer.width, layer.height) / 2f
                        for (i in 0 until edges) {
                            val angle = -Math.PI / 2.0 + i * 2.0 * Math.PI / edges
                            val x = (cx + r * Math.cos(angle)).toFloat()
                            val y = (cy + r * Math.sin(angle)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    pathCache.put(cacheKey, path)
                }
                if (layer.cornerRadius > 0f) {
                    val maxR = minOf(layer.width, layer.height) / 4f
                    val effR = layer.cornerRadius.coerceIn(0f, maxR)
                    if (effR > 0f) {
                        paint.asFrameworkPaint().pathEffect = android.graphics.CornerPathEffect(effR)
                    }
                }
                drawContext.canvas.drawPath(path, paint)
            }
            com.example.studio.model.LayerType.VECTOR_OVAL -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(0f, 0f, layer.width, layer.height))
                }
                drawContext.canvas.drawPath(path, paint)
            }
            com.example.studio.model.LayerType.VECTOR_LINE -> {
                val px = layer.width
                val py = layer.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(px, py)
                }
                drawContext.canvas.drawPath(path, paint)
            }
            com.example.studio.model.LayerType.VECTOR_BEZIER -> {
                if (layer.brushPoints.isNotEmpty()) {
                    val cacheKey = getLayerGeometryHash(layer)
                    var path = pathCache.get(cacheKey)
                    if (path == null) {
                        path = androidx.compose.ui.graphics.Path().apply {
                            if (layer.brushPoints.size >= 2) {
                                moveTo(layer.brushPoints[0].x, layer.brushPoints[0].y)
                                for (i in 1 until layer.brushPoints.size) {
                                    val p0 = layer.brushPoints[i - 1]
                                    val p1 = layer.brushPoints[i]
                                    val mx = (p0.x + p1.x) / 2f
                                    val my = (p0.y + p1.y) / 2f
                                    quadraticBezierTo(p0.x, p0.y, mx, my)
                                }
                                lineTo(layer.brushPoints.last().x, layer.brushPoints.last().y)
                            }
                        }
                        pathCache.put(cacheKey, path)
                    }
                    drawContext.canvas.drawPath(path, paint)
                }
            }
            com.example.studio.model.LayerType.IMAGE_CARD -> {
                drawRect(
                    color = finalColor,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(layer.width, layer.height),
                    style = fillStyle,
                    alpha = finalOpacity * layerOpacity,
                    blendMode = composeBlendMode
                )
            }
            else -> {
                val cacheKey = getLayerGeometryHash(layer)
                var path = pathCache.get(cacheKey)
                if (path == null) {
                    path = createComplexShapePath(layer.type, layer.width, layer.height, layer.polygonEdges, layer.starInnerRadiusRatio, layer.skewX, layer.skewY, cornerRadiusOverride ?: layer.cornerRadius)
                    pathCache.put(cacheKey, path)
                }
                if (layer.cornerRadius > 0f) {
                    val maxR = minOf(layer.width, layer.height) / 4f
                    val effR = layer.cornerRadius.coerceIn(0f, maxR)
                    if (effR > 0f) {
                        paint.asFrameworkPaint().pathEffect = android.graphics.CornerPathEffect(effR)
                    }
                }
                drawContext.canvas.drawPath(path, paint)
            }
        }

        if (needsRestore) {
            originalCanvas.restore()
        } else if (offsetX != 0f || offsetY != 0f) {
            originalCanvas.restore()
        }
    }"""

pattern = re.compile(r'    // Shape/Geometry drawing helper with high-quality GPU BlurMaskFilter support\n    fun drawGeometryWithBlurAndOffset.*?        }\n    }', re.DOTALL)
new_content = pattern.sub(replace_func, content)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(new_content)

INNER
python3 patch_drawblur.py
