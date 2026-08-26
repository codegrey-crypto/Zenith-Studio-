import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

# We want to replace the current `onRasterizeLayer = { id -> ... },` block that we patched.
start_idx = content.find("onRasterizeLayer = { id ->")
if start_idx != -1:
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
                                            fun getDescendants(parentId: String): List<com.example.studio.model.StudioLayer> {
                                                val direct = layers.filter { it.parentGroupId == parentId }
                                                return direct + direct.flatMap { if (it.type == com.example.studio.model.LayerType.GROUP) getDescendants(it.id) else emptyList() }
                                            }
                                            
                                            val isGroup = orig.type == com.example.studio.model.LayerType.GROUP
                                            val descendants = if (isGroup) getDescendants(orig.id) else emptyList()
                                            
                                            val layersToRasterize = if (isGroup) {
                                                val descendantIds = descendants.map { it.id }.toSet()
                                                layers.filter { it.id in descendantIds }.reversed()
                                            } else {
                                                listOf(orig)
                                            }
                                            
                                            if (layersToRasterize.isEmpty() && isGroup) return@launch
                                            
                                            var minX = Float.MAX_VALUE
                                            var minY = Float.MAX_VALUE
                                            var maxX = -Float.MAX_VALUE
                                            var maxY = -Float.MAX_VALUE
                                            var maxPadding = 0f
                                            
                                            if (isGroup) {
                                                layersToRasterize.forEach { l ->
                                                    minX = kotlin.math.min(minX, l.positionX)
                                                    minY = kotlin.math.min(minY, l.positionY)
                                                    maxX = kotlin.math.max(maxX, l.positionX + l.width)
                                                    maxY = kotlin.math.max(maxY, l.positionY + l.height)
                                                }
                                                if (minX == Float.MAX_VALUE) {
                                                    minX = orig.positionX; minY = orig.positionY; maxX = orig.positionX + orig.width; maxY = orig.positionY + orig.height
                                                }
                                            } else {
                                                minX = orig.positionX
                                                minY = orig.positionY
                                                maxX = orig.positionX + orig.width
                                                maxY = orig.positionY + orig.height
                                            }
                                            
                                            layersToRasterize.forEach { l ->
                                                val paddingPx = if (l.effects.any { it.isEnabled && it is com.example.studio.model.StudioEffect.PhotoshopEffect && it.effectType in listOf("DropShadow", "OuterGlow", "BordersAndShadows") }) {
                                                    kotlin.math.max(l.width, l.height) * 0.5f
                                                } else if (l.strokeThickness > 0f) {
                                                    l.strokeThickness + 8f
                                                } else {
                                                    0f
                                                }
                                                maxPadding = kotlin.math.max(maxPadding, paddingPx)
                                            }
                                            
                                            val boundsLeft = minX - maxPadding
                                            val boundsTop = minY - maxPadding
                                            val boundsRight = maxX + maxPadding
                                            val boundsBottom = maxY + maxPadding
                                            
                                            val layW = kotlin.math.max(1f, boundsRight - boundsLeft)
                                            val layH = kotlin.math.max(1f, boundsBottom - boundsTop)
                                            
                                            // Get zoom safely
                                            val zoom = try { currentFitScaleState.value * currentScaleFactorState.value } catch (e: Exception) { 1.0f }
                                            val pxPerUnit = kotlin.math.min(zoom * 2f, 4096f / kotlin.math.max(layW, layH))
                                            
                                            val sizePx = androidx.compose.ui.unit.IntSize(
                                                kotlin.math.ceil(layW * pxPerUnit).toInt().coerceAtLeast(1),
                                                kotlin.math.ceil(layH * pxPerUnit).toInt().coerceAtLeast(1)
                                            )
                                            
                                            val oldCenterX = orig.positionX + orig.width * orig.pivotX
                                            val oldCenterY = orig.positionY + orig.height * orig.pivotY
                                            val newPivotX = if (layW > 0f) (oldCenterX - boundsLeft) / layW else 0.5f
                                            val newPivotY = if (layH > 0f) (oldCenterY - boundsTop) / layH else 0.5f
                                            
                                            val untransformedOrig = orig.copy(
                                                rotation = 0f, scaleX = 1f, scaleY = 1f, skewX = 0f, skewY = 0f, perspX = 0f, perspY = 0f
                                            )
                                            val tempAllLayers = layers.map { if (it.id == orig.id) untransformedOrig else it }
                                            
                                            rasterGraphicsLayer.record(
                                                density = androidx.compose.ui.unit.Density(1f, 1f),
                                                layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
                                                size = sizePx
                                            ) {
                                                withTransform({
                                                    scale(pxPerUnit, pxPerUnit, pivot = androidx.compose.ui.geometry.Offset.Zero)
                                                    translate(-boundsLeft, -boundsTop)
                                                }) {
                                                    layersToRasterize.forEach { layerToDraw ->
                                                        if (layerToDraw.isClippingMask) return@forEach
                                                        
                                                        val targetLayer = tempAllLayers.find { it.id == layerToDraw.id } ?: layerToDraw
                                                        
                                                        drawLayerWithFullPipeline(
                                                            layer = targetLayer,
                                                            allLayers = tempAllLayers,
                                                            layerOpacity = targetLayer.opacity,
                                                            selectedLayerId = null,
                                                            pathCache = pathCache,
                                                            pathPointsCountCache = pathPointsCountCache,
                                                            totalScale = 1.0f,
                                                            dashEffect = dashEffect8,
                                                            imageBitmapCache = imageBitmapCache,
                                                            composeBlendMode = targetLayer.blendMode.toComposeBlendMode(),
                                                            sharedTransformMatrix = androidx.compose.ui.graphics.Matrix(),
                                                            activeTool = activeTool,
                                                            liveDragScaleX = 1.0f,
                                                            liveDragScaleY = 1.0f,
                                                            viewportWidth = currentViewportWidthState.value,
                                                            viewportHeight = currentViewportHeightState.value,
                                                            panX = canvasPanX,
                                                            panY = canvasPanY,
                                                            canvasWidth = canvasWidth,
                                                            canvasHeight = canvasHeight,
                                                            isRasterizing = true
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            val imageBitmap = rasterGraphicsLayer.toImageBitmap()
                                            val hardwareBitmap = imageBitmap.asAndroidBitmap()
                                            
                                            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                val dir = java.io.File(context.filesDir, "rasterized").apply { mkdirs() }
                                                val f = java.io.File(dir, "${java.util.UUID.randomUUID()}.png")
                                                java.io.FileOutputStream(f).use { out ->
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
                                                pivotX = newPivotX,
                                                pivotY = newPivotY,
                                                rotation = orig.rotation,
                                                scaleX = orig.scaleX,
                                                scaleY = orig.scaleY,
                                                skewX = orig.skewX,
                                                skewY = orig.skewY,
                                                perspX = orig.perspX,
                                                perspY = orig.perspY,
                                                textContent = "",
                                                richTextSpansJson = "",
                                                brushPoints = emptyList(),
                                                meshWarpPointsStr = "",
                                                perspWarpPointsStr = "",
                                                effects = emptyList()
                                            )
                                            
                                            val descendantIds = descendants.map { it.id }.toSet()
                                            var updated = layers.filter { it.id !in descendantIds }.map { if (it.id == id) rasterized else it }
                                            
                                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.saveUndoState()
                                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.filtersByLayer[id]?.clear()
                                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.filtersByLayer.remove(id)
                                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.changeCounter.value++
                                            
                                            if (cachedImg != null) {
                                                imageBitmapCache[file.absolutePath] = cachedImg
                                            }
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
        print("Patched onRasterizeLayer v3")
    else:
        print("End brace not found")
else:
    print("Start index not found")
