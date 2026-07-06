package com.aistudio.zenithstudio.rpxwtq

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

// Color palette matching Zenith Studio's high-contrast dark space slate theme
val DarkSlateBg = Color(0xFF0F1013)
val MediumSlateCard = Color(0xFF171A21)
val HighslateBorders = Color(0xFF2C313C)
val BrightAccentYellow = Color(0xFFFFCC00)
val DarkAccentRed = Color(0xFFCC3333)
val LightTextPrimary = Color(0xFFEEEEEE)
val LightTextSecondary = Color(0xFF9E9E9E)

@Composable
fun EffectsWindowShell(
    layerId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscapeMode: Boolean = false,
    onOpenRasterExtrudeEditor: (() -> Unit)? = null
) {
    SideEffect {
        EffectStackManager.currentLayerId = layerId
    }

    // 1. Separate State Managers
    var isSearchLibraryOpen by remember { mutableStateOf(false) }
    
    val allFilters = remember { ZenithFilterFactory.createFilterList() }
    val activeFilters = EffectStackManager.activeFilters
    
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

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = HighslateBorders, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable List of active effects in the stack
            if (activeFilters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activeFilters.forEachIndexed { index, activeFilter ->
                        val isExpanded = activeFilter.id == expandedFilterId
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("active_filter_card_${activeFilter.id}")
                                .clickable {
                                    expandedFilterId = if (isExpanded) null else activeFilter.id
                                },
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
                                    modifier = Modifier.fillMaxWidth(),
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
                                                EffectStackManager.toggleFilter(activeFilter.id)
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
                                            text = activeFilter.name,
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
                                            onClick = { EffectStackManager.duplicateFilter(activeFilter.id) },
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
                                                EffectStackManager.removeFilter(activeFilter.id)
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
                                        activeFilter.parameters.forEach { param ->
                                            BiDirectionalSlider(
                                                filterId = activeFilter.id,
                                                param = param,
                                                onParamChange = { id, paramName, newValue ->
                                                    EffectStackManager.updateParameter(id, paramName, newValue)
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
    } else {
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
                .height(320.dp) // Sits down, is taller / higher to fit Active FX + parameter sliders!
                .align(Alignment.BottomCenter)
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

                // Scrollable List of active effects in the stack
                if (activeFilters.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(activeFilters) { index, activeFilter ->
                            val isExpanded = activeFilter.id == expandedFilterId
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("active_filter_card_${activeFilter.id}")
                                    .clickable {
                                        expandedFilterId = if (isExpanded) null else activeFilter.id
                                    },
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
                                        modifier = Modifier.fillMaxWidth(),
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
                                                    EffectStackManager.toggleFilter(activeFilter.id)
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
                                                onClick = { EffectStackManager.duplicateFilter(activeFilter.id) },
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
                                                onClick = { EffectStackManager.removeFilter(activeFilter.id) },
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
                                                activeFilter.parameters.forEach { param ->
                                                    BiDirectionalSlider(
                                                        filterId = activeFilter.id,
                                                        param = param,
                                                        onParamChange = { id, paramName, newValue ->
                                                            EffectStackManager.updateParameter(id, paramName, newValue)
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
                        "Light Effects",
                        "Color Adjustments",
                        "Artistic",
                        "Blur & Gallery",
                        "Brush Strokes",
                        "Distort",
                        "Pixelate",
                        "Noise & Render",
                        "Sketch & Texture",
                        "Stylize",
                        "3D Module"
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
                                    color = if (isSelected) BrightAccentYellow else Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (isSelected) BrightAccentYellow else HighslateBorders)
                                ) {
                                    Text(
                                        text = category,
                                        color = if (isSelected) Color.Black else LightTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Filtered Nodes Grid/List
                        val matchingFilters = remember(librarySearchQuery, activeCategoryTab) {
                            derivedStateOf {
                                allFilters.filter { filter ->
                                    val matchesQuery = if (librarySearchQuery.isEmpty()) true else {
                                        filter.name.contains(librarySearchQuery, ignoreCase = true) ||
                                        filter.category.contains(librarySearchQuery, ignoreCase = true)
                                    }
                                    val matchesCategory = if (activeCategoryTab == "All") true else {
                                        val mappedCategory = when (activeCategoryTab) {
                                            "Artistic" -> "Artistic Effects"
                                            "Blur & Gallery" -> "Blur & Blur Gallery"
                                            "3D Module" -> "3D Module"
                                            else -> activeCategoryTab
                                        }
                                        filter.category.equals(mappedCategory, ignoreCase = true)
                                    }
                                    matchesQuery && matchesCategory
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (matchingFilters.value.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No match found for options in library",
                                            color = LightTextSecondary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                items(matchingFilters.value) { filter ->
                                    val isAdded = activeFilters.any { it.id == filter.id }
                                    Surface(
                                        modifier = Modifier
                                            .testTag("library_filter_node_${filter.id}")
                                            .fillMaxWidth()
                                            .clickable {
                                                EffectStackManager.addFilter(filter)
                                                expandedFilterId = filter.id
                                                isSearchLibraryOpen = false
                                            },
                                        color = MediumSlateCard,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.2.dp, if (isAdded) BrightAccentYellow else HighslateBorders)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = filter.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LightTextPrimary
                                                )
                                                Text(
                                                    text = filter.category,
                                                    fontSize = 12.sp,
                                                    color = LightTextSecondary
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isAdded) {
                                                    Text(
                                                        text = "Added",
                                                        color = BrightAccentYellow,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Filter",
                                                    tint = BrightAccentYellow,
                                                    modifier = Modifier.size(20.dp)
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
}

/**
 * High-performance bi-directional precision text-and-slider modifier engine
 */
@Composable
fun BiDirectionalSlider(
    filterId: String,
    param: FilterParameter,
    onParamChange: (String, String, Float) -> Unit
) {
    // Local transient states to avoid heavy intermediate recomposition locks
    var typedString by remember(param.currentValue) { mutableStateOf("%.2f".format(param.currentValue)) }
    var actualDragVal by remember(param.currentValue) { mutableStateOf(param.currentValue) }
    
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
                BasicTextField(
                    value = typedString,
                    onValueChange = { inputString ->
                        typedString = inputString
                        val numericVal = inputString.toFloatOrNull()
                        if (numericVal != null) {
                            val clamped = numericVal.coerceIn(param.minValue, param.maxValue)
                            actualDragVal = clamped
                            onParamChange(filterId, param.name, clamped)
                        }
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
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .testTag("input_${param.name}")
                        .width(60.dp)
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
                    val precision = (newVal * 100f).roundToInt() / 100f
                    actualDragVal = precision
                    typedString = "%.2f".format(precision)
                    // Update transient values inside continuous drag operations
                },
                onValueChangeFinished = {
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
                    .height(20.dp)
            )
            
            Text(
                text = "%.0f".format(param.maxValue),
                color = LightTextSecondary,
                fontSize = 8.sp
            )
        }
    }
}
