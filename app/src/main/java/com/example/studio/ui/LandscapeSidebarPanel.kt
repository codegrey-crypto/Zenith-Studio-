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
    onActiveToolChange: (String) -> Unit = {},
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
    activeGridType: String = "Standard",
    onGridTypeChange: (String) -> Unit = {},
    polarGridCenterX: Float = 0.5f,
    onPolarGridCenterXChange: (Float) -> Unit = {},
    polarGridCenterY: Float = 0.5f,
    onPolarGridCenterYChange: (Float) -> Unit = {},
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
    warpRepeatMode: String = "Off",
    onWarpRepeatModeChange: (String) -> Unit = {},
    warpRepeatX: Float = 1f,
    onWarpRepeatXChange: (Float) -> Unit = {},
    warpRepeatY: Float = 1f,
    onWarpRepeatYChange: (Float) -> Unit = {},
    warpPhaseX: Float = 0f,
    onWarpPhaseXChange: (Float) -> Unit = {},
    warpPhaseY: Float = 0f,
    onWarpPhaseYChange: (Float) -> Unit = {},
    warpInterpolation: Boolean = true,
    onWarpInterpolationChange: (Boolean) -> Unit = {},
    warpTarget: String = "Layer",
    onWarpTargetChange: (String) -> Unit = {},
    onResetWarp: () -> Unit = {},
    warpMeshDivisionX: Int = 3,
    onWarpMeshDivisionXChange: (Int) -> Unit = {},
    warpMeshDivisionY: Int = 3,
    onWarpMeshDivisionYChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isLandscape) return

    val activeValueEditConfigState = remember { mutableStateOf<SliderValueEditConfig?>(null) }
    val effectiveSheet = activeFullScreenSheet ?: when (activeTool) {
        "Brush", "Eraser", "Pen" -> "Brush"
        "Perspective" -> "Perspective"
        "Mesh" -> "Mesh"
        else -> "Color"
    }
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
                            "Grids" to Icons.Default.GridOn,
                            "Ruler" to Icons.Default.Straighten,
                            "Perspective" to Icons.Default.FilterCenterFocus,
                            "Mesh" to Icons.Default.BlurOn
                        )
                        categories.forEach { (cat, defaultIcon) ->
                            val isSelected = effectiveSheet == cat || (cat == "Grids" && effectiveSheet == "Grid")
                            val icon = if (cat == "Shape") {
                                when (selectedLayer?.type) {
                                    com.example.studio.model.LayerType.IMAGE_CARD -> Icons.Default.Image
                                    com.example.studio.model.LayerType.TEXT -> Icons.Default.TextFields
                                    else -> Icons.Default.Category
                                }
                            } else {
                                defaultIcon
                            }
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
                                        onActiveFullScreenSheetChange(if (cat == "Grids") "Grid" else cat)
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
                                            onToggleEffectEnabled = { effectId ->
                                                val updatedList = layers.map { layer ->
                                                    if (layer.id == selectedLayer.id) {
                                                        layer.copy(effects = layer.effects.map { eff ->
                                                            if (eff.id == effectId) {
                                                                eff.toggleEnabled()
                                                            } else eff
                                                        })
                                                    } else layer
                                                }
                                                onLayersChange(updatedList)
                                            },
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
                                            customFonts = customFonts,
                                            canvasWidth = artboardWidth,
                                            canvasHeight = artboardHeight
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
                                                    isLandscapeMode = true
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
                                    if (activeTool == "Pen") {
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
                                            onClose = {
                                                onActiveToolChange("Move")
                                                onActiveBezierPointIndexChange(-1)
                                                onPenCursorOffsetChange(null)
                                            },
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
                                        activeGridType = activeGridType,
                                        onGridTypeChange = onGridTypeChange,
                                        polarGridCenterX = polarGridCenterX,
                                        onPolarGridCenterXChange = onPolarGridCenterXChange,
                                        polarGridCenterY = polarGridCenterY,
                                        onPolarGridCenterYChange = onPolarGridCenterYChange,
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
                                "Perspective" -> {
                                    PerspectiveControlPane(
                                        warpRepeatMode = warpRepeatMode,
                                        onWarpRepeatModeChange = onWarpRepeatModeChange,
                                        warpRepeatX = warpRepeatX,
                                        onWarpRepeatXChange = onWarpRepeatXChange,
                                        warpRepeatY = warpRepeatY,
                                        onWarpRepeatYChange = onWarpRepeatYChange,
                                        warpPhaseX = warpPhaseX,
                                        onWarpPhaseXChange = onWarpPhaseXChange,
                                        warpPhaseY = warpPhaseY,
                                        onWarpPhaseYChange = onWarpPhaseYChange,
                                        warpInterpolation = warpInterpolation,
                                        onWarpInterpolationChange = onWarpInterpolationChange,
                                        warpTarget = warpTarget,
                                        onWarpTargetChange = onWarpTargetChange,
                                        onResetWarp = onResetWarp,
                                        onClose = { onActiveToolChange("Move") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                "Mesh" -> {
                                    MeshControlPane(
                                        warpMeshDivisionX = warpMeshDivisionX,
                                        onWarpMeshDivisionXChange = onWarpMeshDivisionXChange,
                                        warpMeshDivisionY = warpMeshDivisionY,
                                        onWarpMeshDivisionYChange = onWarpMeshDivisionYChange,
                                        warpInterpolation = warpInterpolation,
                                        onWarpInterpolationChange = onWarpInterpolationChange,
                                        warpTarget = warpTarget,
                                        onWarpTargetChange = onWarpTargetChange,
                                        onResetWarp = onResetWarp,
                                        onClose = { onActiveToolChange("Move") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
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

@Composable
fun PerspectiveControlPane(
    warpRepeatMode: String,
    onWarpRepeatModeChange: (String) -> Unit,
    warpRepeatX: Float,
    onWarpRepeatXChange: (Float) -> Unit,
    warpRepeatY: Float,
    onWarpRepeatYChange: (Float) -> Unit,
    warpPhaseX: Float,
    onWarpPhaseXChange: (Float) -> Unit,
    warpPhaseY: Float,
    onWarpPhaseYChange: (Float) -> Unit,
    warpInterpolation: Boolean,
    onWarpInterpolationChange: (Boolean) -> Unit,
    warpTarget: String,
    onWarpTargetChange: (String) -> Unit,
    onResetWarp: () -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PERSPECTIVE FORM",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EnergeticYellow
            )
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close Perspective", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        
        // Target: Current Layer / Canvas
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Target", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Layer", "Canvas").forEach { mode ->
                    val isSel = warpTarget == mode
                    Button(
                        onClick = { onWarpTargetChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) IndustrialAmber else MidSlate,
                            contentColor = if (isSel) DarkOnyx else TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(mode, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Repeat mode options: Off, Inner, Horizon, Full
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Repeat Mode", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Off", "Inner", "Horizon", "Full").forEach { mode ->
                    val isSel = warpRepeatMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) IndustrialAmber else MidSlate)
                            .clickable { onWarpRepeatModeChange(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) DarkOnyx else TextPrimary
                        )
                    }
                }
            }
        }

        // Repeat X slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Repeat X", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(String.format("%d", Math.round(warpRepeatX)), style = MaterialTheme.typography.bodySmall, color = EnergeticYellow)
            }
            Slider(
                value = warpRepeatX,
                onValueChange = { onWarpRepeatXChange(Math.round(it).toFloat()) },
                valueRange = 1f..20f,
                steps = 18,
                colors = SliderDefaults.colors(
                    activeTrackColor = IndustrialAmber,
                    thumbColor = EnergeticYellow
                )
            )
        }

        // Repeat Y slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Repeat Y", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(String.format("%d", Math.round(warpRepeatY)), style = MaterialTheme.typography.bodySmall, color = EnergeticYellow)
            }
            Slider(
                value = warpRepeatY,
                onValueChange = { onWarpRepeatYChange(Math.round(it).toFloat()) },
                valueRange = 1f..20f,
                steps = 18,
                colors = SliderDefaults.colors(
                    activeTrackColor = IndustrialAmber,
                    thumbColor = EnergeticYellow
                )
            )
        }

        // Phase X slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Phase X", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(String.format("%.1f°", warpPhaseX * 360f - 180f), style = MaterialTheme.typography.bodySmall, color = EnergeticYellow)
            }
            Slider(
                value = warpPhaseX,
                onValueChange = onWarpPhaseXChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    activeTrackColor = IndustrialAmber,
                    thumbColor = EnergeticYellow
                )
            )
        }

        // Phase Y slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Phase Y", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(String.format("%.1f°", warpPhaseY * 360f - 180f), style = MaterialTheme.typography.bodySmall, color = EnergeticYellow)
            }
            Slider(
                value = warpPhaseY,
                onValueChange = onWarpPhaseYChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    activeTrackColor = IndustrialAmber,
                    thumbColor = EnergeticYellow
                )
            )
        }

        // Interpolation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Bilinear Interpolation", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = warpInterpolation,
                onCheckedChange = onWarpInterpolationChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EnergeticYellow,
                    checkedTrackColor = IndustrialAmber
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnergeticYellow,
                    contentColor = DarkOnyx
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Done Editing", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onResetWarp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Nodes", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset Nodes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MeshControlPane(
    warpMeshDivisionX: Int,
    onWarpMeshDivisionXChange: (Int) -> Unit,
    warpMeshDivisionY: Int,
    onWarpMeshDivisionYChange: (Int) -> Unit,
    warpInterpolation: Boolean,
    onWarpInterpolationChange: (Boolean) -> Unit,
    warpTarget: String,
    onWarpTargetChange: (String) -> Unit,
    onResetWarp: () -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MESH FORM",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EnergeticYellow
            )
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close Mesh", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // Target: Current Layer / Canvas
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Target", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Layer", "Canvas").forEach { mode ->
                    val isSel = warpTarget == mode
                    Button(
                        onClick = { onWarpTargetChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) IndustrialAmber else MidSlate,
                            contentColor = if (isSel) DarkOnyx else TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(mode, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Division X
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Division X (Cols)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("$warpMeshDivisionX", style = MaterialTheme.typography.bodySmall, color = EnergeticYellow)
            }
            Slider(
                value = warpMeshDivisionX.toFloat(),
                onValueChange = { onWarpMeshDivisionXChange(it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    activeTrackColor = IndustrialAmber,
                    thumbColor = EnergeticYellow
                )
            )
        }

        // Division Y
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Division Y (Rows)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("$warpMeshDivisionY", style = MaterialTheme.typography.bodySmall, color = EnergeticYellow)
            }
            Slider(
                value = warpMeshDivisionY.toFloat(),
                onValueChange = { onWarpMeshDivisionYChange(it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    activeTrackColor = IndustrialAmber,
                    thumbColor = EnergeticYellow
                )
            )
        }

        // Interpolation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Bilinear Interpolation", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Switch(
                checked = warpInterpolation,
                onCheckedChange = onWarpInterpolationChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EnergeticYellow,
                    checkedTrackColor = IndustrialAmber
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnergeticYellow,
                    contentColor = DarkOnyx
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Done Editing", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onResetWarp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Mesh Grid", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset Grid Nodes", fontWeight = FontWeight.Bold)
            }
        }
    }
}
