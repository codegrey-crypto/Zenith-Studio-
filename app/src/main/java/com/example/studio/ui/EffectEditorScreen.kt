package com.example.studio.ui

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.studio.model.StudioLayer
import com.example.studio.render.DepthMapGenerator
import com.example.studio.render.RasterExtrudeRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectEditorScreen(
    selectedLayer: StudioLayer? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // UI state for OpenGL Extrusion attributes
    var extrusionDepth by remember { mutableStateOf(40f) }
    var lightX by remember { mutableStateOf(0.5f) }
    var lightY by remember { mutableStateOf(0.5f) }
    var lightZ by remember { mutableStateOf(1.0f) }
    var shadingIntensity by remember { mutableStateOf(1.0f) }
    var rotationX by remember { mutableStateOf(25f) }
    var rotationY by remember { mutableStateOf(-20f) }
    var rotationZ by remember { mutableStateOf(0f) }
    var numberOfLayers by remember { mutableStateOf(30f) }
    
    // Shape Preset State
    var selectedPresetType by remember { mutableStateOf("Star") }
    
    // OpenGL Renderer setup
    val renderer = remember { RasterExtrudeRenderer(context) }
    
    // Generate bitmap based on selection or workspace layer
    val activeBitmap = remember(selectedPresetType, selectedLayer) {
        if (selectedPresetType == "Workspace Layer" && selectedLayer != null) {
            getLayerBitmap(selectedLayer) ?: generateShapePreset("Star")
        } else {
            generateShapePreset(selectedPresetType)
        }
    }
    
    // Update renderer parameters whenever state variables change
    LaunchedEffect(
        extrusionDepth, lightX, lightY, lightZ,
        shadingIntensity, rotationX, rotationY, rotationZ,
        numberOfLayers, activeBitmap
    ) {
        renderer.extrusionDepth = extrusionDepth
        renderer.lightX = lightX
        renderer.lightY = lightY
        renderer.lightZ = lightZ
        renderer.shadingIntensity = shadingIntensity
        renderer.rotationX = rotationX
        renderer.rotationY = rotationY
        renderer.rotationZ = rotationZ
        renderer.numberOfLayers = numberOfLayers.toInt().coerceIn(5, 100)
        renderer.setInputBitmap(activeBitmap)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = null,
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                "3D Raster Extrude",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "Offline GPU Extrusion Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF08080A),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF08080A),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF08080A))
        ) {
            // LEFT COLUMN: OpenGL Interactive 3D Canvas
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .border(BorderStroke(1.dp, Color(0xFF1E1E24)), RoundedCornerShape(12.dp))
                    .background(Color(0xFF131317), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        GLSurfaceView(ctx).apply {
                            setEGLContextClientVersion(3)
                            setRenderer(renderer)
                            // Continuous rendering ensures real-time responsive 3D rotations
                            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Rotation status label in lower left
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x99000000)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "Rotation X: ${rotationX.toInt()}° | Y: ${rotationY.toInt()}°",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00FF66),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // RIGHT COLUMN: Controller Sidebar
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preset Source Selector Section
                Text(
                    "EXTRUSION IMAGE SOURCE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF8E8E93)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = if (selectedLayer != null) {
                        listOf("Star", "Heart", "Logo", "Ring", "Workspace Layer")
                    } else {
                        listOf("Star", "Heart", "Logo", "Ring")
                    }
                    
                    presets.forEach { preset ->
                        val isSelected = selectedPresetType == preset
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Color(0xFF00FF66) else Color(0xFF1C1C1E),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedPresetType = preset }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            )
                        }
                    }
                }
                
                Divider(color = Color(0xFF1E1E24))
                
                // Sliders section
                Text(
                    "EXTRUSION CONFIGURATION",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF8E8E93)
                )
                
                // 1. Extrusion Depth
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Extrusion Depth", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("${extrusionDepth.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF00FF66))
                    }
                    Slider(
                        value = extrusionDepth,
                        onValueChange = { extrusionDepth = it },
                        valueRange = 0f..120f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00FF66),
                            inactiveTrackColor = Color(0xFF1C1C1E),
                            thumbColor = Color(0xFF00FF66)
                        )
                    )
                }
                
                // 2. Multi-slice Layer Count
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Raster Slices", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("${numberOfLayers.toInt()} Layers", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF00FF66))
                    }
                    Slider(
                        value = numberOfLayers,
                        onValueChange = { numberOfLayers = it },
                        valueRange = 5f..80f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00FF66),
                            inactiveTrackColor = Color(0xFF1C1C1E),
                            thumbColor = Color(0xFF00FF66)
                        )
                    )
                }
                
                Divider(color = Color(0xFF1E1E24))
                
                // 3D Angle Knobs (Rotations)
                Text(
                    "3D PERSPECTIVE ANGLE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF8E8E93)
                )
                
                // Rotation X
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rotation X (Pitch)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("${rotationX.toInt()}°", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF00FF66))
                    }
                    Slider(
                        value = rotationX,
                        onValueChange = { rotationX = it },
                        valueRange = -90f..90f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00FF66),
                            inactiveTrackColor = Color(0xFF1C1C1E),
                            thumbColor = Color(0xFF00FF66)
                        )
                    )
                }
                
                // Rotation Y
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rotation Y (Yaw)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("${rotationY.toInt()}°", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF00FF66))
                    }
                    Slider(
                        value = rotationY,
                        onValueChange = { rotationY = it },
                        valueRange = -90f..90f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00FF66),
                            inactiveTrackColor = Color(0xFF1C1C1E),
                            thumbColor = Color(0xFF00FF66)
                        )
                    )
                }
                
                Divider(color = Color(0xFF1E1E24))
                
                // Lighting
                Text(
                    "DIRECTIONAL SHADING",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF8E8E93)
                )
                
                // Light Angle (X)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Light Angle X", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text(String.format("%.2f", lightX), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF00FF66))
                    }
                    Slider(
                        value = lightX,
                        onValueChange = { lightX = it },
                        valueRange = -1.5f..1.5f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00FF66),
                            inactiveTrackColor = Color(0xFF1C1C1E),
                            thumbColor = Color(0xFF00FF66)
                        )
                    )
                }
                
                // Shading Intensity
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Light Shading Intensity", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text(String.format("%.2f", shadingIntensity), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF00FF66))
                    }
                    Slider(
                        value = shadingIntensity,
                        onValueChange = { shadingIntensity = it },
                        valueRange = 0f..2.5f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00FF66),
                            inactiveTrackColor = Color(0xFF1C1C1E),
                            thumbColor = Color(0xFF00FF66)
                        )
                    )
                }
                
                // Action Buttons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFF8E8E93)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onClose,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Apply Effect", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Creates a beautiful neon colored shape preset for offline-first usage
 */
