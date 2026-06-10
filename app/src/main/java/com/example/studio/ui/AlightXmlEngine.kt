package com.example.studio.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Xml
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.studio.model.LayerType
import com.example.studio.model.StudioLayer
import com.example.studio.model.StudioEffect
import com.example.studio.model.EffectParameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.UUID

object AlightXmlEngine {

    class ParsedAlightXml(
        val canvasWidth: Float,
        val canvasHeight: Float,
        val layers: List<StudioLayer>
    )

    private class ShapeData {
        var id: String = UUID.randomUUID().toString()
        var label: String = "Rectangle"
        var s: String = ".rect"
        var location: Offset = Offset.Zero
        var scale: Offset = Offset(1f, 1f)
        var rotation: Float = 0f
        var fillColor: String? = null
        var startColor: String? = null
        var size: Offset = Offset(100f, 100f)
        var cornerRadius: Float = 0f
        var gaussianBlurStrength: Float? = null
        var solidColorHex: String? = null
    }

    private class TextData {
        var id: String = UUID.randomUUID().toString()
        var label: String = "Text"
        var size: Float = 36f
        var wrapWidth: Float = 512f
        var align: String = "center"
        var location: Offset = Offset.Zero
        var scale: Offset = Offset(1f, 1f)
        var rotation: Float = 0f
        var content: String = ""
        var fillColor: String? = null
        var gaussianBlurStrength: Float? = null
        var solidColorHex: String? = null
    }

    /**
     * Parse Hexadecimal color codes (such as #ff81d595 or #81d595) to Compose Color
     */
    fun parseHexColor(hex: String?): Color {
        if (hex == null) return Color.White
        return try {
            val cleanHex = hex.trim().removePrefix("#")
            if (cleanHex.length == 8) {
                val argb = cleanHex.toLong(16)
                Color(argb)
            } else if (cleanHex.length == 6) {
                val rgb = cleanHex.toLong(16) or 0xFF000000L
                Color(rgb)
            } else {
                Color.White
            }
        } catch (e: Exception) {
            Color.White
        }
    }

    /**
     * Formats Compose Color to XML compatible #AARRGGBB hex string.
     */
    fun formatHexColor(color: Color): String {
        val a = (color.alpha * 255).toInt().coerceIn(0, 255)
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02x%02x%02x%02x", a, r, g, b)
    }

    private fun parseLocation(value: String?): Offset {
        if (value == null) return Offset.Zero
        val parts = value.split(",")
        if (parts.size >= 2) {
            val x = parts[0].toFloatOrNull() ?: 0f
            val y = parts[1].toFloatOrNull() ?: 0f
            return Offset(x, y)
        }
        return Offset.Zero
    }

    private fun parseScale(value: String?): Offset {
        if (value == null) return Offset(1f, 1f)
        val parts = value.split(",")
        if (parts.size >= 2) {
            val x = parts[0].toFloatOrNull() ?: 1f
            val y = parts[1].toFloatOrNull() ?: 1f
            return Offset(x, y)
        } else if (parts.isNotEmpty()) {
            val x = parts[0].toFloatOrNull() ?: 1f
            return Offset(x, x)
        }
        return Offset(1f, 1f)
    }

    /**
     * STREAM XML PARSING: Background worker XML parsing loop
     */
    suspend fun parseAlightXml(
        inputStream: InputStream,
        targetWidth: Float? = null,
        targetHeight: Float? = null
    ): ParsedAlightXml = withContext(Dispatchers.IO) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")

        var xmlSceneWidth = 1080f
        var xmlSceneHeight = 1350f
        var firstTag = true
        val parsedLayers = mutableListOf<StudioLayer>()

