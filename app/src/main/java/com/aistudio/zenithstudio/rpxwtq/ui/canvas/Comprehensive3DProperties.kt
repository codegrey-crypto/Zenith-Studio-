package com.aistudio.zenithstudio.rpxwtq.ui.canvas

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndustrialAmber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class LightSourceType { POINT, DIRECTIONAL, FLAT }

data class Comprehensive3DProperties(
    // 1. Geometric Transforms & Bevel Profiles
    val rotationX: Float = 45f,
    val rotationY: Float = 30f,
    val rotationZ: Float = 0f,
    val extrusionDepth: Float = 120f,
    val bevelRadius: Float = 8f,
    val bevelSegments: Int = 4,
    
    // 2. Material System Shading Properties
    val specularIntensity: Float = 0.8f,
    val roughness: Float = 0.2f,
    val ambientOcclusion: Float = 0.5f,
    val metallic: Float = 0.0f,
    
    // 3. Ambient Environment Light Controls
    val lightType: LightSourceType = LightSourceType.DIRECTIONAL,
    val lightAzimuth: Float = 135f,
    val lightElevation: Float = 45f,
    val lightIntensity: Float = 1.2f,
    val lightColor: Color = Color.White,
    
    // 4. UV Texture Material Mapping
    val textureMaterial: ImageBitmap? = null,
    val uvScaleX: Float = 1.0f,
    val uvScaleY: Float = 1.0f,
    val uvOffsetX: Float = 0.0f,
    val uvOffsetY: Float = 0.0f
)

