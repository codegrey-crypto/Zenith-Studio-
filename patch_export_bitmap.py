import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

start_marker = "val clippedLayers = mutableListOf<com.example.studio.model.StudioLayer>()"
start_idx = content.find(start_marker, 10000)

if start_idx == -1:
    print("Start marker not found")
    exit(1)

# Find the first one after index 10000. Wait, there are two now: the one in generateExportBitmap and the one in drawLayerWithFullPipeline.
# We want the one in generateExportBitmap (which is around line 17485).
# Let's find it more robustly:
generate_export_marker = "fun generateExportBitmap("
gen_idx = content.find(generate_export_marker)

start_idx = content.find(start_marker, gen_idx)

end_marker = "if (false) {"
end_marker_idx = content.find(end_marker, start_idx)

close_if_false_marker = "allLayers = layers\n                                         )\n                                     }"
close_if_false_idx = content.find(close_if_false_marker, end_marker_idx)
if close_if_false_idx != -1:
    end_idx = close_if_false_idx + len(close_if_false_marker)
else:
    end_idx = content.find("}\n                                if (gridEnabled) {", end_marker_idx)
    if end_idx == -1:
        end_idx = content.find("}\n            }\n            \n            if (watermark) {", end_marker_idx)
        if end_idx == -1:
            end_idx = content.find("}\n            }\n            \n            // Apply watermarking", end_marker_idx)
            if end_idx == -1:
                end_idx = content.find("}\n            }", end_marker_idx)

if end_idx == -1:
    print("End marker not found for export bitmap")
    exit(1)

# The replacement for the generateExportBitmap loop
replacement = """drawLayerWithFullPipeline(
                    layer = originalLayer,
                    allLayers = layers,
                    layerOpacity = originalLayer.opacity,
                    selectedLayerId = null,
                    pathCache = localPathCache,
                    pathPointsCountCache = localPathPointsCountCache,
                    totalScale = 1.0f,
                    dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                    imageBitmapCache = exportImageCache,
                    composeBlendMode = originalLayer.blendMode.toComposeBlendMode(),
                    sharedTransformMatrix = localTransformMatrix,
                    activeTool = "Pointer",
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
                            currentIndex = index,
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

content = content[:start_idx] + replacement + "\n" + content[end_idx:]

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
print("Patched export bitmap loop")
