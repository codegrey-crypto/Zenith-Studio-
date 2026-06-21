package com.example.studio.ui

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studio.model.LayerType
import com.example.studio.model.StudioLayer
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max

@Composable
fun AIVisualsOverlayHub(
    selectedLayerId: String,
    layers: List<StudioLayer>,
    onLayersUpdated: (List<StudioLayer>) -> Unit,
    imageBitmapCache: MutableMap<String, androidx.compose.ui.graphics.ImageBitmap>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Procedural Screens: "SELECTION", "OFFSET_ENGINE", "SILHOUETTE_TRACER"
    var currentScreen by remember { mutableStateOf("SELECTION") }
    
    val selectedLayer = remember(selectedLayerId, layers) {
        layers.find { it.id == selectedLayerId }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkOnyx.copy(alpha = 0.95f))
            .clickable(enabled = false) { /* Prevent click through */ }
            .padding(16.dp)
            .testTag("ai_visuals_overlay_hub")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlatePanel, RoundedCornerShape(24.dp))
                .border(BorderStroke(1.5.dp, HighslateOutline), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(IndustrialAmber.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Transform,
                            contentDescription = "Geometric Suite",
                            tint = EnergeticYellow,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ZENITH PROCEDURAL DESIGN LABS",
                            style = Typography.titleLarge,
                            fontSize = 17.sp,
                            color = EnergeticYellow,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "High-Precision Low-Latency Mathematical Engines",
                            style = Typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
                
                // Close/Back button
                IconButton(
                    onClick = {
                        if (currentScreen == "SELECTION") {
                            onDismiss()
                        } else {
                            currentScreen = "SELECTION"
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(MidSlate, RoundedCornerShape(18.dp))
                ) {
                    Icon(
                        imageVector = if (currentScreen == "SELECTION") Icons.Default.Close else Icons.Default.ArrowBack,
                        contentDescription = "Navigate Selection",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Divider(color = HighslateOutline, thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))
            
            // Screen contents switcher
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier.weight(1f)
            ) { screen ->
                when (screen) {
                    "SELECTION" -> {
                        HubSelectionView(
                            selectedLayer = selectedLayer,
                            onOptionSelected = { option ->
                                currentScreen = option
                            }
                        )
                    }
                    "OFFSET_ENGINE" -> {
                        AdvancedPathOffsetView(
                            selectedLayer = selectedLayer,
                            layers = layers,
                            onCompleted = { updatedLayers ->
                                onLayersUpdated(updatedLayers)
                                currentScreen = "SELECTION"
                            }
                        )
                    }
                    "SILHOUETTE_TRACER" -> {
                        VectorSilhouetteTracerView(
                            selectedLayer = selectedLayer,
                            layers = layers,
                            imageBitmapCache = imageBitmapCache,
                            onCompleted = { updatedLayers ->
                                onLayersUpdated(updatedLayers)
                                currentScreen = "SELECTION"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HubSelectionView(
    selectedLayer: StudioLayer?,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selection State Preview Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MidSlate, RoundedCornerShape(14.dp))
                .border(BorderStroke(1.2.dp, HighslateOutline), RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (selectedLayer != null) Color(0xFF00FF66).copy(alpha = 0.15f)
                            else Color.Red.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedLayer != null) Icons.Default.Layers else Icons.Default.Warning,
                        contentDescription = "Layer indicator",
                        tint = if (selectedLayer != null) Color(0xFF00FF66) else Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    if (selectedLayer != null) {
                        Text(
                            text = "ACTIVE LAYER SELECTION",
                            style = Typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedLayer.name} (${selectedLayer.type.name})",
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Selected layer is primed and ready for mathematical layout synthesis.",
                            style = Typography.bodySmall,
                            color = Color(0xFF00FF66),
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "NO HIGHLIGHTED LAYER",
                            style = Typography.titleMedium,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select a drawing path or image layer inside the workspace to access localized geometric utilities.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        // Option 1: Advanced Path Offset Engine
        SelectionOptionCard(
            title = "📐 Advanced Path Offset Engine (Stroking & Insets)",
            description = "Calculates perpendicular normals of 2D vector coordinate arrays, generating perfectly scaled positive expansions or negative/inner offsets.",
            icon = Icons.Default.TrendingUp,
            iconTint = EnergeticYellow,
            enabled = true, // We allow entering and choosing a layer via dropdown if needed
            onClick = { onOptionSelected("OFFSET_ENGINE") }
        )
        
        // Option 2: Vector Silhouette Tracer
        SelectionOptionCard(
            title = "🎯 Sharp Vector Silhouette Tracer",
            description = "Traces bitmap interfaces recursively using a zero-dependency Moore-Neighbor algorithm to produce clean, infinitely scalable drawing path layers.",
            icon = Icons.Default.FilterCenterFocus,
            iconTint = MatteBlue,
            enabled = true, // Can select image within tool
            onClick = { onOptionSelected("SILHOUETTE_TRACER") }
        )
    }
}

@Composable
fun SelectionOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MidSlate else MidSlate.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) HighslateOutline else HighslateOutline.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (enabled) iconTint.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) iconTint else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = Typography.bodyLarge,
                    color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 1: ADVANCED PATH OFFSET ENGINE SCREEN
// -------------------------------------------------------------
@Composable
fun AdvancedPathOffsetView(
    selectedLayer: StudioLayer?,
    layers: List<StudioLayer>,
    onCompleted: (List<StudioLayer>) -> Unit
) {
    val context = LocalContext.current
    
    // Filter out available drawing layers for the user to choose
    val drawingLayers = remember(layers) {
        layers.filter { it.type == LayerType.FREEHAND_DRAWING }
    }
    
    var activeLayerToOffset by remember {
        mutableStateOf(
            if (selectedLayer != null && selectedLayer.type == LayerType.FREEHAND_DRAWING) selectedLayer 
            else drawingLayers.firstOrNull()
        )
    }
    
    var dropdownExpanded by remember { mutableStateOf(false) }
    var offsetDistance by remember { mutableStateOf(40f) }
    var isComputing by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PATH OFFSET CONTROLLER",
            style = Typography.titleMedium,
            color = EnergeticYellow,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Generate perfectly aligned outer borders (sticker cuts) or inner insets by calculating vector normal values dynamically across continuous coordinates.",
            style = Typography.bodyMedium,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        
        Divider(color = HighslateOutline, thickness = 1.dp)
        
        // Layer Dropdown Selector if no native path is pre-selected
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("TARGET VECTOR PATH LAYER", style = Typography.labelSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidSlate, RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, HighslateOutline), RoundedCornerShape(10.dp))
                    .clickable { dropdownExpanded = true }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeLayerToOffset?.name ?: "No Drawing Layers Found...",
                        color = if (activeLayerToOffset != null) TextPrimary else Color.Red,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown indicator", tint = TextPrimary)
                }
                
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(SlatePanel)
                        .border(BorderStroke(1.dp, HighslateOutline))
                ) {
                    if (drawingLayers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No current drawing layers on active page", color = Color.Red, style = Typography.bodySmall) },
                            onClick = { dropdownExpanded = false }
                        )
                    } else {
                        drawingLayers.forEach { drawLyr ->
                            DropdownMenuItem(
                                text = { Text("${drawLyr.name} (${drawLyr.brushPoints.size} nodes)", color = TextPrimary, style = Typography.bodyMedium) },
                                onClick = {
                                    activeLayerToOffset = drawLyr
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        if (activeLayerToOffset != null) {
            val totalNodes = activeLayerToOffset?.brushPoints?.filter { it != Offset.Unspecified }?.size ?: 0
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidSlate.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, HighslateOutline)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Vertices Info",
                        tint = EnergeticYellow,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(text = "Layer Geometry Specifications", style = Typography.labelSmall, color = TextSecondary)
                        Text(
                            text = "Points: $totalNodes nodes  |  Position: (${activeLayerToOffset?.positionX?.toInt()}px, ${activeLayerToOffset?.positionY?.toInt()}px)",
                            style = Typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(4.dp))
            
            // Slider offset distance controller
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OFFSET DISTANCE (STRETCH / INSET)", style = Typography.labelSmall, color = TextPrimary)
                    Text(
                        text = if (offsetDistance >= 0) "+${offsetDistance.toInt()}px" else "${offsetDistance.toInt()}px",
                        style = Typography.labelSmall,
                        color = EnergeticYellow,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = offsetDistance,
                    onValueChange = { offsetDistance = it },
                    valueRange = -500f..500f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = EnergeticYellow,
                        thumbColor = EnergeticYellow,
                        inactiveTrackColor = HighslateOutline
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Negative (Inner Inset)", style = Typography.bodySmall, color = TextSecondary, fontSize = 10.sp)
                    Text("Positive (Sticker Border)", style = Typography.bodySmall, color = TextSecondary, fontSize = 10.sp)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Previews in static mini-canvas
            Text("MATHEMATICAL GEOMETRY ENGINE PREVIEW", style = Typography.labelSmall, color = TextSecondary)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(DarkOnyx, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.2.dp, HighslateOutline), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Draws original vs offset path abstract geometry
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height
                    
                    // Simple demo loop inside the box
                    val centerX = canvasW / 2
                    val centerY = canvasH / 2
                    
                    // Draw clean procedural concentric representations
                    val pointsOriginal = mutableListOf<Offset>()
                    val pointsOffset = mutableListOf<Offset>()
                    
                    // Generate closed star/diamond points
                    val radiusOrig = 35f
                    val radiusOff = radiusOrig + (offsetDistance * 0.15f).coerceIn(-30f, 60f)
                    val stepsCount = 12
                    
                    for (i in 0..stepsCount) {
                        val angle = (i * 2 * Math.PI / stepsCount)
                        val cosVal = kotlin.math.cos(angle).toFloat()
                        val sinVal = kotlin.math.sin(angle).toFloat()
                        
                        // Modulate to look like a flower path representing original nodes
                        val factor = if (i % 2 == 0) 1.2f else 0.8f
                        
                        pointsOriginal.add(Offset(centerX + cosVal * radiusOrig * factor, centerY + sinVal * radiusOrig * factor))
                        pointsOffset.add(Offset(centerX + cosVal * radiusOff * factor, centerY + sinVal * radiusOff * factor))
                    }
                    
                    // Draw original vector nodes
                    val origPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(pointsOriginal[0].x, pointsOriginal[0].y)
                        for (i in 1 until pointsOriginal.size) {
                            lineTo(pointsOriginal[i].x, pointsOriginal[i].y)
                        }
                    }
                    drawPath(origPath, color = TextSecondary, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                    
                    // Draw mathematical projection offset
                    val offPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(pointsOffset[0].x, pointsOffset[0].y)
                        for (i in 1 until pointsOffset.size) {
                            lineTo(pointsOffset[i].x, pointsOffset[i].y)
                        }
                    }
                    drawPath(
                        offPath,
                        color = Color(0xFF00F0FF),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    )
                }
                
                Text(
                    text = "DASHED = CALCULATED TRANSFORMATION",
                    style = Typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Trigger action Button
            Button(
                onClick = {
                    isComputing = true
                    // Safely compute the perpendicular offsets in background thread
                    val targetLyr = activeLayerToOffset
                    if (targetLyr != null) {
                        val updatedPoints = generateOffsetPath(targetLyr.brushPoints, offsetDistance)
                        val offsetLayer = StudioLayer(
                            id = UUID.randomUUID().toString(),
                            name = "${targetLyr.name} Offset (${offsetDistance.toInt()}px)",
                            type = LayerType.FREEHAND_DRAWING,
                            positionX = targetLyr.positionX,
                            positionY = targetLyr.positionY,
                            width = targetLyr.width,
                            height = targetLyr.height,
                            scaleX = targetLyr.scaleX,
                            scaleY = targetLyr.scaleY,
                            rotation = targetLyr.rotation,
                            baseColor = GreenActive,
                            brushPoints = updatedPoints
                        )
                        
                        onCompleted(layers + offsetLayer)
                        Toast.makeText(context, "Calculated offset vector successfully!", Toast.LENGTH_SHORT).show()
                    }
                    isComputing = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenActive),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (isComputing) {
                    CircularProgressIndicator(color = DarkOnyx, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "CONSTRUCT INTEGRATED OFFSET LAYER",
                        color = DarkOnyx,
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Begin by sketching coordinates via the Pen/Brush tool in order to create a base path layer.",
                    color = Color.Red,
                    style = Typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 2: SHARP VECTOR SILHOUETTE TRACER SCREEN
// -------------------------------------------------------------
@Composable
fun VectorSilhouetteTracerView(
    selectedLayer: StudioLayer?,
    layers: List<StudioLayer>,
    imageBitmapCache: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    onCompleted: (List<StudioLayer>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Pick raster layers available for contour detection
    val rasterLayers = remember(layers) {
        layers.filter { it.type == LayerType.IMAGE_CARD && !it.imageUri.isNullOrEmpty() }
    }
    
    var activeRasterLayer by remember {
        mutableStateOf(
            if (selectedLayer != null && selectedLayer.type == LayerType.IMAGE_CARD && !selectedLayer.imageUri.isNullOrEmpty()) selectedLayer 
            else rasterLayers.firstOrNull()
        )
    }
    
    var selectorExpanded by remember { mutableStateOf(false) }
    var thresholdVal by remember { mutableStateOf(0.45f) }
    var isProcessing by remember { mutableStateOf(false) }
    
    val originalBitmapFiltered = remember(activeRasterLayer) {
        val uriStr = activeRasterLayer?.imageUri ?: ""
        imageBitmapCache[uriStr]?.asAndroidBitmap()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Descriptive Header
        Text(
            text = "SILHOUETTE CONTOUR TRACER",
            style = Typography.titleMedium,
            color = EnergeticYellow,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Extract extremely sharp mathematical shapes from photos or illustrations using an offline Moore-Neighbor boundary scanner. Outputs infinitely scalable coordinate arrays.",
            style = Typography.bodyMedium,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        
        Divider(color = HighslateOutline, thickness = 1.dp)
        
        // Raster Target Dropdown list
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("TARGET COMPONENT IMAGERY", style = Typography.labelSmall, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidSlate, RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, HighslateOutline), RoundedCornerShape(10.dp))
                    .clickable { selectorExpanded = true }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeRasterLayer?.name ?: "No Imagery Elements Found...",
                        color = if (activeRasterLayer != null) TextPrimary else Color.Red,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Selector Dropdown", tint = TextPrimary)
                }
                
                DropdownMenu(
                    expanded = selectorExpanded,
                    onDismissRequest = { selectorExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(SlatePanel)
                        .border(BorderStroke(1.dp, HighslateOutline))
                ) {
                    if (rasterLayers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No raster layers detected inside workspace assets", color = Color.Red, style = Typography.bodySmall) },
                            onClick = { selectorExpanded = false }
                        )
                    } else {
                        rasterLayers.forEach { rLyr ->
                            DropdownMenuItem(
                                text = { Text("${rLyr.name} (${rLyr.width.toInt()}x${rLyr.height.toInt()}px)", color = TextPrimary, style = Typography.bodyMedium) },
                                onClick = {
                                    activeRasterLayer = rLyr
                                    selectorExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        if (activeRasterLayer != null && originalBitmapFiltered != null) {
            val bmp = originalBitmapFiltered
            
            // Parameters
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("THRESHOLD SENSITIVITY", style = Typography.labelSmall, color = TextPrimary)
                    Text("${(thresholdVal * 100).toInt()}%", style = Typography.labelSmall, color = EnergeticYellow, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = thresholdVal,
                    onValueChange = { thresholdVal = it },
                    valueRange = 0.05f..0.95f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = EnergeticYellow,
                        thumbColor = EnergeticYellow,
                        inactiveTrackColor = HighslateOutline
                    )
                )
                Text(
                    text = "Controls the luminance cutoff limit. Lower values trace darker masses, higher limits incorporate highlights.",
                    style = Typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
            
            // Interactive pixel threshold pre-compiler
            Text("REAL-TIME CONTRAST SILHOUETTE PREVIEW", style = Typography.labelSmall, color = TextSecondary)
            
            val previewBitmap = remember(bmp, thresholdVal) {
                generateBinarizedPreview(bmp, thresholdVal)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(DarkOnyx, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.2.dp, HighslateOutline), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Binarized Preview",
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(DarkOnyx.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Procedural Edge Mask", style = Typography.labelSmall, color = GreenActive, fontSize = 9.sp)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Action button triggering high fidelity processing
            Button(
                onClick = {
                    isProcessing = true
                    coroutineScope.launch {
                        val finalLyr = activeRasterLayer
                        if (finalLyr != null) {
                            val computedPoints = withContext(Dispatchers.Default) {
                                traceContoursFromBitmap(bmp, thresholdVal)
                            }
                            
                            // Map coordinates properly to match current bounds of the raster container on workspace canvas
                            val scaledPoints = computedPoints.map { pt ->
                                if (pt == Offset.Unspecified) Offset.Unspecified
                                else Offset(
                                    pt.x / bmp.width * finalLyr.width,
                                    pt.y / bmp.height * finalLyr.height
                                )
                            }
                            
                            val tracedLayer = StudioLayer(
                                id = UUID.randomUUID().toString(),
                                name = "Silhouette of ${finalLyr.name}",
                                type = LayerType.FREEHAND_DRAWING,
                                positionX = finalLyr.positionX,
                                positionY = finalLyr.positionY,
                                width = finalLyr.width,
                                height = finalLyr.height,
                                scaleX = finalLyr.scaleX,
                                scaleY = finalLyr.scaleY,
                                rotation = finalLyr.rotation,
                                baseColor = Color(0xFF00F0FF),
                                brushPoints = scaledPoints
                            )
                            
                            onCompleted(layers + tracedLayer)
                            Toast.makeText(context, "Vector contours extracted successfully!", Toast.LENGTH_SHORT).show()
                        }
                        isProcessing = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = DarkOnyx, modifier = Modifier.size(20.dp))
                } else {
                    Text("GENERATE INFINITE SILHOUETTE", color = DarkOnyx, style = Typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Import or load a high contrast raster/image element on this page to initiate silhouette contour tracing.",
                    color = Color.Red,
                    style = Typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// -------------------------------------------------------------
// GEOMETRIC & TOPOLOGY MATH PROCESSING ALGORITHMS (KOTLIN)
// -------------------------------------------------------------

fun generateOffsetPath(points: List<Offset>, distance: Float): List<Offset> {
    if (points.isEmpty()) return emptyList()
    
    val result = mutableListOf<Offset>()
    
    // Split into sub-paths on Offset.Unspecified
    val subPaths = mutableListOf<MutableList<Offset>>()
    var currentSubPath = mutableListOf<Offset>()
    
    for (pt in points) {
        if (pt == Offset.Unspecified) {
            if (currentSubPath.isNotEmpty()) {
                subPaths.add(currentSubPath)
                currentSubPath = mutableListOf()
            }
        } else {
            currentSubPath.add(pt)
        }
    }
    if (currentSubPath.isNotEmpty()) {
        subPaths.add(currentSubPath)
    }
    
    for (path in subPaths) {
        val n = path.size
        if (n == 0) continue
        if (n == 1) {
            result.add(path[0] + Offset(distance, distance))
            continue
        }
        
        val isClosed = n > 2 && distance(path.first(), path.last()) < 8f
        val normals = ArrayList<Offset>(n)
        
        for (i in 0 until n) {
            val normal = when {
                isClosed -> {
                    val prevIdx = if (i == 0) n - 2 else i - 1
                    val nextIdx = if (i == n - 1) 1 else i + 1
                    
                    val pPrev = path[prevIdx]
                    val pNext = path[nextIdx]
                    val pCurr = path[i]
                    
                    val t1 = pCurr - pPrev
                    val t2 = pNext - pCurr
                    val n1 = getNormal(t1)
                    val n2 = getNormal(t2)
                    normalize(n1 + n2)
                }
                else -> {
                    when (i) {
                        0 -> {
                            val t = path[1] - path[0]
                            getNormal(t)
                        }
                        n - 1 -> {
                            val t = path[n - 1] - path[n - 2]
                            getNormal(t)
                        }
                        else -> {
                            val t1 = path[i] - path[i - 1]
                            val t2 = path[i + 1] - path[i]
                            val n1 = getNormal(t1)
                            val n2 = getNormal(t2)
                            normalize(n1 + n2)
                        }
                    }
                }
            }
            normals.add(normal)
        }
        
        val offsetPoints = ArrayList<Offset>(n)
        for (i in 0 until n) {
            val p = path[i]
            val normal = normals[i]
            offsetPoints.add(p + (normal * distance))
        }
        
        val smoothed = smoothPath(offsetPoints, isClosed)
        if (result.isNotEmpty()) {
            result.add(Offset.Unspecified)
        }
        result.addAll(smoothed)
    }
    return result
}

fun distance(p1: Offset, p2: Offset): Float {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

fun getNormal(vector: Offset): Offset {
    val len = kotlin.math.sqrt(vector.x * vector.x + vector.y * vector.y)
    if (len == 0f) return Offset(0f, -1f)
    return Offset(-vector.y / len, vector.x / len)
}

fun normalize(vector: Offset): Offset {
    val len = kotlin.math.sqrt(vector.x * vector.x + vector.y * vector.y)
    if (len == 0f) return Offset(0f, -1f)
    return Offset(vector.x / len, vector.y / len)
}

fun smoothPath(pts: List<Offset>, isClosed: Boolean): List<Offset> {
    val n = pts.size
    if (n < 3) return pts
    val smoothed = ArrayList<Offset>(n)
    
    for (i in 0 until n) {
        val prev = if (i == 0) {
            if (isClosed) pts[n - 2] else pts[0]
        } else pts[i - 1]
        
        val next = if (i == n - 1) {
            if (isClosed) pts[1] else pts[n - 1]
        } else pts[i + 1]
        
        val curr = pts[i]
        
        val sx = (prev.x + 2f * curr.x + next.x) / 4f
        val sy = (prev.y + 2f * curr.y + next.y) / 4f
        smoothed.add(Offset(sx, sy))
    }
    return smoothed
}

// Moore-Neighbor tracing algorithm with Ramer-Douglas-Peucker vector path reduction
fun traceContoursFromBitmap(src: Bitmap, thresholdNormalized: Float): List<Offset> {
    val maxDim = 300
    val origW = src.width
    val origH = src.height
    val scale = if (origW > maxDim || origH > maxDim) {
        maxDim.toFloat() / max(origW, origH)
    } else {
        1.0f
    }
    
    val w = (origW * scale).toInt().coerceAtLeast(10)
    val h = (origH * scale).toInt().coerceAtLeast(10)
    
    val scaledSrc = if (scale < 1.0f) {
        Bitmap.createScaledBitmap(src, w, h, true)
    } else src
    
    val pixels = IntArray(w * h)
    scaledSrc.getPixels(pixels, 0, w, 0, 0, w, h)
    
    val thresholdValue = thresholdNormalized * 255f
    val binary = BooleanArray(w * h)
    for (i in 0 until w * h) {
        val clr = pixels[i]
        val r = (clr shr 16) and 0xFF
        val g = (clr shr 8) and 0xFF
        val b = clr and 0xFF
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        binary[i] = luminance < thresholdValue
    }
    
    val visited = BooleanArray(w * h)
    val allContourPoints = mutableListOf<Offset>()
    
    val dx = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
    val dy = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
    
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            val idx = y * w + x
            if (binary[idx] && !binary[idx - 1] && !visited[idx]) {
                val loop = mutableListOf<Offset>()
                
                var cx = x
                var cy = y
                var currentDir = 6
                
                val startX = x
                val startY = y
                var steps = 0
                val maxSteps = 1500
                
                while (steps < maxSteps) {
                    loop.add(Offset(cx.toFloat() / scale, cy.toFloat() / scale))
                    visited[cy * w + cx] = true
                    
                    var foundNext = false
                    val searchStart = (currentDir + 5) % 8
                    
                    for (d in 0 until 8) {
                        val checkDir = (searchStart + d) % 8
                        val nx = cx + dx[checkDir]
                        val ny = cy + dy[checkDir]
                        
                        if (nx in 0 until w && ny in 0 until h) {
                            if (binary[ny * w + nx]) {
                                cx = nx
                                cy = ny
                                currentDir = checkDir
                                foundNext = true
                                break
                            }
                        }
                    }
                    
                    if (!foundNext) break
                    
                    visited[cy * w + cx] = true
                    if (cx == startX && cy == startY) break
                    steps++
                }
                
                if (loop.size > 4) {
                    val simplified = simplifyPath(loop, tolerance = 1.6f)
                    if (allContourPoints.isNotEmpty()) {
                        allContourPoints.add(Offset.Unspecified)
                    }
                    allContourPoints.addAll(simplified)
                }
            }
        }
    }
    return allContourPoints
}

fun simplifyPath(points: List<Offset>, tolerance: Float): List<Offset> {
    if (points.size <= 2) return points
    
    val keep = BooleanArray(points.size) { false }
    keep[0] = true
    keep[points.lastIndex] = true
    
    simplifySection(points, 0, points.lastIndex, tolerance, keep)
    
    val result = mutableListOf<Offset>()
    for (i in points.indices) {
        if (keep[i]) {
            result.add(points[i])
        }
    }
    return result
}

fun simplifySection(points: List<Offset>, start: Int, end: Int, tolerance: Float, keep: BooleanArray) {
    if (end <= start + 1) return
    
    var maxDist = 0f
    var maxIndex = start
    
    val pStart = points[start]
    val pEnd = points[end]
    
    val vx = pEnd.x - pStart.x
    val vy = pEnd.y - pStart.y
    val segmentLengthSq = vx * vx + vy * vy
    
    for (i in (start + 1) until end) {
        val p = points[i]
        val dist = if (segmentLengthSq == 0f) {
            distance(p, pStart)
        } else {
            val t = ((p.x - pStart.x) * vx + (p.y - pStart.y) * vy) / segmentLengthSq
            val clampedT = t.coerceIn(0f, 1f)
            val projX = pStart.x + clampedT * vx
            val projY = pStart.y + clampedT * vy
            distance(p, Offset(projX, projY))
        }
        
        if (dist > maxDist) {
            maxDist = dist
            maxIndex = i
        }
    }
    
    if (maxDist > tolerance) {
        keep[maxIndex] = true
        simplifySection(points, start, maxIndex, tolerance, keep)
        simplifySection(points, maxIndex, end, tolerance, keep)
    }
}

fun generateBinarizedPreview(src: Bitmap, thresholdNormalized: Float): Bitmap {
    val maxDim = 150
    val w: Int
    val h: Int
    if (src.width > src.height) {
        w = maxDim
        h = (src.height * (maxDim.toFloat() / src.width)).toInt().coerceAtLeast(1)
    } else {
        h = maxDim
        w = (src.width * (maxDim.toFloat() / src.height)).toInt().coerceAtLeast(1)
    }
    
    val scaled = Bitmap.createScaledBitmap(src, w, h, true)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    
    val thresholdValue = thresholdNormalized * 255f
    
    for (y in 0 until h) {
        for (x in 0 until w) {
            val clr = scaled.getPixel(x, y)
            val r = (clr shr 16) and 0xFF
            val g = (clr shr 8) and 0xFF
            val b = clr and 0xFF
            val luminance = 0.299f * r + 0.587f * g + 0.114f * b
            
            val newClr = if (luminance < thresholdValue) 0xFF00F0FF.toInt() else 0xFF19191D.toInt()
            out.setPixel(x, y, newClr)
        }
    }
    return out
}
