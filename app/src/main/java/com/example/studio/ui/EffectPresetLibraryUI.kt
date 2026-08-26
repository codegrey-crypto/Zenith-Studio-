package com.example.studio.ui
import com.example.studio.model.FilterConfig
import com.example.studio.model.LayerAdjustmentsConfig
import com.example.studio.model.EffectPreset
import com.aistudio.zenithstudio.rpxwtq.EffectStackManager
import com.example.studio.model.EffectPresetManager
import androidx.compose.material.icons.filled.ArrowBack

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.studio.model.StudioLayer
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.SlatePanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectPresetLibraryDialog(
    layerId: String,
    currentLayer: StudioLayer? = null,
    onClose: () -> Unit,
    onUpdateLayer: ((StudioLayer) -> StudioLayer)? = null
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("effect_preset_library_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = SlatePanel,
            tonalElevation = 8.dp
        ) {
            EffectPresetLibraryContent(
                layerId = layerId,
                currentLayer = currentLayer,
                onClose = onClose,
                onUpdateLayer = onUpdateLayer
            )
        }
    }
}

@Composable
fun EffectPresetLibraryContent(
    layerId: String,
    currentLayer: StudioLayer? = null,
    onClose: (() -> Unit)? = null,
    onUpdateLayer: ((StudioLayer) -> StudioLayer)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSaveDialogOpen by remember { mutableStateOf(false) }
    var isImportDialogOpen by remember { mutableStateOf(false) }

    // Trigger re-read on save/delete changes
    val changeCounter = EffectPresetManager.presetChangeCounter.value
    val allPresets = remember(changeCounter) {
        EffectPresetManager.getAllPresets(context)
    }

    val activeLayerFiltersCount = remember(layerId, EffectStackManager.changeCounter.value) {
        EffectStackManager.getFiltersForLayer(layerId).size
    }

    val filteredPresets = remember(selectedCategory, searchQuery, allPresets) {
        allPresets.filter { preset ->
            val matchesCat = when (selectedCategory) {
                "All" -> true
                "Custom Saved" -> preset.isCustom
                else -> preset.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                preset.name.contains(searchQuery, ignoreCase = true) ||
                        preset.description.contains(searchQuery, ignoreCase = true) ||
                        preset.tags.any { it.contains(searchQuery, ignoreCase = true) }
            }
            matchesCat && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("preset_library_content")
    ) {
        // Top Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = IndustrialAmber)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(IndustrialAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Presets Icon",
                        tint = IndustrialAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Effect Presets Library",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Single-tap layer compositing & FX presets (${allPresets.size} total)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Save Current Stack Button
                Button(
                    onClick = { isSaveDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndustrialAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("save_current_preset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Save Preset Icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Preset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Close Button
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_preset_library_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Presets",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search & Quick Import Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search presets by name, tag, or effect...",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndustrialAmber,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("preset_search_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Import JSON Preset Button
            IconButton(
                onClick = { isImportDialogOpen = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .testTag("import_preset_json_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Import Preset JSON",
                    tint = IndustrialAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Selection Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("preset_category_chips")
        ) {
            items(items = EffectPresetManager.categories, key = { it }) { cat ->
                val isSelected = cat == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndustrialAmber,
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.White.copy(alpha = 0.15f),
                        selectedBorderColor = IndustrialAmber
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Presets List Grid
        if (filteredPresets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "No Presets",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No presets found matching '$searchQuery'",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    if (selectedCategory == "Custom Saved") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Apply effects to a layer, then tap 'Save Preset' to create your custom library!",
                            fontSize = 12.sp,
                            color = IndustrialAmber.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("preset_cards_list")
            ) {
                items(items = filteredPresets, key = { it.id }) { preset ->
                    PresetCardItem(
                        preset = preset,
                        onApply = {
                            EffectPresetManager.applyPresetToLayer(
                                context = context,
                                layerId = layerId,
                                preset = preset,
                                updateLayerCallback = onUpdateLayer
                            )
                        },
                        onDelete = if (preset.isCustom) {
                            { EffectPresetManager.deleteCustomPreset(context, preset.id) }
                        } else null
                    )
                }
            }
        }
    }

    // Modal: Save Current Layer Setup as Preset
    if (isSaveDialogOpen) {
        SavePresetModalDialog(
            layerId = layerId,
            currentLayer = currentLayer,
            onDismiss = { isSaveDialogOpen = false },
            onSaved = { isSaveDialogOpen = false }
        )
    }

    // Modal: Import Preset JSON
    if (isImportDialogOpen) {
        ImportPresetModalDialog(
            onDismiss = { isImportDialogOpen = false },
            onImported = { isImportDialogOpen = false }
        )
    }
}

@Composable
fun PresetCardItem(
    preset: EffectPreset,
    onApply: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (preset.isCustom) IndustrialAmber.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onApply() }
            .testTag("preset_card_${preset.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (preset.isCustom) Color(0xFF22252A) else Color(0xFF1B1D21)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (preset.isCustom) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = IndustrialAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "USER SAVED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndustrialAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preset.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = if (isExpanded) 10 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Single Tap Apply Button
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndustrialAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Apply Preset",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // FX Badge Row & Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${preset.filters.size} FX Filters",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = IndustrialAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = preset.category,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy JSON button
                    IconButton(
                        onClick = {
                            val json = EffectPresetManager.exportPresetJson(preset)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Effect Preset JSON", json)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Copied preset config JSON to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Preset JSON",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Delete button for custom presets
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Custom Preset",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavePresetModalDialog(
    layerId: String,
    currentLayer: StudioLayer? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Custom Saved") }

    val activeFilters = remember(layerId) {
        EffectStackManager.getFiltersForLayer(layerId)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SlatePanel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("save_preset_modal")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Save Effect Stack as Preset",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Capture ${activeFilters.size} active filters and adjustments into a single-tap preset.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Title") },
                    placeholder = { Text("e.g. My Cyberpunk Glow") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndustrialAmber,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_preset_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Heavy bloom with teal tint for nighttime scenes") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndustrialAmber,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_preset_desc_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            EffectPresetManager.saveCustomPreset(
                                context = context,
                                name = name,
                                description = description,
                                category = selectedCategory,
                                layerId = layerId,
                                currentLayer = currentLayer
                            )
                            onSaved()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndustrialAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_save_preset_button")
                    ) {
                        Text("Save Preset", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ImportPresetModalDialog(
    onDismiss: () -> Unit,
    onImported: () -> Unit
) {
    val context = LocalContext.current
    var jsonInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SlatePanel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("import_preset_modal")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Import Preset JSON",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Paste a preset JSON configuration string below:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    placeholder = { Text("{\"id\": \"custom...\", \"name\": \"...\"}") },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndustrialAmber,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val imported = EffectPresetManager.importPresetJson(context, jsonInput)
                            if (imported != null) {
                                onImported()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndustrialAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Import", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
