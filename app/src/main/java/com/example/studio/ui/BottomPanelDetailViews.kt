package com.example.studio.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studio.model.LayerType
import com.example.studio.model.StudioLayer
import com.example.studio.model.StudioEffect
import com.example.ui.theme.Typography
import com.example.ui.theme.*
import java.io.File

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
            
            // Pos-X
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pos-X", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                val valX = selectedLayer.positionX
                Slider(
                    value = valX,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(positionX = it)) },
                    valueRange = -500f..1500f,
                    colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("transform_pos_x_slider")
                )
                val trigger = LocalSliderValueEditTrigger.current
                Text("${valX.toInt()} px", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(60.dp).clickable {
                    trigger?.invoke(SliderValueEditConfig("Position X", valX, -500f..1500f, isInt = true) { onUpdateLayer(selectedLayer.copy(positionX = it)) })
                }, textAlign = TextAlign.End)
            }

            // Pos-Y
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pos-Y", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                val valY = selectedLayer.positionY
                Slider(
                    value = valY,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(positionY = it)) },
                    valueRange = -500f..1500f,
                    colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("transform_pos_y_slider")
                )
                val trigger = LocalSliderValueEditTrigger.current
                Text("${valY.toInt()} px", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(60.dp).clickable {
                    trigger?.invoke(SliderValueEditConfig("Position Y", valY, -500f..1500f, isInt = true) { onUpdateLayer(selectedLayer.copy(positionY = it)) })
                }, textAlign = TextAlign.End)
            }

            // Rotation
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rotation", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                val valRot = selectedLayer.rotation
                Slider(
                    value = valRot,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(rotation = it)) },
                    valueRange = -180f..180f,
                    colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("transform_rotation_slider")
                )
                val trigger = LocalSliderValueEditTrigger.current
                Text("${valRot.toInt()}°", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(60.dp).clickable {
                    trigger?.invoke(SliderValueEditConfig("Rotation", valRot, -180f..180f, isInt = true) { onUpdateLayer(selectedLayer.copy(rotation = it)) })
                }, textAlign = TextAlign.End)
            }

            // Width
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Width", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                val valW = selectedLayer.width
                Slider(
                    value = valW,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(width = it)) },
                    valueRange = 10f..1500f,
                    colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("transform_width_slider")
                )
                val trigger = LocalSliderValueEditTrigger.current
                Text("${valW.toInt()} px", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(60.dp).clickable {
                    trigger?.invoke(SliderValueEditConfig("Width", valW, 10f..1500f, isInt = true) { onUpdateLayer(selectedLayer.copy(width = it)) })
                }, textAlign = TextAlign.End)
            }

            // Height
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Height", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                val valH = selectedLayer.height
                Slider(
                    value = valH,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(height = it)) },
                    valueRange = 10f..1500f,
                    colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("transform_height_slider")
                )
                val trigger = LocalSliderValueEditTrigger.current
                Text("${valH.toInt()} px", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(60.dp).clickable {
                    trigger?.invoke(SliderValueEditConfig("Height", valH, 10f..1500f, isInt = true) { onUpdateLayer(selectedLayer.copy(height = it)) })
                }, textAlign = TextAlign.End)
            }
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
                    // Pivot X
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pivot X", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(44.dp), color = TextSecondary)
                        Slider(
                            value = selectedLayer.pivotX,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(pivotX = it)) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                            modifier = Modifier.weight(1f).height(26.dp).testTag("transform_pivot_x_slider")
                        )
                        Text(String.format("%.2f", selectedLayer.pivotX), style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                    }

                    // Pivot Y
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pivot Y", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(44.dp), color = TextSecondary)
                        Slider(
                            value = selectedLayer.pivotY,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(pivotY = it)) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                            modifier = Modifier.weight(1f).height(26.dp).testTag("transform_pivot_y_slider")
                        )
                        Text(String.format("%.2f", selectedLayer.pivotY), style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
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
            
            // Skew X
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Skew-X", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                Slider(
                    value = selectedLayer.skewX,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(skewX = it)) },
                    valueRange = -1.5f..1.5f,
                    colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("transform_skew_x_slider")
                )
                Text(String.format("%.2f", selectedLayer.skewX), style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(46.dp), textAlign = TextAlign.End)
            }

            // Skew Y
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Skew-Y", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                Slider(
                    value = selectedLayer.skewY,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(skewY = it)) },
                    valueRange = -1.5f..1.5f,
                    colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("transform_skew_y_slider")
                )
                Text(String.format("%.2f", selectedLayer.skewY), style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(46.dp), textAlign = TextAlign.End)
            }

            // Persp X
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PerspX", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                Slider(
                    value = selectedLayer.perspX,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(perspX = it)) },
                    valueRange = -0.005f..0.005f,
                    colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("transform_persp_x_slider")
                )
                Text(String.format("%.4f", selectedLayer.perspX), style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary, modifier = Modifier.width(46.dp), textAlign = TextAlign.End)
            }

            // Persp Y
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PerspY", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                Slider(
                    value = selectedLayer.perspY,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(perspY = it)) },
                    valueRange = -0.005f..0.005f,
                    colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("transform_persp_y_slider")
                )
                Text(String.format("%.4f", selectedLayer.perspY), style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary, modifier = Modifier.width(46.dp), textAlign = TextAlign.End)
            }

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
    onImportFontClick: () -> Unit
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
                Text("Typography Style & Font 🔠", style = Typography.labelSmall, color = EnergeticYellow)
                
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font Size", style = Typography.labelSmall, fontSize = 9.sp, color = TextSecondary)
                        val trigger = LocalSliderValueEditTrigger.current
                        Text("${selectedLayer.fontSize.toInt()}sp", style = Typography.labelSmall, fontSize = 9.sp, color = TextPrimary, modifier = Modifier.clickable {
                            trigger?.invoke(SliderValueEditConfig("Font Size", selectedLayer.fontSize, 8f..150f, isInt = true) { onUpdateLayer(selectedLayer.copy(fontSize = it)) })
                        })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onUpdateLayer(selectedLayer.copy(fontSize = maxOf(8f, selectedLayer.fontSize - 2f))) },
                            modifier = Modifier.size(24.dp).testTag("font_size_decrease")
                        ) {
                            Icon(Icons.Default.Remove, "Decrease", modifier = Modifier.size(16.dp), tint = TextSecondary)
                        }
                        Slider(
                            value = selectedLayer.fontSize,
                            onValueChange = { onUpdateLayer(selectedLayer.copy(fontSize = it)) },
                            valueRange = 8f..150f,
                            colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                            modifier = Modifier.weight(1f).height(32.dp).testTag("font_size_slider")
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

                val importedFontsList = remember(FontFavoritesState.favoriteFontsList) {
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
                    Text("Parametric Shape Editor 📐", style = Typography.labelSmall, color = EnergeticYellow)
                    
                    Text(
                        text = "Shape Type: ${selectedLayer.type.name.removePrefix("VECTOR_").replace("_", " ")}",
                        style = Typography.labelSmall,
                        fontSize = 11.sp,
                        color = MatteBlue,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Divider(color = HighslateOutline.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Text("Dimensions & Scale", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Width", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                        Slider(
                            value = selectedLayer.width,
                            onValueChange = {
                                val newW = it.coerceIn(10f, 1500f)
                                if (selectedLayer.type == LayerType.VECTOR_CIRCLE) {
                                    onUpdateLayer(selectedLayer.copy(width = newW, height = newW))
                                } else {
                                    onUpdateLayer(selectedLayer.copy(width = newW))
                                }
                            },
                            valueRange = 10f..1200f,
                            colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("shape_width_slider")
                        )
                        val trigger = LocalSliderValueEditTrigger.current
                        Text("${selectedLayer.width.toInt()}px", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(46.dp).clickable {
                            trigger?.invoke(SliderValueEditConfig("Shape Width", selectedLayer.width, 10f..1200f, isInt = true) { onUpdateLayer(selectedLayer.copy(width = it)) })
                        }, textAlign = TextAlign.End)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Height", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                        Slider(
                            value = selectedLayer.height,
                            onValueChange = {
                                val newH = it.coerceIn(10f, 1500f)
                                if (selectedLayer.type == LayerType.VECTOR_CIRCLE) {
                                    onUpdateLayer(selectedLayer.copy(width = newH, height = newH))
                                } else {
                                    onUpdateLayer(selectedLayer.copy(height = newH))
                                }
                            },
                            valueRange = 10f..1200f,
                            colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("shape_height_slider")
                        )
                        val trigger = LocalSliderValueEditTrigger.current
                        Text("${selectedLayer.height.toInt()}px", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(46.dp).clickable {
                            trigger?.invoke(SliderValueEditConfig("Shape Height", selectedLayer.height, 10f..1200f, isInt = true) { onUpdateLayer(selectedLayer.copy(height = it)) })
                        }, textAlign = TextAlign.End)
                    }

                    Spacer(Modifier.height(4.dp))
                    Divider(color = HighslateOutline.copy(alpha = 0.3f), thickness = 0.5.dp)

                    val canHaveCornerRadius = selectedLayer.type in listOf(
                        LayerType.VECTOR_RECT, LayerType.VECTOR_TRIANGLE, LayerType.VECTOR_PENTAGON,
                        LayerType.VECTOR_HEXAGON, LayerType.VECTOR_STAR, LayerType.VECTOR_BEZIER
                    )
                    if (canHaveCornerRadius) {
                        Text("Corners Aesthetics", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Radius", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                            Slider(
                                value = selectedLayer.cornerRadius,
                                onValueChange = { onUpdateLayer(selectedLayer.copy(cornerRadius = it)) },
                                valueRange = 0f..250f,
                                colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("shape_radius_slider")
                            )
                            val trigger = LocalSliderValueEditTrigger.current
                            Text("${selectedLayer.cornerRadius.toInt()}px", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(46.dp).clickable {
                                trigger?.invoke(SliderValueEditConfig("Shape Corner Radius", selectedLayer.cornerRadius, 0f..250f, isInt = true) { onUpdateLayer(selectedLayer.copy(cornerRadius = it)) })
                            }, textAlign = TextAlign.End)
                        }
                    }

                    val hasEdges = selectedLayer.type in listOf(LayerType.VECTOR_TRIANGLE, LayerType.VECTOR_PENTAGON, LayerType.VECTOR_HEXAGON, LayerType.VECTOR_STAR)
                    if (hasEdges) {
                        Text("Structural Edges & Points", style = Typography.labelSmall, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val labelText = if (selectedLayer.type == LayerType.VECTOR_STAR) "Points" else "Sides"
                            Text(labelText, style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                            val minEdges = 3
                            val maxEdges = 20
                            val activeEdges = if (selectedLayer.type == LayerType.VECTOR_TRIANGLE && selectedLayer.polygonEdges == 5) 3 else selectedLayer.polygonEdges
                            Slider(
                                value = activeEdges.toFloat(),
                                onValueChange = { onUpdateLayer(selectedLayer.copy(polygonEdges = it.toInt())) },
                                valueRange = minEdges.toFloat()..maxEdges.toFloat(),
                                steps = maxEdges - minEdges - 1,
                                colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("shape_sides_slider")
                            )
                            val trigger = LocalSliderValueEditTrigger.current
                            Text("${activeEdges}", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(46.dp).clickable {
                                trigger?.invoke(SliderValueEditConfig("Edges / Points", activeEdges.toFloat(), 3f..20f, isInt = true) { onUpdateLayer(selectedLayer.copy(polygonEdges = it.toInt())) })
                            }, textAlign = TextAlign.End)
                        }
                    }

                    if (selectedLayer.type == LayerType.VECTOR_STAR) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Inner Ratio", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                            Slider(
                                value = selectedLayer.starInnerRadiusRatio,
                                onValueChange = { onUpdateLayer(selectedLayer.copy(starInnerRadiusRatio = it)) },
                                valueRange = 0.05f..0.95f,
                                colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("shape_star_ratio_slider")
                            )
                            val trigger = LocalSliderValueEditTrigger.current
                            Text(String.format("%.2f", selectedLayer.starInnerRadiusRatio), style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(46.dp).clickable {
                                trigger?.invoke(SliderValueEditConfig("Star Inner Ratio", selectedLayer.starInnerRadiusRatio, 0.05f..0.95f) { onUpdateLayer(selectedLayer.copy(starInnerRadiusRatio = it)) })
                            }, textAlign = TextAlign.End)
                        }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Thickness", style = Typography.labelSmall, fontSize = 10.sp, modifier = Modifier.width(55.dp), color = TextSecondary)
                            Slider(
                                value = selectedLayer.strokeThickness,
                                onValueChange = { onUpdateLayer(selectedLayer.copy(strokeThickness = it)) },
                                valueRange = 1f..60f,
                                colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("shape_stroke_slider")
                            )
                            val trigger = LocalSliderValueEditTrigger.current
                            Text("${selectedLayer.strokeThickness.toInt()}px", style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.width(46.dp).clickable {
                                trigger?.invoke(SliderValueEditConfig("Stroke Thickness", selectedLayer.strokeThickness, 1f..60f, isInt = true) { onUpdateLayer(selectedLayer.copy(strokeThickness = it)) })
                            }, textAlign = TextAlign.End)
                        }
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
                    Text("📐", fontSize = 28.sp)
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
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(param.name, style = Typography.labelSmall, fontSize = 10.sp, color = TextPrimary)
                                            if (param.value != 0f) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Reset ${param.name}",
                                                    tint = IndustrialAmber.copy(alpha = 0.6f),
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clickable { onUpdateEffectParam(actEff.id, pName, 0f) }
                                                )
                                            }
                                        }
                                        val displayVal = if (actEff is StudioEffect.PhotoshopEffect && actEff.effectType == "ColorGrading" && pName == "Preset") {
                                            val LUTs = listOf("Cinema Golden", "Teal & Orange", "Mono B&W", "Cold Frost", "Dreamy Pastel", "Vintage Sepia", "Acid Neon")
                                            LUTs.getOrNull(param.value.toInt()) ?: "Preset ${param.value.toInt()}"
                                        } else {
                                            "${"%.2f".format(param.value)} ${param.unit}".trim()
                                        }
                                        val trigger = LocalSliderValueEditTrigger.current
                                        Text(
                                            text = displayVal,
                                            style = Typography.labelSmall,
                                            fontSize = 11.sp,
                                            color = IndustrialAmber,
                                            modifier = Modifier.clickable {
                                                trigger?.invoke(
                                                    SliderValueEditConfig(
                                                        title = param.name,
                                                        currentValue = param.value,
                                                        valueRange = param.rangeMin..param.rangeMax,
                                                        isInt = param.unit.contains("px") || param.unit.contains("%") || (actEff is StudioEffect.PhotoshopEffect && actEff.effectType == "ColorGrading" && pName == "Preset"),
                                                        onConfirm = { onUpdateEffectParam(actEff.id, pName, it) }
                                                    )
                                                )
                                            }
                                        )
                                    }
                                    Slider(
                                        value = param.value,
                                        onValueChange = { onUpdateEffectParam(actEff.id, pName, it) },
                                        valueRange = param.rangeMin..param.rangeMax,
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = IndustrialAmber,
                                            thumbColor = IndustrialAmber
                                        ),
                                        modifier = Modifier.height(28.dp).testTag("effect_param_slider_${param.name}")
                                    )
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


