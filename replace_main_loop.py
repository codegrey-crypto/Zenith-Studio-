import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

start_marker = "val clippedLayers = mutableListOf<com.example.studio.model.StudioLayer>()"
start_idx = content.find(start_marker, 9000, 15000)

if start_idx == -1:
    print("Start marker not found")
    exit(1)

# Find the end by looking for `isDraggingBezierNode = isDraggingBezierNode` and then `}`
end_marker = "isDraggingBezierNode = isDraggingBezierNode"
end_marker_idx = content.find(end_marker, start_idx)

# Find the closing parenthesis `)` after end_marker
close_paren_idx = content.find(")", end_marker_idx)

# Find the closing brace `}` after the closing parenthesis
close_brace_idx = content.find("}", close_paren_idx)

if close_brace_idx == -1:
    print("Close brace not found")
    exit(1)

replacement = """drawLayerWithFullPipeline(
                                        layer = layer,
                                        allLayers = layers,
                                        layerOpacity = layerOpacity,
                                        selectedLayerId = selectedLayerId,
                                        pathCache = pathCache,
                                        pathPointsCountCache = pathPointsCountCache,
                                        totalScale = totalScale,
                                        dashEffect = dashEffect8,
                                        imageBitmapCache = imageBitmapCache,
                                        composeBlendMode = composeBlendMode,
                                        sharedTransformMatrix = sharedTransformMatrix,
                                        activeTool = activeTool,
                                        liveDragScaleX = if (isResizingActive && layer.id == activeResizingLayerId) liveDragScaleX else 1.0f,
                                        liveDragScaleY = if (isResizingActive && layer.id == activeResizingLayerId) liveDragScaleY else 1.0f,
                                        viewportWidth = currentViewportWidthState.value,
                                        viewportHeight = currentViewportHeightState.value,
                                        panX = canvasPanX,
                                        panY = canvasPanY,
                                        canvasWidth = canvasWidth,
                                        canvasHeight = canvasHeight,
                                        pulseAlpha = pulseAlpha,
                                        spinAngle = spinAngle,
                                        activeBezierPointIndex = activeBezierPointIndex,
                                        activeWarpNodeIndex = activeWarpNodeIndex,
                                        warpRepeatMode = warpRepeatMode,
                                        warpRepeatX = warpRepeatX,
                                        warpRepeatY = warpRepeatY,
                                        warpPhaseX = warpPhaseX,
                                        warpPhaseY = warpPhaseY,
                                        warpInterpolation = warpInterpolation,
                                        warpTarget = warpTarget,
                                        warpMeshDivisionX = warpMeshDivisionX,
                                        warpMeshDivisionY = warpMeshDivisionY,
                                        isDraggingBezierNode = isDraggingBezierNode,
                                        showGradientControls = showGradientControls,
                                        isRasterizing = false
                                    )"""

# Look at what is exactly being replaced
print("Replacing block starting with:")
print(content[start_idx:start_idx+100])
print("Ending with:")
print(content[close_brace_idx-50:close_brace_idx+1])

# Also we need to make sure we include the `val hasAdjustmentEffect` lines in the replaced block because they are inside the `drawLayerWithFullPipeline`!
# The current code has `val hasAdjustmentEffect` before the `if (clippedLayers.isNotEmpty())` block.
# Let's check if the start_marker is BEFORE `val hasAdjustmentEffect`.
