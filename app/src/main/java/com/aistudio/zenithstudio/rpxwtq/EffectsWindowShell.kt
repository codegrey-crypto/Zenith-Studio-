package com.aistudio.zenithstudio.rpxwtq

import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.OptIn
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown

// Dynamic theme-aware color properties matching the active theme
val DarkSlateBg: Color @Composable get() = com.example.ui.theme.DarkOnyx
val MediumSlateCard: Color @Composable get() = com.example.ui.theme.SlatePanel
val HighslateBorders: Color @Composable get() = com.example.ui.theme.HighslateOutline
val BrightAccentYellow: Color @Composable get() = com.example.ui.theme.IndustrialAmber
val DarkAccentRed = Color(0xFFCC3333)
val LightTextPrimary: Color @Composable get() = com.example.ui.theme.TextPrimary
val LightTextSecondary: Color @Composable get() = com.example.ui.theme.TextSecondary

@Composable
fun EffectsWindowShell(
    layerId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscapeMode: Boolean = false,
    baseBitmap: android.graphics.Bitmap? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            com.example.studio.ui.GradientEyedropperState.isActive = false
            com.example.studio.ui.GradientEyedropperState.onColorSampled = null
            com.example.studio.ui.GradientEyedropperState.liveSampledColor = null
        }
    }

    // 1. Separate State Managers
    var isSearchLibraryOpen by remember { mutableStateOf(false) }
    var isPresetLibraryOpen by remember { mutableStateOf(false) }
    var favoritesState by remember { mutableStateOf(EffectPreferences.getFavoriteEffectIds(context)) }
    var recentsState by remember { mutableStateOf(EffectPreferences.getRecentEffectIds(context)) }
    
    val allFilters = remember { ZenithFilterFactory.getAllFilters() }
    val activeFilters = EffectStackManager.getFiltersForLayer(layerId)
    
    // Auto-expand last added / selected active filter parameters
    var expandedFilterId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeFilters.size) {
        if (activeFilters.isNotEmpty()) {
            expandedFilterId = activeFilters.last().id
        }
    }

    if (isLandscapeMode) {
        Column(
            modifier = modifier
                .testTag("effects_window_shell")
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .consumeAllTouches()
        ) {
            // Header row: Info, Add Button (NO close panel button needed because it is in a collapsible sidebar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zenith Effects",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightAccentYellow,
                        modifier = Modifier.testTag("effects_stack_header")
                    )
                    Text(
                        text = "${activeFilters.size} effects applied",
                        fontSize = 11.sp,
                        color = LightTextSecondary
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isPresetLibraryOpen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = BrightAccentYellow
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("open_preset_library_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Presets icon",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { isSearchLibraryOpen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrightAccentYellow,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("add_effect_trigger_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add effect button icon",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Effect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = HighslateBorders, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Column of active effects in the stack for landscape mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeFilters.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "No FX Added",
                                    color = LightTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Tap 'Add Effect' to explore filters.",
                                    color = LightTextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        QuickAccessEffectsBlock(
                            layerId = layerId,
                            favoritesState = favoritesState,
                            recentsState = recentsState,
                            allFilters = allFilters,
                            onFilterAdded = { addedId ->
                                expandedFilterId = addedId
                                val updatedRecents = EffectPreferences.getRecentEffectIds(context)
                                recentsState = updatedRecents
                            }
                        )
                    }
                } else {
                    activeFilters.forEachIndexed { index, activeFilter ->
                        val isExpanded = activeFilter.id == expandedFilterId
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("active_filter_card_${activeFilter.id}"),
                            color = MediumSlateCard,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isExpanded) BrightAccentYellow else HighslateBorders
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                // Header row of this filter
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedFilterId = if (isExpanded) null else activeFilter.id
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Checkbox(
                                            checked = activeFilter.isEnabled,
                                            onCheckedChange = { 
                                                EffectStackManager.toggleFilter(layerId, activeFilter.id)
                                            },
                                            modifier = Modifier
                                                .testTag("toggle_${activeFilter.id}")
                                                .size(24.dp),
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = BrightAccentYellow,
                                                uncheckedColor = LightTextSecondary
                                            )
                                        )
                                        Text(
                                            text = "#$index ${activeFilter.name}",
                                            color = LightTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { EffectStackManager.duplicateFilter(layerId, activeFilter.id) },
                                            modifier = Modifier
                                                .testTag("duplicate_${activeFilter.id}")
                                                .size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Duplicate filter",
                                                tint = BrightAccentYellow,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { 
                                                EffectStackManager.removeFilter(layerId, activeFilter.id)
                                            },
                                            modifier = Modifier
                                                .testTag("delete_${activeFilter.id}")
                                                .size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove filter",
                                                tint = DarkAccentRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = HighslateBorders, thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (activeFilter.parameters.isEmpty()) {
                                        Text(
                                            text = "Static filter (no parameters to edit)",
                                            color = LightTextSecondary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        ZenithFilterParametersEditor(
                                            activeFilter = activeFilter,
                                            onParamChange = { id, paramName, newValue ->
                                                EffectStackManager.updateParameter(layerId, id, paramName, newValue)
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
    } else {
        var isMaximized by remember { mutableStateOf(false) }

        Box(
            modifier = modifier
                .testTag("effects_window_shell")
                .fillMaxSize()
        ) {
        
        // ==========================================
        // COMPONENT 1: ACTIVE STACK & PARAMETER LIST (Bottom-aligned panel, slightly higher!)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = DarkSlateBg,
            border = BorderStroke(1.5.dp, HighslateBorders),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (isMaximized) 0.85f else 0.48f) // Aligned to same height as other bottom panels or maximized
                .align(Alignment.BottomCenter)
                .consumeAllTouches()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header row of bottom panel: Info, Add Button, Close panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Effects Stack",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightAccentYellow,
                            modifier = Modifier.testTag("effects_stack_header")
                        )
                        Text(
                            text = "${activeFilters.size} effects applied",
                            fontSize = 11.sp,
                            color = LightTextSecondary
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Maximize/minimize button
                        IconButton(
                            onClick = { isMaximized = !isMaximized },
                            modifier = Modifier
                                .testTag("toggle_maximize_button")
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isMaximized) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isMaximized) "Minimize panel" else "Maximize panel",
                                tint = BrightAccentYellow,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Presets Library button
                        Button(
                            onClick = { isPresetLibraryOpen = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = BrightAccentYellow
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("open_preset_library_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Presets icon",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // The requested "Add Effect" small button
                        Button(
                            onClick = { isSearchLibraryOpen = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrightAccentYellow,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("add_effect_trigger_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add effect button icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Effect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Close whole overlay button
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .testTag("close_effects_shell_button")
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close overlay",
                                tint = LightTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = HighslateBorders, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Column of active effects in the stack for portrait mode
                if (activeFilters.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "No FX Added",
                                    color = LightTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Tap 'Add Effect' to explore filters in the FX library.",
                                    color = LightTextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        QuickAccessEffectsBlock(
                            layerId = layerId,
                            favoritesState = favoritesState,
                            recentsState = recentsState,
                            allFilters = allFilters,
                            onFilterAdded = { addedId ->
                                expandedFilterId = addedId
                                val updatedRecents = EffectPreferences.getRecentEffectIds(context)
                                recentsState = updatedRecents
                            }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeFilters.forEachIndexed { index, activeFilter ->
                            val isExpanded = activeFilter.id == expandedFilterId
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("active_filter_card_${activeFilter.id}"),
                                color = MediumSlateCard,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isExpanded) BrightAccentYellow else HighslateBorders
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    // Header row of this filter
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedFilterId = if (isExpanded) null else activeFilter.id
                                            }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Checkbox(
                                                checked = activeFilter.isEnabled,
                                                onCheckedChange = { 
                                                    EffectStackManager.toggleFilter(layerId, activeFilter.id)
                                                },
                                                modifier = Modifier
                                                    .testTag("toggle_${activeFilter.id}")
                                                    .size(24.dp),
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = BrightAccentYellow,
                                                    checkmarkColor = Color.Black
                                                )
                                            )
                                            
                                            Text(
                                                text = "#$index ${activeFilter.name}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (activeFilter.isEnabled) LightTextPrimary else LightTextSecondary
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { EffectStackManager.duplicateFilter(layerId, activeFilter.id) },
                                                modifier = Modifier
                                                    .testTag("duplicate_${activeFilter.id}")
                                                    .size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Duplicate effect",
                                                    tint = BrightAccentYellow,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { EffectStackManager.removeFilter(layerId, activeFilter.id) },
                                                modifier = Modifier
                                                    .testTag("delete_${activeFilter.id}")
                                                    .size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove effect",
                                                    tint = DarkAccentRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Display parameters inline when expanded / active
                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp)
                                        ) {
                                            if (activeFilter.parameters.isEmpty()) {
                                                Text(
                                                    text = "Static filter (no parameters to edit)",
                                                    color = LightTextSecondary,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            } else {
                                                ZenithFilterParametersEditor(
                                                    activeFilter = activeFilter,
                                                    onParamChange = { id, paramName, newValue ->
                                                        EffectStackManager.updateParameter(layerId, id, paramName, newValue)
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
            }
        }
    }

        // ==========================================
        // COMPONENT 2: THE FULL SCREEN SEARCH FX LIBRARY WINDOW
        // Completely covering the screen (Full Screen Window dialog)!
        // ==========================================
        if (isSearchLibraryOpen) {
            Dialog(
                onDismissRequest = { isSearchLibraryOpen = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                // A true full screen surface matching top edges as well!
                Surface(
                    color = DarkSlateBg,
                    modifier = Modifier
                        .fillMaxSize() // STRICT REQUIREMENT: "properly full screen, not half screen or anything"
                        .testTag("effects_search_window_dialog")
                ) {
                    var librarySearchQuery by remember { mutableStateOf("") }
                    val categories = listOf(
                        "All",
                        "Favorites",
                        "Recent",
                        "Color Adjust",
                        "Blur",
                        "Distort",
                        "Artistic",
                        "Stylize",
                        "Layer Styles",
                        "Special Effects",
                        "Advanced & AI"
                    )
                    var activeCategoryTab by remember { mutableStateOf("All") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding() 
                            .padding(20.dp)
                    ) {
                        // Top Header/Nav Bar of the Full Screen View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "FX Library Browser",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrightAccentYellow
                                )
                                Text(
                                    text = "Select any filter below to apply it to your stack",
                                    fontSize = 12.sp,
                                    color = LightTextSecondary
                                )
                            }
                            
                            IconButton(
                                onClick = { isSearchLibraryOpen = false },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(HighslateBorders, RoundedCornerShape(22.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close library search",
                                    tint = LightTextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = HighslateBorders, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Search Input
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.2.dp, HighslateBorders, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = BrightAccentYellow,
                                modifier = Modifier.size(22.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Box(modifier = Modifier.weight(1f)) {
                                if (librarySearchQuery.isEmpty()) {
                                    Text(
                                        text = "Search filters (e.g. Blur, Gaussian, Pinch...)",
                                        color = LightTextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                                BasicTextField(
                                    value = librarySearchQuery,
                                    onValueChange = { librarySearchQuery = it },
                                    textStyle = TextStyle(
                                        color = LightTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("effects_search_input")
                                )
                            }

                            if (librarySearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { librarySearchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search query",
                                        tint = LightTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category tabs scrollable horizontal list
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = category == activeCategoryTab
                                Surface(
                                    modifier = Modifier
                                        .testTag("tab_pill_${category.replace(" ", "_").replace("&", "And")}")
                                        .clickable { activeCategoryTab = category },
                                    color = if (isSelected) BrightAccentYellow else MediumSlateCard.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (isSelected) BrightAccentYellow else HighslateBorders)
                                ) {
                                    Text(
                                        text = category,
                                        color = if (isSelected) DarkSlateBg else LightTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Filtered Nodes Grid/List
                        val matchingFilters = remember(librarySearchQuery, activeCategoryTab, favoritesState, recentsState) {
                            derivedStateOf {
                                val baseList = when (activeCategoryTab) {
                                    "Favorites" -> allFilters.filter { favoritesState.contains(it.id) }
                                    "Recent" -> {
                                        recentsState.mapNotNull { id -> allFilters.find { it.id == id } }
                                    }
                                    else -> allFilters
                                }
                                baseList.filter { filter ->
                                    val matchesQuery = if (librarySearchQuery.isEmpty()) true else {
                                        filter.name.contains(librarySearchQuery, ignoreCase = true) ||
                                        filter.category.contains(librarySearchQuery, ignoreCase = true)
                                    }
                                    val matchesCategory = if (activeCategoryTab == "All" || activeCategoryTab == "Favorites" || activeCategoryTab == "Recent") true else {
                                        val fCat = filter.category
                                        val fId = filter.id
                                        val fName = filter.name
                                        when (activeCategoryTab) {
                                            "Color Adjust" -> {
                                                fCat == "Color Adjustments"
                                            }
                                            "Blur" -> {
                                                fCat == "Blur & Blur Gallery" || 
                                                fName.contains("Blur", ignoreCase = true) || 
                                                fCat.contains("Blur", ignoreCase = true) || 
                                                fId.contains("blur", ignoreCase = true)
                                            }
                                            "Distort" -> {
                                                fCat == "Distort" || 
                                                listOf("Pinch", "Ripple", "Spherize", "Twirl", "Wave", "ZigZag", "PixelStretch", "Liquify")
                                                    .any { fId.contains(it, ignoreCase = true) || fName.contains(it, ignoreCase = true) }
                                            }
                                            "Artistic" -> {
                                                fCat == "Artistic Effects" || 
                                                fCat == "Artistic" || 
                                                fCat == "Brush Strokes" || 
                                                fCat == "Sketch & Texture" ||
                                                listOf("OilPaint", "Solarize", "Wind")
                                                    .any { fId.contains(it, ignoreCase = true) || fName.contains(it, ignoreCase = true) }
                                            }
                                            "Stylize" -> {
                                                fCat == "Stylize" || 
                                                fCat == "Halftone Effects" || 
                                                fCat == "Pixelate" ||
                                                listOf("ColorHalftone", "Crystallize", "Mosaic", "Pointillize", "FindEdges", "Emboss")
                                                    .any { fId.contains(it, ignoreCase = true) || fName.contains(it, ignoreCase = true) }
                                            }
                                            "Layer Styles" -> {
                                                fCat == "Layer Styles (fx)"
                                            }
                                            "Special Effects" -> {
                                                fCat == "Light Effects" || 
                                                fCat == "Noise & Render" || 
                                                fCat == "Pattern Maker" ||
                                                listOf("AddNoise", "Despeckle", "DustScratches", "Median", "Clouds", "LensFlare", "LightingEffects")
                                                    .any { fId.contains(it, ignoreCase = true) || fName.contains(it, ignoreCase = true) }
                                            }
                                            "Advanced & AI" -> {
                                                fCat == "Advanced & AI" || 
                                                listOf("NeuralFilters", "CameraRaw")
                                                    .any { fId.contains(it, ignoreCase = true) || fName.contains(it, ignoreCase = true) }
                                            }
                                            else -> false
                                        }
                                    }
                                    matchesQuery && matchesCategory
                                }
                            }
                        }

                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 250.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (matchingFilters.value.isEmpty()) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (activeCategoryTab == "Favorites") "No favorite effects yet. Tap the heart icon next to any effect to save it here!" else "No match found for options in library",
                                            color = LightTextSecondary,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(
                                    count = matchingFilters.value.size,
                                    key = { idx -> matchingFilters.value[idx].id }
                                ) { idx ->
                                    val filter = matchingFilters.value[idx]
                                    val isAdded = activeFilters.any { it.id == filter.id }
                                    FilterItemCard(
                                        filter = filter,
                                        baseBitmap = baseBitmap,
                                        isAdded = isAdded,
                                        favoritesState = favoritesState,
                                        onFavoriteToggle = {
                                            val updatedFavs = EffectPreferences.toggleFavoriteEffect(context, filter.id)
                                            favoritesState = updatedFavs
                                        },
                                        onClick = {
                                            EffectStackManager.addFilter(layerId, filter)
                                            expandedFilterId = filter.id
                                            val updatedRecents = EffectPreferences.addRecentEffect(context, filter.id)
                                            recentsState = updatedRecents
                                            isSearchLibraryOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isPresetLibraryOpen) {
            com.example.studio.ui.EffectPresetLibraryDialog(
                layerId = layerId,
                onClose = { isPresetLibraryOpen = false }
            )
        }
    }
}

// Global Touch-Friendly Slider Wrapper for EffectsWindowShell
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    
    LaunchedEffect(isDragged) {
        com.aistudio.zenithstudio.rpxwtq.EffectStackManager.isDraggingSlider = isDragged
    }

    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(36.dp), // Visually small and compact, yet comfortable touch height
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource
    )
}

/**
 * High-performance bi-directional precision text-and-slider modifier engine
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiDirectionalSlider(
    filterId: String,
    param: FilterParameter,
    onParamChange: (String, String, Float) -> Unit
) {
    // Local transient states to avoid heavy intermediate recomposition locks
    var typedString by remember(param.currentValue) { mutableStateOf("%.2f".format(param.currentValue)) }
    var actualDragVal by remember(param.currentValue) { mutableStateOf(param.currentValue) }
    var lastThrottledTime by remember { mutableStateOf(0L) }
    
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = param.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = LightTextPrimary
            )
            
            // Interactive BasicTextField override inputs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                var isFocused by remember { mutableStateOf(false) }
                BasicTextField(
                    value = typedString,
                    onValueChange = { inputString ->
                        typedString = inputString
                    },
                    textStyle = TextStyle(
                        color = BrightAccentYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        fontFamily = FontFamily.Monospace
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val numericVal = typedString.toFloatOrNull()
                            if (numericVal != null) {
                                val clamped = numericVal.coerceIn(param.minValue, param.maxValue)
                                actualDragVal = clamped
                                onParamChange(filterId, param.name, clamped)
                            }
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .testTag("input_${param.name}")
                        .width(60.dp)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (!focusState.isFocused) {
                                val numericVal = typedString.toFloatOrNull()
                                if (numericVal != null) {
                                    val clamped = numericVal.coerceIn(param.minValue, param.maxValue)
                                    actualDragVal = clamped
                                    onParamChange(filterId, param.name, clamped)
                                }
                            }
                        }
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .border(1.dp, HighslateBorders, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                
                if (param.unit.isNotEmpty()) {
                    Text(
                        text = param.unit,
                        fontSize = 10.sp,
                        color = LightTextSecondary,
                        modifier = Modifier.width(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "%.0f".format(param.minValue),
                color = LightTextSecondary,
                fontSize = 8.sp
            )
            
            Slider(
                value = actualDragVal,
                valueRange = param.minValue..param.maxValue,
                onValueChange = { newVal ->
                    com.aistudio.zenithstudio.rpxwtq.EffectStackManager.isDraggingSlider = true
                    val precision = (newVal * 100f).roundToInt() / 100f
                    actualDragVal = precision
                    typedString = "%.2f".format(precision)
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastThrottledTime > 30L) { // ~33fps update rate during active drag
                        lastThrottledTime = currentTime
                        onParamChange(filterId, param.name, precision)
                    }
                },
                onValueChangeFinished = {
                    com.aistudio.zenithstudio.rpxwtq.EffectStackManager.isDraggingSlider = false
                    onParamChange(filterId, param.name, actualDragVal)
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = BrightAccentYellow,
                    thumbColor = BrightAccentYellow,
                    inactiveTrackColor = HighslateBorders
                ),
                modifier = Modifier
                    .testTag("slider_${param.name}")
                    .weight(1f)
            )
            
            Text(
                text = "%.0f".format(param.maxValue),
                color = LightTextSecondary,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
fun FilterBlendModeSelector(
    currentVal: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val blendModes = listOf(
        "Normal" to 0f,
        "Multiply" to 1f,
        "Screen" to 2f,
        "Add" to 3f,
        "Overlay" to 4f,
        "Lighten" to 5f,
        "Darken" to 6f
    )
    var expanded by remember { mutableStateOf(false) }
    val currentModeName = blendModes.firstOrNull { kotlin.math.abs(it.second - currentVal) < 0.1f }?.first ?: "Screen"

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            color = Color(0xFF1E1E24),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(currentModeName, fontSize = 12.sp, color = Color.White)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand blend mode",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2E2E38))
        ) {
            blendModes.forEach { (name, value) ->
                DropdownMenuItem(
                    text = { Text(name, color = Color.White, fontSize = 12.sp) },
                    onClick = {
                        onValueChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FilterDropdownSelector(
    label: String,
    currentVal: Float,
    options: List<Pair<String, Float>>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentOptionName = options.firstOrNull { kotlin.math.abs(it.second - currentVal) < 0.1f }?.first ?: options.firstOrNull()?.first ?: ""

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = LightTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { expanded = true },
                color = Color(0xFF1E1E24),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentOptionName, fontSize = 12.sp, color = Color.White)
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Expand",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF2E2E38))
            ) {
                options.forEach { (name, value) ->
                    DropdownMenuItem(
                        text = { Text(name, color = Color.White, fontSize = 12.sp) },
                        onClick = {
                            onValueChange(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ZenithFilterParametersEditor(
    activeFilter: ZenithFilter,
    onParamChange: (String, String, Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        activeFilter.parameters.forEach { param ->
            val isInnerGlowColorParam = (activeFilter.id == "ibis_inner_glow" || activeFilter.id == "image_toolbox_inner_glow") && 
                (param.name in listOf("Red", "Green", "Blue", "Color_R", "Color_G", "Color_B"))
            val isSatinColorParam = (activeFilter.id == "ibis_satin" || activeFilter.id == "image_toolbox_satin") &&
                (param.name in listOf("Red", "Green", "Blue"))
            val isGridsColorParam = activeFilter.id == "ibis_grids" &&
                (param.name in listOf("ColorRed", "ColorGreen", "ColorBlue", "ColorAlpha"))
            val isReplaceColorParam = activeFilter.id == "color_replace_color" &&
                (param.name in listOf("SourceRed", "SourceGreen", "SourceBlue", "TargetRed", "TargetGreen", "TargetBlue"))
            val isSolidColorParam = (activeFilter.name.contains("Solid Color", ignoreCase = true) || 
                activeFilter.name.contains("Color Overlay", ignoreCase = true) ||
                activeFilter.id.contains("solidcolor", ignoreCase = true) ||
                activeFilter.id.contains("coloroverlay", ignoreCase = true)) &&
                (param.name in listOf("Red", "Green", "Blue", "Color Red", "Color Green", "Color Blue", "ColorRed", "ColorGreen", "ColorBlue"))
            val isChromaKeyColorParam = (activeFilter.id.contains("chromakey", ignoreCase = true) ||
                activeFilter.name.contains("Chroma Key", ignoreCase = true)) &&
                (param.name in listOf("KeyRed", "KeyGreen", "KeyBlue", "Key Red", "Key Green", "Key Blue"))
            
            val isShapeTypeParam = param.name == "Shape Type"
            val isBackgroundStyleParam = param.name == "Background Style"
            val isImageTriggerParam = param.name == "ImageTrigger"
            val isGlassStyleParam = param.name == "Glass Style"
            val isDashedParam = param.name == "Dashed"
            val isGrayScaleParam = param.name == "Gray Scale"
            val isColorBlendParam = param.name == "Color Blend"
            val isPolarIntensityParam = activeFilter.id == "distort_polar" && param.name == "Intensity"
            val isOctavesParam = param.name == "Octaves"
            val isPassesParam = param.name == "Passes"
            
            val isDropdownParam = isShapeTypeParam || isBackgroundStyleParam || isImageTriggerParam || 
                    isGlassStyleParam || isDashedParam || isGrayScaleParam || isColorBlendParam || 
                    isPolarIntensityParam || isOctavesParam || isPassesParam
            
            if (!isInnerGlowColorParam && !isReplaceColorParam && !isSatinColorParam && !isGridsColorParam && !isSolidColorParam && !isChromaKeyColorParam && !isDropdownParam) {
                if (param.name == "BlendMode") {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            text = "Blend Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = LightTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FilterBlendModeSelector(
                            currentVal = param.currentValue,
                            onValueChange = { newValue ->
                                onParamChange(activeFilter.id, param.name, newValue)
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    BiDirectionalSlider(
                        filterId = activeFilter.id,
                        param = param,
                        onParamChange = onParamChange
                    )
                }
            } else if (isShapeTypeParam) {
                val options = listOf(
                    "Circle" to 0f,
                    "Square" to 1f,
                    "Diamond" to 2f,
                    "Cross" to 3f
                )
                FilterDropdownSelector(
                    label = "Shape Type",
                    currentVal = param.currentValue,
                    options = options,
                    onValueChange = { newValue ->
                        onParamChange(activeFilter.id, param.name, newValue)
                    }
                )
            } else if (isBackgroundStyleParam) {
                val options = if (param.minValue == 0f) {
                    listOf(
                        "Transparent" to 0f,
                        "White" to 1f,
                        "Black" to 2f,
                        "Original Image" to 3f
                    )
                } else {
                    listOf(
                        "White" to 1f,
                        "Black" to 2f,
                        "Original Image" to 3f
                    )
                }
                FilterDropdownSelector(
                    label = "Background Style",
                    currentVal = param.currentValue,
                    options = options,
                    onValueChange = { newValue ->
                        onParamChange(activeFilter.id, param.name, newValue)
                    }
                )
            } else if (isGlassStyleParam) {
                val options = listOf(
                    "Ribbed / Linear" to 0f,
                    "Hexagonal / Voronoi" to 1f,
                    "Wavy / Sinusoidal" to 2f,
                    "Triangular / Facets" to 3f,
                    "Frosted Glass" to 4f,
                    "Glass Bricks" to 5f
                )
                FilterDropdownSelector(
                    label = "Glass Style",
                    currentVal = param.currentValue,
                    options = options,
                    onValueChange = { newValue ->
                        onParamChange(activeFilter.id, param.name, newValue)
                    }
                )
            } else if (isDashedParam) {
                val options = listOf(
                    "Solid Line" to 0f,
                    "Dashed Line" to 1f
                )
                FilterDropdownSelector(
                    label = "Line Type (Dashed)",
                    currentVal = param.currentValue,
                    options = options,
                    onValueChange = { newValue ->
                        onParamChange(activeFilter.id, param.name, newValue)
                    }
                )
            } else if (isGrayScaleParam) {
                val options = listOf(
                    "Color Emboss" to 0f,
                    "Grayscale Emboss" to 1f
                )
                FilterDropdownSelector(
                    label = "Color Mode",
                    currentVal = param.currentValue,
                    options = options,
                    onValueChange = { newValue ->
                        onParamChange(activeFilter.id, param.name, newValue)
                    }
                )
            } else if (isColorBlendParam) {
                val options = listOf(
                    "B&W Halftone" to 0f,
                    "Color Halftone" to 1f
                )
                FilterDropdownSelector(
                    label = "Halftone Mode",
                    currentVal = param.currentValue,
                    options = options,
                    onValueChange = { newValue ->
                        onParamChange(activeFilter.id, param.name, newValue)
                    }
                )
            } else if (isPolarIntensityParam) {
                val options = listOf(
                    "Rectangular to Polar" to 0f,
                    "Polar to Rectangular" to 1f
                )
                FilterDropdownSelector(
                    label = "Conversion Mode",
                    currentVal = param.currentValue,
                    options = options,
                    onValueChange = { newValue ->
                        onParamChange(activeFilter.id, param.name, newValue)
                    }
                )
            } else if (isOctavesParam) {
                val options = listOf(
                    "1 Octave" to 1f,
                    "2 Octaves" to 2f,
                    "3 Octaves" to 3f,
                    "4 Octaves" to 4f,
                    "5 Octaves" to 5f,
                    "6 Octaves" to 6f,
                    "7 Octaves" to 7f,
                    "8 Octaves" to 8f
                )
                FilterDropdownSelector(
                    label = "Fractal Octaves",
                    currentVal = param.currentValue,
                    options = options,
                    onValueChange = { newValue ->
                        onParamChange(activeFilter.id, param.name, newValue)
                    }
                )
            } else if (isPassesParam) {
                val options = listOf(
                    "1 Pass" to 1f,
                    "2 Passes" to 2f,
                    "3 Passes" to 3f,
                    "4 Passes" to 4f,
                    "5 Passes" to 5f,
                    "6 Passes" to 6f,
                    "7 Passes" to 7f,
                    "8 Passes" to 8f
                )
                FilterDropdownSelector(
                    label = "Denoise Passes",
                    currentVal = param.currentValue,
                    options = options,
                    onValueChange = { newValue ->
                        onParamChange(activeFilter.id, param.name, newValue)
                    }
                )
            }
        }
        
        if (activeFilter.id == "ibis_inner_glow" || activeFilter.id == "image_toolbox_inner_glow") {
            val rParam = activeFilter.parameters.find { it.name == "Red" || it.name == "Color_R" }
            val gParam = activeFilter.parameters.find { it.name == "Green" || it.name == "Color_G" }
            val bParam = activeFilter.parameters.find { it.name == "Blue" || it.name == "Color_B" }
            val opacityParam = activeFilter.parameters.find { it.name == "Opacity" }
            
            if (rParam != null && gParam != null && bParam != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Glow Color Wheel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightAccentYellow)
                Spacer(modifier = Modifier.height(6.dp))
                com.example.studio.ui.EffectColorPicker(
                    red = rParam.currentValue,
                    green = gParam.currentValue,
                    blue = bParam.currentValue,
                    opacity = opacityParam?.currentValue ?: 1.0f,
                    onColorChanged = { newCol ->
                        onParamChange(activeFilter.id, rParam.name, newCol.red)
                        onParamChange(activeFilter.id, gParam.name, newCol.green)
                        onParamChange(activeFilter.id, bParam.name, newCol.blue)
                    }
                )
            }
        }

        if (activeFilter.id == "ibis_satin" || activeFilter.id == "image_toolbox_satin") {
            val rParam = activeFilter.parameters.find { it.name == "Red" }
            val gParam = activeFilter.parameters.find { it.name == "Green" }
            val bParam = activeFilter.parameters.find { it.name == "Blue" }
            val opacityParam = activeFilter.parameters.find { it.name == "Opacity" }
            
            if (rParam != null && gParam != null && bParam != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Satin Color Wheel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightAccentYellow)
                Spacer(modifier = Modifier.height(6.dp))
                com.example.studio.ui.EffectColorPicker(
                    red = rParam.currentValue,
                    green = gParam.currentValue,
                    blue = bParam.currentValue,
                    opacity = opacityParam?.currentValue ?: 1.0f,
                    onColorChanged = { newCol ->
                        onParamChange(activeFilter.id, rParam.name, newCol.red)
                        onParamChange(activeFilter.id, gParam.name, newCol.green)
                        onParamChange(activeFilter.id, bParam.name, newCol.blue)
                    }
                )
            }
        }

        if (activeFilter.id == "ibis_grids") {
            val rParam = activeFilter.parameters.find { it.name == "ColorRed" }
            val gParam = activeFilter.parameters.find { it.name == "ColorGreen" }
            val bParam = activeFilter.parameters.find { it.name == "ColorBlue" }
            val opacityParam = activeFilter.parameters.find { it.name == "ColorAlpha" }
            
            if (rParam != null && gParam != null && bParam != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Grid Color Wheel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightAccentYellow)
                Spacer(modifier = Modifier.height(6.dp))
                com.example.studio.ui.EffectColorPicker(
                    red = rParam.currentValue,
                    green = gParam.currentValue,
                    blue = bParam.currentValue,
                    opacity = opacityParam?.currentValue ?: 1.0f,
                    onColorChanged = { newCol ->
                        onParamChange(activeFilter.id, rParam.name, newCol.red)
                        onParamChange(activeFilter.id, gParam.name, newCol.green)
                        onParamChange(activeFilter.id, bParam.name, newCol.blue)
                        onParamChange(activeFilter.id, opacityParam?.name ?: "ColorAlpha", newCol.alpha)
                    }
                )
            }
        }

        if (activeFilter.id == "color_replace_color") {
            val sRParam = activeFilter.parameters.find { it.name == "SourceRed" }
            val sGParam = activeFilter.parameters.find { it.name == "SourceGreen" }
            val sBParam = activeFilter.parameters.find { it.name == "SourceBlue" }
            
            val tRParam = activeFilter.parameters.find { it.name == "TargetRed" }
            val tGParam = activeFilter.parameters.find { it.name == "TargetGreen" }
            val tBParam = activeFilter.parameters.find { it.name == "TargetBlue" }
            
            if (sRParam != null && sGParam != null && sBParam != null &&
                tRParam != null && tGParam != null && tBParam != null) {
                
                val isEyedropperActive = com.example.studio.ui.GradientEyedropperState.isActive &&
                    com.example.studio.ui.GradientEyedropperState.onColorSampled != null
                    
                val sourceColor = if (isEyedropperActive) {
                    com.example.studio.ui.GradientEyedropperState.liveSampledColor ?: Color(sRParam.currentValue, sGParam.currentValue, sBParam.currentValue)
                } else {
                    Color(sRParam.currentValue, sGParam.currentValue, sBParam.currentValue)
                }
                
                val targetColor = Color(tRParam.currentValue, tGParam.currentValue, tBParam.currentValue)
                
                var showSourceColorPicker by remember { mutableStateOf(false) }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Section 1: Color to Replace (Source Color)
                Text(
                    text = "Color to Replace",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Preview Color Card
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(sourceColor, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable { showSourceColorPicker = !showSourceColorPicker }
                    )
                    
                    // Eyedropper Button
                        
                    Button(
                        onClick = {
                            if (isEyedropperActive) {
                                com.example.studio.ui.GradientEyedropperState.isActive = false
                                com.example.studio.ui.GradientEyedropperState.onColorSampled = null
                            } else {
                                com.example.studio.ui.GradientEyedropperState.isSamplingForSolidColor = false
                                com.example.studio.ui.GradientEyedropperState.onColorSampled = { sampledColor ->
                                    onParamChange(activeFilter.id, sRParam.name, sampledColor.red)
                                    onParamChange(activeFilter.id, sGParam.name, sampledColor.green)
                                    onParamChange(activeFilter.id, sBParam.name, sampledColor.blue)
                                }
                                com.example.studio.ui.GradientEyedropperState.isActive = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEyedropperActive) BrightAccentYellow else Color(0xFF1E1E24)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Eyedropper",
                                tint = if (isEyedropperActive) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isEyedropperActive) "Tap on Image..." else "Use Eyedropper",
                                color = if (isEyedropperActive) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                if (showSourceColorPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Refine Source Color", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightAccentYellow)
                    Spacer(modifier = Modifier.height(6.dp))
                    com.example.studio.ui.EffectColorPicker(
                        red = sRParam.currentValue,
                        green = sGParam.currentValue,
                        blue = sBParam.currentValue,
                        opacity = 1.0f,
                        onColorChanged = { newCol ->
                            onParamChange(activeFilter.id, sRParam.name, newCol.red)
                            onParamChange(activeFilter.id, sGParam.name, newCol.green)
                            onParamChange(activeFilter.id, sBParam.name, newCol.blue)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Section 2: Replacement Color (Target Color)
                Text(
                    text = "New Replacement Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(targetColor, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    )
                    Column {
                        val hexCode = String.format("#%02X%02X%02X", (targetColor.red * 255f).toInt(), (targetColor.green * 255f).toInt(), (targetColor.blue * 255f).toInt())
                        Text(text = "Target Hex: $hexCode", fontSize = 12.sp, color = LightTextSecondary)
                        Text(text = "RGB: (${(targetColor.red * 255f).roundToInt()}, ${(targetColor.green * 255f).roundToInt()}, ${(targetColor.blue * 255f).roundToInt()})", fontSize = 11.sp, color = LightTextSecondary.copy(alpha = 0.7f))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text("Replacement Color Wheel & Sliders", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightAccentYellow)
                Spacer(modifier = Modifier.height(6.dp))
                com.example.studio.ui.EffectColorPicker(
                    red = tRParam.currentValue,
                    green = tGParam.currentValue,
                    blue = tBParam.currentValue,
                    opacity = 1.0f,
                    onColorChanged = { newCol ->
                        onParamChange(activeFilter.id, tRParam.name, newCol.red)
                        onParamChange(activeFilter.id, tGParam.name, newCol.green)
                        onParamChange(activeFilter.id, tBParam.name, newCol.blue)
                    }
                )
            }
        }

        val isSolidColor = activeFilter.name.contains("Solid Color", ignoreCase = true) || 
            activeFilter.name.contains("Color Overlay", ignoreCase = true) ||
            activeFilter.id.contains("solidcolor", ignoreCase = true) ||
            activeFilter.id.contains("coloroverlay", ignoreCase = true)

        if (isSolidColor) {
            val rParam = activeFilter.parameters.find { it.name == "Red" || it.name == "Color Red" || it.name == "ColorRed" }
            val gParam = activeFilter.parameters.find { it.name == "Green" || it.name == "Color Green" || it.name == "ColorGreen" }
            val bParam = activeFilter.parameters.find { it.name == "Blue" || it.name == "Color Blue" || it.name == "ColorBlue" }
            val opacityParam = activeFilter.parameters.find { it.name == "Opacity" || it.name == "Alpha" || it.name == "ColorAlpha" }

            if (rParam != null && gParam != null && bParam != null) {
                val activeColor = Color(rParam.currentValue, gParam.currentValue, bParam.currentValue, opacityParam?.currentValue ?: 1.0f)

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Coloring Palette", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightAccentYellow)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(activeColor)
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                val colorSwatches = listOf(
                    Color(0xFF00E5FF), // Cyan
                    Color(0xFF00FF66), // Emerald
                    Color(0xFFFFCC00), // Amber
                    Color(0xFFFF3366), // Pink
                    Color(0xFFFF1744), // Crimson
                    Color(0xFF7C4DFF), // Purple
                    Color(0xFF2979FF), // Electric Blue
                    Color(0xFF00E676), // Green
                    Color(0xFFFF9100), // Orange
                    Color(0xFFFFFFFF), // White
                    Color(0xFF000000)  // Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    colorSwatches.forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .border(
                                    width = if (Math.abs(color.red - rParam.currentValue) < 0.05f && Math.abs(color.green - gParam.currentValue) < 0.05f && Math.abs(color.blue - bParam.currentValue) < 0.05f) 2.dp else 0.5.dp,
                                    color = if (Math.abs(color.red - rParam.currentValue) < 0.05f && Math.abs(color.green - gParam.currentValue) < 0.05f && Math.abs(color.blue - bParam.currentValue) < 0.05f) BrightAccentYellow else HighslateBorders,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    onParamChange(activeFilter.id, rParam.name, color.red)
                                    onParamChange(activeFilter.id, gParam.name, color.green)
                                    onParamChange(activeFilter.id, bParam.name, color.blue)
                                }
                        )
                    }
                }

                com.example.studio.ui.EffectColorPicker(
                    red = rParam.currentValue,
                    green = gParam.currentValue,
                    blue = bParam.currentValue,
                    opacity = opacityParam?.currentValue ?: 1.0f,
                    onColorChanged = { newCol ->
                        onParamChange(activeFilter.id, rParam.name, newCol.red)
                        onParamChange(activeFilter.id, gParam.name, newCol.green)
                        onParamChange(activeFilter.id, bParam.name, newCol.blue)
                        if (opacityParam != null) {
                            onParamChange(activeFilter.id, opacityParam.name, newCol.alpha)
                        }
                    }
                )
            }
        }

        val isChromaKeyFilter = activeFilter.id.contains("chromakey", ignoreCase = true) ||
            activeFilter.name.contains("Chroma Key", ignoreCase = true) ||
            activeFilter.parameters.any { it.name in listOf("KeyRed", "KeyGreen", "KeyBlue", "Key Red", "Key Green", "Key Blue") }

        if (isChromaKeyFilter) {
            val kRParam = activeFilter.parameters.find { it.name == "KeyRed" || it.name == "Key Red" }
            val kGParam = activeFilter.parameters.find { it.name == "KeyGreen" || it.name == "Key Green" }
            val kBParam = activeFilter.parameters.find { it.name == "KeyBlue" || it.name == "Key Blue" }

            if (kRParam != null && kGParam != null && kBParam != null) {
                val isEyedropperActive = com.example.studio.ui.GradientEyedropperState.isActive &&
                    com.example.studio.ui.GradientEyedropperState.onColorSampled != null

                val keyColor = if (isEyedropperActive) {
                    com.example.studio.ui.GradientEyedropperState.liveSampledColor ?: Color(kRParam.currentValue, kGParam.currentValue, kBParam.currentValue)
                } else {
                    Color(kRParam.currentValue, kGParam.currentValue, kBParam.currentValue)
                }

                var showColorWheelPicker by remember { mutableStateOf(false) }

                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = "Key Color (Screen Color)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Selected Key Color Swatch Card
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(keyColor, RoundedCornerShape(8.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { showColorWheelPicker = !showColorWheelPicker }
                    )

                    // Eyedropper Button
                    Button(
                        onClick = {
                            if (isEyedropperActive) {
                                com.example.studio.ui.GradientEyedropperState.isActive = false
                                com.example.studio.ui.GradientEyedropperState.onColorSampled = null
                            } else {
                                com.example.studio.ui.GradientEyedropperState.isSamplingForSolidColor = false
                                com.example.studio.ui.GradientEyedropperState.onColorSampled = { sampledColor ->
                                    onParamChange(activeFilter.id, kRParam.name, sampledColor.red)
                                    onParamChange(activeFilter.id, kGParam.name, sampledColor.green)
                                    onParamChange(activeFilter.id, kBParam.name, sampledColor.blue)
                                }
                                com.example.studio.ui.GradientEyedropperState.isActive = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEyedropperActive) BrightAccentYellow else Color(0xFF1E1E24)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Eyedropper Color Picker",
                                tint = if (isEyedropperActive) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isEyedropperActive) "Tap Image to Pick..." else "Use Eyedropper",
                                color = if (isEyedropperActive) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Color Wheel toggle button
                    IconButton(
                        onClick = { showColorWheelPicker = !showColorWheelPicker },
                        modifier = Modifier
                            .size(44.dp)
                            .background(if (showColorWheelPicker) Color(0xFF00E5FF) else Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Color Wheel Picker",
                            tint = if (showColorWheelPicker) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Preset Swatches (Green, Blue, Red, Magenta, Cyan, White, Black)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val keyPresets = listOf(
                        "Green" to Color(0xFF00FF00),
                        "Blue" to Color(0xFF0000FF),
                        "Red" to Color(0xFFFF0000),
                        "Magenta" to Color(0xFFFF00FF),
                        "Cyan" to Color(0xFF00FFFF),
                        "White" to Color(0xFFFFFFFF),
                        "Black" to Color(0xFF000000)
                    )
                    keyPresets.forEach { (_, presetCol) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(presetCol)
                                .border(
                                    width = if (Math.abs(presetCol.red - kRParam.currentValue) < 0.05f &&
                                        Math.abs(presetCol.green - kGParam.currentValue) < 0.05f &&
                                        Math.abs(presetCol.blue - kBParam.currentValue) < 0.05f) 2.dp else 0.5.dp,
                                    color = BrightAccentYellow,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    onParamChange(activeFilter.id, kRParam.name, presetCol.red)
                                    onParamChange(activeFilter.id, kGParam.name, presetCol.green)
                                    onParamChange(activeFilter.id, kBParam.name, presetCol.blue)
                                }
                        )
                    }
                }

                if (showColorWheelPicker) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Refine Key Color Wheel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrightAccentYellow)
                    Spacer(modifier = Modifier.height(6.dp))
                    com.example.studio.ui.EffectColorPicker(
                        red = kRParam.currentValue,
                        green = kGParam.currentValue,
                        blue = kBParam.currentValue,
                        opacity = 1.0f,
                        onColorChanged = { newCol ->
                            onParamChange(activeFilter.id, kRParam.name, newCol.red)
                            onParamChange(activeFilter.id, kGParam.name, newCol.green)
                            onParamChange(activeFilter.id, kBParam.name, newCol.blue)
                        }
                    )
                }
            }
        }
        
        if (activeFilter.id.startsWith("pattern_maker")) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = rememberCoroutineScope()
            
            // Get currently active pattern image path
            val patternImgPath = EffectStackManager.patternImagesForFilters[activeFilter.id]
            
            val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        try {
                            val importedDir = java.io.File(context.filesDir, "pattern_images").apply {
                                if (!exists()) mkdirs()
                            }
                            val uniqueName = "pattern_${activeFilter.id}_${System.currentTimeMillis()}.png"
                            val tempFile = java.io.File(importedDir, uniqueName)
                            
                            // Copy to local file
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            if (tempFile.exists() && tempFile.length() > 0) {
                                // Clear cache to force reload
                                EffectStackManager.decodedTexturesCache.remove(tempFile.absolutePath)
                                
                                // Update patternImagesForFilters
                                EffectStackManager.patternImagesForFilters[activeFilter.id] = tempFile.absolutePath
                                
                                // Trigger parameter change to force redraw/update
                                val triggerValue = (System.currentTimeMillis() % 100000000).toFloat()
                                onParamChange(activeFilter.id, "ImageTrigger", triggerValue)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PatternMaker", "Failed to upload custom pattern image", e)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pattern Custom Image",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrightAccentYellow
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Upload any image from your gallery to repeat as a tiled pattern on your canvas layer.",
                fontSize = 11.sp,
                color = LightTextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1B22), RoundedCornerShape(10.dp))
                    .border(1.dp, HighslateBorders, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image Preview
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (patternImgPath != null) {
                        val bitmap = remember(patternImgPath) {
                            try {
                                android.graphics.BitmapFactory.decodeFile(patternImgPath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Pattern Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = LightTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = LightTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Default",
                                fontSize = 9.sp,
                                color = LightTextSecondary
                            )
                        }
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (patternImgPath != null) "Custom Image Loaded" else "Using Default Layer Image",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (patternImgPath != null) "The uploaded image is tiled as a pattern." else "Upload an image to tile it, or keep it default to tile the layer's original image.",
                        fontSize = 10.sp,
                        color = LightTextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrightAccentYellow,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("Upload Image", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                if (patternImgPath != null) {
                    OutlinedButton(
                        onClick = {
                            EffectStackManager.patternImagesForFilters.remove(activeFilter.id)
                            val triggerValue = (System.currentTimeMillis() % 100000000).toFloat()
                            onParamChange(activeFilter.id, "ImageTrigger", triggerValue)
                        },
                        border = BorderStroke(1.dp, Color(0xFFFF4D4D)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF4D4D)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessEffectsBlock(
    layerId: String,
    favoritesState: Set<String>,
    recentsState: List<String>,
    allFilters: List<ZenithFilter>,
    onFilterAdded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val favoriteFilters = remember(favoritesState, allFilters) {
        allFilters.filter { favoritesState.contains(it.id) }
    }
    val recentFilters = remember(recentsState, allFilters) {
        recentsState.mapNotNull { id -> allFilters.find { it.id == id } }
    }

    if (favoriteFilters.isNotEmpty() || recentFilters.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (favoriteFilters.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF4D4D),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "QUICK FAVORITES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4D4D)
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(items = favoriteFilters, key = { filter -> filter.id }) { filter ->
                            Surface(
                                onClick = {
                                    EffectStackManager.addFilter(layerId, filter)
                                    onFilterAdded(filter.id)
                                    EffectPreferences.addRecentEffect(context, filter.id)
                                },
                                color = Color(0xFF1B1B22),
                                border = BorderStroke(0.5.dp, HighslateBorders),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(filter.name, fontSize = 11.sp, color = Color.White)
                                    Icon(Icons.Default.Add, null, tint = BrightAccentYellow, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (recentFilters.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = BrightAccentYellow,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "RECENTLY USED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightAccentYellow
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(items = recentFilters, key = { filter -> filter.id }) { filter ->
                            Surface(
                                onClick = {
                                    EffectStackManager.addFilter(layerId, filter)
                                    onFilterAdded(filter.id)
                                },
                                color = Color(0xFF1B1B22),
                                border = BorderStroke(0.5.dp, HighslateBorders),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(filter.name, fontSize = 11.sp, color = Color.White)
                                    Icon(Icons.Default.Add, null, tint = BrightAccentYellow, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

object EffectPreferences {
    private const val PREFS_NAME = "effect_preferences"
    private const val FAV_KEY = "favorite_effects"
    private const val REC_KEY = "recent_effects"
    private const val MAX_RECENTS = 8

    fun getFavoriteEffectIds(context: android.content.Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getStringSet(FAV_KEY, emptySet()) ?: emptySet()
    }

    fun toggleFavoriteEffect(context: android.content.Context, effectId: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(FAV_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (favorites.contains(effectId)) {
            favorites.remove(effectId)
        } else {
            favorites.add(effectId)
        }
        prefs.edit().putStringSet(FAV_KEY, favorites).apply()
        return favorites
    }

    fun getRecentEffectIds(context: android.content.Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val recentStr = prefs.getString(REC_KEY, "") ?: ""
        if (recentStr.isEmpty()) return emptyList()
        return recentStr.split(",")
    }

    fun addRecentEffect(context: android.content.Context, effectId: String): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val recentStr = prefs.getString(REC_KEY, "") ?: ""
        val recents = if (recentStr.isEmpty()) mutableListOf() else recentStr.split(",").toMutableList()
        
        // Remove if already exists to move to top
        recents.remove(effectId)
        recents.add(0, effectId)
        
        // Limit size
        val trimmed = if (recents.size > MAX_RECENTS) recents.subList(0, MAX_RECENTS) else recents
        prefs.edit().putString(REC_KEY, trimmed.joinToString(",")).apply()
        return trimmed
    }
}

@Composable
fun Modifier.consumeAllTouches(): Modifier = this.clickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() }
) { /* Intercept tap fall-through */ }

@Composable
fun FilterItemCard(
    filter: ZenithFilter,
    baseBitmap: android.graphics.Bitmap?,
    isAdded: Boolean,
    favoritesState: Set<String>,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var previewBitmap by remember(filter, baseBitmap) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(filter, baseBitmap) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val bmp = FilterPreviewCache.getOrCreatePreview(filter, context, baseBitmap)
            previewBitmap = bmp
        }
    }
    
    val displayBitmap = previewBitmap ?: FilterPreviewCache.getBaseSample(baseBitmap)
    
    Surface(
        modifier = Modifier
            .testTag("library_filter_node_${filter.id}")
            .fillMaxWidth()
            .clickable { onClick() },
        color = MediumSlateCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, if (isAdded) BrightAccentYellow else HighslateBorders)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = "Effect preview for ${filter.name}",
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, HighslateBorders, RoundedCornerShape(8.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = filter.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = filter.category,
                    fontSize = 11.sp,
                    color = LightTextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isFavorited = favoritesState.contains(filter.id)
                IconButton(
                    onClick = { onFavoriteToggle() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (isFavorited) Color(0xFFFF4D4D) else LightTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (isAdded) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Added icon marker",
                        tint = BrightAccentYellow,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Filter",
                        tint = LightTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}