@Composable
fun Comprehensive3DPropertySheet(
    modifier: Modifier = Modifier,
    initialProperties: Comprehensive3DProperties = remember { Comprehensive3DProperties() },
    onPropertiesChanged: (Comprehensive3DProperties) -> Unit = {}
) {
    val context = LocalContext.current
    var currentProps by remember(initialProperties) { mutableStateOf(initialProperties) }
    val scrollState = rememberScrollState()

    // Setup photo/image picker to select the texture material
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        // Store image locally
                        val texturesDir = File(context.filesDir, "3d_textures").apply {
                            if (!exists()) mkdirs()
                        }
                        val uniqueName = "texture_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
                        val file = File(texturesDir, uniqueName)
                        FileOutputStream(file).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        }
                        
                        // Update globally tracked file path
                        val layerId = com.aistudio.zenithstudio.rpxwtq.EffectStackManager.currentLayerId
                        if (layerId != null) {
                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.texturesByLayer[layerId] = file.absolutePath
                        }
                        
                        val updated = currentProps.copy(textureMaterial = bitmap.asImageBitmap())
                        currentProps = updated
                        onPropertiesChanged(updated)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = modifier
            .testTag("comprehensive_3d_property_sheet")
            .fillMaxWidth()
            .background(Color(0xFF0F0F12))
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER TITLE
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "3D Parameters Suite",
                tint = IndustrialAmber,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "3D PARAMETERS MATRIX",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = IndustrialAmber,
                letterSpacing = 1.2.sp
            )
        }

        Divider(color = Color(0xFF23232C), thickness = 0.5.dp)

        // SECTION 1: GEOMETRIC TRANSFORMS & BEVELS
        Text(
            text = "1. GEOMETRIC TRANSFORMS & BEVELS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9E9EAF)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Rotation X
            PropertySlider(
                label = "Rotation X (Pitch)",
                value = currentProps.rotationX,
                range = 0f..360f,
                unit = "°",
                onValueChange = {
                    val updated = currentProps.copy(rotationX = it)
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )

            // Rotation Y
            PropertySlider(
                label = "Rotation Y (Yaw)",
                value = currentProps.rotationY,
                range = 0f..360f,
                unit = "°",
                onValueChange = {
                    val updated = currentProps.copy(rotationY = it)
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )

            // Rotation Z
            PropertySlider(
                label = "Rotation Z (Roll)",
                value = currentProps.rotationZ,
                range = 0f..360f,
                unit = "°",
                onValueChange = {
                    val updated = currentProps.copy(rotationZ = it)
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )

            // Extrusion Depth
            PropertySlider(
                label = "Extrusion Depth",
                value = currentProps.extrusionDepth,
                range = 0f..300f,
                unit = "px",
                onValueChange = {
                    val updated = currentProps.copy(extrusionDepth = it)
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )

            // Bevel Radius
            PropertySlider(
                label = "Bevel Radius",
                value = currentProps.bevelRadius,
                range = 0f..50f,
                unit = "px",
                onValueChange = {
                    val updated = currentProps.copy(bevelRadius = it)
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )

            // Bevel Segments
            PropertySlider(
                label = "Bevel Segments",
                value = currentProps.bevelSegments.toFloat(),
                range = 1f..10f,
                isInt = true,
                onValueChange = {
                    val updated = currentProps.copy(bevelSegments = it.toInt())
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )
        }

        Divider(color = Color(0xFF1E1E26), thickness = 0.5.dp)

        // SECTION 2: MATERIAL SYSTEM SHADING PROPERTIES
        Text(
            text = "2. MATERIAL SYSTEM SHADING",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9E9EAF)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Specular Intensity
            PropertySlider(
                label = "Specular Intensity",
                value = currentProps.specularIntensity,
                range = 0f..1f,
                onValueChange = {
                    val updated = currentProps.copy(specularIntensity = it)
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )

            // Roughness
            PropertySlider(
                label = "Roughness",
                value = currentProps.roughness,
                range = 0.01f..1f,
                onValueChange = {
                    val updated = currentProps.copy(roughness = it)
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )

            // Ambient Occlusion
            PropertySlider(
                label = "Ambient Occlusion",
                value = currentProps.ambientOcclusion,
                range = 0f..1f,
                onValueChange = {
                    val updated = currentProps.copy(ambientOcclusion = it)
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )

            // Metallic
            PropertySlider(
                label = "Metallic Ratio",
                value = currentProps.metallic,
                range = 0f..1f,
                onValueChange = {
                    val updated = currentProps.copy(metallic = it)
                    currentProps = updated
                    onPropertiesChanged(updated)
                }
            )
        }

        Divider(color = Color(0xFF1E1E26), thickness = 0.5.dp)

        // SECTION 3: AMBIENT ENVIRONMENT LIGHT CONTROLS
        Text(
            text = "3. ENVIRONMENT LIGHT CONTROLS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9E9EAF)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Light Source Type Segmented Row
            Text(
                text = "Light Source Type",
                fontSize = 9.sp,
                color = Color(0xFF787888)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF16161D)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LightSourceType.values().forEach { type ->
                    val isSelected = currentProps.lightType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isSelected) IndustrialAmber else Color.Transparent)
                            .clickable {
                                val updated = currentProps.copy(lightType = type)
                                currentProps = updated
                                onPropertiesChanged(updated)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.name,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (currentProps.lightType != LightSourceType.FLAT) {
                // Light Azimuth
                PropertySlider(
                    label = "Light Azimuth",
                    value = currentProps.lightAzimuth,
                    range = 0f..360f,
                    unit = "°",
                    onValueChange = {
                        val updated = currentProps.copy(lightAzimuth = it)
                        currentProps = updated
                        onPropertiesChanged(updated)
                    }
                )

                // Light Elevation
                PropertySlider(
                    label = "Light Elevation",
                    value = currentProps.lightElevation,
                    range = -90f..90f,
                    unit = "°",
                    onValueChange = {
                        val updated = currentProps.copy(lightElevation = it)
                        currentProps = updated
                        onPropertiesChanged(updated)
                    }
                )

                // Light Intensity
                PropertySlider(
                    label = "Light Intensity",
                    value = currentProps.lightIntensity,
                    range = 0f..5f,
                    onValueChange = {
                        val updated = currentProps.copy(lightIntensity = it)
                        currentProps = updated
                        onPropertiesChanged(updated)
                    }
                )

                // Custom Light Color Sliders (R, G, B)
                Text(
                    text = "Light RGB Color Spectrum",
                    fontSize = 9.sp,
                    color = Color(0xFF787888),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(currentProps.lightColor)
                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Red
                        PropertySlider(
                            label = "Red Channel",
                            value = currentProps.lightColor.red * 255f,
                            range = 0f..255f,
                            isInt = true,
                            onValueChange = { r ->
                                val updated = currentProps.copy(
                                    lightColor = Color(r / 255f, currentProps.lightColor.green, currentProps.lightColor.blue, 1f)
                                )
                                currentProps = updated
                                onPropertiesChanged(updated)
                            }
                        )
                        // Green
                        PropertySlider(
                            label = "Green Channel",
                            value = currentProps.lightColor.green * 255f,
                            range = 0f..255f,
                            isInt = true,
                            onValueChange = { g ->
                                val updated = currentProps.copy(
                                    lightColor = Color(currentProps.lightColor.red, g / 255f, currentProps.lightColor.blue, 1f)
                                )
                                currentProps = updated
                                onPropertiesChanged(updated)
                            }
                        )
                        // Blue
                        PropertySlider(
                            label = "Blue Channel",
                            value = currentProps.lightColor.blue * 255f,
                            range = 0f..255f,
                            isInt = true,
                            onValueChange = { b ->
                                val updated = currentProps.copy(
                                    lightColor = Color(currentProps.lightColor.red, currentProps.lightColor.green, b / 255f, 1f)
                                )
                                currentProps = updated
                                onPropertiesChanged(updated)
                            }
                        )
                    }
                }
            }
        }

        Divider(color = Color(0xFF1E1E26), thickness = 0.5.dp)

        // SECTION 4: UV TEXTURE MATERIAL MAPPING
        Text(
            text = "4. UV TEXTURE MATERIAL MAPPING",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9E9EAF)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Import Texture Button
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E26)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Import Gallery",
                        tint = IndustrialAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (currentProps.textureMaterial != null) "Change Material Texture" else "Import Material Texture",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (currentProps.textureMaterial != null) {
                Text(
                    text = "✓ Texture Material Loaded",
                    color = IndustrialAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // UV Scale X
                PropertySlider(
                    label = "UV Scale X",
                    value = currentProps.uvScaleX,
                    range = 0.1f..10f,
                    onValueChange = {
                        val updated = currentProps.copy(uvScaleX = it)
                        currentProps = updated
                        onPropertiesChanged(updated)
                    }
                )

                // UV Scale Y
                PropertySlider(
                    label = "UV Scale Y",
                    value = currentProps.uvScaleY,
                    range = 0.1f..10f,
                    onValueChange = {
                        val updated = currentProps.copy(uvScaleY = it)
                        currentProps = updated
                        onPropertiesChanged(updated)
                    }
                )

                // UV Offset X
                PropertySlider(
                    label = "UV Offset X",
                    value = currentProps.uvOffsetX,
                    range = -5f..5f,
                    onValueChange = {
                        val updated = currentProps.copy(uvOffsetX = it)
                        currentProps = updated
                        onPropertiesChanged(updated)
                    }
                )

                // UV Offset Y
                PropertySlider(
                    label = "UV Offset Y",
                    value = currentProps.uvOffsetY,
                    range = -5f..5f,
                    onValueChange = {
                        val updated = currentProps.copy(uvOffsetY = it)
                        currentProps = updated
                        onPropertiesChanged(updated)
                    }
                )
                
                // Button to Clear Texture
                TextButton(
                    onClick = {
                        val layerId = com.aistudio.zenithstudio.rpxwtq.EffectStackManager.currentLayerId
                        if (layerId != null) {
                            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.texturesByLayer.remove(layerId)
                        }
                        val updated = currentProps.copy(textureMaterial = null)
                        currentProps = updated
                        onPropertiesChanged(updated)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Clear Texture", color = Color(0xFFEF4444), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun PropertySlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "",
    isInt: Boolean = false,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = Color(0xFF787888)
            )
            Text(
                text = if (isInt) "${value.toInt()}$unit" else "${String.format("%.2f", value)}$unit",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                activeTrackColor = IndustrialAmber,
                inactiveTrackColor = Color(0xFF1E1E26),
                thumbColor = IndustrialAmber
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        )
    }
}

// Global conversion utility helper
fun get3DPropertiesFromFilter(filter: com.aistudio.zenithstudio.rpxwtq.ZenithFilter, textureBmp: ImageBitmap?): Comprehensive3DProperties {
    val params = filter.parameters
    val rotX = params.find { it.name == "Rotation X" }?.currentValue ?: 45f
    val rotY = params.find { it.name == "Rotation Y" }?.currentValue ?: 30f
    val rotZ = params.find { it.name == "Rotation Z" }?.currentValue ?: 0f
    val depth = params.find { it.name == "Extrusion Depth" }?.currentValue ?: 120f
    val bevelRadius = params.find { it.name == "Bevel Radius" }?.currentValue ?: 8f
    val bevelSegments = (params.find { it.name == "Bevel Segments" }?.currentValue ?: 4f).toInt()
    val specularIntensity = params.find { it.name == "Specular Intensity" }?.currentValue ?: 0.8f
    val roughness = params.find { it.name == "Roughness" }?.currentValue ?: 0.2f
    val ao = params.find { it.name == "Ambient Occlusion" }?.currentValue ?: 0.5f
    val metallic = params.find { it.name == "Metallic" }?.currentValue ?: 0.0f
    val lightTypeInt = (params.find { it.name == "Light Type" }?.currentValue ?: 1f).toInt()
    val lightType = when (lightTypeInt) {
        0 -> LightSourceType.FLAT
        2 -> LightSourceType.POINT
        else -> LightSourceType.DIRECTIONAL
    }
    val azimuth = params.find { it.name == "Light Azimuth" }?.currentValue ?: 135f
    val elevation = params.find { it.name == "Light Elevation" }?.currentValue ?: 45f
    val intensity = params.find { it.name == "Light Intensity" }?.currentValue ?: 1.2f
    val r = (params.find { it.name == "Light Color R" }?.currentValue ?: 255f) / 255f
    val g = (params.find { it.name == "Light Color G" }?.currentValue ?: 255f) / 255f
    val b = (params.find { it.name == "Light Color B" }?.currentValue ?: 255f) / 255f
    
    val uvScaleX = params.find { it.name == "UV Scale X" }?.currentValue ?: 1.0f
    val uvScaleY = params.find { it.name == "UV Scale Y" }?.currentValue ?: 1.0f
    val uvOffsetX = params.find { it.name == "UV Offset X" }?.currentValue ?: 0.0f
    val uvOffsetY = params.find { it.name == "UV Offset Y" }?.currentValue ?: 0.0f
    
    return Comprehensive3DProperties(
        rotationX = rotX,
        rotationY = rotY,
        rotationZ = rotZ,
        extrusionDepth = depth,
        bevelRadius = bevelRadius,
        bevelSegments = bevelSegments,
        specularIntensity = specularIntensity,
        roughness = roughness,
        ambientOcclusion = ao,
        metallic = metallic,
        lightType = lightType,
        lightAzimuth = azimuth,
        lightElevation = elevation,
        lightIntensity = intensity,
        lightColor = Color(r, g, b, 1.0f),
        textureMaterial = textureBmp,
        uvScaleX = uvScaleX,
        uvScaleY = uvScaleY,
        uvOffsetX = uvOffsetX,
        uvOffsetY = uvOffsetY
    )
}
