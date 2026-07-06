package com.example.studio.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.studio.model.StudioLayer
import androidx.compose.ui.geometry.Offset
import com.example.studio.model.LayerType
import com.example.ui.theme.*

@Composable
fun LandscapeSidebarPanel(
    isLandscape: Boolean,
    activeTool: String,
    isRightSidebarExpanded: Boolean,
    onRightSidebarExpandedChange: (Boolean) -> Unit,
    activeFullScreenSheet: String?,
    onActiveFullScreenSheetChange: (String?) -> Unit,
    layers: List<StudioLayer>,
    onLayersChange: (List<StudioLayer>) -> Unit,
    selectedLayer: StudioLayer?,
    activeGradientStopIndex: Int,
    onActiveGradientStopIndexChange: (Int) -> Unit,
    showGradientColorPickerDialog: Boolean,
    onShowGradientColorPickerDialogChange: (Boolean) -> Unit,
    undoStack: CappedHistoryStack,
    redoStack: CappedHistoryStack,
    selectedEffectIndex: Int,
    onSelectedEffectIndexChange: (Int) -> Unit,
    fontSearchQuery: String,
    onFontSearchQueryChange: (String) -> Unit,
    selectedCategoryFilter: String,
    onSelectedCategoryFilterChange: (String) -> Unit,
    showFontScannerDialog: Boolean,
    onShowFontScannerDialogChange: (Boolean) -> Unit,
    customFonts: List<com.example.studio.database.CustomFontEntity>,
    showZenithEffectsOverlay: Boolean,
    onShowZenithEffectsOverlayChange: (Boolean) -> Unit,
    showEffectsGallery: Boolean,
    onShowEffectsGalleryChange: (Boolean) -> Unit,
    brushSmoothing: Boolean,
    onBrushSmoothingChange: (Boolean) -> Unit,
    brushPresetIndex: Int,
    onBrushPresetIndexChange: (Int) -> Unit,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    brushOpacity: Float,
    onBrushOpacityChange: (Float) -> Unit,
    brushColor: Color,
    onBrushColorChange: (Color) -> Unit,
    brushHardness: Float,
    onBrushHardnessChange: (Float) -> Unit,
    showBrushesLibrary: Boolean,
    onShowBrushesLibraryChange: (Boolean) -> Unit,
    gridEnabled: Boolean,
    onGridEnabledChange: (Boolean) -> Unit,
    rulers: List<StudioRuler>,
    onRulersChange: (List<StudioRuler>) -> Unit,
    selectedRulerId: String,
    onSelectedRulerIdChange: (String) -> Unit,
    rulerOrientation: String,
    rulerPosition: Float,
    eraserSize: Float,
    onEraserSizeChange: (Float) -> Unit,
    eraserHardness: Float,
    onEraserHardnessChange: (Float) -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    gridRows: Int,
    onGridRowsChange: (Int) -> Unit,
    snapToRuler: Boolean,
    onSnapToRulerChange: (Boolean) -> Unit,
    allRulersLocked: Boolean,
    onAllRulersLockedChange: (Boolean) -> Unit,
    activeBezierPointIndex: Int = -1,
    onActiveBezierPointIndexChange: (Int) -> Unit = {},
    penCursorOffset: Offset? = null,
    onPenCursorOffsetChange: (Offset?) -> Unit = {},
    penSubTool: String = "Standard",
    onPenSubToolChange: (String) -> Unit = {},
    artboardWidth: Float = 1080f,
    artboardHeight: Float = 1080f,
    totalScale: Float = 1f,
    onSelectedLayerIdChange: (String) -> Unit = {},
    keepBezierSymmetrical: Boolean = true,
    onKeepBezierSymmetricalChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenRasterExtrudeEditor: (() -> Unit)? = null
) {
    if (!isLandscape) return

    val activeValueEditConfigState = remember { mutableStateOf<SliderValueEditConfig?>(null) }
    val effectiveSheet = activeFullScreenSheet ?: if (activeTool == "Brush" || activeTool == "Eraser" || activeTool == "Pen") "Brush" else "Color"
    val sidebarWidth by animateDpAsState(
        targetValue = if (isRightSidebarExpanded) 320.dp else 0.dp,
        label = "SidebarWidth"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(if (isRightSidebarExpanded) 320.dp else 64.dp)
    ) {
        if (!isRightSidebarExpanded) {
            // Floating expand button on the right edge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .zIndex(9f)
            ) {
                FloatingActionButton(
                    onClick = { onRightSidebarExpandedChange(true) },
                    containerColor = SlatePanel,
                    contentColor = EnergeticYellow,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Expand Sidebar"
                    )
                }
            }
        }

        // Sidebar container
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(sidebarWidth)
                .background(SlatePanel.copy(alpha = 0.95f))
                .border(BorderStroke(1.2.dp, HighslateOutline))
                .zIndex(9f)
                .graphicsLayer {
                    alpha = if (isRightSidebarExpanded) 1f else 0f
                }
        ) {
            if (isRightSidebarExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, "Settings", tint = EnergeticYellow, modifier = Modifier.size(20.dp))
                            Text("Zenith Parameters", style = Typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { onRightSidebarExpandedChange(false) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Collapse Sidebar",
                                tint = TextSecondary
                            )
                        }
                    }

                    Divider(color = HighslateOutline, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // Category Selector Row / Grid (compact layout of category options)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf(
                            "Brush" to Icons.Default.Brush,
                            "Color" to Icons.Default.Palette,
                            "Border" to Icons.Default.Deblur,
                            "Transform" to Icons.Default.AspectRatio,
                            "Shape" to Icons.Default.Category,
                            "Effects" to Icons.Default.Tune,
                            "Grid" to Icons.Default.GridOn,
                            "Ruler" to Icons.Default.Straighten
                        )
                        categories.forEach { (cat, icon) ->
                            val isSelected = effectiveSheet == cat
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) IndustrialAmber.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) IndustrialAmber else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onActiveFullScreenSheetChange(cat)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = cat,
                                    tint = if (isSelected) IndustrialAmber else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Divider(color = HighslateOutline, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // Active Sheet content scrollable list
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        CompositionLocalProvider(LocalSliderValueEditTrigger provides { activeValueEditConfigState.value = it }) {
                            when (effectiveSheet) {
                                "Color" -> {
                                    if (selectedLayer != null) {
                                        HsvColorPickerPanel(
                                            currentColor = selectedLayer.baseColor,
                                            currentOpacity = selectedLayer.opacity,
                                            onColorChanged = { newColor ->
                                                onLayersChange(layers.map { if (it.id == selectedLayer.id) it.copy(baseColor = newColor) else it })
                                            },
                                            selectedLayer = selectedLayer,
                                            onUpdateLayer = { updatedLayer ->
                                                onLayersChange(layers.map { if (it.id == updatedLayer.id) updatedLayer else it })
                                            },
                                            onOpenColorPickerDialog = { idx ->
                                                onActiveGradientStopIndexChange(idx)
                                                onShowGradientColorPickerDialogChange(true)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text("Select a layer to customize colors.", color = TextSecondary, style = Typography.bodyMedium, modifier = Modifier.padding(8.dp))
                                    }
                                }
                                "Border" -> {
                                    if (selectedLayer != null) {
                                        BordersAndShadowsTabPanel(
                                            selectedLayer = selectedLayer,
                                            onAddEffect = { effect ->
                                                undoStack.add(layers)
                                                redoStack.clear()
                                                val updatedList = layers.map { layer ->
                                                    if (layer.id == selectedLayer.id) {
                                                        layer.copy(effects = layer.effects + effect)
                                                    } else layer
                                                }
                                                onLayersChange(updatedList)
                                                onSelectedEffectIndexChange(selectedLayer.effects.size)
                                            },
                                            onUpdateEffectParam = { effectId, paramName, newValue ->
                                                val updatedList = layers.map { layer ->
                                                    if (layer.id == selectedLayer.id) {
                                                        val updatedEffects = layer.effects.map { eff ->
                                                            if (eff.id == effectId) {
                                                                eff.updateParameter(paramName, newValue)
                                                            } else eff
                                                        }
                                                        layer.copy(effects = updatedEffects)
                                                    } else layer
                                                }
                                                onLayersChange(updatedList)
                                            }
                                        )
                                    } else {
                                        Text("Select a vector/shape layer first.", color = TextSecondary, style = Typography.bodyMedium, modifier = Modifier.padding(8.dp))
                                    }
                                }
                                "Transform" -> {
                                    if (selectedLayer != null) {
                                        TransformDetailView(
                                            selectedLayer = selectedLayer,
                                            onUpdateLayer = { updatedLayer ->
                                                onLayersChange(layers.map { if (it.id == updatedLayer.id) updatedLayer else it })
                                            }
                                        )
                                    } else {
                                        Text("Select a layer first.", color = TextSecondary, style = Typography.bodyMedium, modifier = Modifier.padding(8.dp))
                                    }
                                }
                                "Shape" -> {
                                    if (selectedLayer != null) {
                                        TypographyOrShapeDetailView(
                                            selectedLayer = selectedLayer,
                                            onUpdateLayer = { updatedLayer ->
                                                onLayersChange(layers.map { if (it.id == updatedLayer.id) updatedLayer else it })
                                            },
                                            fontSearchQuery = fontSearchQuery,
                                            onFontSearchQueryChange = onFontSearchQueryChange,
                                            selectedCategoryFilter = selectedCategoryFilter,
                                            onSelectedCategoryFilterChange = onSelectedCategoryFilterChange,
                                            onImportFontClick = { onShowFontScannerDialogChange(true) },
                                            customFonts = customFonts
                                        )
                                    } else {
                                        Text("Select a text or shape layer first.", color = TextSecondary, style = Typography.bodyMedium, modifier = Modifier.padding(8.dp))
                                    }
                                }
                                "Effects" -> {
                                    if (selectedLayer != null) {
                                        if (showZenithEffectsOverlay) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // "Back to FX List" Button
                                                Button(
                                                    onClick = { onShowZenithEffectsOverlayChange(false) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = HighslateOutline,
                                                        contentColor = TextPrimary
                                                    ),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronLeft,
                                                        contentDescription = "Back to list",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Back to Core Filters")
                                                }

                                                Divider(color = HighslateOutline, thickness = 0.5.dp)

                                                com.aistudio.zenithstudio.rpxwtq.EffectsWindowShell(
                                                    layerId = selectedLayer.id,
                                                    onClose = { onShowZenithEffectsOverlayChange(false) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    isLandscapeMode = true,
                                                    onOpenRasterExtrudeEditor = onOpenRasterExtrudeEditor
                                                )
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Button(
                                                    onClick = { onShowZenithEffectsOverlayChange(true) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = IndustrialAmber,
                                                        contentColor = Color.Black
                                                    ),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Tune,
                                                        contentDescription = "Zenith Parameters",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Zenith Parameters", fontWeight = FontWeight.Bold)
                                                }

                                                Divider(color = HighslateOutline, thickness = 1.dp)

                                                FiltersAndFxDetailView(
                                                    selectedLayer = selectedLayer,
                                                    selectedEffectIndex = selectedEffectIndex,
                                                    onSelectEffectIndex = onSelectedEffectIndexChange,
                                                    onRemoveEffect = { effectId ->
                                                        undoStack.add(layers)
                                                        redoStack.clear()
                                                        val updatedList = layers.map { layer ->
                                                            if (layer.id == selectedLayer.id) {
                                                                layer.copy(effects = layer.effects.filter { it.id != effectId })
                                                            } else layer
                                                        }
                                                        onLayersChange(updatedList)
                                                    },
                                                    onDuplicateEffect = { effectId ->
                                                        undoStack.add(layers)
                                                        redoStack.clear()
                                                        val updatedList = layers.map { layer ->
                                                            if (layer.id == selectedLayer.id) {
                                                                val effectToDuplicate = layer.effects.find { it.id == effectId }
                                                                if (effectToDuplicate != null) {
                                                                    val duplicated = effectToDuplicate.duplicate()
                                                                    val idx = layer.effects.indexOf(effectToDuplicate)
                                                                    val newList = layer.effects.toMutableList()
                                                                    newList.add(idx + 1, duplicated)
                                                                    layer.copy(effects = newList)
                                                                } else layer
                                                            } else layer
                                                        }
                                                        onLayersChange(updatedList)
                                                    },
                                                    onToggleEffectEnabled = { effectId ->
                                                        undoStack.add(layers)
                                                        redoStack.clear()
                                                        val updatedList = layers.map { layer ->
                                                            if (layer.id == selectedLayer.id) {
                                                                val updatedEffects = layer.effects.map { eff ->
                                                                    if (eff.id == effectId) {
                                                                        eff.toggleEnabled()
                                                                    } else eff
                                                                }
                                                                layer.copy(effects = updatedEffects)
                                                            } else layer
                                                        }
                                                        onLayersChange(updatedList)
                                                    },
                                                    onUpdateEffectParam = { effectId, paramName, newValue ->
                                                        com.example.studio.ui.CanvasEventLoop.emit(
                                                            com.example.studio.ui.CanvasEvent.UpdateSlider(
                                                                layerId = selectedLayer.id,
                                                                effectId = effectId,
                                                                paramName = paramName,
                                                                value = newValue
                                                            )
                                                        )
                                                    },
                                                    onOpenEffectsGallery = { onShowEffectsGalleryChange(true) }
                                                )
                                            }
                                        }
                                    } else {
                                        Text("Select a layer to apply filters.", color = TextSecondary, style = Typography.bodyMedium, modifier = Modifier.padding(8.dp))
                                    }
                                }
                                "Brush" -> {
                                    if (activeTool == "Pen" || selectedLayer?.type == LayerType.VECTOR_BEZIER) {
                                        BezierVectorControlPane(
                                            layer = selectedLayer,
                                            activeBezierPointIndex = activeBezierPointIndex,
                                            onActiveBezierPointIndexChange = onActiveBezierPointIndexChange,
                                            penCursorOffset = penCursorOffset,
                                            onPenCursorOffsetChange = onPenCursorOffsetChange,
                                            penSubTool = penSubTool,
                                            onPenSubToolChange = onPenSubToolChange,
                                            onUpdateLayerProperties = { thickness, color ->
                                                if (selectedLayer != null) {
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    val updatedList = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(strokeThickness = thickness, baseColor = color)
                                                        } else it
                                                    }
                                                    onLayersChange(updatedList)
                                                    com.example.studio.ui.ParametricLayerCache.invalidate(selectedLayer.id)
                                                }
                                            },
                                            onUpdatePoints = { newPoints ->
                                                if (selectedLayer != null) {
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    val updatedList = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(brushPoints = newPoints)
                                                        } else it
                                                    }
                                                    onLayersChange(updatedList)
                                                    com.example.studio.ui.ParametricLayerCache.invalidate(selectedLayer.id)
                                                }
                                            },
                                            onAddPoint = {
                                                val cursor = penCursorOffset ?: Offset(artboardWidth / 2f, artboardHeight / 2f)
                                                if (selectedLayer != null && selectedLayer.type == LayerType.VECTOR_BEZIER) {
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    val updatedPoints = selectedLayer.brushPoints + cursor + cursor + cursor
                                                    val updatedList = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(brushPoints = updatedPoints)
                                                        } else it
                                                    }
                                                    onLayersChange(updatedList)
                                                    onActiveBezierPointIndexChange(updatedPoints.size - 3)
                                                    com.example.studio.ui.ParametricLayerCache.invalidate(selectedLayer.id)
                                                } else {
                                                    val newL = StudioLayer(
                                                        name = "Pen Vector Path ${layers.filter { it.type == LayerType.VECTOR_BEZIER }.size + 1}",
                                                        type = LayerType.VECTOR_BEZIER,
                                                        positionX = 0f,
                                                        positionY = 0f,
                                                        width = artboardWidth,
                                                        height = artboardHeight,
                                                        baseColor = brushColor,
                                                        brushPoints = listOf(cursor, cursor, cursor)
                                                    )
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    onLayersChange(listOf(newL) + layers)
                                                    onSelectedLayerIdChange(newL.id)
                                                    onActiveBezierPointIndexChange(0)
                                                }
                                            },
                                            onDeletePoint = {
                                                if (selectedLayer != null && selectedLayer.type == LayerType.VECTOR_BEZIER && activeBezierPointIndex != -1) {
                                                    val k = activeBezierPointIndex / 3
                                                    val updatedPoints = selectedLayer.brushPoints.toMutableList()
                                                    if (k * 3 in updatedPoints.indices) {
                                                        updatedPoints.removeAt(k * 3)
                                                        if (k * 3 in updatedPoints.indices) updatedPoints.removeAt(k * 3)
                                                        if (k * 3 in updatedPoints.indices) updatedPoints.removeAt(k * 3)
                                                        
                                                        undoStack.add(layers)
                                                        redoStack.clear()
                                                        val updatedList = layers.map {
                                                            if (it.id == selectedLayer.id) {
                                                                it.copy(brushPoints = updatedPoints)
                                                            } else it
                                                        }
                                                        onLayersChange(updatedList)
                                                        onActiveBezierPointIndexChange(
                                                            if (updatedPoints.isNotEmpty()) {
                                                                (k * 3).coerceAtMost(updatedPoints.size - 3)
                                                            } else -1
                                                        )
                                                        com.example.studio.ui.ParametricLayerCache.invalidate(selectedLayer.id)
                                                    }
                                                }
                                            },
                                            onToggleClosePath = {
                                                if (selectedLayer != null && selectedLayer.type == LayerType.VECTOR_BEZIER) {
                                                    val newVal = if (selectedLayer.polygonEdges == 1) 0 else 1
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    val updatedList = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(polygonEdges = newVal)
                                                        } else it
                                                    }
                                                    onLayersChange(updatedList)
                                                    com.example.studio.ui.ParametricLayerCache.invalidate(selectedLayer.id)
                                                }
                                            },
                                            onToggleNodeType = {
                                                if (selectedLayer != null && selectedLayer.type == LayerType.VECTOR_BEZIER && activeBezierPointIndex != -1) {
                                                    val k = activeBezierPointIndex / 3
                                                    val currentTypes = selectedLayer.bezierNodeTypes.toMutableList()
                                                    while (currentTypes.size <= k) {
                                                        currentTypes.add("SMOOTH")
                                                    }
                                                    val oldType = currentTypes[k]
                                                    val newType = if (oldType == "CORNER") "SMOOTH" else "CORNER"
                                                    currentTypes[k] = newType
                                                    
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    val updatedList = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(bezierNodeTypes = currentTypes)
                                                        } else it
                                                    }
                                                    onLayersChange(updatedList)
                                                    com.example.studio.ui.ParametricLayerCache.invalidate(selectedLayer.id)
                                                }
                                            },
                                            onToggleFillStyle = {
                                                if (selectedLayer != null && selectedLayer.type == LayerType.VECTOR_BEZIER) {
                                                    val newVal = if (selectedLayer.strokeThickness > 0f) -1f else 8f
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    val updatedList = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(strokeThickness = newVal)
                                                        } else it
                                                    }
                                                    onLayersChange(updatedList)
                                                    com.example.studio.ui.ParametricLayerCache.invalidate(selectedLayer.id)
                                                }
                                            },
                                            artboardWidth = artboardWidth,
                                            artboardHeight = artboardHeight,
                                            totalScale = totalScale,
                                            isScrollable = false,
                                            keepBezierSymmetrical = keepBezierSymmetrical,
                                            onKeepBezierSymmetricalChange = onKeepBezierSymmetricalChange,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        val currentSmoothing = if (selectedLayer?.type == LayerType.FREEHAND_DRAWING) {
                                            val valSmooth = (selectedLayer.effects.find { it is com.example.studio.model.StudioEffect.PhotoshopEffect && it.effectType == "BrushConfig" } as? com.example.studio.model.StudioEffect.PhotoshopEffect)?.parameters?.get("Smoothing")?.value ?: 0.0f
                                            valSmooth > 0.5f
                                        } else {
                                            brushSmoothing
                                        }
                                        val currentPreset = if (selectedLayer?.type == LayerType.FREEHAND_DRAWING) {
                                            val config = selectedLayer.effects.find { it is com.example.studio.model.StudioEffect.PhotoshopEffect && it.effectType == "BrushConfig" } as? com.example.studio.model.StudioEffect.PhotoshopEffect
                                            config?.parameters?.get("Preset")?.value?.toInt() ?: brushPresetIndex
                                        } else {
                                            brushPresetIndex
                                        }
                                        val currentSize = if (selectedLayer?.type == LayerType.FREEHAND_DRAWING) {
                                            (selectedLayer.effects.find { it is com.example.studio.model.StudioEffect.PhotoshopEffect && it.effectType == "BrushConfig" } as? com.example.studio.model.StudioEffect.PhotoshopEffect)?.parameters?.get("Size")?.value ?: brushSize
                                        } else {
                                            brushSize
                                        }
                                        val currentOpacity = if (selectedLayer?.type == LayerType.FREEHAND_DRAWING) {
                                            (selectedLayer.effects.find { it is com.example.studio.model.StudioEffect.PhotoshopEffect && it.effectType == "BrushConfig" } as? com.example.studio.model.StudioEffect.PhotoshopEffect)?.parameters?.get("Opacity")?.value ?: brushOpacity
                                        } else {
                                            brushOpacity
                                        }
                                        val updateBrushParamsLocal = { newSize: Float?, newOpacity: Float?, newSmooth: Boolean?, newPreset: Int?, newColor: Color?, newHardness: Float? ->
                                            if (newSize != null) onBrushSizeChange(newSize)
                                            if (newOpacity != null) onBrushOpacityChange(newOpacity)
                                            if (newSmooth != null) onBrushSmoothingChange(newSmooth)
                                            if (newPreset != null) onBrushPresetIndexChange(newPreset)
                                            if (newColor != null) onBrushColorChange(newColor)
                                            if (newHardness != null) onBrushHardnessChange(newHardness)

                                            if (selectedLayer?.type == LayerType.FREEHAND_DRAWING) {
                                                val config = selectedLayer.effects.find { it is com.example.studio.model.StudioEffect.PhotoshopEffect && it.effectType == "BrushConfig" } as? com.example.studio.model.StudioEffect.PhotoshopEffect
                                                if (config != null) {
                                                    var updatedConfig = config
                                                    if (newSize != null) updatedConfig = updatedConfig.updateParameter("Size", newSize) as com.example.studio.model.StudioEffect.PhotoshopEffect
                                                    if (newOpacity != null) updatedConfig = updatedConfig.updateParameter("Opacity", newOpacity) as com.example.studio.model.StudioEffect.PhotoshopEffect
                                                    if (newSmooth != null) updatedConfig = updatedConfig.updateParameter("Smoothing", if (newSmooth) 1.0f else 0.0f) as com.example.studio.model.StudioEffect.PhotoshopEffect
                                                    if (newPreset != null) updatedConfig = updatedConfig.updateParameter("Preset", newPreset.toFloat()) as com.example.studio.model.StudioEffect.PhotoshopEffect
                                                    if (newHardness != null) updatedConfig = updatedConfig.updateParameter("Hardness", newHardness) as com.example.studio.model.StudioEffect.PhotoshopEffect

                                                    val updatedEffects = selectedLayer.effects.map { if (it.id == config.id) updatedConfig else it }
                                                    val updatedList = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(
                                                                baseColor = newColor ?: selectedLayer.baseColor,
                                                                effects = updatedEffects
                                                            )
                                                        } else it
                                                    }
                                                    onLayersChange(updatedList)
                                                } else {
                                                    val updatedList = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(baseColor = newColor ?: selectedLayer.baseColor)
                                                        } else it
                                                    }
                                                    onLayersChange(updatedList)
                                                }
                                            }
                                        }

                                        BrushStudioControlPane(
                                            currentPreset = currentPreset,
                                            currentSize = currentSize,
                                            currentOpacity = currentOpacity,
                                            currentColor = if (selectedLayer?.type == LayerType.FREEHAND_DRAWING) selectedLayer.baseColor else brushColor,
                                            currentSmoothing = currentSmoothing,
                                            updateBrushParams = updateBrushParamsLocal,
                                            onOpenBrushesLibrary = { onShowBrushesLibraryChange(true) },
                                            gridEnabled = gridEnabled,
                                            onGridEnabledChange = onGridEnabledChange,
                                            rulerEnabled = rulers.find { it.id == selectedRulerId }?.enabled ?: false,
                                            onRulerEnabledChange = { value -> onRulersChange(rulers.map { if (it.id == selectedRulerId) it.copy(enabled = value) else it }) },
                                            rulerOrientation = rulerOrientation,
                                            onRulerOrientationChange = { value -> onRulersChange(rulers.map { if (it.id == selectedRulerId) it.copy(orientation = value) else it }) },
                                            rulerPosition = rulerPosition,
                                            onRulerPositionChange = { value -> onRulersChange(rulers.map { if (it.id == selectedRulerId) it.copy(position = value) else it }) },
                                            activeTool = activeTool,
                                            eraserSize = eraserSize,
                                            onEraserSizeChange = onEraserSizeChange,
                                            eraserHardness = eraserHardness,
                                            onEraserHardnessChange = onEraserHardnessChange,
                                            brushHardness = brushHardness,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                "Grid" -> {
                                    GridControlPane(
                                        gridEnabled = gridEnabled,
                                        onGridEnabledChange = onGridEnabledChange,
                                        gridColumns = gridColumns,
                                        onGridColumnsChange = onGridColumnsChange,
                                        gridRows = gridRows,
                                        onGridRowsChange = onGridRowsChange,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                "Ruler" -> {
                                    val settings = WorkspaceRulerSettings(
                                        rulers = rulers,
                                        onRulersChange = onRulersChange,
                                        selectedRulerId = selectedRulerId,
                                        onSelectedRulerIdChange = onSelectedRulerIdChange,
                                        snapToRuler = snapToRuler,
                                        onSnapToRulerChange = onSnapToRulerChange,
                                        allRulersLocked = allRulersLocked,
                                        onAllRulersLockedChange = onAllRulersLockedChange
                                    )
                                    CompositionLocalProvider(LocalRulerSettings provides settings) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MidSlate),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("Guide Indicator Attributes", style = Typography.labelMedium, color = EnergeticYellow, fontWeight = FontWeight.Bold)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                        Text("Magnet: ${if (snapToRuler) "ON" else "OFF"}", style = Typography.labelSmall, color = if (snapToRuler) Color(0xFF00FF66) else TextSecondary)
                                                        Text("Angle Lock: ${if (allRulersLocked) "LOCKED" else "FREE"}", style = Typography.labelSmall, color = if (allRulersLocked) Color.Red else TextSecondary)
                                                    }
                                                }
                                            }
                                            RulerControlPane(
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeValueEditConfigState.value != null) {
            SliderValueEditDialog(
                config = activeValueEditConfigState.value!!,
                onDismiss = { activeValueEditConfigState.value = null }
            )
        }
    }
}
