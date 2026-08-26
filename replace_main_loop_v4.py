import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

start_marker = "val clippedLayers = mutableListOf<com.example.studio.model.StudioLayer>()"
start_idx = content.find(start_marker)
if start_idx == -1:
    print("Start marker not found")
    exit(1)

# Find the first one and the second one
second_idx = content.find(start_marker, start_idx + 10)
print(f"First idx: {start_idx}, Second idx: {second_idx}")

def replace_block(content_str, s_idx):
    end_marker = "if (false) {"
    end_marker_idx = content_str.find(end_marker, s_idx)

    close_if_false_marker = "allLayers = layers\n                                         )\n                                     }"
    close_if_false_idx = content_str.find(close_if_false_marker, end_marker_idx)
    if close_if_false_idx != -1:
        e_idx = close_if_false_idx + len(close_if_false_marker)
    else:
        e_idx = content_str.find("}\n                                if (gridEnabled) {", end_marker_idx)
        if e_idx == -1:
            e_idx = content_str.find("}\n                            }\n                        }\n                    }\n                }", end_marker_idx)
            
    if e_idx == -1:
        print("End marker not found")
        return content_str
        
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
    return content_str[:s_idx] + replacement + "\n" + content_str[e_idx:]

new_content = replace_block(content, start_idx)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(new_content)
print("Replaced main loop body")