        var currentShape: ShapeData? = null
        var currentText: TextData? = null
        var currentEffectId: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (firstTag) {
                        firstTag = false
                        if (name != "scene") {
                            throw IllegalArgumentException("Invalid file structure: Root element must be '<scene>' tag")
                        }
                        val widthAttr = parser.getAttributeValue(null, "width")
                        if (widthAttr != null) {
                            xmlSceneWidth = widthAttr.toFloatOrNull() ?: 1080f
                        }
                        val heightAttr = parser.getAttributeValue(null, "height")
                        if (heightAttr != null) {
                            xmlSceneHeight = heightAttr.toFloatOrNull() ?: 1350f
                        }
                    } else {
                        when (name) {
                            "scene" -> {
                                val widthAttr = parser.getAttributeValue(null, "width")
                                val heightAttr = parser.getAttributeValue(null, "height")
                                if (widthAttr != null && heightAttr != null) {
                                    val w = widthAttr.toFloatOrNull()
                                    val h = heightAttr.toFloatOrNull()
                                    if (w != null && h != null && w > 10f && h > 10f) {
                                        xmlSceneWidth = w
                                        xmlSceneHeight = h
                                    }
                                }
                            }
                            "shape" -> {
                                currentShape = ShapeData().apply {
                                    id = parser.getAttributeValue(null, "id") ?: UUID.randomUUID().toString()
                                    label = parser.getAttributeValue(null, "label") ?: "Shape"
                                    s = parser.getAttributeValue(null, "s") ?: ".rect"
                                }
                            }
                            "text" -> {
                                currentText = TextData().apply {
                                    id = parser.getAttributeValue(null, "id") ?: UUID.randomUUID().toString()
                                    label = parser.getAttributeValue(null, "label") ?: "Text"
                                    size = parser.getAttributeValue(null, "size")?.toFloatOrNull() ?: 36f
                                    wrapWidth = parser.getAttributeValue(null, "wrapWidth")?.toFloatOrNull() ?: 512f
                                    align = parser.getAttributeValue(null, "align") ?: "center"
                                }
                            }
                            "effect" -> {
                                currentEffectId = parser.getAttributeValue(null, "id")
                            }
                            "location" -> {
                                val value = parser.getAttributeValue(null, "value")
                                currentShape?.location = parseLocation(value)
                                currentText?.location = parseLocation(value)
                            }
                            "scale" -> {
                                val value = parser.getAttributeValue(null, "value")
                                currentShape?.scale = parseScale(value)
                                currentText?.scale = parseScale(value)
                            }
                            "rotation" -> {
                                val value = parser.getAttributeValue(null, "value")
                                val rot = value?.toFloatOrNull() ?: 0f
                                currentShape?.rotation = rot
                                currentText?.rotation = rot
                            }
                            "fillColor" -> {
                                val value = parser.getAttributeValue(null, "value")
                                currentShape?.fillColor = value
                                currentText?.fillColor = value
                            }
                            "property" -> {
                                val propName = parser.getAttributeValue(null, "name")
                                val propValue = parser.getAttributeValue(null, "value")
                                if (currentEffectId != null) {
                                    if (currentEffectId == "com.alightcreative.effects.gaussianblur" && propName == "strength" && propValue != null) {
                                        val strVal = propValue.toFloatOrNull() ?: 0f
                                        currentShape?.gaussianBlurStrength = strVal
                                        currentText?.gaussianBlurStrength = strVal
                                    } else if (currentEffectId == "com.alightcreative.effects.solidcolor" && propName == "color" && propValue != null) {
                                        currentShape?.solidColorHex = propValue
                                        currentText?.solidColorHex = propValue
                                    }
                                } else {
                                    if (currentShape != null) {
                                        if (propName == "size" && propValue != null) {
                                            currentShape!!.size = parseLocation(propValue)
                                        } else if (propName == "cornerRadius" && propValue != null) {
                                            currentShape!!.cornerRadius = propValue.toFloatOrNull() ?: 0f
                                        }
                                    }
                                }
                            }
                            "gradient" -> {
                                val startCol = parser.getAttributeValue(null, "startColor")
                                if (startCol != null && currentShape != null) {
                                    currentShape!!.startColor = startCol
                                }
                            }
                            "content" -> {
                                if (currentText != null) {
                                    currentText!!.content = parser.nextText() ?: ""
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (name == "effect") {
                        currentEffectId = null
                    } else if (name == "shape" && currentShape != null) {
                        val cs = currentShape!!
                        val activeCanvasW = targetWidth ?: xmlSceneWidth
                        val activeCanvasH = targetHeight ?: xmlSceneHeight

                        val scaleFactor = if (xmlSceneWidth > 0f && xmlSceneHeight > 0f) {
                            minOf(activeCanvasW / xmlSceneWidth, activeCanvasH / xmlSceneHeight)
                        } else {
                            1f
                        }

                        val offsetX = (activeCanvasW - (xmlSceneWidth * scaleFactor)) / 2f
                        val offsetY = (activeCanvasH - (xmlSceneHeight * scaleFactor)) / 2f

                        val sizeW = cs.size.x
                        val sizeH = cs.size.y
                        val scaleX = cs.scale.x
                        val scaleY = cs.scale.y
                        val finalW = (sizeW * scaleX) * scaleFactor
                        val finalH = (sizeH * scaleY) * scaleFactor

                        val posX = offsetX + (cs.location.x * scaleFactor) - finalW / 2f
                        val posY = offsetY + (cs.location.y * scaleFactor) - finalH / 2f

                        val layerType = when (cs.s) {
                            ".rect" -> LayerType.VECTOR_RECT
                            ".circle" -> LayerType.VECTOR_CIRCLE
                            ".triangle" -> LayerType.VECTOR_TRIANGLE
                            ".roundrect" -> LayerType.VECTOR_RECT
                            else -> LayerType.VECTOR_RECT
                        }

                        // Apply the solidcolor overlay filter hex configuration directly to baseColor if present
                        val baseColor = parseHexColor(cs.solidColorHex ?: cs.startColor ?: cs.fillColor)

                        // If Gaussian Blur strength property was extracted, translate straight into StudioEffect list
                        val effectsList = mutableListOf<StudioEffect>()
                        cs.gaussianBlurStrength?.let { strength ->
                            val radiusInPixels = strength * 200f
                            val blurEffect = StudioEffect.GaussianBlur()
                                .updateParameter("Radius", radiusInPixels)
                                .updateParameter("Intensity", 1.0f)
                            effectsList.add(blurEffect)
                        }

                        parsedLayers.add(
                            StudioLayer(
                                id = cs.id,
                                name = cs.label,
                                type = layerType,
                                positionX = posX,
                                positionY = posY,
                                width = finalW,
                                height = finalH,
                                rotation = cs.rotation,
                                baseColor = baseColor,
                                effects = effectsList,
                                cornerRadius = if (cs.s == ".roundrect" || cs.cornerRadius > 0f) {
                                    if (cs.cornerRadius > 0f) cs.cornerRadius else 20f
                                } else 0f
                            )
                        )
                        currentShape = null
                    } else if (name == "text" && currentText != null) {
                        val ct = currentText!!
                        val activeCanvasW = targetWidth ?: xmlSceneWidth
                        val activeCanvasH = targetHeight ?: xmlSceneHeight

                        val scaleFactor = if (xmlSceneWidth > 0f && xmlSceneHeight > 0f) {
                            minOf(activeCanvasW / xmlSceneWidth, activeCanvasH / xmlSceneHeight)
                        } else {
                            1f
                        }

                        val offsetX = (activeCanvasW - (xmlSceneWidth * scaleFactor)) / 2f
                        val offsetY = (activeCanvasH - (xmlSceneHeight * scaleFactor)) / 2f

                        val scaleX = ct.scale.x
                        val scaleY = ct.scale.y
                        val finalW = (ct.wrapWidth * scaleX) * scaleFactor
                        val finalH = ((ct.size * 1.5f) * scaleY) * scaleFactor

                        val posX = offsetX + (ct.location.x * scaleFactor) - finalW / 2f
                        val posY = offsetY + (ct.location.y * scaleFactor) - finalH / 2f

                        val baseColor = parseHexColor(ct.solidColorHex ?: ct.fillColor)

                        // Gaussian Blur translate straight into StudioEffect list
                        val effectsList = mutableListOf<StudioEffect>()
                        ct.gaussianBlurStrength?.let { strength ->
                            val radiusInPixels = strength * 200f
                            val blurEffect = StudioEffect.GaussianBlur()
                                .updateParameter("Radius", radiusInPixels)
                                .updateParameter("Intensity", 1.0f)
                            effectsList.add(blurEffect)
                        }

                        parsedLayers.add(
                            StudioLayer(
                                id = ct.id,
                                name = ct.label,
                                type = LayerType.TEXT,
                                positionX = posX,
                                positionY = posY,
                                width = finalW,
                                height = finalH,
                                rotation = ct.rotation,
                                fontSize = ct.size * scaleFactor,
                                fontAlign = when (ct.align.lowercase()) {
                                    "left" -> "Left"
                                    "right" -> "Right"
                                    else -> "Center"
                                },
                                textContent = ct.content,
                                baseColor = baseColor,
                                effects = effectsList
                            )
                        )
                        currentText = null
                    }
                }
            }
            eventType = parser.next()
        }

        ParsedAlightXml(
            canvasWidth = targetWidth ?: xmlSceneWidth,
            canvasHeight = targetHeight ?: xmlSceneHeight,
            layers = parsedLayers
        )
    }

