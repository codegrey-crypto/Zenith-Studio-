package com.example.studio.ui

import androidx.compose.foundation.*
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
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }

    // 1. High-speed local state allowing dragging to be extremely responsive at 120 FPS
    var localValue by remember(value, highFreqKey) { mutableStateOf(SlidersHighFreqState.get(highFreqKey ?: label, value)) }
    val coroutineScope = rememberCoroutineScope()
    var pendingUpdateJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val isColorGradingFilter = label == "Exposure" || label == "Contrast" || label == "Highlights" || label == "Shadows" || label == "Whites" || label == "Blacks" || label == "Temp" || label == "Tint" || label == "Vibrance" || label == "Saturation" || label == "Clarity" || label == "Dehaze" || label == "Brightness" || label == "Sat" || label == "Bright" || label == "Opacity"
    
    // Style active filter tracking sliders, thumb controls, and numeric value tracks in Vibrant Neon Purple (#A855F7).
    val activeColor = if (isColorGradingFilter) Color(0xFFA855F7) else EnergeticYellow

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
        Slider(
            value = localValue.coerceIn(valueRange),
            onValueChange = { newValue ->
                // Quantize outputs to 2 decimal places
                val rawQuantized = (newValue * 100f).roundToInt() / 100f
                val validatedValue = if (isInt) kotlin.math.round(rawQuantized) else rawQuantized
                
                localValue = validatedValue
                
                // Track value in central SlidersHighFreqState immediately
                SlidersHighFreqState.set(highFreqKey ?: label, validatedValue)
            },
            onValueChangeFinished = {
                onValueChange(localValue)
            },
            valueRange = valueRange.start..valueRange.endInclusive,
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
                var editBuf by remember {
                    mutableStateOf(if (isInt) localValue.toInt().toString() else "%.2f".format(localValue))
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
                val displayVal = if (valueFormatter != null) {
                    valueFormatter(localValue)
                } else if (isInt) {
                    "${localValue.toInt()}"
                } else {
                    "%.2f".format(localValue)
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
    onUpdateLayer: (StudioLayer) -> Unit
) {
    val detailScrollState = rememberScrollState()
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
                .background(Color(0xFF131317), RoundedCornerShape(8.dp))
                .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Geometric Coordinates & Size", style = Typography.labelSmall, color = EnergeticYellow, fontWeight = FontWeight.Bold)
            
            PrecisionJogWheel(
                value = selectedLayer.positionX,
                onValueChange = { onUpdateLayer(selectedLayer.copy(positionX = it)) },
                valueRange = -5000f..5000f,
                label = "Pos-X",
                isInt = true,
                testTag = "transform_pos_x_slider"
            )

            PrecisionJogWheel(
                value = selectedLayer.positionY,
                onValueChange = { onUpdateLayer(selectedLayer.copy(positionY = it)) },
                valueRange = -5000f..5000f,
                label = "Pos-Y",
                isInt = true,
                testTag = "transform_pos_y_slider"
            )

            PrecisionJogWheel(
                value = selectedLayer.rotation,
                onValueChange = { onUpdateLayer(selectedLayer.copy(rotation = it)) },
                valueRange = -180f..180f,
                label = "Rotation",
                isInt = true,
                testTag = "transform_rotation_slider"
            )

            PrecisionJogWheel(
                value = selectedLayer.width,
                onValueChange = { onUpdateLayer(selectedLayer.copy(width = it)) },
                valueRange = 1f..5000f,
                label = "Width",
                isInt = true,
                testTag = "transform_width_slider"
            )

            PrecisionJogWheel(
                value = selectedLayer.height,
                onValueChange = { onUpdateLayer(selectedLayer.copy(height = it)) },
                valueRange = 1f..5000f,
                label = "Height",
                isInt = true,
                testTag = "transform_height_slider"
            )
        }

        // Section 2: Pivot
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131317), RoundedCornerShape(8.dp))
                .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Rotational Anchor & Pivot Axis", style = Typography.labelSmall, color = EnergeticYellow, fontWeight = FontWeight.Bold)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrecisionJogWheel(
                        value = selectedLayer.pivotX,
                        onValueChange = { onUpdateLayer(selectedLayer.copy(pivotX = it)) },
                        valueRange = 0f..1f,
                        label = "Pivot X",
                        isInt = false,
                        testTag = "transform_pivot_x_slider"
                    )
                    Spacer(Modifier.height(4.dp))
                    PrecisionJogWheel(
                        value = selectedLayer.pivotY,
                        onValueChange = { onUpdateLayer(selectedLayer.copy(pivotY = it)) },
                        valueRange = 0f..1f,
                        label = "Pivot Y",
                        isInt = false,
                        testTag = "transform_pivot_y_slider"
                    )
                }

                Spacer(Modifier.width(4.dp))

                Column(
                    modifier = Modifier
                        .background(Color(0xFF181822), RoundedCornerShape(6.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("QUICK ANCHORS", style = Typography.labelSmall, fontSize = 7.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    for (row in 0..2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (col in 0..2) {
                                val px = col * 0.5f
                                val py = row * 0.5f
                                val isSelected = Math.abs(selectedLayer.pivotX - px) < 0.1f && Math.abs(selectedLayer.pivotY - py) < 0.1f
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isSelected) EnergeticYellow else MidSlate,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            onUpdateLayer(selectedLayer.copy(pivotX = px, pivotY = py))
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
                .background(Color(0xFF131317), RoundedCornerShape(8.dp))
                .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Perspective & Skew 3D Effects", style = Typography.labelSmall, color = EnergeticYellow, fontWeight = FontWeight.Bold)
            
            PrecisionJogWheel(
                value = selectedLayer.skewX,
                onValueChange = { onUpdateLayer(selectedLayer.copy(skewX = it)) },
                valueRange = -1.5f..1.5f,
                label = "Skew-X",
                isInt = false,
                testTag = "transform_skew_x_slider"
            )

            PrecisionJogWheel(
                value = selectedLayer.skewY,
                onValueChange = { onUpdateLayer(selectedLayer.copy(skewY = it)) },
                valueRange = -1.5f..1.5f,
                label = "Skew-Y",
                isInt = false,
                testTag = "transform_skew_y_slider"
            )

            PrecisionJogWheel(
                value = selectedLayer.perspX,
                onValueChange = { onUpdateLayer(selectedLayer.copy(perspX = it)) },
                valueRange = -0.005f..0.005f,
                label = "PerspX",
                isInt = false,
                testTag = "transform_persp_x_slider"
            )

            PrecisionJogWheel(
                value = selectedLayer.perspY,
                onValueChange = { onUpdateLayer(selectedLayer.copy(perspY = it)) },
                valueRange = -0.005f..0.005f,
                label = "PerspY",
                isInt = false,
                testTag = "transform_persp_y_slider"
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
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1.1f).height(28.dp)
                ) {
                    Text("Reset All", style = Typography.labelSmall, fontSize = 9.sp, color = DarkOnyx, fontWeight = FontWeight.Bold)
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
        } else {
            val isShapeLayer = selectedLayer.type in listOf(
                LayerType.VECTOR_RECT, LayerType.VECTOR_CIRCLE, LayerType.VECTOR_STAR,
                LayerType.VECTOR_TRIANGLE, LayerType.VECTOR_PENTAGON, LayerType.VECTOR_HEXAGON,
                LayerType.VECTOR_OVAL, LayerType.VECTOR_LINE, LayerType.VECTOR_BEZIER
            )
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

                    val canHaveCornerRadius = selectedLayer.type in listOf(
                        LayerType.VECTOR_RECT, LayerType.VECTOR_TRIANGLE, LayerType.VECTOR_PENTAGON,
                        LayerType.VECTOR_HEXAGON, LayerType.VECTOR_STAR, LayerType.VECTOR_BEZIER
                    )
                    if (canHaveCornerRadius) {
                        Text("Corners Aesthetics", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        PrecisionJogWheel(
                            value = selectedLayer.cornerRadius,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(cornerRadius = it)) },
                            valueRange = 0f..250f,
                            label = "Radius",
                            isInt = true,
                            testTag = "shape_radius_slider"
                        )
                    }

                    val hasEdges = selectedLayer.type in listOf(LayerType.VECTOR_TRIANGLE, LayerType.VECTOR_PENTAGON, LayerType.VECTOR_HEXAGON, LayerType.VECTOR_STAR)
                    if (hasEdges) {
                        Text("Structural Edges & Points", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        val labelText = if (selectedLayer.type == LayerType.VECTOR_STAR) "Points" else "Sides"
                        val activeEdges = if (selectedLayer.type == LayerType.VECTOR_TRIANGLE && selectedLayer.polygonEdges == 5) 3 else selectedLayer.polygonEdges
                        PrecisionJogWheel(
                            value = activeEdges.toFloat(),
                            onValueChange = { onUpdateLayer(selectedLayer.copy(polygonEdges = it.toInt())) },
                            valueRange = 3f..20f,
                            label = labelText,
                            isInt = true,
                            testTag = "shape_sides_slider"
                        )
                    }

                    if (selectedLayer.type == LayerType.VECTOR_STAR) {
                        PrecisionJogWheel(
                            value = selectedLayer.starInnerRadiusRatio,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(starInnerRadiusRatio = it)) },
                            valueRange = 0.05f..0.95f,
                            label = "Inner Ratio",
                            isInt = false,
                            testTag = "shape_star_ratio_slider"
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
    onToggleEffectEnabled: (String) -> Unit,
    onUpdateEffectParam: (String, String, Float) -> Unit,
    onOpenEffectsGallery: () -> Unit
) {
    val detailScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(detailScrollState)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131317), RoundedCornerShape(8.dp))
                .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Active Effects Chain", style = Typography.labelSmall, color = EnergeticYellow, fontWeight = FontWeight.Bold)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenEffectsGallery,
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(34.dp).testTag("detail_add_effect_button")
                ) {
                    Icon(Icons.Default.Add, "Add Effect", tint = DarkOnyx, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", style = Typography.labelSmall, color = DarkOnyx, fontWeight = FontWeight.Bold)
                }

                selectedLayer.effects.forEachIndexed { index, eff ->
                    val isEffSelected = index == selectedEffectIndex
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isEffSelected) IndustrialAmber.copy(0.15f) else MidSlate)
                            .border(BorderStroke(1.dp, if (isEffSelected) IndustrialAmber else HighslateOutline), RoundedCornerShape(6.dp))
                            .clickable { onSelectEffectIndex(index) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("detail_effect_item_$index"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggleEffectEnabled(eff.id) },
                            modifier = Modifier.size(20.dp).testTag("effect_toggle_visibility_${eff.id}")
                        ) {
                            Icon(
                                imageVector = if (eff.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Visibility of ${eff.name}",
                                tint = if (eff.isEnabled) IndustrialAmber else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = eff.name,
                            style = Typography.labelSmall.copy(
                                textDecoration = if (eff.isEnabled) null else androidx.compose.ui.text.style.TextDecoration.LineThrough
                            ),
                            fontSize = 11.sp,
                            color = if (eff.isEnabled) TextPrimary else TextSecondary
                        )
                        IconButton(
                            onClick = { onRemoveEffect(eff.id) },
                            modifier = Modifier.size(20.dp).testTag("effect_remove_${eff.id}")
                        ) {
                            Icon(Icons.Default.Close, "Remove fx", tint = Color.Red, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        val actEff = selectedLayer.effects.getOrNull(selectedEffectIndex)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131317), RoundedCornerShape(8.dp))
                .border(BorderStroke(0.5.dp, HighslateOutline), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            if (actEff != null) {
                Text("Modify Parameter: ${actEff.name}", style = Typography.labelSmall, color = EnergeticYellow)
                Spacer(Modifier.height(8.dp))

                val categories = if (actEff is StudioEffect.PhotoshopEffect && actEff.effectType == "ColorGrading") {
                    listOf(
                        "LUT Presets" to listOf("Preset", "Intensity"),
                        "Basic Tonal Adjustments" to listOf("Exposure", "Contrast", "Highlights", "Shadows", "Whites", "Blacks"),
                        "Color & White Balance" to listOf("Temperature", "Tint", "Vibrance", "Saturation"),
                        "Detail & Presence" to listOf("Clarity", "Texture", "Sharpening", "SharpeningRadius", "SharpeningMasking", "Dehaze"),
                        "HSL: Red Channel" to listOf("HSL_Red_Hue", "HSL_Red_Sat", "HSL_Red_Lum"),
                        "HSL: Orange Channel" to listOf("HSL_Orange_Hue", "HSL_Orange_Sat", "HSL_Orange_Lum"),
                        "HSL: Yellow Channel" to listOf("HSL_Yellow_Hue", "HSL_Yellow_Sat", "HSL_Yellow_Lum"),
                        "HSL: Green Channel" to listOf("HSL_Green_Hue", "HSL_Green_Sat", "HSL_Green_Lum"),
                        "HSL: Aqua Channel" to listOf("HSL_Aqua_Hue", "HSL_Aqua_Sat", "HSL_Aqua_Lum"),
                        "HSL: Blue Channel" to listOf("HSL_Blue_Hue", "HSL_Blue_Sat", "HSL_Blue_Lum"),
                        "HSL: Purple Channel" to listOf("HSL_Purple_Hue", "HSL_Purple_Sat", "HSL_Purple_Lum"),
                        "HSL: Magenta Channel" to listOf("HSL_Magenta_Hue", "HSL_Magenta_Sat", "HSL_Magenta_Lum"),
                        "Technical Correction" to listOf("NoiseLuminance", "NoiseColor", "Vignetting", "Grain")
                    )
                } else if (actEff is StudioEffect.PhotoshopEffect && actEff.effectType == "CameraRaw") {
                    listOf(
                        "Light" to listOf("Exposure", "Contrast", "Highlights", "Shadows", "Whites", "Blacks"),
                        "Color" to listOf("Temp", "Tint", "Vibrance", "Saturation"),
                        "Detail" to listOf("Texture", "Clarity", "Dehaze")
                    )
                } else if (actEff is StudioEffect.PhotoshopEffect && actEff.effectType == "GlassMorphism") {
                    listOf(
                        "Glass Fracture & Torsion" to listOf("FractalIntensity", "FractalType", "EdgeTorsion"),
                        "Optics & Surface Tension" to listOf("RefractionIndex", "SurfaceTension", "Radius")
                    )
                } else if (actEff is StudioEffect.PhotoshopEffect && actEff.effectType == "ReededGlass") {
                    listOf(
                        "Structural Ripples" to listOf("LineDensity", "Rotation"),
                        "Optics & Diffusive Blur" to listOf("RefractionStrength", "BlurMix", "SpecularHighlight")
                    )
                } else if (actEff is StudioEffect.PhotoshopEffect && actEff.effectType == "PixelStretch") {
                    listOf(
                        "Pixel Extraction & Stretch Direction" to listOf("SliceLine", "Orientation"),
                        "Deformation Grid (Warp & Curves)" to listOf("WarpBend", "WarpFrequency"),
                        "Depth & Composition Masking" to listOf("SubjectCutout", "CutoutThreshold")
                    )
                } else if (actEff is StudioEffect.PhotoshopEffect && (
                    actEff.effectType == "ColoredPencil" ||
                    actEff.effectType == "Cutout" ||
                    actEff.effectType == "PlasticWrap" ||
                    actEff.effectType == "FilmGrain" ||
                    actEff.effectType == "BrushStrokes" ||
                    actEff.effectType == "AccentedEdges" ||
                    actEff.effectType == "Crosshatch" ||
                    actEff.effectType == "SumiE" ||
                    actEff.effectType == "OceanRipple" ||
                    actEff.effectType == "Glass" ||
                    actEff.effectType == "BasRelief" ||
                    actEff.effectType == "HalftonePattern" ||
                    actEff.effectType == "Photocopy" ||
                    actEff.effectType == "StainedGlass" ||
                    actEff.effectType == "Craquelure" ||
                    actEff.effectType == "Texturizer"
                )) {
                    listOf(
                        "📐 1. Geometry Control (shape, distortion)" to listOf(
                            "StrokeThickness", "StrokeDirectionBias", "StrokeCurvature", "LineJitterAmount",
                            "RegionSegmentationStrength", "EdgeSimplificationLevel", "ShapeMergingRadius", "ObjectIsolationThreshold",
                            "HighlightStrength", "Detail", "Smoothness", "ShrinkWrapFactor",
                            "GrainDistributionField", "GrainClusteringStrength", "SpatialGrainFlowDirection",
                            "StrokeLengthVariability", "StrokeBreakFrequency", "BrushAngleVariation", "FlowDirectionMapping", "InkDensity",
                            "EdgeWidth", "EdgeScale",
                            "StrokeLength", "LineDensityField", "CrossAngleOffset", "StrokeInterferencePattern", "HatchLayerDepth",
                            "StrokePressure", "StrokeAngle",
                            "RippleSize", "RippleMagnitude", "PhaseOffsetMap", "DirectionalFlowField", "TurbulenceInjection",
                            "Distortion", "SpiralCenterDrift", "RotationGradientMap", "RadialFalloffCurve",
                            "PerspectiveDeformation",
                            "DotGridType", "DotScalingCurve", "SpatialFrequencyMap",
                            "EdgeCollapseStrength", "DocumentFoldSimulation",
                            "CellSize", "BorderThickness", "GridDeformation",
                            "CrackSpacing", "CrackDirectionStressMap", "FracturePropagation",
                            "Scaling", "GridOrientationBias"
                        ),
                        "🎭 2. Tone Control (brightness, contrast)" to listOf(
                            "ContrastCompression", "ShadowLift", "HighlightClamp", "MidtoneBias",
                            "PosterizationLevels", "ShadowFlattening", "HighlightCompression", "DynamicRangeReduction",
                            "ReflectionContrast", "GlossinessIndex",
                            "ExposureNoiseBias", "ShadowGrainEmphasis", "HighlightGrainSuppression", "GammaLinkedGrain", "Amount",
                            "InkLoadSimulation", "DrynessLevel", "PressureFalloffCurve", "InkSaturationDecay",
                            "EdgeBrightness", "BackgroundDarkness",
                            "Contrast", "InkPressureCurve", "ShadowMappingIntensity", "TonalBandSeparation",
                            "DarkArea", "InkFlowLimit",
                            "LuminanceBasedWarp",
                            "BrightnessCompression",
                            "StonePlasterContrast", "HighlightSmoothness",
                            "InkDensityResponse", "ShadowDotExpansion", "HighlightDotSuppression",
                            "ThresholdCurve", "ContrastHardening", "ShadowBlowoutControl",
                            "LightTranslucency", "HighlightIntensity",
                            "CrackDepth", "CrackShadowDepth", "SurfaceAgingCurve",
                            "Relief", "LightDirectionSource"
                        ),
                        "🎨 3. Color Control (grading, saturation)" to listOf(
                            "ColorSaturationBoost", "HueDrift", "PaletteLimiting", "SkinTonePreservation",
                            "PaletteSizeControl", "ColorBandShifting", "ChannelQuantization", "ColorNoiseSuppression",
                            "SpecularColorShift", "SubsurfaceScattering",
                            "ChromaticGrainSeparation", "RGBChannelGrainOffset", "ColorTempNoiseShift",
                            "PigmentMixingStrength", "ColorBleedFactor", "MultiColorStrokeBlending", "HueJitter",
                            "EdgeColorShifting",
                            "InkColorBlendMode", "MultiInkLayerMixing", "ColorTintDrift",
                            "ColorBleeding",
                            "ChromaticAberrationShift",
                            "HueSpiralShift", "ChannelRotationOffset",
                            "CMYKSimulationMode", "ChannelSeparatedDot",
                            "TonerSpreadModel", "BlackInkSaturation",
                            "TileColorAveraging", "ColorVibranceBoost",
                            "OxidationColorShift", "DirtAccumulation"
                        ),
                        "🧫 4. Texture Control (grain, paper, surface)" to listOf(
                            "PaperGrainStrength", "FiberDirection", "PaperRoughnessScale", "FiberContrast",
                            "FlatSurfaceBias", "MicroTextureRetention", "SurfaceUniformity",
                            "SurfaceRoughness", "MicroHighlightDetail",
                            "GrainSizeDistribution", "FilmStockType", "EmulsionLayerDepth",
                            "CanvasRoughness", "BrushFiberSimulation", "PaintDragTexture", "SurfaceAbsorptionRate",
                            "EdgeTextureOverlay",
                            "PaperFiberInteraction", "InkAbsorptionSpread", "BleedDiffusionModel",
                            "RicePaperTexture",
                            "MicroRippleLayering",
                            "SwirlNoiseOverlay", "VortexTurbulenceField",
                            "PlasterGranularity",
                            "PaperScreenType", "PrintingNoiseSimulation",
                            "PaperRollerNoise", "ScanlineArtifacts",
                            "GlassRoughnessOverlay",
                            "MaterialHardnessMap", "SurfaceBrittleness",
                            "FabricDensitySimulation"
                        ),
                        "⚡ 5. Edge Control (sharpness, contour)" to listOf(
                            "EdgeReinforcementStrength", "EdgeBleedControl", "EdgeSofteningRadius",
                            "EdgeHardness", "EdgeGlowSuppression", "EdgeAntiAliasStrength",
                            "BoundaryWrapGlow", "EdgeRefractionStrength",
                            "EdgeGrainReduction", "EdgeNoiseSharpening",
                            "StrokeEdgeFraying", "EdgeBreakupIntensity", "EdgeSofteningCurve",
                            "EdgeDetectionThreshold",
                            "EdgeReinforcementMatrix", "ContourDetectionSensitivity",
                            "WetEdgeDiffusion",
                            "AntiTearBoundary",
                            "EdgeCurlStrength", "BoundaryWarpProtection",
                            "EdgeSculpting",
                            "EdgeDotClustering",
                            "EdgeClippingStrength",
                            "LeadBorderSoftness",
                            "CrackEdgeSharpness", "FractureAntiAliasing"
                        ),
                        "🧪 6. Style Behavior Control (flow, random)" to listOf(
                            "HandTremorSimulation", "StrokeRandomSeed", "StrokeDensityMap", "StrokeOverlapFactor",
                            "RegionRandomizationFactor", "ArtisticAbstractionStrength", "StylizationDrift",
                            "WrinkleFrequency", "RandomWrinkleSeed",
                            "FilmStockRandomSeed", "VintageAgingCurve", "SensorNoiseModelType",
                            "HandMotionNoise", "StrokeClumpingFactor", "RandomStrokeOffset",
                            "StylizationAmount",
                            "ArtistStylePreset", "ScribbleRandomnessEngine", "HandwritingSimModel",
                            "InkSplatterIntensity",
                            "ChaosFactor", "SpiralStabilityIndex",
                            "PrinterModelEmulation", "VintagePrintAging",
                            "ScannerQualityModel", "LowInkSimulation",
                            "ImperfectTileMode",
                            "EnvironmentalWeathering"
                        )
                    )
                } else {
                    listOf("" to actEff.parameters.keys.toList())
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { (catName, keys) ->
                        if (catName.isNotEmpty() && keys.any { actEff.parameters.containsKey(it) }) {
                            Text(
                                text = catName,
                                style = Typography.labelSmall,
                                color = EnergeticYellow,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                        }
                        keys.forEach { pName ->
                            val param = actEff.parameters[pName]
                            if (param != null) {
                                val isPreset = actEff is StudioEffect.PhotoshopEffect && actEff.effectType == "ColorGrading" && pName == "Preset"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PrecisionJogWheel(
                                        value = param.value,
                                        onValueChange = { onUpdateEffectParam(actEff.id, pName, it) },
                                        valueRange = param.rangeMin..param.rangeMax,
                                        highFreqKey = "${selectedLayer.id}_effect_${actEff.id}_${pName}",
                                        label = param.name,
                                        isInt = param.unit.contains("px") || param.unit.contains("%") || isPreset,
                                        valueFormatter = if (isPreset) {
                                            { v ->
                                                val LUTs = listOf("Cinema Golden", "Teal & Orange", "Mono B&W", "Cold Frost", "Dreamy Pastel", "Vintage Sepia", "Acid Neon")
                                                LUTs.getOrNull(v.toInt()) ?: "Preset ${v.toInt()}"
                                            }
                                        } else {
                                            { v -> "${"%.2f".format(v)} ${param.unit}".trim() }
                                        },
                                        testTag = "effect_param_slider_${param.name}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (param.value != 0f) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reset ${param.name}",
                                            tint = IndustrialAmber.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { onUpdateEffectParam(actEff.id, pName, 0f) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Select or Add an Effect to Stack parameters non-destructively", style = Typography.labelSmall, textAlign = TextAlign.Center, color = TextSecondary)
                }
            }
        }
    }
}
