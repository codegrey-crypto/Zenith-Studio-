package com.example.studio.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onGroupSelected: () -> Unit = {},
    onMergeDown: (String) -> Unit = {},
    collapsedGroupIds: Set<String> = emptySet(),
    onToggleGroupCollapse: (String) -> Unit = {},
    activeClipboard: StudioLayer? = null,
    onCopyLayer: (StudioLayer) -> Unit = {},
    onPasteLayer: () -> Unit = {}
) {
    var expandedBlendList by remember { mutableStateOf(false) }
    val selLayer = layers.find { it.id == selectedLayerId }

    var panelWidth by remember { mutableStateOf(275.dp) }
    var panelHeight by remember { mutableStateOf(530.dp) }
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current

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
                    if (selLayer != null && !isMultiSelectMode) {
                        IconButton(onClick = { onDuplicateLayer(selLayer.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, "Duplicate", tint = TextPrimary, modifier = Modifier.size(14.dp))
                        }
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

            // Layers Scrollable Core Stack (Reversing direction for canvas-compliant top-layer priority)
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(items = displayedLayers, key = { _, item -> item.id }) { index, item ->
                    val isSelected = if (isMultiSelectMode) selectedLayersSet.contains(item.id) else item.id == selectedLayerId
                    var showContextMenu by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isSelected) 4.dp else 0.dp)
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
                                .padding(8.dp),
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

                            // Styled preview thumbnail representing layer color characteristics beautifully
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(item.baseColor.copy(alpha = 0.85f))
                                    .border(0.5.dp, HighslateOutline, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val glyph = when (item.type) {
                                    LayerType.VECTOR_CIRCLE -> "○"
                                    LayerType.VECTOR_RECT -> "□"
                                    LayerType.VECTOR_STAR -> "★"
                                    LayerType.VECTOR_TRIANGLE -> "△"
                                    LayerType.VECTOR_PENTAGON -> "⬠"
                                    LayerType.VECTOR_HEXAGON -> "⬡"
                                    LayerType.VECTOR_OVAL -> "⬭"
                                    LayerType.VECTOR_LINE -> "╱"
                                    LayerType.VECTOR_BEZIER -> "∿"
                                    LayerType.TEXT -> "T"
                                    LayerType.FREEHAND_DRAWING -> "✎"
                                    LayerType.IMAGE_CARD -> "▨"
                                    LayerType.GROUP -> ""
                                }
                                Text(
                                    text = glyph,
                                    color = if (item.baseColor == Color.White) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

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
                                            .size(20.dp)
                                            .testTag("layer_context_menu_button_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Expandable Context Menu",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(12.dp)
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
                                                onDeleteLayer(item.id)
                                                showContextMenu = false
                                            }
                                        )
                                    }
                                }

                                // Reorder buttons
                                IconButton(onClick = { onLayerReorderUp(index) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.ArrowDropUp, "Up", tint = TextPrimary, modifier = Modifier.size(15.dp))
                                }
                                IconButton(onClick = { onLayerReorderDown(index) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.ArrowDropDown, "Down", tint = TextPrimary, modifier = Modifier.size(15.dp))
                                }

                                // Alpha-Lock selector action padlock
                                IconButton(
                                    onClick = { onChangeAlphaLock(item.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isAlphaLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Alpha Lock Toggle",
                                        tint = if (item.isAlphaLocked) EnergeticYellow else TextSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }

                                // Clipping Mask toggle action
                                IconButton(
                                    onClick = { onChangeClippingMask(item.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlipToBack,
                                        contentDescription = "Clipping Mask Toggle",
                                        tint = if (item.isClippingMask) IndustrialAmber else TextSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }

                                // Visibility Toggle eye
                                IconButton(
                                    onClick = { onChangeVisibility(item.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle visibility",
                                        tint = if (item.isVisible) TextPrimary else TextSecondary,
                                        modifier = Modifier.size(11.dp)
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
