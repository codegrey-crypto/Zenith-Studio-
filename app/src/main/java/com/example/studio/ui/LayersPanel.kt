package com.example.studio.ui
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.studio.model.LayerType
import com.example.studio.model.StudioLayer
import com.example.studio.model.ZenithBlendMode
import com.example.ui.theme.*
import java.util.UUID
@Composable
fun RightsideLayerDrawer(
    layers: List<StudioLayer>,
    selectedLayerId: String,
    onSelectLayer: (String) -> Unit,
    onChangeVisibility: (String) -> Unit,
    onChangeAlphaLock: (String) -> Unit,
    onChangeClippingMask: (String) -> Unit,
    onLayerReorderUp: (Int) -> Unit,
    onLayerReorderDown: (Int) -> Unit,
    onLayerMove: (Int, Int) -> Unit = { _, _ -> },
    onAddLayer: () -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onBlendModeChange: (String, ZenithBlendMode) -> Unit,
    onOpacityChange: (String, Float) -> Unit,
    onTriggerRename: (String, String) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    onToggleMultiSelect: () -> Unit = {},
    selectedLayersSet: Set<String> = emptySet(),
    onToggleSelectLayerMulti: (String) -> Unit = {},
    onSelectAllLayers: (Boolean) -> Unit = {},
    onGroupSelected: () -> Unit = {},
    onMergeDown: (String) -> Unit = {},
    collapsedGroupIds: Set<String> = emptySet(),
    onToggleGroupCollapse: (String) -> Unit = {},
    activeClipboard: StudioLayer? = null,
    onCopyLayer: (StudioLayer) -> Unit = {},
    onPasteLayer: () -> Unit = {},
    onFlipHorizontal: (String) -> Unit = {},
    onFlipVertical: (String) -> Unit = {},
    onRasterizeLayer: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var expandedBlendList by remember { mutableStateOf(false) }
    val selLayer = layers.find { it.id == selectedLayerId }
    var panelWidth by remember { mutableStateOf(275.dp) }
    var panelHeight by remember { mutableStateOf(530.dp) }
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val totalSlotHeightPx = with(currentDensity) { 62.dp.toPx() }
    val currentLayers by rememberUpdatedState(layers)
    val currentOnLayerMove by rememberUpdatedState(onLayerMove)
    Box(
        modifier = Modifier
            .size(panelWidth, panelHeight)
            .then(modifier)
    ) {
        // Drag handle on the LEFT edge (vertical track)
        Box(
            modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaDp = with(currentDensity) { dragAmount.x.toDp() }
                        panelWidth = (panelWidth - deltaDp).coerceIn(240.dp, 400.dp)
                    }
                }
        )
        // Drag handle on the BOTTOM edge (horizontal track)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaDp = with(currentDensity) { dragAmount.y.toDp() }
                        panelHeight = (panelHeight + deltaDp).coerceIn(300.dp, 850.dp)
                    }
                }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, bottom = 4.dp)
                .background(SlatePanel, RoundedCornerShape(16.dp))
                .border(BorderStroke(1.2.dp, HighslateOutline), RoundedCornerShape(16.dp))
                .padding(12.dp)
                .testTag("layers_panel")
        ) {
            // Drawer title & Quick adding actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Layers, "Layers list icon", tint = IndustrialAmber, modifier = Modifier.size(16.dp))
                    Text("Layers (${layers.size})", style = MaterialTheme.typography.titleLarge, fontSize = 13.sp, color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Paste Layer Into Stack Action
                    val isPasteEnabled = activeClipboard != null
                    Button(
                        onClick = onPasteLayer,
                        enabled = isPasteEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPasteEnabled) EnergeticYellow else SlatePanel,
                            contentColor = if (isPasteEnabled) DarkOnyx else TextSecondary,
                            disabledContainerColor = SlatePanel,
                            disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = if (isPasteEnabled) null else BorderStroke(1.dp, HighslateOutline),
                        modifier = Modifier
                            .height(24.dp)
                            .testTag("paste_layer_into_stack_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste Layer Into Stack",
                                tint = if (isPasteEnabled) DarkOnyx else TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text("Paste", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Multi Select Mode Toggle Button
                    IconButton(onClick = onToggleMultiSelect, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isMultiSelectMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Multi Select Mode",
                            tint = if (isMultiSelectMode) IndustrialAmber else TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    // Select All / Deselect All Button
                    if (isMultiSelectMode) {
                        val allSelected = selectedLayersSet.size == layers.size
                        IconButton(
                            onClick = { onSelectAllLayers(!allSelected) },
                            modifier = Modifier.size(24.dp).testTag("select_all_layers_button")
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.LibraryAddCheck else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect All Layers" else "Select All Layers",
                                tint = if (allSelected) IndustrialAmber else TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    // Group Selected Layers Action
                    if (isMultiSelectMode && selectedLayersSet.size >= 2) {
                        IconButton(onClick = onGroupSelected, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.CreateNewFolder, "Group Selected", tint = EnergeticYellow, modifier = Modifier.size(14.dp))
                        }
                    }
                    // Merge Down Action
                    if (!isMultiSelectMode && selLayer != null) {
                        val activeIndex = layers.indexOfFirst { it.id == selLayer.id }
                        if (activeIndex != -1 && activeIndex < layers.size - 1) { // has layer beneath
                            IconButton(onClick = { onMergeDown(selLayer.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.VerticalAlignBottom, "Merge Down", tint = EnergeticYellow, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    IconButton(onClick = onAddLayer, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, "Add Layer", tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }
                    if (selLayer != null && !isMultiSelectMode && !(selLayer.type == LayerType.FREEHAND_DRAWING && selLayer.brushPoints.isEmpty())) {
                        IconButton(onClick = { onDeleteLayer(selLayer.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                        }
                    }
                    IconButton(onClick = onCloseDrawer, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ChevronRight, "Collapse Drawer", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            // Expanded Layer Settings: Opacity and Blending modes
            if (selLayer != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidSlate, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                        .padding(bottom = 4.dp)
                ) {
                    // Opacity slide bar
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Opacity", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("${(selLayer.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = EnergeticYellow)
                    }
                    Slider(
                        value = selLayer.opacity,
                        onValueChange = { onOpacityChange(selLayer.id, it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = IndustrialAmber,
                            thumbColor = IndustrialAmber
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                    // Blend Modes Dropdown
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expandedBlendList = true },
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SlatePanel),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Blend: ${selLayer.blendMode.displayName}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                Icon(Icons.Default.ArrowDropDown, "Expand", tint = TextPrimary, modifier = Modifier.size(12.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = expandedBlendList,
                            onDismissRequest = { expandedBlendList = false },
                            modifier = Modifier.background(MidSlate)
                        ) {
                            ZenithBlendMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.displayName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onBlendModeChange(selLayer.id, mode)
                                        expandedBlendList = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Alpha Lock & Clipping Mask quick toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Alpha Lock Toggle Row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(SlatePanel, RoundedCornerShape(4.dp))
                                .clickable { onChangeAlphaLock(selLayer.id) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (selLayer.isAlphaLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Alpha Lock Toggle",
                                    tint = if (selLayer.isAlphaLocked) EnergeticYellow else TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text("Alpha Lock", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextPrimary)
                            }
                            androidx.compose.material3.Switch(
                                checked = selLayer.isAlphaLocked,
                                onCheckedChange = { onChangeAlphaLock(selLayer.id) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = IndustrialAmber,
                                    checkedTrackColor = IndustrialAmber.copy(alpha = 0.4f),
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SlatePanel
                                ),
                                modifier = Modifier.scale(0.6f).height(16.dp)
                            )
                        }
                        // Clipping Mask Toggle Row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(SlatePanel, RoundedCornerShape(4.dp))
                                .clickable { onChangeClippingMask(selLayer.id) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.FlipToBack,
                                    contentDescription = "Clipping Mask Toggle",
                                    tint = if (selLayer.isClippingMask) IndustrialAmber else TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text("Clip Mask", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextPrimary)
                            }
                            androidx.compose.material3.Switch(
                                checked = selLayer.isClippingMask,
                                onCheckedChange = { onChangeClippingMask(selLayer.id) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = IndustrialAmber,
                                    checkedTrackColor = IndustrialAmber.copy(alpha = 0.4f),
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SlatePanel
                                ),
                                modifier = Modifier.scale(0.6f).height(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Filter out layers whose parent group is collapsed
            val displayedLayers = layers.filter { item ->
                val pId = item.parentGroupId
                if (pId != null) {
                    !collapsedGroupIds.contains(pId)
                } else {
                    true
                }
            }
            val currentDisplayedLayers by rememberUpdatedState(displayedLayers)
            // Layers Scrollable Core Stack (Reversing direction for canvas-compliant top-layer priority)
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(items = displayedLayers, key = { _, item -> item.id }) { index, item ->
                    val isSelected = if (isMultiSelectMode) selectedLayersSet.contains(item.id) else item.id == selectedLayerId
                    var showContextMenu by remember { mutableStateOf(false) }
                    val isDraggingThis = draggedItemId == item.id
                    val itemOffsetY = if (isDraggingThis) dragOffsetY else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isSelected) 4.dp else 0.dp)
                            .zIndex(if (isDraggingThis) 10f else 1f)
                            .graphicsLayer {
                                translationY = itemOffsetY
                            }
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val isEmptyLayer = item.type == LayerType.FREEHAND_DRAWING && item.brushPoints.isEmpty()
                                        if (!isEmptyLayer) {
                                            draggedItemId = item.id
                                            dragOffsetY = 0f
                                        }
                                    },
                                    onDrag = onDragLabel@ { change, dragAmount ->
                                        if (draggedItemId != item.id) return@onDragLabel
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        
                                        val activeList = currentDisplayedLayers
                                        val masterList = currentLayers
                                        val currentIndex = activeList.indexOfFirst { it.id == item.id }
                                        if (currentIndex != -1) {
                                            val threshold = totalSlotHeightPx * 0.5f
                                            if (dragOffsetY > threshold) {
                                                if (currentIndex < activeList.size - 1) {
                                                    val nextItem = activeList[currentIndex + 1]
                                                    val masterCurr = masterList.indexOfFirst { it.id == item.id }
                                                    val masterNext = masterList.indexOfFirst { it.id == nextItem.id }
                                                    if (masterCurr != -1 && masterNext != -1) {
                                                        currentOnLayerMove(masterCurr, masterNext)
                                                        dragOffsetY -= totalSlotHeightPx
                                                    }
                                                }
                                            } else if (dragOffsetY < -threshold) {
                                                if (currentIndex > 0) {
                                                    val prevItem = activeList[currentIndex - 1]
                                                    val masterCurr = masterList.indexOfFirst { it.id == item.id }
                                                    val masterPrev = masterList.indexOfFirst { it.id == prevItem.id }
                                                    if (masterCurr != -1 && masterPrev != -1) {
                                                        currentOnLayerMove(masterCurr, masterPrev)
                                                        dragOffsetY += totalSlotHeightPx
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItemId = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedItemId = null
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                    ) {
                        if (isSelected) {
                            // Offset stacked background shadow card for physical stacking visual elevation!
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 3.dp, y = 3.dp)
                                    .background(DarkOnyx, RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, HighslateOutline.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(start = if (item.parentGroupId != null && item.isClippingMask) 24.dp else if (item.parentGroupId != null) 12.dp else if (item.isClippingMask) 14.dp else 0.dp)
                                .offset(x = if (isSelected) (-3).dp else 0.dp, y = if (isSelected) (-3).dp else 0.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) IndustrialAmber.copy(0.24f) else MidSlate)
                                .border(BorderStroke(1.2.dp, if (isSelected) IndustrialAmber else HighslateOutline), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isMultiSelectMode) {
                                        onToggleSelectLayerMulti(item.id)
                                    } else {
                                        onSelectLayer(item.id)
                                    }
                                }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox for Multi-Select mode
                            if (isMultiSelectMode) {
                                val isMultiChecked = selectedLayersSet.contains(item.id)
                                IconButton(
                                    onClick = { onToggleSelectLayerMulti(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMultiChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = "Select status",
                                        tint = if (isMultiChecked) IndustrialAmber else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            // Group collapse/expand caret button
                            if (item.type == LayerType.GROUP) {
                                val isCollapsed = collapsedGroupIds.contains(item.id)
                                IconButton(
                                    onClick = { onToggleGroupCollapse(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCollapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Collapse",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (item.isClippingMask) {
                                Text(
                                    text = "↳",
                                    color = IndustrialAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            // Mini layer icon effect thumbnail displaying exact layer content & effects
                            LayerMiniIconThumbnail(item)
                            Spacer(Modifier.width(8.dp))
                            // Title info & Opacity
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${(item.opacity * 100).toInt()}% Opacity",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = TextSecondary
                                    )
                                    if (item.isAlphaLocked) {
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "[Alpha Locked]",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            color = EnergeticYellow
                                        )
                                    }
                                }
                            }
                            // Interactive utilities directly inside the Layer Item
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                // Dropdown Context Menu Trigger for Copy/Paste/Duplicate/Delete
                                Box {
                                    IconButton(
                                        onClick = { showContextMenu = true },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .testTag("layer_context_menu_button_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Expandable Context Menu",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showContextMenu,
                                        onDismissRequest = { showContextMenu = false },
                                        modifier = Modifier.background(MidSlate)
                                    ) {
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy Icon",
                                                    tint = IndustrialAmber,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = "Copy Layer Line Asset",
                                                    color = TextPrimary,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                onCopyLayer(item)
                                                showContextMenu = false
                                            },
                                            modifier = Modifier.testTag("copy_layer_menu_item_${item.id}")
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Extension,
                                                    contentDescription = "Save to Elements",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = "Save to Elements",
                                                    color = TextPrimary,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                coroutineScope.launch {
                                                    com.example.studio.model.ElementsManager.saveElement(context, item.copy(id = java.util.UUID.randomUUID().toString()))
                                                    android.widget.Toast.makeText(context, "Saved to Elements", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                showContextMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Rename Icon",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = "Rename Layer",
                                                    color = TextPrimary,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                onTriggerRename(item.id, item.name)
                                                showContextMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Duplicate Icon",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = "Duplicate Layer",
                                                    color = TextPrimary,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                onDuplicateLayer(item.id)
                                                showContextMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.SwapHoriz,
                                                    contentDescription = "Flip Horizontal Icon",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                 )
                                             },
                                             text = {
                                                 Text(
                                                     text = "Flip Horizontally",
                                                     color = TextPrimary,
                                                     style = MaterialTheme.typography.bodyMedium
                                                 )
                                             },
                                             onClick = {
                                                 onFlipHorizontal(item.id)
                                                 showContextMenu = false
                                             }
                                         )
                                         DropdownMenuItem(
                                             leadingIcon = {
                                                 Icon(
                                                     imageVector = Icons.Default.SwapVert,
                                                     contentDescription = "Flip Vertical Icon",
                                                     tint = TextSecondary,
                                                     modifier = Modifier.size(16.dp)
                                                 )
                                             },
                                             text = {
                                                 Text(
                                                     text = "Flip Vertically",
                                                     color = TextPrimary,
                                                     style = MaterialTheme.typography.bodyMedium
                                                 )
                                             },
                                             onClick = {
                                                 onFlipVertical(item.id)
                                                 showContextMenu = false
                                             }
                                         )
                                         DropdownMenuItem(
                                             leadingIcon = {
                                                 Icon(
                                                     imageVector = Icons.Default.Layers,
                                                     contentDescription = "Rasterize Icon",
                                                     tint = TextSecondary,
                                                     modifier = Modifier.size(16.dp)
                                                 )
                                             },
                                             text = {
                                                 Text(
                                                     text = "Rasterize Layer",
                                                     color = TextPrimary,
                                                     style = MaterialTheme.typography.bodyMedium
                                                 )
                                             },
                                             onClick = {
                                                 onRasterizeLayer(item.id)
                                                 showContextMenu = false
                                             }
                                         )
                                         DropdownMenuItem(
                                             enabled = !(item.type == LayerType.FREEHAND_DRAWING && item.brushPoints.isEmpty()),
                                             leadingIcon = {
                                                 Icon(
                                                     imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Icon",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = "Delete Layer",
                                                    color = Color.Red,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                val isItemEmptyLayer = item.type == LayerType.FREEHAND_DRAWING && item.brushPoints.isEmpty()
                                                if (!isItemEmptyLayer) {
                                                    onDeleteLayer(item.id)
                                                }
                                                showContextMenu = false
                                            }
                                        )
                                    }
                                }
                                // Visibility Toggle eye
                                IconButton(
                                    onClick = { onChangeVisibility(item.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle visibility",
                                        tint = if (item.isVisible) TextPrimary else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                // Drag Handle icon for touch reorder
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder layer",
                                    tint = if (isDraggingThis) IndustrialAmber else TextSecondary,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(4.dp)
                                        .pointerInput(item.id) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    val isEmptyLayer = item.type == LayerType.FREEHAND_DRAWING && item.brushPoints.isEmpty()
                                                    if (!isEmptyLayer) {
                                                        draggedItemId = item.id
                                                        dragOffsetY = 0f
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    if (draggedItemId != item.id) return@detectDragGestures
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    
                                                    val activeList = currentDisplayedLayers
                                                    val masterList = currentLayers
                                                    val currentIndex = activeList.indexOfFirst { it.id == item.id }
                                                    if (currentIndex != -1) {
                                                        val threshold = totalSlotHeightPx * 0.5f
                                                        if (dragOffsetY > threshold) {
                                                            if (currentIndex < activeList.size - 1) {
                                                                val nextItem = activeList[currentIndex + 1]
                                                                val masterCurr = masterList.indexOfFirst { it.id == item.id }
                                                                val masterNext = masterList.indexOfFirst { it.id == nextItem.id }
                                                                if (masterCurr != -1 && masterNext != -1) {
                                                                    currentOnLayerMove(masterCurr, masterNext)
                                                                    dragOffsetY -= totalSlotHeightPx
                                                                }
                                                            }
                                                        } else if (dragOffsetY < -threshold) {
                                                            if (currentIndex > 0) {
                                                                val prevItem = activeList[currentIndex - 1]
                                                                val masterCurr = masterList.indexOfFirst { it.id == item.id }
                                                                val masterPrev = masterList.indexOfFirst { it.id == prevItem.id }
                                                                if (masterCurr != -1 && masterPrev != -1) {
                                                                    currentOnLayerMove(masterCurr, masterPrev)
                                                                    dragOffsetY += totalSlotHeightPx
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedItemId = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggedItemId = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun LayerMiniIconThumbnail(
    layer: StudioLayer,
    modifier: Modifier = Modifier
) {
    // Check if layer has active SolidColor / ColorOverlay effect
    val activeSolidEffect = layer.effects.find { 
        it.isEnabled && it is com.example.studio.model.StudioEffect.PhotoshopEffect && 
        (it.effectType == "SolidColor" || it.effectType == "ColorOverlay") 
    } as? com.example.studio.model.StudioEffect.PhotoshopEffect
    val effectiveColor = if (activeSolidEffect != null) {
        val r = activeSolidEffect.parameters["Red"]?.value ?: activeSolidEffect.parameters["ColorRed"]?.value ?: 0f
        val g = activeSolidEffect.parameters["Green"]?.value ?: activeSolidEffect.parameters["ColorGreen"]?.value ?: 0.9f
        val b = activeSolidEffect.parameters["Blue"]?.value ?: activeSolidEffect.parameters["ColorBlue"]?.value ?: 1.0f
        val op = activeSolidEffect.parameters["Opacity"]?.value ?: 1.0f
        Color(r, g, b, op)
    } else {
        layer.baseColor
    }
    val displayColor = if (effectiveColor == Color.Transparent || effectiveColor.alpha < 0.05f) {
        Color.White
    } else {
        effectiveColor
    }
    val hasActiveFx = layer.effects.any { it.isEnabled }
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1B1E24))
            .border(0.8.dp, if (hasActiveFx) IndustrialAmber else HighslateOutline, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            // Render checkerboard pattern for transparent/alpha representation
            val checkSize = 4f
            for (cx in 0 until (w / checkSize).toInt()) {
                for (cy in 0 until (h / checkSize).toInt()) {
                    if ((cx + cy) % 2 == 0) {
                        drawRect(
                            color = Color(0xFF2A2E37),
                            topLeft = androidx.compose.ui.geometry.Offset(cx * checkSize, cy * checkSize),
                            size = androidx.compose.ui.geometry.Size(checkSize, checkSize)
                        )
                    }
                }
            }
            // Draw miniature shape or stroke content based on layer type
            when (layer.type) {
                LayerType.VECTOR_CIRCLE, LayerType.VECTOR_OVAL, LayerType.VECTOR_RING -> {
                    drawCircle(color = displayColor, radius = w * 0.38f)
                }
                LayerType.VECTOR_RECT, LayerType.VECTOR_ROUNDED_RECT -> {
                    drawRect(color = displayColor, topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.15f), size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.7f))
                }
                LayerType.VECTOR_STAR -> {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.5f, h * 0.1f)
                        lineTo(w * 0.62f, h * 0.38f)
                        lineTo(w * 0.9f, h * 0.38f)
                        lineTo(w * 0.68f, h * 0.58f)
                        lineTo(w * 0.78f, h * 0.88f)
                        lineTo(w * 0.5f, h * 0.7f)
                        lineTo(w * 0.22f, h * 0.88f)
                        lineTo(w * 0.32f, h * 0.58f)
                        lineTo(w * 0.1f, h * 0.38f)
                        lineTo(w * 0.38f, h * 0.38f)
                        close()
                    }
                    drawPath(path, displayColor)
                }
                LayerType.VECTOR_TRIANGLE, LayerType.VECTOR_ROUNDED_TRIANGLE -> {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.5f, h * 0.15f)
                        lineTo(w * 0.85f, h * 0.85f)
                        lineTo(w * 0.15f, h * 0.85f)
                        close()
                    }
                    drawPath(path, displayColor)
                }
                LayerType.FREEHAND_DRAWING -> {
                    if (layer.brushPoints.isNotEmpty()) {
                        val path = androidx.compose.ui.graphics.Path()
                        val pts = layer.brushPoints
                        if (pts.isNotEmpty()) {
                            val minX = pts.minOf { it.x }
                            val maxX = pts.maxOf { it.x }.coerceAtLeast(minX + 1f)
                            val minY = pts.minOf { it.y }
                            val maxY = pts.maxOf { it.y }.coerceAtLeast(minY + 1f)
                            
                            val scaleX = (w * 0.7f) / (maxX - minX)
                            val scaleY = (h * 0.7f) / (maxY - minY)
                            val s = minOf(scaleX, scaleY)
                            
                            path.moveTo(w * 0.15f + (pts[0].x - minX) * s, h * 0.15f + (pts[0].y - minY) * s)
                            for (i in 1 until pts.size step 2) {
                                path.lineTo(w * 0.15f + (pts[i].x - minX) * s, h * 0.15f + (pts[i].y - minY) * s)
                            }
                        }
                        drawPath(
                            path = path,
                            color = displayColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    } else {
                        drawLine(color = displayColor, start = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.8f), end = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.2f), strokeWidth = 3f)
                    }
                }
                LayerType.IMAGE_CARD -> {
                    drawRect(color = displayColor.copy(alpha = 0.5f), topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.1f), size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f))
                    drawCircle(color = displayColor, center = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.35f), radius = w * 0.12f)
                }
                else -> {
                    drawCircle(color = displayColor, radius = w * 0.32f)
                }
            }
        }
        if (hasActiveFx) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(8.dp)
                    .background(IndustrialAmber, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
