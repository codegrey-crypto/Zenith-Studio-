package com.example.studio.ui

import androidx.compose.foundation.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.example.studio.model.LayerType
import com.example.studio.model.StudioLayer
import com.example.studio.model.StudioEffect
import com.example.ui.theme.Typography
import com.example.ui.theme.*
import java.io.File

@Composable
fun PrecisionJogWheel(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedRange<Float>,
    label: String,
    isInt: Boolean = false,
    valueFormatter: ((Float) -> String)? = null,
    testTag: String = "",
    highFreqKey: String? = null,
    activeColorOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }

    // 1. High-speed local state allowing dragging to be extremely responsive at 120 FPS
    var localValue by remember(value, highFreqKey) { mutableStateOf(SlidersHighFreqState.get(highFreqKey ?: label, value)) }
    val coroutineScope = rememberCoroutineScope()
    var pendingUpdateJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val isColorGradingFilter = label == "Exposure" || label == "Contrast" || label == "Highlights" || label == "Shadows" || label == "Whites" || label == "Blacks" || label == "Temp" || label == "Tint" || label == "Vibrance" || label == "Saturation" || label == "Clarity" || label == "Dehaze" || label == "Brightness" || label == "Sat" || label == "Bright" || label == "Opacity"
    
    // Style active filter tracking sliders, thumb controls, and numeric value tracks
    val activeColor = activeColorOverride ?: (if (isColorGradingFilter) Color(0xFFA855F7) else EnergeticYellow)

    // Mathematical Calibration for Perspective (exponential/cubic mapping curves for extreme sub-pixel control)
    val mapSliderToPersp = { s: Float ->
        s * s * s * 0.015f
    }
    val mapPerspToSlider = { p: Float ->
        val absP = kotlin.math.abs(p)
        val s = java.lang.Math.pow((absP / 0.015f).toDouble(), (1.0 / 3.0)).toFloat()
        if (p >= 0f) s else -s
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Parameter Label
        Text(
            text = label,
            style = Typography.labelSmall,
            fontSize = 10.sp,
            modifier = Modifier.width(55.dp),
            color = TextSecondary
        )

        // Native/Standard Linear Compose Slider with Debounced, Quantized Low-Latency
        val sliderValue = if (label.contains("Persp")) {
            mapPerspToSlider(localValue)
        } else {
            localValue.coerceIn(valueRange)
        }

        val range = if (label.contains("Persp")) {
            -1f..1f
        } else {
            valueRange.start..valueRange.endInclusive
        }

        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                val validatedValue = if (label.contains("Persp")) {
                    mapSliderToPersp(newValue)
                } else {
                    val rawQuantized = (newValue * 100f).roundToInt() / 100f
                    if (isInt) kotlin.math.round(rawQuantized) else rawQuantized
                }
                
                localValue = validatedValue
                SlidersHighFreqState.set(highFreqKey ?: label, validatedValue)
            },
            onValueChangeFinished = {
                onValueChange(localValue)
            },
            valueRange = range.start..range.endInclusive,
            colors = SliderDefaults.colors(
                activeTrackColor = activeColor,
                thumbColor = activeColor,
                inactiveTrackColor = HighslateOutline.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(testTag)
        )

        // Numerical Override Field
        Box(
            modifier = Modifier.width(75.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (isEditing) {
                val formatStr = if (label.contains("Persp")) "%.5f" else "%.2f"
                var editBuf by remember {
                    mutableStateOf(if (isInt) localValue.toInt().toString() else formatStr.format(localValue))
                }
                val focusRequester = remember { FocusRequester() }

                BasicTextField(
                    value = editBuf,
                    onValueChange = { editBuf = it },
                    textStyle = Typography.labelSmall.copy(
                        color = activeColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            editBuf.toFloatOrNull()?.let {
                                val valCoerced = it.coerceIn(valueRange)
                                localValue = valCoerced
                                onValueChange(valCoerced)
                            }
                            isEditing = false
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF09090C), RoundedCornerShape(4.dp))
                        .border(BorderStroke(1.dp, activeColor), RoundedCornerShape(4.dp))
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                        .focusRequester(focusRequester)
                        .testTag(testTag + "_input")
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                val formatStr = if (label.contains("Persp")) "%.5f" else "%.2f"
                val displayVal = if (valueFormatter != null) {
                    valueFormatter(localValue)
                } else if (isInt) {
                    "${localValue.toInt()}"
                } else {
                    formatStr.format(localValue)
                }
                
                val displaySuffix = if (valueFormatter != null) "" else {
                    if (label == "Rotation") "°" else if (label.contains("Ratio") || label.contains("Skew") || label.contains("Persp") || label.contains("Pivot") || label.contains("Size") || label.contains("Radius")) "" else " px"
                }

                Text(
                    text = displayVal + displaySuffix,
                    color = activeColor,
                    style = Typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditing = true }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .testTag(testTag + "_readout")
                )
            }
        }
    }
}