private fun generateShapePreset(type: String): Bitmap {
    val size = 512
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }
    
    when (type) {
        "Star" -> {
            paint.color = android.graphics.Color.parseColor("#FF00D4") // Cyberpunk Magenta
            val path = android.graphics.Path()
            val midX = size / 2f
            val midY = size / 2f
            val radius = size * 0.42f
            val innerRadius = size * 0.17f
            var angle = -Math.PI / 2
            val step = Math.PI / 5
            
            path.moveTo(midX + (radius * Math.cos(angle)).toFloat(), midY + (radius * Math.sin(angle)).toFloat())
            for (i in 0 until 10) {
                angle += step
                val r = if (i % 2 == 0) innerRadius else radius
                path.lineTo(midX + (r * Math.cos(angle)).toFloat(), midY + (r * Math.sin(angle)).toFloat())
            }
            path.close()
            canvas.drawPath(path, paint)
        }
        "Heart" -> {
            paint.color = android.graphics.Color.parseColor("#FF3B30") // Radiant red
            val path = android.graphics.Path()
            val width = size.toFloat()
            val height = size.toFloat()
            path.moveTo(width / 2, height / 4)
            path.cubicTo(5 * width / 6, 0f, width, height / 3, width / 2, 9 * height / 10)
            path.cubicTo(0f, height / 3, width / 6, 0f, width / 2, height / 4)
            path.close()
            canvas.drawPath(path, paint)
        }
        "Logo" -> {
            paint.color = android.graphics.Color.parseColor("#00FF66") // Neon green
            canvas.drawCircle(size / 2f, size / 2f, size * 0.38f, paint)
            
            paint.color = android.graphics.Color.parseColor("#08080A")
            canvas.drawCircle(size / 2f, size / 2f, size * 0.25f, paint)
            
            paint.color = android.graphics.Color.parseColor("#00FF66")
            canvas.drawRect(size * 0.43f, size * 0.12f, size * 0.57f, size * 0.88f, paint)
        }
        else -> { // Ring
            paint.color = android.graphics.Color.parseColor("#00C2FF") // Neon blue
            canvas.drawCircle(size / 2f, size / 2f, size * 0.4f, paint)
            
            paint.color = android.graphics.Color.TRANSPARENT
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
            canvas.drawCircle(size / 2f, size / 2f, size * 0.25f, paint)
        }
    }
    return bitmap
}

/**
 * Extracts a dynamic preview bitmap from the active workspace layer for 3D extrusion
 */
private fun getLayerBitmap(layer: StudioLayer): Bitmap? {
    val size = 512
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    
    when (layer.type) {
        com.example.studio.model.LayerType.TEXT -> {
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 68f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.isFakeBoldText = true
            val x = size / 2f
            val y = size / 2f - ((paint.descent() + paint.ascent()) / 2f)
            canvas.drawText(layer.textContent.ifEmpty { "EXTRUDE" }, x, y, paint)
            return bitmap
        }
        else -> {
            // Draw a stylish layered rounded square with workspace layer accent color
            val layerColor = android.graphics.Color.rgb(
                (layer.baseColor.red * 255).toInt(),
                (layer.baseColor.green * 255).toInt(),
                (layer.baseColor.blue * 255).toInt()
            )
            paint.color = layerColor
            canvas.drawRoundRect(
                size * 0.2f, size * 0.2f, size * 0.8f, size * 0.8f,
                48f, 48f, paint
            )
            return bitmap
        }
    }
}
