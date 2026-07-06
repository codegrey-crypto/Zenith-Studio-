package com.example.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studio.model.StudioLayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Color Constants matching Zenith Studio Design System
private val DarkCanvas = Color(0xFF121212)
private val SurfaceDark = Color(0xFF1E1E1E)
private val IndustrialAmber = Color(0xFFFFB300)
private val EnergeticYellow = Color(0xFFFFD54F)
private val TextPrimary = Color(0xFFEEEEEE)
private val TextSecondary = Color(0xB3EEEEEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BezierVectorControlPane(
    layer: StudioLayer?,
    activeBezierPointIndex: Int,
    onActiveBezierPointIndexChange: (Int) -> Unit,
    penCursorOffset: Offset?,
    onPenCursorOffsetChange: (Offset?) -> Unit,
    onUpdatePoints: (List<Offset>) -> Unit,
    onAddPoint: () -> Unit,
    onDeletePoint: () -> Unit,
    onToggleClosePath: () -> Unit,
    onToggleNodeType: () -> Unit,
    onToggleFillStyle: () -> Unit,
    artboardWidth: Float,
    artboardHeight: Float,
    totalScale: Float,
    penSubTool: String = "Standard",
    onPenSubToolChange: (String) -> Unit = {},
    onUpdateLayerProperties: ((strokeThickness: Float, baseColor: Color) -> Unit)? = null,
    isScrollable: Boolean = true,
    keepBezierSymmetrical: Boolean = true,
    onKeepBezierSymmetricalChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var nudgeStep by remember { mutableStateOf(5f) }
    val anchors = layer?.brushPoints?.toAnchorPoints() ?: emptyList()
    val isClosed = layer?.polygonEdges == 1
    val isFilled = layer != null && layer.strokeThickness <= 0f
    
    // Determine what kind of point is currently selected
    val selectedNodeIndex = if (activeBezierPointIndex >= 0) activeBezierPointIndex / 3 else -1
    val selectedSubPointType = if (activeBezierPointIndex >= 0) {
        when (activeBezierPointIndex % 3) {
            0 -> "Anchor Point"
            1 -> "In-Handle Control"
            else -> "Out-Handle Control"
        }
    } else "None"

    val scrollState = rememberScrollState()
    val scrollModifier = if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier

    Column(
        modifier = modifier
            .background(DarkCanvas)
            .then(scrollModifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 1: Tool Header with quick stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BÉZIER PEN STUDIO",
                    color = IndustrialAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (layer != null) "${layer.name} (${anchors.size} nodes)" else "Create a new vector path",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            
            // Quick style chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Fill / Stroke toggle chip
                InputChip(
                    selected = isFilled,
                    onClick = onToggleFillStyle,
                    label = { Text(if (isFilled) "Filled" else "Outline", fontSize = 10.sp, color = TextPrimary) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = IndustrialAmber.copy(0.2f),
                        selectedLabelColor = IndustrialAmber
                    )
                )
                // Close path toggle chip
                InputChip(
                    selected = isClosed,
                    onClick = onToggleClosePath,
                    label = { Text(if (isClosed) "Closed" else "Open", fontSize = 10.sp, color = TextPrimary) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = EnergeticYellow.copy(0.2f),
                        selectedLabelColor = EnergeticYellow
                    )
                )
            }
        }

        // Section 1.5: Photoshop-Style Pen Sub-Tools Selector Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PHOTOSHOP PEN TOOLS SELECTOR",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tools = listOf(
                        Triple("Standard", "Pen", Icons.Default.Edit),
                        Triple("Freeform", "Freeform", Icons.Default.Create),
                        Triple("AddAnchor", "+ Node", Icons.Default.Add),
                        Triple("DeleteAnchor", "- Node", Icons.Default.Delete),
                        Triple("ConvertPoint", "Convert", Icons.Default.Refresh)
                    )

                    tools.forEach { (mode, label, icon) ->
                        val isSelected = penSubTool == mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) IndustrialAmber.copy(0.15f) else Color.Transparent)
                                .clickable { onPenSubToolChange(mode) }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) IndustrialAmber else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                color = if (isSelected) IndustrialAmber else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Selected Point properties or Pen controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedNodeIndex >= 0 && selectedNodeIndex < anchors.size && layer != null) {
                    val ap = anchors[selectedNodeIndex]
                    val currentNodeType = if (selectedNodeIndex < layer.bezierNodeTypes.size) {
                        layer.bezierNodeTypes[selectedNodeIndex]
                    } else "SMOOTH"

                    // Active Node Inspector Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Active point",
                                tint = EnergeticYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Node #$selectedNodeIndex",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "($selectedSubPointType)",
                                color = EnergeticYellow,
                                fontSize = 11.sp
                            )
                        }

                        // Delete point action
                        IconButton(
                            onClick = onDeletePoint,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete anchor node",
                                tint = Color(0xFFFF1744),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Divider(color = Color.White.copy(0.08f))

                    // Dynamic Handle manipulation sliders (Collinear & Symmetrical)
                    val pts = layer.brushPoints
                    val k = selectedNodeIndex
                    val anchor = pts[k * 3]

                    // Calculate current active angle and distance relative to the anchor
                    val targetPt = when (activeBezierPointIndex % 3) {
                        1 -> pts[k * 3 + 1]
                        2 -> pts[k * 3 + 2]
                        else -> pts[k * 3 + 2] // default to out-handle
                    }

                    val dx = (targetPt.x - anchor.x).toDouble()
                    val dy = (targetPt.y - anchor.y).toDouble()
                    var currentAngle = Math.toDegrees(Math.atan2(dy, dx)).toFloat()
                    if (currentAngle < 0f) currentAngle += 360f
                    val currentDist = Math.hypot(dx, dy).toFloat().coerceIn(0f, 250f)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "INTUITIVE HANDLE CURVATURE ADJUSTMENT",
                            color = IndustrialAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 1. Straighten and Symmetric Reset Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val updated = pts.toMutableList()
                                    if (k * 3 + 2 in updated.indices) {
                                        updated[k * 3 + 1] = anchor
                                        updated[k * 3 + 2] = anchor
                                        onUpdatePoints(updated)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.08f)),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Straighten", tint = Color.Red, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Straighten (Sharp)", fontSize = 10.sp, color = TextPrimary)
                            }

                            Button(
                                onClick = {
                                    val updated = pts.toMutableList()
                                    if (k * 3 + 2 in updated.indices) {
                                        updated[k * 3 + 1] = anchor - Offset(50f, 0f)
                                        updated[k * 3 + 2] = anchor + Offset(50f, 0f)
                                        onUpdatePoints(updated)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.08f)),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Symmetric", tint = EnergeticYellow, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Symmetric Curve", fontSize = 10.sp, color = TextPrimary)
                            }
                        }

                        // 2. Curvature Handle Length Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Curve Amount:", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(75.dp))
                            Slider(
                                value = currentDist,
                                onValueChange = { newDist ->
                                    val updated = pts.toMutableList()
                                    if (k * 3 + 2 in updated.indices) {
                                        val angleRad = Math.toRadians(currentAngle.toDouble())
                                        val cosVal = Math.cos(angleRad).toFloat()
                                        val sinVal = Math.sin(angleRad).toFloat()
                                        
                                        if (activeBezierPointIndex % 3 == 0) {
                                            // Anchor selected: change both symmetrically
                                            updated[k * 3 + 1] = anchor - Offset(cosVal * newDist, sinVal * newDist)
                                            updated[k * 3 + 2] = anchor + Offset(cosVal * newDist, sinVal * newDist)
                                        } else {
                                            // Single handle selected
                                            val currentIdx = activeBezierPointIndex
                                            updated[currentIdx] = anchor + Offset(cosVal * newDist, sinVal * newDist)
                                            if (currentNodeType == "SMOOTH" && keepBezierSymmetrical) {
                                                val otherIdx = if (currentIdx % 3 == 1) k * 3 + 2 else k * 3 + 1
                                                val otherPt = updated[otherIdx]
                                                val otherDist = Math.hypot((otherPt.x - anchor.x).toDouble(), (otherPt.y - anchor.y).toDouble()).toFloat()
                                                val oppAngle = angleRad + Math.PI
                                                updated[otherIdx] = anchor + Offset(Math.cos(oppAngle).toFloat() * otherDist, Math.sin(oppAngle).toFloat() * otherDist)
                                            }
                                        }
                                        onUpdatePoints(updated)
                                    }
                                },
                                valueRange = 0f..200f,
                                modifier = Modifier.weight(1f).height(30.dp),
                                colors = SliderDefaults.colors(thumbColor = EnergeticYellow, activeTrackColor = EnergeticYellow)
                            )
                            Text("${currentDist.toInt()}px", color = TextPrimary, fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                        }

                        // 3. Curvature Handle Angle Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Curve Angle:", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(75.dp))
                            Slider(
                                value = currentAngle,
                                onValueChange = { newAngle ->
                                    val updated = pts.toMutableList()
                                    if (k * 3 + 2 in updated.indices) {
                                        val angleRad = Math.toRadians(newAngle.toDouble())
                                        val cosVal = Math.cos(angleRad).toFloat()
                                        val sinVal = Math.sin(angleRad).toFloat()

                                        if (activeBezierPointIndex % 3 == 0) {
                                            // Anchor selected: rotate both symmetrically
                                            updated[k * 3 + 1] = anchor - Offset(cosVal * currentDist, sinVal * currentDist)
                                            updated[k * 3 + 2] = anchor + Offset(cosVal * currentDist, sinVal * currentDist)
                                        } else {
                                            // Single handle selected
                                            val currentIdx = activeBezierPointIndex
                                            updated[currentIdx] = anchor + Offset(cosVal * currentDist, sinVal * currentDist)
                                            if (currentNodeType == "SMOOTH" && keepBezierSymmetrical) {
                                                val otherIdx = if (currentIdx % 3 == 1) k * 3 + 2 else k * 3 + 1
                                                val otherPt = updated[otherIdx]
                                                val otherDist = Math.hypot((otherPt.x - anchor.x).toDouble(), (otherPt.y - anchor.y).toDouble()).toFloat()
                                                val oppAngle = angleRad + Math.PI
                                                updated[otherIdx] = anchor + Offset(Math.cos(oppAngle).toFloat() * otherDist, Math.sin(oppAngle).toFloat() * otherDist)
                                            }
                                        }
                                        onUpdatePoints(updated)
                                    }
                                },
                                valueRange = 0f..360f,
                                modifier = Modifier.weight(1f).height(30.dp),
                                colors = SliderDefaults.colors(thumbColor = EnergeticYellow, activeTrackColor = EnergeticYellow)
                            )
                            Text("${currentAngle.toInt()}°", color = TextPrimary, fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                        }

                        Divider(color = Color.White.copy(0.04f))

                        // Toggle Curve Smoothness
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Collinear Constraint:", color = TextSecondary, fontSize = 11.sp)
                            Button(
                                onClick = onToggleNodeType,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentNodeType == "SMOOTH") IndustrialAmber else Color.White.copy(0.08f),
                                    contentColor = if (currentNodeType == "SMOOTH") Color.Black else TextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(if (currentNodeType == "SMOOTH") "SMOOTH (SYMMETRIC)" else "CORNER (SHARP)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Toggle Symmetrical Handles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Symmetric Handles:", color = TextSecondary, fontSize = 11.sp)
                            Button(
                                onClick = { onKeepBezierSymmetricalChange(!keepBezierSymmetrical) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (keepBezierSymmetrical) IndustrialAmber else Color.White.copy(0.08f),
                                    contentColor = if (keepBezierSymmetrical) Color.Black else TextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(if (keepBezierSymmetrical) "SYMMETRIC" else "DECOUPLED (SINGLE)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // No selected point prompt
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Instruction hint",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap-to-select any anchor point or handle on the canvas above to unlock live handles, straighten features, and curvature sliders.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // PATH SIMPLIFICATION UTILITIES CARD
        if (layer != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PATH SIMPLIFICATION UTILITIES",
                        color = IndustrialAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Reduce anchor point density to clean up paths or smooth jagged curves:",
                        color = TextSecondary,
                        fontSize = 8.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Simplify Entire Path button
                        Button(
                            onClick = {
                                val simplified = simplifyBezierPoints(layer.brushPoints, tolerance = 8f)
                                onUpdatePoints(simplified)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.08f)),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Simplify Entire Path", tint = EnergeticYellow, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simplify Path", fontSize = 10.sp, color = TextPrimary)
                        }

                        // Simplify Selected Part button
                        val isNodeSelected = selectedNodeIndex >= 0 && selectedNodeIndex < anchors.size
                        Button(
                            onClick = {
                                if (isNodeSelected) {
                                    val simplified = simplifySelectedPart(layer.brushPoints, selectedNodeIndex, tolerance = 8f)
                                    onUpdatePoints(simplified)
                                }
                            },
                            enabled = isNodeSelected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNodeSelected) Color.White.copy(0.12f) else Color.White.copy(0.02f),
                                disabledContainerColor = Color.White.copy(0.02f)
                            ),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Simplify Part", tint = if (isNodeSelected) IndustrialAmber else TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simplify Part", fontSize = 10.sp, color = if (isNodeSelected) TextPrimary else TextSecondary.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // Section 3: Stroke & Fill Properties Customizer
        if (layer != null && onUpdateLayerProperties != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "STROKE & FILL STYLE SETTINGS",
                        color = IndustrialAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    // Stroke Width Slider
                    val currentStrokeVal = if (layer.strokeThickness < 0f) 0f else layer.strokeThickness
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stroke Width:", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(85.dp))
                        Slider(
                            value = currentStrokeVal,
                            onValueChange = { onUpdateLayerProperties(if (it == 0f) -1f else it, layer.baseColor) },
                            valueRange = 0f..24f,
                            modifier = Modifier.weight(1f).height(30.dp),
                            colors = SliderDefaults.colors(thumbColor = IndustrialAmber, activeTrackColor = IndustrialAmber)
                        )
                        Text("${currentStrokeVal.toInt()}px", color = TextPrimary, fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                    }

                    // Stroke / Fill Preset Color Swatches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color Preset:", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(85.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val swatches = listOf(
                                Color(0xFFFFB300), // Industrial Amber
                                Color(0xFFFF5252), // Coral Red
                                Color(0xFF69F0AE), // Mint Green
                                Color(0xFF40C4FF), // Cyan
                                Color(0xFF536DFE), // Royal Blue
                                Color(0xFFE040FB), // Purple
                                Color(0xFFFFFFFF), // White
                                Color(0xFF263238)  // Dark Charcoal
                            )

                            swatches.forEach { color ->
                                val isSelected = layer.baseColor == color
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.White.copy(0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable { onUpdateLayerProperties(layer.strokeThickness, color) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Fine Precision Virtual Pen Joystick Reticle & Placement Controller
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // D-Pad Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VIRTUAL CURSOR PRECISION D-PAD",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    // Step selection
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1f, 5f, 15f).forEach { step ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (nudgeStep == step) IndustrialAmber else Color.White.copy(0.05f))
                                    .clickable { nudgeStep = step }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("${step.toInt()}px", fontSize = 9.sp, color = if (nudgeStep == step) Color.Black else TextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // High-precision Joystick D-Pad Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left panel: fine trackpad box
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val current = penCursorOffset ?: Offset(artboardWidth / 2f, artboardHeight / 2f)
                                    val scaleAdjusted = dragAmount / totalScale.coerceAtLeast(0.1f)
                                    val updatedX = (current.x + scaleAdjusted.x).coerceIn(0f, artboardWidth)
                                    val updatedY = (current.y + scaleAdjusted.y).coerceIn(0f, artboardHeight)
                                    onPenCursorOffsetChange(Offset(updatedX, updatedY))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Refresh, contentDescription = "Trackpad Icon", tint = IndustrialAmber.copy(0.5f), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Slide Trackpad", color = TextSecondary, fontSize = 9.sp)
                            Text("for fine nudge", color = TextSecondary, fontSize = 8.sp)
                        }
                    }

                    // Right panel: Large physical buttons to nudge
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(90.dp).background(Color.White.copy(0.03f), CircleShape))
                        Box(modifier = Modifier.size(50.dp).background(Color.White.copy(0.03f), CircleShape))

                        val nudgeCursor = { dx: Float, dy: Float ->
                            val current = penCursorOffset ?: Offset(artboardWidth / 2f, artboardHeight / 2f)
                            val updatedX = (current.x + dx).coerceIn(0f, artboardWidth)
                            val updatedY = (current.y + dy).coerceIn(0f, artboardHeight)
                            onPenCursorOffsetChange(Offset(updatedX, updatedY))
                        }

                        // Up Button
                        IconButton(
                            onClick = { nudgeCursor(0f, -nudgeStep) },
                            modifier = Modifier.align(Alignment.TopCenter).size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Cursor Up", tint = IndustrialAmber)
                        }

                        // Left Button
                        IconButton(
                            onClick = { nudgeCursor(-nudgeStep, 0f) },
                            modifier = Modifier.align(Alignment.CenterStart).size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Move Cursor Left", tint = IndustrialAmber)
                        }

                        // Right Button
                        IconButton(
                            onClick = { nudgeCursor(nudgeStep, 0f) },
                            modifier = Modifier.align(Alignment.CenterEnd).size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Move Cursor Right", tint = IndustrialAmber)
                        }

                        // Down Button
                        IconButton(
                            onClick = { nudgeCursor(0f, nudgeStep) },
                            modifier = Modifier.align(Alignment.BottomCenter).size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Cursor Down", tint = IndustrialAmber)
                        }

                        // Center reset button
                        IconButton(
                            onClick = {
                                onPenCursorOffsetChange(Offset(artboardWidth / 2f, artboardHeight / 2f))
                            },
                            modifier = Modifier.align(Alignment.Center).size(24.dp).background(Color.Black, CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Center cursor", tint = TextPrimary, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Huge action button to Add Node at Pen Cursor Offset
                Button(
                    onClick = onAddPoint,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndustrialAmber,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Place point node icon",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (layer != null && layer.type == com.example.studio.model.LayerType.VECTOR_BEZIER) "PLACE VECTOR NODE" else "CREATE PATH",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

private fun simplifySelectedPart(points: List<Offset>, selectedNodeIndex: Int, tolerance: Float = 8f): List<Offset> {
    if (points.size < 6 || selectedNodeIndex < 0) return points
    val anchors = points.toAnchorPoints()
    if (anchors.size <= 3) return points
    
    // Define neighborhood (e.g., center around selectedNodeIndex)
    val startIndex = (selectedNodeIndex - 2).coerceAtLeast(0)
    val endIndex = (selectedNodeIndex + 2).coerceAtMost(anchors.size - 1)
    
    if (endIndex - startIndex < 2) return points
    
    val subList = anchors.subList(startIndex, endIndex + 1)
    val simplifiedSubList = simplifyAnchorPoints(subList, tolerance)
    
    val newAnchors = mutableListOf<AnchorPoint>()
    for (i in 0 until startIndex) {
        newAnchors.add(anchors[i])
    }
    newAnchors.addAll(simplifiedSubList)
    for (i in endIndex + 1 until anchors.size) {
        newAnchors.add(anchors[i])
    }
    
    val results = mutableListOf<Offset>()
    for (a in newAnchors) {
        results.add(a.position)
        results.add(a.handleIn)
        results.add(a.handleOut)
    }
    return results
}

