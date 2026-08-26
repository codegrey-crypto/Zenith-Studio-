import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

start_marker = "val clippedLayers = mutableListOf<com.example.studio.model.StudioLayer>()"

# We know the first start_marker was around 610,000 and was removed. Now the one in generateExportBitmap should be the first one!
start_idx = content.find(start_marker)

if start_idx == -1:
    print("Start marker not found")
    exit(1)

# Find the end of `drawSingleConnectedLayer`
end_call_marker = "isExporting = true\n                    )\n                }"
end_call_idx = content.find(end_call_marker, start_idx)

if end_call_idx == -1:
    print("End call marker not found")
    exit(1)

end_idx = end_call_idx + len(end_call_marker)

replacement = """drawLayerWithFullPipeline(
                    layer = originalLayer,
                    allLayers = layers,
                    layerOpacity = originalLayer.opacity,
                    selectedLayerId = null,
                    pathCache = localPathCache,
                    pathPointsCountCache = localPathPointsCountCache,
                    totalScale = 1.0f,
                    dashEffect = localDashEffect,
                    imageBitmapCache = exportImageCache,
                    composeBlendMode = originalLayer.blendMode.toComposeBlendMode(),
                    sharedTransformMatrix = localTransformMatrix,
                    activeTool = "None",
                    liveDragScaleX = 1.0f,
                    liveDragScaleY = 1.0f,
                    viewportWidth = canvasWidth,
                    viewportHeight = canvasHeight,
                    panX = 0f,
                    panY = 0f,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    pulseAlpha = 1.0f,
                    spinAngle = 0f,
                    activeBezierPointIndex = -1,
                    activeWarpNodeIndex = -1,
                    warpRepeatMode = warpRepeatMode,
                    warpRepeatX = warpRepeatX,
                    warpRepeatY = warpRepeatY,
                    warpPhaseX = warpPhaseX,
                    warpPhaseY = warpPhaseY,
                    warpInterpolation = warpInterpolation,
                    warpTarget = warpTarget,
                    warpMeshDivisionX = warpMeshDivisionX,
                    warpMeshDivisionY = warpMeshDivisionY,
                    isDraggingBezierNode = false,
                    showGradientControls = false,
                    isRasterizing = true,
                    backdropProvider = {
                        val computedBackdrop = generateBackdropForLayer(
                            layers = layers,
                            currentIndex = layers.indexOfFirst { it.id == originalLayer.id },
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            imageBitmapCache = exportImageCache,
                            pathCache = localPathCache,
                            pathPointsCountCache = localPathPointsCountCache,
                            existingBitmap = reusableBackdropBitmap,
                            warpMeshDivisionX = warpMeshDivisionX,
                            warpMeshDivisionY = warpMeshDivisionY,
                            warpRepeatMode = warpRepeatMode,
                            warpRepeatX = warpRepeatX,
                            warpRepeatY = warpRepeatY,
                            warpPhaseX = warpPhaseX,
                            warpPhaseY = warpPhaseY,
                            warpInterpolation = warpInterpolation,
                            warpTarget = warpTarget,
                            isExporting = true
                        )
                        if (computedBackdrop != null) {
                            reusableBackdropBitmap = computedBackdrop
                        }
                        computedBackdrop
                    }
                )"""

content = content[:start_idx] + replacement + content[end_idx:]

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
print("Patched export bitmap loop v2")