@Composable
fun TransformDetailView(
    selectedLayer: StudioLayer,
    onUpdateLayer: (StudioLayer) -> Unit,
    artboardWidth: Float = 1080f,
    artboardHeight: Float = 1080f
) {
    val detailScrollState = rememberScrollState()
    val electricCyan = Color(0xFF00E5FF)
    val darkSteelNavy = Color(0xFF161F32)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(detailScrollState)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 1: Dimensions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkSteelNavy, RoundedCornerShape(8.dp))
                .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Geometric Coordinates & Size", style = Typography.labelSmall, color = electricCyan, fontWeight = FontWeight.Bold)
            
            PrecisionJogWheel(
                value = selectedLayer.positionX,
                onValueChange = { onUpdateLayer(selectedLayer.copy(positionX = it)) },
                valueRange = -5000f..5000f,
                label = "Pos-X",
                isInt = true,
                testTag = "transform_pos_x_slider",
                activeColorOverride = electricCyan
            )

            PrecisionJogWheel(
                value = selectedLayer.positionY,
                onValueChange = { onUpdateLayer(selectedLayer.copy(positionY = it)) },
                valueRange = -5000f..5000f,
                label = "Pos-Y",
                isInt = true,
                testTag = "transform_pos_y_slider",
                activeColorOverride = electricCyan
            )

            PrecisionJogWheel(
                value = selectedLayer.rotation,
                onValueChange = { onUpdateLayer(selectedLayer.copy(rotation = it)) },
                valueRange = -180f..180f,
                label = "Rotation",
                isInt = true,
                testTag = "transform_rotation_slider",
                activeColorOverride = electricCyan
            )

            PrecisionJogWheel(
                value = selectedLayer.width,
                onValueChange = { onUpdateLayer(selectedLayer.copy(width = it)) },
                valueRange = 1f..5000f,
                label = "Width",
                isInt = true,
                testTag = "transform_width_slider",
                activeColorOverride = electricCyan
            )

            PrecisionJogWheel(
                value = selectedLayer.height,
                onValueChange = { onUpdateLayer(selectedLayer.copy(height = it)) },
                valueRange = 1f..5000f,
                label = "Height",
                isInt = true,
                testTag = "transform_height_slider",
                activeColorOverride = electricCyan
            )
        }

        // Section 2: Pivot
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkSteelNavy, RoundedCornerShape(8.dp))
                .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nudge Positioning & Auto Align", style = Typography.labelSmall, color = electricCyan, fontWeight = FontWeight.Bold)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Pixel Nudge Controls (D-pad + Step configuration)
                Column(
                    modifier = Modifier.weight(1.3f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Step Picker Row
                    var nudgeAmount by remember { mutableStateOf(2) }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(Color(0xFF101622), RoundedCornerShape(24.dp))
                            .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(24.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        // Minus button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF1C273E), RoundedCornerShape(12.dp))
                                .clickable { if (nudgeAmount > 1) nudgeAmount-- }
                                .testTag("nudge_decrement"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = Color.White, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            text = "${nudgeAmount}px",
                            color = Color.White,
                            style = Typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 36.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        // Plus button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF1C273E), RoundedCornerShape(12.dp))
                                .clickable { if (nudgeAmount < 100) nudgeAmount++ }
                                .testTag("nudge_increment"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color.White, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Arrow Keys (D-Pad Grid)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Up Row
                        Row {
                            Box(modifier = Modifier.size(32.dp)) // Spacer
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1C273E), RoundedCornerShape(6.dp))
                                    .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(6.dp))
                                    .clickable {
                                        onUpdateLayer(selectedLayer.copy(positionY = selectedLayer.positionY - nudgeAmount))
                                    }
                                    .testTag("nudge_up"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "Move Up", tint = electricCyan, modifier = Modifier.size(24.dp))
                            }
                            Box(modifier = Modifier.size(32.dp)) // Spacer
                        }
                        
                        // Left/Right Row
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Left Arrow
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1C273E), RoundedCornerShape(6.dp))
                                    .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(6.dp))
                                    .clickable {
                                        onUpdateLayer(selectedLayer.copy(positionX = selectedLayer.positionX - nudgeAmount))
                                    }
                                    .testTag("nudge_left"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowLeft, "Move Left", tint = electricCyan, modifier = Modifier.size(24.dp))
                            }
                            
                            // Center spacer
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF101622), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocationOn, "Move", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            
                            // Right Arrow
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1C273E), RoundedCornerShape(6.dp))
                                    .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(6.dp))
                                    .clickable {
                                        onUpdateLayer(selectedLayer.copy(positionX = selectedLayer.positionX + nudgeAmount))
                                    }
                                    .testTag("nudge_right"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowRight, "Move Right", tint = electricCyan, modifier = Modifier.size(24.dp))
                            }
                        }
                        
                        // Down Row
                        Row {
                            Box(modifier = Modifier.size(32.dp)) // Spacer
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1C273E), RoundedCornerShape(6.dp))
                                    .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(6.dp))
                                    .clickable {
                                        onUpdateLayer(selectedLayer.copy(positionY = selectedLayer.positionY + nudgeAmount))
                                    }
                                    .testTag("nudge_down"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "Move Down", tint = electricCyan, modifier = Modifier.size(24.dp))
                            }
                            Box(modifier = Modifier.size(32.dp)) // Spacer
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                Column(
                    modifier = Modifier
                        .background(Color(0xFF181822), RoundedCornerShape(6.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("QUICK ALIGN", style = Typography.labelSmall, fontSize = 7.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    for (row in 0..2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (col in 0..2) {
                                val targetX = when (col) {
                                    0 -> 0f
                                    1 -> (artboardWidth - selectedLayer.width) / 2f
                                    else -> artboardWidth - selectedLayer.width
                                }
                                val targetY = when (row) {
                                    0 -> 0f
                                    1 -> (artboardHeight - selectedLayer.height) / 2f
                                    else -> artboardHeight - selectedLayer.height
                                }
                                val isSelected = Math.abs(selectedLayer.positionX - targetX) < 2f && Math.abs(selectedLayer.positionY - targetY) < 2f
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isSelected) electricCyan else MidSlate,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            onUpdateLayer(
                                                selectedLayer.copy(
                                                    positionX = targetX,
                                                    positionY = targetY,
                                                    pivotX = col * 0.5f,
                                                    pivotY = row * 0.5f
                                                )
                                            )
                                        }
                                        .testTag("pivot_grid_${row}_${col}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (isSelected) DarkOnyx else TextSecondary,
                                                RoundedCornerShape(12.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Perspective & Skew
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkSteelNavy, RoundedCornerShape(8.dp))
                .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Perspective & Skew 3D Effects", style = Typography.labelSmall, color = electricCyan, fontWeight = FontWeight.Bold)
            
            PrecisionJogWheel(
                value = selectedLayer.skewX,
                onValueChange = { onUpdateLayer(selectedLayer.copy(skewX = it)) },
                valueRange = -1.5f..1.5f,
                label = "Skew-X",
                isInt = false,
                testTag = "transform_skew_x_slider",
                activeColorOverride = electricCyan
            )

            PrecisionJogWheel(
                value = selectedLayer.skewY,
                onValueChange = { onUpdateLayer(selectedLayer.copy(skewY = it)) },
                valueRange = -1.5f..1.5f,
                label = "Skew-Y",
                isInt = false,
                testTag = "transform_skew_y_slider",
                activeColorOverride = electricCyan
            )

            PrecisionJogWheel(
                value = selectedLayer.perspX,
                onValueChange = { onUpdateLayer(selectedLayer.copy(perspX = it)) },
                valueRange = -0.015f..0.015f,
                label = "PerspX",
                isInt = false,
                testTag = "transform_persp_x_slider",
                activeColorOverride = electricCyan
            )

            PrecisionJogWheel(
                value = selectedLayer.perspY,
                onValueChange = { onUpdateLayer(selectedLayer.copy(perspY = it)) },
                valueRange = -0.015f..0.015f,
                label = "PerspY",
                isInt = false,
                testTag = "transform_persp_y_slider",
                activeColorOverride = electricCyan
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onUpdateLayer(selectedLayer.copy(skewX = 0f, skewY = 0f)) },
                    colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("Reset Skew", style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary)
                }

                Button(
                    onClick = { onUpdateLayer(selectedLayer.copy(perspX = 0f, perspY = 0f)) },
                    colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f).height(28.dp)
                ) {
                    Text("Reset Persp", style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary)
                }

                Button(
                    onClick = { onUpdateLayer(selectedLayer.copy(skewX = 0f, skewY = 0f, perspX = 0f, perspY = 0f)) },
                    colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1.1f).height(28.dp)
                ) {
                    Text("Reset All", style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider(color = HighslateOutline.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(Modifier.height(4.dp))

            Text("Photoshop Perspective Warp", style = Typography.labelSmall, color = electricCyan, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Perspective Warp", style = Typography.labelSmall, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedLayer.perspWarpEnabled) electricCyan else MidSlate)
                        .clickable {
                            onUpdateLayer(selectedLayer.copy(perspWarpEnabled = !selectedLayer.perspWarpEnabled))
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (selectedLayer.perspWarpEnabled) "ENABLED" else "DISABLED",
                        style = Typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (selectedLayer.perspWarpEnabled) DarkOnyx else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedLayer.perspWarpEnabled) {
                PrecisionJogWheel(
                    value = selectedLayer.perspWarpSplitY,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(perspWarpSplitY = it)) },
                    valueRange = 0.1f..0.9f,
                    label = "Warp Split Y",
                    isInt = false,
                    activeColorOverride = electricCyan
                )

                PrecisionJogWheel(
                    value = selectedLayer.perspWarpWidth,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(perspWarpWidth = it)) },
                    valueRange = 0.5f..3.0f,
                    label = "Warp Width",
                    isInt = false,
                    activeColorOverride = electricCyan
                )

                PrecisionJogWheel(
                    value = selectedLayer.perspWarpHeight,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(perspWarpHeight = it)) },
                    valueRange = 0.5f..2.0f,
                    label = "Warp Height",
                    isInt = false,
                    activeColorOverride = electricCyan
                )

                Button(
                    onClick = {
                        onUpdateLayer(selectedLayer.copy(
                            perspWarpSplitY = 0.5f,
                            perspWarpWidth = 1.0f,
                            perspWarpHeight = 1.0f
                        ))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                ) {
                    Text("Reset Warp Parameters", style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun TypographyOrShapeDetailView(
    selectedLayer: StudioLayer,
    onUpdateLayer: (StudioLayer) -> Unit,
    fontSearchQuery: String,
    onFontSearchQueryChange: (String) -> Unit,
    selectedCategoryFilter: String,
    onSelectedCategoryFilterChange: (String) -> Unit,
    onImportFontClick: () -> Unit,
    customFonts: List<com.example.studio.database.CustomFontEntity> = emptyList()
) {
    val detailScrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(detailScrollState)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (selectedLayer.type == LayerType.TEXT) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131317), RoundedCornerShape(8.dp))
                    .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Typography Style & Font ", style = Typography.labelSmall, color = EnergeticYellow)
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Source Text Content", style = Typography.labelSmall, fontSize = 9.sp, color = TextSecondary)
                    var txtInputBuf by remember(selectedLayer.id) { mutableStateOf(selectedLayer.textContent) }
                    OutlinedTextField(
                        value = txtInputBuf,
                        onValueChange = {
                            txtInputBuf = it
                            onUpdateLayer(selectedLayer.copy(textContent = it))
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("text_layer_input"),
                        textStyle = Typography.labelSmall.copy(color = TextPrimary),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndustrialAmber,
                            unfocusedBorderColor = HighslateOutline,
                            cursorColor = IndustrialAmber
                        )
                    )
                }
                
                Divider(color = HighslateOutline.copy(alpha = 0.3f), thickness = 0.5.dp)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onUpdateLayer(selectedLayer.copy(fontSize = maxOf(8f, selectedLayer.fontSize - 2f))) },
                            modifier = Modifier.size(24.dp).testTag("font_size_decrease")
                        ) {
                            Icon(Icons.Default.Remove, "Decrease", modifier = Modifier.size(16.dp), tint = TextSecondary)
                        }

                        PrecisionJogWheel(
                            value = selectedLayer.fontSize,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(fontSize = it)) },
                            valueRange = 8f..150f,
                            label = "Font Size",
                            isInt = true,
                            testTag = "font_size_slider",
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { onUpdateLayer(selectedLayer.copy(fontSize = minOf(150f, selectedLayer.fontSize + 2f))) },
                            modifier = Modifier.size(24.dp).testTag("font_size_increase")
                        ) {
                            Icon(Icons.Default.Add, "Increase", modifier = Modifier.size(16.dp), tint = TextSecondary)
                        }
                    }
                }

                Divider(color = HighslateOutline.copy(alpha = 0.3f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Format Styles", style = Typography.labelSmall, fontSize = 9.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { onUpdateLayer(selectedLayer.copy(fontIsBold = !selectedLayer.fontIsBold)) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(if (selectedLayer.fontIsBold) IndustrialAmber else MidSlate, RoundedCornerShape(4.dp))
                                    .testTag("text_style_bold")
                            ) {
                                Text("B", fontWeight = FontWeight.Bold, color = if (selectedLayer.fontIsBold) DarkOnyx else TextPrimary, fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = { onUpdateLayer(selectedLayer.copy(fontIsItalic = !selectedLayer.fontIsItalic)) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(if (selectedLayer.fontIsItalic) IndustrialAmber else MidSlate, RoundedCornerShape(4.dp))
                                    .testTag("text_style_italic")
                            ) {
                                Text("I", style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = if (selectedLayer.fontIsItalic) DarkOnyx else TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                        Text("Alignment", style = Typography.labelSmall, fontSize = 9.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val alignments = listOf("Left", "Center", "Right")
                            alignments.forEach { align ->
                                val isActive = selectedLayer.fontAlign.equals(align, ignoreCase = true)
                                IconButton(
                                    onClick = { onUpdateLayer(selectedLayer.copy(fontAlign = align)) },
                                    modifier = Modifier
                                        .height(34.dp)
                                        .widthIn(min = 40.dp)
                                        .background(if (isActive) IndustrialAmber else MidSlate, RoundedCornerShape(4.dp))
                                        .testTag("text_align_${align.lowercase()}")
                                ) {
                                    Text(align.take(3), style = Typography.labelMedium, color = if (isActive) DarkOnyx else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Divider(color = HighslateOutline.copy(alpha = 0.3f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Typography Font", style = Typography.labelSmall, fontSize = 10.sp, color = EnergeticYellow)
                    Button(
                        onClick = onImportFontClick,
                        colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp).testTag("import_font_button")
                    ) {
                        Icon(Icons.Default.Add, "Import", modifier = Modifier.size(10.dp), tint = DarkOnyx)
                        Spacer(Modifier.width(2.dp))
                        Text("Import Font", style = Typography.labelSmall, fontSize = 9.sp, color = DarkOnyx, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = fontSearchQuery,
                    onValueChange = onFontSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("font_search_input"),
                    placeholder = { Text("Search font family name...", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary) },
                    textStyle = Typography.labelSmall.copy(color = TextPrimary),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndustrialAmber,
                        unfocusedBorderColor = HighslateOutline,
                        cursorColor = IndustrialAmber
                    )
                )

                val categories = listOf("All", "Imported", "Sans-Serif", "Serif", "Monospace", "Display", "Script", "Handwritten")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Button(
                            onClick = { onSelectedCategoryFilterChange(cat) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) IndustrialAmber else MidSlate),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(24.dp).testTag("font_cat_$cat")
                        ) {
                            Text(cat, style = Typography.labelSmall, fontSize = 9.sp, color = if (isSelected) DarkOnyx else TextPrimary)
                        }
                    }
                }

                val importedFontsList = remember(FontFavoritesState.favoriteFontsList, customFonts) {
                    try {
                        val fontsDir = File(context.filesDir, "fonts")
                        if (!fontsDir.exists()) fontsDir.mkdirs()
                        val files = fontsDir.listFiles { file ->
                            file.isFile && (file.extension.lowercase() == "ttf" || file.extension.lowercase() == "otf")
                        } ?: emptyArray()
                        files.map { file ->
                            val cleanName = file.nameWithoutExtension.replace("_", " ").replace("-", " ")
                            val category = if (cleanName.contains("script", ignoreCase = true)) "Script"
                                else if (cleanName.contains("hand", ignoreCase = true) || cleanName.contains("write", ignoreCase = true)) "Handwritten"
                                else if (cleanName.contains("mono", ignoreCase = true)) "Monospace"
                                else if (cleanName.contains("serif", ignoreCase = true)) "Serif"
                                else "Display"
                            FontResource(name = cleanName, category = category, path = file.absolutePath, isImported = true)
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val consolidatedFonts = remember(importedFontsList) {
                    val allFonts = listOf(
                        FontResource(name = "Roboto (Sans-serif)", category = "Sans-Serif", systemFamily = "sans-serif"),
                        FontResource(name = "Noto Serif (Serif)", category = "Serif", systemFamily = "serif"),
                        FontResource(name = "Roboto Mono (Monospace)", category = "Monospace", systemFamily = "monospace"),
                        FontResource(name = "Montserrat", category = "Sans-Serif", systemFamily = "sans-serif-condensed"),
                        FontResource(name = "Merriweather", category = "Serif", systemFamily = "serif"),
                        FontResource(name = "Playfair Display", category = "Display", systemFamily = "serif"),
                        FontResource(name = "Pacifico (Script)", category = "Script", systemFamily = "sans-serif"),
                        FontResource(name = "Dancing Script", category = "Script", systemFamily = "serif"),
                        FontResource(name = "Caveat (Handwritten)", category = "Handwritten", systemFamily = "sans-serif"),
                        FontResource(name = "Indie Flower", category = "Handwritten", systemFamily = "sans-serif")
                    )
                    allFonts + importedFontsList
                }

                val filteredFonts = if (selectedCategoryFilter == "All" && fontSearchQuery.isEmpty()) emptyList() else consolidatedFonts.filter { font ->
                    val matchesQuery = font.name.contains(fontSearchQuery, ignoreCase = true)
                    val matchesCategory = when (selectedCategoryFilter) {
                        "All" -> true
                        "Imported" -> font.isImported
                        else -> font.category == selectedCategoryFilter
                    }
                    matchesQuery && matchesCategory
                }

                if (filteredFonts.isEmpty()) {
                    if (selectedCategoryFilter == "All" && fontSearchQuery.isEmpty()) {
                        val favorites = consolidatedFonts.filter { FontFavoritesState.favoriteFontsList.contains(it.name) }
                        val imported = consolidatedFonts.filter { it.isImported }
                        val system = consolidatedFonts.filter { !it.isImported }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 1. Favorites Section
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
                                ) {
                                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(12.dp), tint = Color(0xFFFF4D4D))
                                    Spacer(Modifier.width(6.dp))
                                    Text("FAVORITE FONTS", style = Typography.labelSmall, fontSize = 9.sp, color = Color(0xFFFF4D4D), fontWeight = FontWeight.Bold)
                                }
                                if (favorites.isEmpty()) {
                                    Text("No favorite fonts yet. Tap the heart next to any font to save it here!", style = Typography.labelSmall, fontSize = 9.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 8.dp))
                                } else {
                                    favorites.forEach { font ->
                                        RenderFontRow(font, selectedLayer, onUpdateLayer, context)
                                    }
                                }
                            }

                            Divider(color = HighslateOutline.copy(alpha = 0.15f), thickness = 0.5.dp)

                            // 2. Imported Section
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
                                ) {
                                    Icon(Icons.Default.Folder, null, modifier = Modifier.size(12.dp), tint = EnergeticYellow)
                                    Spacer(Modifier.width(6.dp))
                                    Text("IMPORTED FONTS", style = Typography.labelSmall, fontSize = 9.sp, color = EnergeticYellow, fontWeight = FontWeight.Bold)
                                }
                                if (imported.isEmpty()) {
                                    Text("No imported fonts yet. Tap 'Import Font' above to load custom TTF/OTF files.", style = Typography.labelSmall, fontSize = 9.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 8.dp))
                                } else {
                                    imported.forEach { font ->
                                        RenderFontRow(font, selectedLayer, onUpdateLayer, context)
                                    }
                                }
                            }

                            Divider(color = HighslateOutline.copy(alpha = 0.15f), thickness = 0.5.dp)

                            // 3. System Fonts Section
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
                                ) {
                                    Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = TextSecondary)
                                    Spacer(Modifier.width(6.dp))
                                    Text("SYSTEM FONTS", style = Typography.labelSmall, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                }
                                system.forEach { font ->
                                    RenderFontRow(font, selectedLayer, onUpdateLayer, context)
                                }
                            }
                        }
                    } else {
                        Text("No matching fonts found.", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        filteredFonts.forEach { font ->
                            RenderFontRow(font, selectedLayer, onUpdateLayer, context)
                        }
                    }
                }
            }
        } else {            val isShapeLayer = selectedLayer.type.name.startsWith("VECTOR_")
            if (isShapeLayer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131317), RoundedCornerShape(8.dp))
                        .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Parametric Shape Editor", style = Typography.labelSmall, color = EnergeticYellow)
                    
                    Text(
                        text = "Shape Type: ${selectedLayer.type.name.removePrefix("VECTOR_").replace("_", " ")}",
                        style = Typography.labelSmall,
                        fontSize = 11.sp,
                        color = MatteBlue,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Divider(color = HighslateOutline.copy(alpha = 0.5f), thickness = 0.5.dp)
 
                    Text("Dimensions & Scale", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    
                    PrecisionJogWheel(
                        value = selectedLayer.width,
                        onValueChange = {
                            val newW = it.coerceIn(1f, 5000f)
                            val oldCenterX = selectedLayer.positionX + selectedLayer.width / 2f
                            val newX = oldCenterX - newW / 2f
                            if (selectedLayer.type == LayerType.VECTOR_CIRCLE) {
                                val oldCenterY = selectedLayer.positionY + selectedLayer.height / 2f
                                val newY = oldCenterY - newW / 2f
                                onUpdateLayer(selectedLayer.copy(width = newW, height = newW, positionX = newX, positionY = newY))
                            } else {
                                onUpdateLayer(selectedLayer.copy(width = newW, positionX = newX))
                            }
                        },
                        valueRange = 1f..5000f,
                        label = "Width",
                        isInt = true,
                        testTag = "shape_width_slider"
                    )
 
                    PrecisionJogWheel(
                        value = selectedLayer.height,
                        onValueChange = {
                            val newH = it.coerceIn(1f, 5000f)
                            val oldCenterY = selectedLayer.positionY + selectedLayer.height / 2f
                            val newY = oldCenterY - newH / 2f
                            if (selectedLayer.type == LayerType.VECTOR_CIRCLE) {
                                val oldCenterX = selectedLayer.positionX + selectedLayer.width / 2f
                                val newX = oldCenterX - newH / 2f
                                onUpdateLayer(selectedLayer.copy(width = newH, height = newH, positionX = newX, positionY = newY))
                            } else {
                                onUpdateLayer(selectedLayer.copy(height = newH, positionY = newY))
                            }
                        },
                        valueRange = 1f..5000f,
                        label = "Height",
                        isInt = true,
                        testTag = "shape_height_slider"
                    )
 
                    Spacer(Modifier.height(4.dp))
                    Divider(color = HighslateOutline.copy(alpha = 0.3f), thickness = 0.5.dp)
                     // 1. Corner Radius / Corner Cut / Head Size Slider
                    val usesCornerRadius = selectedLayer.type in listOf(
                        LayerType.VECTOR_RECT, LayerType.VECTOR_ROUNDED_RECT, LayerType.VECTOR_ROUNDED_TRIANGLE,
                        LayerType.VECTOR_CUT_CORNER_SQUARE, LayerType.VECTOR_ARROW, LayerType.VECTOR_DOUBLE_ARROW
                    )
                    if (usesCornerRadius) {
                        val labelText = when (selectedLayer.type) {
                            LayerType.VECTOR_CUT_CORNER_SQUARE -> "Cut Size"
                            LayerType.VECTOR_ARROW, LayerType.VECTOR_DOUBLE_ARROW -> "Head Size"
                            else -> "Radius"
                        }
                        val maxVal = if (selectedLayer.type in listOf(LayerType.VECTOR_ARROW, LayerType.VECTOR_DOUBLE_ARROW)) 300f else 250f
                        Text("Corners & Aesthetics", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        PrecisionJogWheel(
                            value = selectedLayer.cornerRadius,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(cornerRadius = it)) },
                            valueRange = 0f..maxVal,
                            label = labelText,
                            isInt = true,
                            testTag = "shape_radius_slider"
                        )
                    }

                    // 2. Structural Edges / Points / Turns / Frequency / Alignment
                    val usesSidesOrEdges = selectedLayer.type in listOf(
                        LayerType.VECTOR_TRIANGLE, LayerType.VECTOR_PENTAGON, LayerType.VECTOR_HEXAGON,
                        LayerType.VECTOR_STAR, LayerType.VECTOR_GEAR, LayerType.VECTOR_POLYGON,
                        LayerType.VECTOR_SPIRAL, LayerType.VECTOR_WAVE, LayerType.VECTOR_BLOB,
                        LayerType.VECTOR_TRAPEZOID
                    )
                    if (usesSidesOrEdges) {
                        val labelText = when (selectedLayer.type) {
                            LayerType.VECTOR_STAR -> "Points"
                            LayerType.VECTOR_GEAR -> "Teeth"
                            LayerType.VECTOR_SPIRAL -> "Coils / Turns"
                            LayerType.VECTOR_WAVE -> "Wave Frequency"
                            LayerType.VECTOR_BLOB -> "Sectors (Detail)"
                            LayerType.VECTOR_TRAPEZOID -> "Alignment (0=Ctr,1=L,2=R)"
                            else -> "Sides Count"
                        }
                        val minVal = if (selectedLayer.type == LayerType.VECTOR_SPIRAL) 1f else if (selectedLayer.type == LayerType.VECTOR_TRAPEZOID) 0f else 3f
                        val maxVal = if (selectedLayer.type == LayerType.VECTOR_TRAPEZOID) 2f else 24f
                        val activeEdges = if (selectedLayer.type == LayerType.VECTOR_TRIANGLE && selectedLayer.polygonEdges == 5) 3 else selectedLayer.polygonEdges
                        Text("Structural Settings", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        PrecisionJogWheel(
                            value = activeEdges.toFloat(),
                            onValueChange = { onUpdateLayer(selectedLayer.copy(polygonEdges = it.toInt())) },
                            valueRange = minVal..maxVal,
                            label = labelText,
                            isInt = true,
                            testTag = "shape_sides_slider"
                        )
                    }

                    // 3. Primary Parameter Ratio Slider (starInnerRadiusRatio)
                    val usesInnerRadiusRatio = selectedLayer.type in listOf(
                        LayerType.VECTOR_STAR, LayerType.VECTOR_CROSS, LayerType.VECTOR_RING,
                        LayerType.VECTOR_CRESCENT, LayerType.VECTOR_PIE_SLICE, LayerType.VECTOR_RING_SEGMENT,
                        LayerType.VECTOR_CROSSHAIR, LayerType.VECTOR_TILTED_RECT, LayerType.VECTOR_TRAPEZOID,
                        LayerType.VECTOR_CONTAINER, LayerType.VECTOR_ARROW, LayerType.VECTOR_DOUBLE_ARROW,
                        LayerType.VECTOR_SPEECH_BUBBLE, LayerType.VECTOR_BRACKETS, LayerType.VECTOR_SPIRAL,
                        LayerType.VECTOR_WAVE, LayerType.VECTOR_BLOB
                    )
                    if (usesInnerRadiusRatio) {
                        val labelText = when (selectedLayer.type) {
                            LayerType.VECTOR_CROSS -> "Arm Thickness"
                            LayerType.VECTOR_CRESCENT -> "Inner Arc Ratio"
                            LayerType.VECTOR_GEAR -> "Hole Ratio"
                            LayerType.VECTOR_TILTED_RECT -> "Tilt Angle Ratio"
                            LayerType.VECTOR_TRAPEZOID -> "Top Width Ratio"
                            LayerType.VECTOR_CONTAINER -> "Header Ratio"
                            LayerType.VECTOR_PIE_SLICE, LayerType.VECTOR_RING_SEGMENT -> "Sweep Angle Ratio"
                            LayerType.VECTOR_ARROW, LayerType.VECTOR_DOUBLE_ARROW -> "Shaft Thickness Ratio"
                            LayerType.VECTOR_SPEECH_BUBBLE -> "Tail Length Ratio"
                            LayerType.VECTOR_BRACKETS -> "Bracket Thickness"
                            LayerType.VECTOR_SPIRAL -> "Growth Expansion"
                            LayerType.VECTOR_WAVE -> "Wave Amplitude"
                            LayerType.VECTOR_BLOB -> "Random Wobble"
                            else -> "Inner Ratio"
                        }
                        val minVal = if (selectedLayer.type == LayerType.VECTOR_TILTED_RECT || selectedLayer.type == LayerType.VECTOR_BLOB) 0f else 0.05f
                        val maxVal = if (selectedLayer.type == LayerType.VECTOR_CROSS) 0.9f else if (selectedLayer.type == LayerType.VECTOR_GEAR) 0.5f else if (selectedLayer.type == LayerType.VECTOR_BLOB) 0.4f else 0.95f
                        Text("Primary Parameter", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        PrecisionJogWheel(
                            value = selectedLayer.starInnerRadiusRatio,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(starInnerRadiusRatio = it)) },
                            valueRange = minVal..maxVal,
                            label = labelText,
                            isInt = false,
                            testTag = "shape_star_ratio_slider"
                        )
                    }

                    // 4. Secondary Parameter Slider (skewX)
                    val usesSkewX = selectedLayer.type in listOf(
                        LayerType.VECTOR_PIE_SLICE, LayerType.VECTOR_RING_SEGMENT,
                        LayerType.VECTOR_SPEECH_BUBBLE, LayerType.VECTOR_WAVE, LayerType.VECTOR_BLOB
                    )
                    if (usesSkewX) {
                        val labelText = when (selectedLayer.type) {
                            LayerType.VECTOR_PIE_SLICE, LayerType.VECTOR_RING_SEGMENT -> "Start Angle (deg)"
                            LayerType.VECTOR_SPEECH_BUBBLE -> "Tail Width Ratio"
                            LayerType.VECTOR_WAVE -> "Phase Shift (deg)"
                            LayerType.VECTOR_BLOB -> "Random Seed"
                            else -> "Slant X Factor"
                        }
                        val minVal = if (selectedLayer.type == LayerType.VECTOR_WAVE) -180f else 0f
                        val maxVal = if (selectedLayer.type in listOf(LayerType.VECTOR_PIE_SLICE, LayerType.VECTOR_RING_SEGMENT)) 360f else if (selectedLayer.type == LayerType.VECTOR_WAVE) 180f else if (selectedLayer.type == LayerType.VECTOR_SPEECH_BUBBLE) 0.5f else 100f
                        Text("Secondary Parameter", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        PrecisionJogWheel(
                            value = selectedLayer.skewX,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(skewX = it)) },
                            valueRange = minVal..maxVal,
                            label = labelText,
                            isInt = false,
                            testTag = "shape_skew_slider"
                        )
                    }

                    // 5. Tertiary Parameter Slider (skewY)
                    val usesSkewY = selectedLayer.type in listOf(
                        LayerType.VECTOR_PIE_SLICE, LayerType.VECTOR_RING_SEGMENT, LayerType.VECTOR_ARROW
                    )
                    if (usesSkewY) {
                        val labelText = when (selectedLayer.type) {
                            LayerType.VECTOR_PIE_SLICE, LayerType.VECTOR_RING_SEGMENT -> "Inner Radius Ratio"
                            LayerType.VECTOR_ARROW -> "Shaft Curvature"
                            else -> "Slant Y Factor"
                        }
                        val minVal = if (selectedLayer.type == LayerType.VECTOR_ARROW) -1f else 0f
                        val maxVal = if (selectedLayer.type == LayerType.VECTOR_ARROW) 1f else 0.95f
                        Text("Tertiary Parameter", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        PrecisionJogWheel(
                            value = selectedLayer.skewY,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(skewY = it)) },
                            valueRange = minVal..maxVal,
                            label = labelText,
                            isInt = false,
                            testTag = "shape_skew_y_slider"
                        )
                    }

                    if (selectedLayer.type == LayerType.VECTOR_BEZIER) {
                        Spacer(Modifier.height(4.dp))
                        Divider(color = HighslateOutline.copy(alpha = 0.3f), thickness = 0.5.dp)
                        Text("Bézier Vector Path Actions", style = Typography.labelSmall, fontSize = 10.sp, color = EnergeticYellow, fontWeight = FontWeight.Bold)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isClosed = selectedLayer.polygonEdges == 1
                            Button(
                                onClick = { onUpdateLayer(selectedLayer.copy(polygonEdges = 0)) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (!isClosed) IndustrialAmber else MidSlate),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp).testTag("bezier_path_open")
                            ) {
                                Text("Open Path", style = Typography.labelSmall, fontSize = 9.sp, color = if (!isClosed) DarkOnyx else TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onUpdateLayer(selectedLayer.copy(polygonEdges = 1)) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isClosed) IndustrialAmber else MidSlate),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp).testTag("bezier_path_closed")
                            ) {
                                Text("Closed Path", style = Typography.labelSmall, fontSize = 9.sp, color = if (isClosed) DarkOnyx else TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { 
                                    val smoothed = smoothBezierPoints(selectedLayer.brushPoints)
                                    onUpdateLayer(selectedLayer.copy(brushPoints = smoothed))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp).testTag("bezier_auto_smooth")
                            ) {
                                Text("Auto-Smooth", style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { 
                                    val collapsed = collapseBezierHandles(selectedLayer.brushPoints)
                                    onUpdateLayer(selectedLayer.copy(brushPoints = collapsed))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp).testTag("bezier_collapse_handles")
                            ) {
                                Text("Collapse Handles", style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { 
                                    val pulled = pullOutBezierHandles(selectedLayer.brushPoints)
                                    onUpdateLayer(selectedLayer.copy(brushPoints = pulled))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp).testTag("bezier_pull_handles")
                            ) {
                                Text("Pull Handles", style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { 
                                    val simplified = simplifyBezierPoints(selectedLayer.brushPoints, tolerance = 8f)
                                    onUpdateLayer(selectedLayer.copy(brushPoints = simplified))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f).height(28.dp).testTag("bezier_simplify_path")
                            ) {
                                Text("Simplify Path", style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "💡 Tap & drag any node on the canvas to draw. Double-tap any node/handle to toggle between SMOOTH and CORNER handle symmetry!",
                            style = Typography.bodySmall,
                            fontSize = 8.5.sp,
                            color = TextSecondary,
                            lineHeight = 11.sp
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Divider(color = HighslateOutline.copy(alpha = 0.3f), thickness = 0.5.dp)

                    Text("Rendering Mode", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isFilled = selectedLayer.strokeThickness <= 0f
                        Button(
                            onClick = { onUpdateLayer(selectedLayer.copy(strokeThickness = -1f)) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isFilled) IndustrialAmber else MidSlate),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(28.dp).testTag("shape_mode_fill")
                        ) {
                            Text("Solid Fill", style = Typography.labelSmall, fontSize = 9.sp, color = if (isFilled) DarkOnyx else TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { 
                                val currentStroke = if (selectedLayer.strokeThickness > 0f) selectedLayer.strokeThickness else 4f
                                onUpdateLayer(selectedLayer.copy(strokeThickness = currentStroke)) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (!isFilled) IndustrialAmber else MidSlate),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(28.dp).testTag("shape_mode_stroke")
                        ) {
                            Text("Outline Stroke", style = Typography.labelSmall, fontSize = 9.sp, color = if (!isFilled) DarkOnyx else TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (selectedLayer.strokeThickness > 0f) {
                        PrecisionJogWheel(
                            value = selectedLayer.strokeThickness,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(strokeThickness = it)) },
                            valueRange = 1f..60f,
                            label = "Thickness",
                            isInt = true,
                            testTag = "shape_stroke_slider"
                        )
                    }

                    val filters = com.aistudio.zenithstudio.rpxwtq.EffectStackManager.getFiltersForLayer(selectedLayer.id)
                    val filter3D = filters.find { it.id == "3d_raster_extrude" }
                    if (filter3D != null) {
                        Spacer(Modifier.height(10.dp))
                        Divider(color = HighslateOutline.copy(alpha = 0.5f), thickness = 0.5.dp)
                        
                        val texPath = com.aistudio.zenithstudio.rpxwtq.EffectStackManager.texturesByLayer[selectedLayer.id]
                        val texBmp = remember(texPath) {
                            if (texPath != null) {
                                try {
                                    android.graphics.BitmapFactory.decodeFile(texPath)?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            } else null
                        }
                        
                        val props = remember(filter3D, texBmp) {
                            com.aistudio.zenithstudio.rpxwtq.ui.canvas.get3DPropertiesFromFilter(filter3D, texBmp)
                        }
                        
                        com.aistudio.zenithstudio.rpxwtq.ui.canvas.Comprehensive3DPropertySheet(
                            initialProperties = props,
                            onPropertiesChanged = { updatedProps ->
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Rotation X", updatedProps.rotationX)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Rotation Y", updatedProps.rotationY)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Rotation Z", updatedProps.rotationZ)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Extrusion Depth", updatedProps.extrusionDepth)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Bevel Radius", updatedProps.bevelRadius)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Bevel Segments", updatedProps.bevelSegments.toFloat())
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Specular Intensity", updatedProps.specularIntensity)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Roughness", updatedProps.roughness)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Ambient Occlusion", updatedProps.ambientOcclusion)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Metallic", updatedProps.metallic)
                                
                                val typeVal = when(updatedProps.lightType) {
                                    com.aistudio.zenithstudio.rpxwtq.ui.canvas.LightSourceType.FLAT -> 0f
                                    com.aistudio.zenithstudio.rpxwtq.ui.canvas.LightSourceType.DIRECTIONAL -> 1f
                                    com.aistudio.zenithstudio.rpxwtq.ui.canvas.LightSourceType.POINT -> 2f
                                }
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Light Type", typeVal)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Light Azimuth", updatedProps.lightAzimuth)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Light Elevation", updatedProps.lightElevation)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Light Intensity", updatedProps.lightIntensity)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Light Color R", updatedProps.lightColor.red * 255f)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Light Color G", updatedProps.lightColor.green * 255f)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Light Color B", updatedProps.lightColor.blue * 255f)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "UV Scale X", updatedProps.uvScaleX)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "UV Scale Y", updatedProps.uvScaleY)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "UV Offset X", updatedProps.uvOffsetX)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "UV Offset Y", updatedProps.uvOffsetY)
                                com.aistudio.zenithstudio.rpxwtq.EffectStackManager.updateParameter("3d_raster_extrude", "Use Texture", if (updatedProps.textureMaterial != null) 1.0f else 0.0f)
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131317), RoundedCornerShape(8.dp))
                        .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("S", fontSize = 28.sp, color = EnergeticYellow)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Vector Shape Layer Required",
                        style = Typography.labelSmall,
                        color = EnergeticYellow,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Select a shape layer (Rectangle, Circle, Star, Polygon) from your layer stack to edit its radius, sides, inner ratio and outlines here.",
                        style = Typography.labelSmall,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FiltersAndFxDetailView(
    selectedLayer: StudioLayer,
    selectedEffectIndex: Int,
    onSelectEffectIndex: (Int) -> Unit,
    onRemoveEffect: (String) -> Unit,
    onDuplicateEffect: (String) -> Unit = {},
    onToggleEffectEnabled: (String) -> Unit,
    onUpdateEffectParam: (String, String, Float) -> Unit,
    onOpenEffectsGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkOnyx)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Layer FX Stack",
                    color = TextPrimary,
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedLayer.effects.size} active effects on [${selectedLayer.name}]",
                    color = TextSecondary,
                    style = Typography.labelSmall,
                    fontSize = 10.sp
                )
            }
            
            Button(
                onClick = onOpenEffectsGallery,
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Effect", tint = DarkOnyx, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Effect", style = Typography.labelSmall, color = DarkOnyx, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = HighslateOutline, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(12.dp))

        if (selectedLayer.effects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MidSlate, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, HighslateOutline), RoundedCornerShape(8.dp))
                    .clickable { onOpenEffectsGallery() }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add FX",
                        tint = IndustrialAmber,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "No Effects Applied",
                        color = TextPrimary,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap to browse our non-destructive effects library, featuring 120 FPS Chromatic Aberration, Glitch, Neons, Blurs, and Artistic Filters.",
                        color = TextSecondary,
                        style = Typography.labelSmall,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedLayer.effects.size) { index ->
                    val effect = selectedLayer.effects[index]
                    val isSelected = index == selectedEffectIndex
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectEffectIndex(if (isSelected) -1 else index)
                            },
                        color = if (isSelected) MidSlate else Color(0xFF1B1E24),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) IndustrialAmber else HighslateOutline
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            // Effect Row Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onToggleEffectEnabled(effect.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (effect.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Effect",
                                            tint = if (effect.isEnabled) IndustrialAmber else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = effect.name,
                                        color = if (effect.isEnabled) TextPrimary else TextSecondary,
                                        style = Typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (effect.parameters.isNotEmpty() && !isSelected) {
                                        Text(
                                            text = "EDIT",
                                            color = IndustrialAmber,
                                            style = Typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveEffect(effect.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Effect",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Collapsible Sliders for Parameters
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = HighslateOutline, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (effect.parameters.isEmpty()) {
                                    Text(
                                        text = "Static effect (no adjustable parameters).",
                                        color = TextSecondary,
                                        style = Typography.labelSmall,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        effect.parameters.forEach { (pName, param) ->
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = param.name,
                                                            color = TextPrimary,
                                                            style = Typography.labelSmall,
                                                            fontSize = 11.sp
                                                        )
                                                        if (param.value != 0f) {
                                                            Icon(
                                                                imageVector = Icons.Default.Refresh,
                                                                contentDescription = "Reset",
                                                                tint = TextSecondary,
                                                                modifier = Modifier
                                                                    .size(12.dp)
                                                                    .clickable {
                                                                        onUpdateEffectParam(effect.id, pName, 0f)
                                                                    }
                             )
                                                        }
                                                    }
                                                    
                                                    Text(
                                                        text = "${"%.2f".format(param.value)} ${param.unit}".trim(),
                                                        color = EnergeticYellow,
                                                        style = Typography.labelSmall,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                
                                                Slider(
                                                    value = param.value,
                                                    valueRange = param.rangeMin..param.rangeMax,
                                                    onValueChange = { newValue ->
                                                        onUpdateEffectParam(effect.id, pName, newValue)
                                                    },
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = IndustrialAmber,
                                                        activeTrackColor = IndustrialAmber,
                                                        inactiveTrackColor = HighslateOutline
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(24.dp)
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

@Composable
fun RasterExtrude3DDetailView(
    selectedLayer: StudioLayer,
    onUpdateEffectParam: (String, String, Float) -> Unit,
    onAddEffect: (StudioEffect) -> Unit,
    modifier: Modifier = Modifier
) {
    val electricCyan = Color(0xFF00E5FF)
    val extrudeEffect = remember(selectedLayer.effects) {
        selectedLayer.effects.find { it is StudioEffect.PhotoshopEffect && it.effectType == "RasterExtrude" } as? StudioEffect.PhotoshopEffect
    }

    LaunchedEffect(extrudeEffect) {
        if (extrudeEffect == null) {
            val newEffect = com.example.studio.model.PhotoshopEffectTemplates.create(effectType = "RasterExtrude")
            onAddEffect(newEffect)
        }
    }

    if (extrudeEffect == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = electricCyan)
        }
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Configure 3D Depth, angles and rotation directly on the active canvas layer in real-time.",
            style = Typography.labelSmall,
            color = TextSecondary
        )

        // 1. Extrusion Depth
        val depthParam = extrudeEffect.parameters["ExtrusionDepth"] ?: com.example.studio.model.EffectParameter("Extrusion Depth", 20f, 0f, 200f)
        PrecisionJogWheel(
            value = depthParam.value,
            onValueChange = { onUpdateEffectParam(extrudeEffect.id, "ExtrusionDepth", it) },
            valueRange = depthParam.rangeMin..depthParam.rangeMax,
            label = "Depth",
            isInt = true,
            highFreqKey = "${selectedLayer.id}_extrude_depth",
            activeColorOverride = electricCyan
        )

        // 2. Alpha (Orientation)
        val alphaParam = extrudeEffect.parameters["Alpha"] ?: com.example.studio.model.EffectParameter("Orientation Alpha", 57f, -180f, 180f)
        PrecisionJogWheel(
            value = alphaParam.value,
            onValueChange = { onUpdateEffectParam(extrudeEffect.id, "Alpha", it) },
            valueRange = alphaParam.rangeMin..alphaParam.rangeMax,
            label = "Alpha",
            isInt = true,
            highFreqKey = "${selectedLayer.id}_extrude_alpha",
            activeColorOverride = electricCyan
        )

        // 3. Beta (Orientation)
        val betaParam = extrudeEffect.parameters["Beta"] ?: com.example.studio.model.EffectParameter("Orientation Beta", 0f, -180f, 180f)
        PrecisionJogWheel(
            value = betaParam.value,
            onValueChange = { onUpdateEffectParam(extrudeEffect.id, "Beta", it) },
            valueRange = betaParam.rangeMin..betaParam.rangeMax,
            label = "Beta",
            isInt = true,
            highFreqKey = "${selectedLayer.id}_extrude_beta",
            activeColorOverride = electricCyan
        )

        // 4. RotX
        val rotXParam = extrudeEffect.parameters["RotX"] ?: com.example.studio.model.EffectParameter("Rotation X", 0f, -180f, 180f)
        PrecisionJogWheel(
            value = rotXParam.value,
            onValueChange = { onUpdateEffectParam(extrudeEffect.id, "RotX", it) },
            valueRange = rotXParam.rangeMin..rotXParam.rangeMax,
            label = "Rot X",
            isInt = true,
            highFreqKey = "${selectedLayer.id}_extrude_rot_x",
            activeColorOverride = electricCyan
        )

        // 5. RotY
        val rotYParam = extrudeEffect.parameters["RotY"] ?: com.example.studio.model.EffectParameter("Rotation Y", 39f, -180f, 180f)
        PrecisionJogWheel(
            value = rotYParam.value,
            onValueChange = { onUpdateEffectParam(extrudeEffect.id, "RotY", it) },
            valueRange = rotYParam.rangeMin..rotYParam.rangeMax,
            label = "Rot Y",
            isInt = true,
            highFreqKey = "${selectedLayer.id}_extrude_rot_y",
            activeColorOverride = electricCyan
        )

        // 6. RotZ
        val rotZParam = extrudeEffect.parameters["RotZ"] ?: com.example.studio.model.EffectParameter("Rotation Z", 0f, -180f, 180f)
        PrecisionJogWheel(
            value = rotZParam.value,
            onValueChange = { onUpdateEffectParam(extrudeEffect.id, "RotZ", it) },
            valueRange = rotZParam.rangeMin..rotZParam.rangeMax,
            label = "Rot Z",
            isInt = true,
            highFreqKey = "${selectedLayer.id}_extrude_rot_z",
            activeColorOverride = electricCyan
        )
    }
}
