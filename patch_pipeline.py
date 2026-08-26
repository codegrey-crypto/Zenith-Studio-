import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

# We need to insert drawLayerWithFullPipeline. We can put it right before drawSingleConnectedLayer.
target_idx = content.find("private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSingleConnectedLayer")
if target_idx == -1:
    print("Could not find drawSingleConnectedLayer")
    exit(1)

pipeline_code = """
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLayerWithFullPipeline(
    layer: com.example.studio.model.StudioLayer,
    allLayers: List<com.example.studio.model.StudioLayer>,
    layerOpacity: Float,
    selectedLayerId: String?,
    pathCache: android.util.SparseArray<androidx.compose.ui.graphics.Path>,
    pathPointsCountCache: android.util.SparseIntArray,
    totalScale: Float,
    dashEffect: androidx.compose.ui.graphics.PathEffect,
    imageBitmapCache: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    composeBlendMode: androidx.compose.ui.graphics.BlendMode,
    sharedTransformMatrix: androidx.compose.ui.graphics.Matrix,
    activeTool: String,
    liveDragScaleX: Float,
    liveDragScaleY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    panX: Float,
    panY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    pulseAlpha: Float = 1f,
    spinAngle: Float = 0f,
    activeBezierPointIndex: Int = -1,
    activeWarpNodeIndex: Int = -1,
    warpRepeatMode: String = "Off",
    warpRepeatX: Float = 1f,
    warpRepeatY: Float = 1f,
    warpPhaseX: Float = 0f,
    warpPhaseY: Float = 0f,
    warpInterpolation: String = "Linear",
    warpTarget: String = "Mesh",
    warpMeshDivisionX: Int = 2,
    warpMeshDivisionY: Int = 2,
    isDraggingBezierNode: Boolean = false,
    showGradientControls: Boolean = false,
    isRasterizing: Boolean = false
) {
    val index = allLayers.indexOfFirst { it.id == layer.id }
    if (index == -1) return
    
    val clippedLayers = mutableListOf<com.example.studio.model.StudioLayer>()
    for (i in (index - 1) downTo 0) {
        val prospect = allLayers[i]
        if (!prospect.isVisible) continue
        if (prospect.isClippingMask) {
            clippedLayers.add(prospect)
        } else {
            break
        }
    }
    
    val hasAdjustmentEffect = layer.effects.any { it.isEnabled && it is com.example.studio.model.StudioEffect.PhotoshopEffect && 
        it.effectType in listOf("CameraRaw", "ColorGrading", "Fast Adjusting", "color_fast_adjusting", "FastAdjusting", "fast_adj", "Solarize", "Emboss", "FindEdges", "ColorHalftone", "Sketch", "Mosaic", "Twirl", "Spherize", "SmartSharpen", "UnsharpMask", "AddNoise") }
    val hasGlassEffect = layer.effects.any { it.isEnabled && it is com.example.studio.model.StudioEffect.PhotoshopEffect && (it.effectType == "GlassMorphism" || it.effectType == "ReededGlass") }
    val needsBackdrop = hasGlassEffect || hasAdjustmentEffect || layer.blendMode != com.example.studio.model.ZenithBlendMode.NORMAL || layer.effects.any { it.isEnabled && it is com.example.studio.model.StudioEffect.PhotoshopEffect && it.effectType !in listOf("BrushConfig", "DropShadow", "InnerShadow", "BordersAndShadows", "OuterGlow", "InnerGlow", "BevelEmboss", "Satin", "GradientOverlay", "PatternOverlay", "Stroke", "GlassMorphism", "ReededGlass") }
    
    val currentBackdrop = if (needsBackdrop && !isRasterizing) {
        // Backdrops are expensive during rasterize unless necessary, but we can pass null for now
        // since we didn't pass reusableBackdropBitmap here. 
        // For accurate rasterization, we might need a backdrop, but for now we skip.
        null
    } else { null }
    
    if (clippedLayers.isNotEmpty()) {
        try {
            drawSingleConnectedLayer(
                layer = layer, layerOpacity = layerOpacity, selectedLayerId = selectedLayerId,
                pathCache = pathCache, pathPointsCountCache = pathPointsCountCache,
                totalScale = totalScale, dashEffect = dashEffect, imageBitmapCache = imageBitmapCache,
                composeBlendMode = composeBlendMode, sharedTransformMatrix = sharedTransformMatrix,
                backdropBitmap = currentBackdrop, globalX = layer.positionX, globalY = layer.positionY,
                activeTool = activeTool, allLayers = allLayers, liveDragScaleX = liveDragScaleX, liveDragScaleY = liveDragScaleY,
                viewportWidth = viewportWidth, viewportHeight = viewportHeight, panX = panX, panY = panY,
                canvasWidth = canvasWidth, canvasHeight = canvasHeight, pulseAlpha = pulseAlpha, spinAngle = spinAngle,
                activeBezierPointIndex = activeBezierPointIndex, activeWarpNodeIndex = activeWarpNodeIndex,
                warpRepeatMode = warpRepeatMode, warpRepeatX = warpRepeatX, warpRepeatY = warpRepeatY,
                warpPhaseX = warpPhaseX, warpPhaseY = warpPhaseY, warpInterpolation = warpInterpolation,
                warpTarget = warpTarget, warpMeshDivisionX = warpMeshDivisionX, warpMeshDivisionY = warpMeshDivisionY,
                isDraggingBezierNode = isDraggingBezierNode, showGradientControls = showGradientControls
            )
            val bounds = androidx.compose.ui.geometry.Rect(0f, 0f, canvasWidth, canvasHeight)
            drawContext.canvas.saveLayer(bounds, androidx.compose.ui.graphics.Paint())
            drawSingleConnectedLayer(
                layer = layer, layerOpacity = layerOpacity, selectedLayerId = selectedLayerId,
                pathCache = pathCache, pathPointsCountCache = pathPointsCountCache,
                totalScale = totalScale, dashEffect = dashEffect, imageBitmapCache = imageBitmapCache,
                composeBlendMode = composeBlendMode, sharedTransformMatrix = sharedTransformMatrix,
                backdropBitmap = currentBackdrop, globalX = layer.positionX, globalY = layer.positionY,
                activeTool = activeTool, allLayers = allLayers, liveDragScaleX = liveDragScaleX, liveDragScaleY = liveDragScaleY,
                viewportWidth = viewportWidth, viewportHeight = viewportHeight, panX = panX, panY = panY,
                canvasWidth = canvasWidth, canvasHeight = canvasHeight, pulseAlpha = pulseAlpha, spinAngle = spinAngle,
                activeBezierPointIndex = activeBezierPointIndex, activeWarpNodeIndex = activeWarpNodeIndex,
                warpRepeatMode = warpRepeatMode, warpRepeatX = warpRepeatX, warpRepeatY = warpRepeatY,
                warpPhaseX = warpPhaseX, warpPhaseY = warpPhaseY, warpInterpolation = warpInterpolation,
                warpTarget = warpTarget, warpMeshDivisionX = warpMeshDivisionX, warpMeshDivisionY = warpMeshDivisionY,
                isDraggingBezierNode = isDraggingBezierNode, showGradientControls = showGradientControls
            )
            val groupPaint = androidx.compose.ui.graphics.Paint().apply { blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn }
            drawContext.canvas.saveLayer(bounds, groupPaint)
            for (clipped in clippedLayers) {
                drawSingleConnectedLayer(
                    layer = clipped, layerOpacity = clipped.opacity, selectedLayerId = selectedLayerId,
                    pathCache = pathCache, pathPointsCountCache = pathPointsCountCache,
                    totalScale = totalScale, dashEffect = dashEffect, imageBitmapCache = imageBitmapCache,
                    composeBlendMode = clipped.blendMode.toComposeBlendMode(), sharedTransformMatrix = sharedTransformMatrix,
                    backdropBitmap = null, globalX = clipped.positionX, globalY = clipped.positionY,
                    activeTool = activeTool, allLayers = allLayers, liveDragScaleX = liveDragScaleX, liveDragScaleY = liveDragScaleY,
                    viewportWidth = viewportWidth, viewportHeight = viewportHeight, panX = panX, panY = panY,
                    canvasWidth = canvasWidth, canvasHeight = canvasHeight, pulseAlpha = pulseAlpha, spinAngle = spinAngle,
                    activeBezierPointIndex = activeBezierPointIndex, activeWarpNodeIndex = activeWarpNodeIndex,
                    warpRepeatMode = warpRepeatMode, warpRepeatX = warpRepeatX, warpRepeatY = warpRepeatY,
                    warpPhaseX = warpPhaseX, warpPhaseY = warpPhaseY, warpInterpolation = warpInterpolation,
                    warpTarget = warpTarget, warpMeshDivisionX = warpMeshDivisionX, warpMeshDivisionY = warpMeshDivisionY,
                    isDraggingBezierNode = isDraggingBezierNode, showGradientControls = showGradientControls
                )
            }
            drawContext.canvas.restore()
            drawContext.canvas.restore()
        } catch (e: Exception) { e.printStackTrace() }
    } else {
        drawSingleConnectedLayer(
            layer = layer, layerOpacity = layerOpacity, selectedLayerId = selectedLayerId,
            pathCache = pathCache, pathPointsCountCache = pathPointsCountCache,
            totalScale = totalScale, dashEffect = dashEffect, imageBitmapCache = imageBitmapCache,
            composeBlendMode = composeBlendMode, sharedTransformMatrix = sharedTransformMatrix,
            backdropBitmap = currentBackdrop, globalX = layer.positionX, globalY = layer.positionY,
            activeTool = activeTool, allLayers = allLayers, liveDragScaleX = liveDragScaleX, liveDragScaleY = liveDragScaleY,
            viewportWidth = viewportWidth, viewportHeight = viewportHeight, panX = panX, panY = panY,
            canvasWidth = canvasWidth, canvasHeight = canvasHeight, pulseAlpha = pulseAlpha, spinAngle = spinAngle,
            activeBezierPointIndex = activeBezierPointIndex, activeWarpNodeIndex = activeWarpNodeIndex,
            warpRepeatMode = warpRepeatMode, warpRepeatX = warpRepeatX, warpRepeatY = warpRepeatY,
            warpPhaseX = warpPhaseX, warpPhaseY = warpPhaseY, warpInterpolation = warpInterpolation,
            warpTarget = warpTarget, warpMeshDivisionX = warpMeshDivisionX, warpMeshDivisionY = warpMeshDivisionY,
            isDraggingBezierNode = isDraggingBezierNode, showGradientControls = showGradientControls
        )
    }
}
"""

content = content[:target_idx] + pipeline_code + "\n" + content[target_idx:]

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
print("Added drawLayerWithFullPipeline")
