import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

# We need to replace the `onRasterizeLayer = { id -> ... },` block.
# We will use regex to find the block and replace it.

start_idx = content.find("onRasterizeLayer = { id ->")
if start_idx != -1:
    # Find matching closing brace
    depth = 0
    end_idx = -1
    for i in range(start_idx, len(content)):
        if content[i] == '{':
            depth += 1
        elif content[i] == '}':
            depth -= 1
            if depth == 0:
                end_idx = i
                break
    
    if end_idx != -1:
        new_block = """onRasterizeLayer = { id ->
                                val orig = layers.find { it.id == id }
                                if (orig != null) {
                                    scope.launch {
                                        try {
                                            val paddingPx = if (orig.effects.any { it.isEnabled && it is com.example.studio.model.StudioEffect.PhotoshopEffect && it.effectType in listOf("DropShadow", "OuterGlow", "BordersAndShadows") }) {
                                                kotlin.math.max(orig.width, orig.height) * 0.5f
                                            } else if (orig.strokeThickness > 0f) {
                                                orig.strokeThickness + 8f
                                            } else {
                                                0f
                                            }
                                            
                                            val layW = kotlin.math.max(1f, orig.width + 2 * paddingPx)
                                            val layH = kotlin.math.max(1f, orig.height + 2 * paddingPx)
                                            
                                            val zoom = currentViewportScaleState.value
                                            val pxPerUnit = kotlin.math.min(zoom * 2f, 4096f / kotlin.math.max(layW, layH))
                                            
                                            val sizePx = androidx.compose.ui.unit.IntSize(
                                                kotlin.math.ceil(layW * pxPerUnit).toInt().coerceAtLeast(1),
                                                kotlin.math.ceil(layH * pxPerUnit).toInt().coerceAtLeast(1)
                                            )
                                            
                                            val boundsLeft = orig.positionX - paddingPx
                                            val boundsTop = orig.positionY - paddingPx
                                            
                                            rasterGraphicsLayer.record(sizePx, androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.unit.Density(1f, 1f)) {
                                                androidx.compose.ui.graphics.drawscope.withTransform({
                                                    scale(pxPerUnit, pxPerUnit, pivot = androidx.compose.ui.geometry.Offset.Zero)
                                                    translate(-boundsLeft, -boundsTop)
                                                }) {
                                                    drawAllEffectsAndLayersLocal(
                                                        layer = orig,
                                                        layerOpacity = 1.0f,
                                                        selectedLayerId = null,
                                                        pathCache = pathCache,
                                                        pathPointsCountCache = pathPointsCountCache,
                                                        totalScale = 1.0f,
                                                        dashEffect = dashEffect8,
                                                        composeBlendMode = androidx.compose.ui.graphics.BlendMode.SrcOver,
                                                        imageBitmapCache = imageBitmapCache,
                                                        backdropBitmap = null,
                                                        globalX = boundsLeft,
                                                        globalY = boundsTop,
                                                        activeTool = activeTool,
                                                        viewportWidth = viewportWidth,
                                                        viewportHeight = viewportHeight,
                                                        panX = canvasPanX,
                                                        panY = canvasPanY,
                                                        canvasWidth = canvasWidth,
                                                        canvasHeight = canvasHeight,
                                                        isExporting = true
                                                    )
                                                }
                                            }
                                            
                                            val imageBitmap = rasterGraphicsLayer.toImageBitmap()
                                            val hardwareBitmap = imageBitmap.asAndroidBitmap()
                                            
                                            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                val f = java.io.File(context.filesDir, "rasterized_${System.currentTimeMillis()}.png")
                                                java.io.FileOutputStream(f).use { out ->
                                                    // In Compose 1.7, toImageBitmap() might return a hardware bitmap, which we can compress directly in most modern Android versions.
                                                    // But to be safe, we'll copy it to software if compression fails.
                                                    try {
                                                        hardwareBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                    } catch (e: Exception) {
                                                        val swBitmap = hardwareBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                                                        swBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                        swBitmap.recycle()
                                                    }
                                                }
                                                f
                                            }
                                            
                                            // We don't recycle the hardware bitmap directly since it is tied to GraphicsLayer.
                                            
                                            // Re-load the saved file into a safe, independent ImageBitmap for the cache
                                            val cachedImg = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                                bmp?.asImageBitmap()
                                            }
                                            
                                            undoStack.add(layers)
                                            redoStack.clear()
                                            
                                            val rasterized = orig.copy(
                                                type = com.example.studio.model.LayerType.IMAGE_CARD,
                                                imageUri = file.absolutePath,
                                                width = layW,
                                                height = layH,
                                                positionX = boundsLeft,
                                                positionY = boundsTop,
                                                pivotX = orig.pivotX,
                                                pivotY = orig.pivotY,
                                                rotation = orig.rotation,
                                                scaleX = orig.scaleX,
                                                scaleY = orig.scaleY,
                                                textContent = "",
                                                richTextSpansJson = "",
                                                brushPoints = emptyList(),
                                                meshWarpPointsStr = "",
                                                perspWarpPointsStr = "",
                                                effects = emptyList()
                                            )
                                            
                                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.saveUndoState()
                                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.filtersByLayer[id]?.clear()
                                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.filtersByLayer.remove(id)
                                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.changeCounter.value++
                                            
                                            if (cachedImg != null) {
                                                imageBitmapCache[file.absolutePath] = cachedImg
                                            }
                                            val updated = layers.map { if (it.id == id) rasterized else it }
                                            layers = updated
                                            artboards = artboards.map { if (it.id == selectedArtboardId) it.copy(layers = updated) else it }
                                            
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }"""
        
        content = content[:start_idx] + new_block + content[end_idx+1:]
        
        with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
            f.write(content)
        print("Patched onRasterizeLayer")
    else:
        print("End brace not found")
else:
    print("Start index not found")