    /**
     * SERIALIZATION EXPORT: Generates Alight Motion compatible XML contents.
     */
    fun exportToAlightXmlString(
        canvasWidth: Float,
        canvasHeight: Float,
        projectName: String,
        layers: List<StudioLayer>
    ): String {
        val xml = java.lang.StringBuilder()
        xml.append("<?xml version='1.0' encoding='UTF-8' ?>\n")
        xml.append("<!--\n")
        xml.append("Created by Alight Motion (http://alightmotion.com)\n")
        xml.append("Exported from Zenith Studio Custom XML Engine\n")
        xml.append("-->\n")

        val titleEscaped = projectName.replace("\"", "&quot;")
        val cw = canvasWidth.toInt()
        val ch = canvasHeight.toInt()

        xml.append("<scene title=\"$titleEscaped\" width=\"$cw\" height=\"$ch\" exportWidth=\"$cw\" exportHeight=\"$ch\" precompose=\"dynamicResolution\" bgcolor=\"#ff000000\" totalTime=\"1000\" fps=\"30\" amver=\"106019\" ffver=\"106\" amplatform=\"android\" retime=\"freeze\">\n")

        for (layer in layers) {
            if (!layer.isVisible) continue

            when (layer.type) {
                LayerType.VECTOR_RECT, LayerType.VECTOR_CIRCLE, LayerType.VECTOR_TRIANGLE -> {
                    val shapeType = when (layer.type) {
                        LayerType.VECTOR_CIRCLE -> ".circle"
                        LayerType.VECTOR_TRIANGLE -> ".triangle"
                        else -> if (layer.cornerRadius > 0f) ".roundrect" else ".rect"
                    }

                    val nameEscaped = layer.name.replace("\"", "&quot;")
                    xml.append("  <shape id=\"${layer.id.hashCode().coerceAtLeast(1)}\" label=\"$nameEscaped\" startTime=\"0\" endTime=\"1000\" fillType=\"color\" mediaFillMode=\"fill\" s=\"$shapeType\">\n")

                    xml.append("    <transform>\n")
                    val amX = layer.positionX + layer.width / 2f
                    val amY = layer.positionY + layer.height / 2f
                    xml.append(String.format(Locale.US, "      <location value=\"%.6f,%.6f,0.000000\" />\n", amX, amY))

                    val scX = layer.width / 100f
                    val scY = layer.height / 100f
                    xml.append(String.format(Locale.US, "      <scale value=\"%.6f,%.6f\" />\n", scX, scY))

                    if (layer.rotation != 0f) {
                        xml.append(String.format(Locale.US, "      <rotation value=\"%.6f\" />\n", layer.rotation))
                    }
                    xml.append("    </transform>\n")

                    val fillStr = formatHexColor(layer.baseColor)
                    xml.append("    <fillColor value=\"$fillStr\" />\n")
                    xml.append("    <property name=\"size\" type=\"vec2\" value=\"100.000000,100.000000\" />\n")

                    if (shapeType == ".roundrect" && layer.cornerRadius > 0f) {
                        xml.append(String.format(Locale.US, "    <property name=\"cornerRadius\" type=\"float\" value=\"%.6f\" />\n", layer.cornerRadius))
                    }

                    // Bi-directional Visual Effects (FX) export compilation
                    // 1. Solid Color
                    xml.append("    <effect id=\"com.alightcreative.effects.solidcolor\">\n")
                    xml.append("      <property name=\"color\" type=\"color\" value=\"$fillStr\" />\n")
                    xml.append("    </effect>\n")

                    // 2. Gaussian Blur
                    val blurEff = layer.effects.firstOrNull { it is StudioEffect.GaussianBlur } as? StudioEffect.GaussianBlur
                    if (blurEff != null) {
                        val radius = blurEff.parameters["Radius"]?.value ?: 0f
                        val intensity = blurEff.parameters["Intensity"]?.value ?: 1f
                        val strength = (radius * intensity) / 200f
                        xml.append("    <effect id=\"com.alightcreative.effects.gaussianblur\">\n")
                        xml.append(String.format(Locale.US, "      <property name=\"strength\" type=\"float\" value=\"%.6f\" />\n", strength))
                        xml.append("    </effect>\n")
                    }

                    xml.append("  </shape>\n")
                }
                LayerType.TEXT -> {
                    val nameEscaped = layer.name.replace("\"", "&quot;")
                    val textAlign = layer.fontAlign.lowercase()
                    val fontSzStr = String.format(Locale.US, "%.6f", layer.fontSize)
                    xml.append("  <text id=\"${layer.id.hashCode().coerceAtLeast(1)}\" label=\"$nameEscaped\" startTime=\"0\" endTime=\"1000\" fillType=\"color\" mediaFillMode=\"fill\" size=\"$fontSzStr\" align=\"$textAlign\">\n")

                    xml.append("    <transform>\n")
                    val amX = layer.positionX + layer.width / 2f
                    val amY = layer.positionY + layer.height / 2f
                    xml.append(String.format(Locale.US, "      <location value=\"%.6f,%.6f,0.000000\" />\n", amX, amY))
                    xml.append("      <scale value=\"1.000000,1.000000\" />\n")
                    if (layer.rotation != 0f) {
                        xml.append(String.format(Locale.US, "      <rotation value=\"%.6f\" />\n", layer.rotation))
                    }
                    xml.append("    </transform>\n")

                    val fillStr = formatHexColor(layer.baseColor)
                    xml.append("    <fillColor value=\"$fillStr\" />\n")

                    val contentEscaped = layer.textContent
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    xml.append("    <content>$contentEscaped</content>\n")

                    // Bi-directional Visual Effects (FX) export compilation
                    // 1. Solid Color
                    xml.append("    <effect id=\"com.alightcreative.effects.solidcolor\">\n")
                    xml.append("      <property name=\"color\" type=\"color\" value=\"$fillStr\" />\n")
                    xml.append("    </effect>\n")

                    // 2. Gaussian Blur
                    val blurEff = layer.effects.firstOrNull { it is StudioEffect.GaussianBlur } as? StudioEffect.GaussianBlur
                    if (blurEff != null) {
                        val radius = blurEff.parameters["Radius"]?.value ?: 0f
                        val intensity = blurEff.parameters["Intensity"]?.value ?: 1f
                        val strength = (radius * intensity) / 200f
                        xml.append("    <effect id=\"com.alightcreative.effects.gaussianblur\">\n")
                        xml.append(String.format(Locale.US, "      <property name=\"strength\" type=\"float\" value=\"%.6f\" />\n", strength))
                        xml.append("    </effect>\n")
                    }

                    xml.append("  </text>\n")
                }
                else -> {}
            }
        }

        xml.append("</scene>\n")
        return xml.toString()
    }

    /**
     * Background thread runner to package and export Alight Motion XML directly to Download storage directory.
     */
    suspend fun saveAlightXmlToDownloads(
        context: Context,
        projectName: String,
        canvasWidth: Float,
        canvasHeight: Float,
        layers: List<StudioLayer>
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val filename = if (projectName.isNotBlank()) projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_") else "AlightExport"
        
        val details = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.xml")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/xml")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/AlightExports")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val fileUri = resolver.insert(collection, details) ?: return@withContext null

        try {
            resolver.openOutputStream(fileUri).use { outStream ->
                if (outStream != null) {
                    val xmlString = exportToAlightXmlString(canvasWidth, canvasHeight, projectName, layers)
                    outStream.write(xmlString.toByteArray(Charsets.UTF_8))
                    outStream.flush()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                details.clear()
                details.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(fileUri, details, null, null)
            }
            return@withContext fileUri
        } catch (e: Throwable) {
            e.printStackTrace()
            try {
                resolver.delete(fileUri, null, null)
            } catch (ignored: Exception) {}
            return@withContext null
        }
    }
}
