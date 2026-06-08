package com.example.studio.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import com.example.studio.model.LayerType
import com.example.studio.model.StudioLayer
import com.example.studio.model.StudioEffect
import com.example.studio.model.ZenithBlendMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.Locale

object PsdExportEngine {

    /**
     * Maps our app blend mode enum to PSD standard 4-byte blend mode keys
     */
    private fun getPsdBlendModeKey(mode: ZenithBlendMode): String {
        return when (mode) {
            ZenithBlendMode.NORMAL -> "norm"
            ZenithBlendMode.ADD -> "add "
            ZenithBlendMode.MULTIPLY -> "mul "
            ZenithBlendMode.SCREEN -> "scrn"
            ZenithBlendMode.OVERLAY -> "over"
            ZenithBlendMode.LINEAR_DODGE -> "add "
            ZenithBlendMode.DARKEN -> "dark"
            ZenithBlendMode.LIGHTEN -> "lite"
            ZenithBlendMode.COLOR_BURN -> "idiv"
            ZenithBlendMode.COLOR_DODGE -> "div "
            ZenithBlendMode.DIFFERENCE -> "diff"
            ZenithBlendMode.EXCLUSION -> "excl"
            ZenithBlendMode.HUE -> "hue "
            ZenithBlendMode.SATURATION -> "sat "
            ZenithBlendMode.COLOR -> "colr"
            ZenithBlendMode.LUMINOSITY -> "lum "
        }
    }

    /**
     * Render an individual layer completely in its local bounds so we can extract its raw pixel values.
     */
    private fun renderLayerToBitmap(
        context: Context,
        layer: StudioLayer,
        imageBitmapCache: Map<String, ImageBitmap>
    ): Bitmap {
        val w = layer.width.toInt().coerceAtLeast(1)
        val h = layer.height.toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val effectiveColor = getLayerEffectiveColor(layer, 1.0f)
        val paint = Paint().apply {
            color = effectiveColor.toArgb()
            val hasStroke = layer.strokeThickness > 0f && layer.type !in listOf(
                LayerType.FREEHAND_DRAWING,
                LayerType.IMAGE_CARD,
                LayerType.TEXT
            )
            if (hasStroke) {
                style = Paint.Style.STROKE
                strokeWidth = layer.strokeThickness
            } else {
                style = Paint.Style.FILL
            }
            isAntiAlias = true
        }

        canvas.save()

        // Apply same local transformation matrix used during main canvas export
        val centerX = layer.width * layer.pivotX
        val centerY = layer.height * layer.pivotY

        val m = Matrix()
        m.reset()
        m.postTranslate(centerX, centerY)

        val sx = layer.skewX
        val sy = layer.skewY
        val px = layer.perspX
        val py = layer.perspY

        if (sx != 0f || sy != 0f || px != 0f || py != 0f) {
            val skewPersp = Matrix()
            val vals = FloatArray(9)
            skewPersp.getValues(vals)
            vals[Matrix.MSKEW_X] = sx
            vals[Matrix.MSKEW_Y] = sy
            vals[Matrix.MPERSP_0] = px
            vals[Matrix.MPERSP_1] = py
            skewPersp.setValues(vals)
            m.postConcat(skewPersp)
        }

        m.postRotate(layer.rotation)
        m.postScale(layer.scaleX, layer.scaleY)
        m.postTranslate(-centerX, -centerY)
        canvas.concat(m)

        when (layer.type) {
            LayerType.VECTOR_RECT -> {
                if (layer.cornerRadius > 0f) {
                    canvas.drawRoundRect(0f, 0f, layer.width, layer.height, layer.cornerRadius, layer.cornerRadius, paint)
                } else {
                    canvas.drawRect(0f, 0f, layer.width, layer.height, paint)
                }
            }
            LayerType.VECTOR_TRIANGLE, LayerType.VECTOR_PENTAGON, LayerType.VECTOR_HEXAGON -> {
                val edges = when (layer.type) {
                    LayerType.VECTOR_TRIANGLE -> if (layer.polygonEdges in 3..25) layer.polygonEdges else 3
                    LayerType.VECTOR_PENTAGON -> if (layer.polygonEdges in 3..25) layer.polygonEdges else 5
                    else -> if (layer.polygonEdges in 3..25) layer.polygonEdges else 6
                }
                val path = Path().apply {
                    val cx = layer.width / 2f
                    val cy = layer.height / 2f
                    val rx = layer.width / 2f
                    val ry = layer.height / 2f
                    for (i in 0 until edges) {
                        val angle = Math.toRadians((i * (360.0 / edges) - 90).toDouble())
                        val x = (cx + rx * Math.cos(angle)).toFloat()
                        val y = (cy + ry * Math.sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                val paintToUse = if (layer.cornerRadius > 0f) {
                    Paint(paint).apply {
                        pathEffect = android.graphics.CornerPathEffect(layer.cornerRadius)
                    }
                } else paint
                canvas.drawPath(path, paintToUse)
            }
            LayerType.VECTOR_OVAL -> {
                canvas.drawOval(0f, 0f, layer.width, layer.height, paint)
            }
            LayerType.VECTOR_LINE -> {
                val strokeW = if (paint.style == Paint.Style.STROKE) paint.strokeWidth else (if (layer.strokeThickness > 0f) layer.strokeThickness else 6f)
                val linePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                }
                canvas.drawLine(0f, 0f, layer.width, layer.height, linePaint)
            }
            LayerType.VECTOR_CIRCLE -> {
                val radius = layer.width / 2f
                canvas.drawCircle(radius, layer.height / 2f, radius, paint)
            }
            LayerType.VECTOR_STAR -> {
                val path = Path().apply {
                    val cx = layer.width / 2f
                    val cy = layer.height / 2f
                    val rOuter = layer.width / 2f
                    val rInner = rOuter * layer.starInnerRadiusRatio.coerceIn(0.01f, 0.99f)
                    val pointsCount = if (layer.polygonEdges >= 3) layer.polygonEdges else 5
                    var angle = Math.PI / 2.0 * 3.0
                    val step = Math.PI / pointsCount

                    moveTo(
                        (cx + Math.cos(angle) * rOuter).toFloat(),
                        (cy + Math.sin(angle) * rOuter).toFloat()
                    )

                    for (i in 0..(pointsCount * 2)) {
                        val r = if (i % 2 == 0) rOuter else rInner
                        lineTo(
                            (cx + Math.cos(angle) * r).toFloat(),
                            (cy + Math.sin(angle) * r).toFloat()
                        )
                        angle += step
                    }
                    close()
                }
                val paintToUse = if (layer.cornerRadius > 0f) {
                    Paint(paint).apply {
                        pathEffect = android.graphics.CornerPathEffect(layer.cornerRadius)
                    }
                } else paint
                canvas.drawPath(path, paintToUse)
            }
            LayerType.VECTOR_BEZIER -> {
                val path = Path()
                val start = Offset(0f, layer.height)
                val end = Offset(layer.width, layer.height)
                val controlLocal = layer.brushPoints.getOrNull(0) ?: Offset(layer.width / 2f, 0f)

                if (layer.brushPoints.size > 1) {
                    val startPt = layer.brushPoints[0]
                    path.moveTo(startPt.x, startPt.y)
                    var i = 1
                    while (i < layer.brushPoints.size) {
                        val ctrl = layer.brushPoints.getOrNull(i) ?: break
                        val endPt = layer.brushPoints.getOrNull(i + 1) ?: ctrl
                        path.quadTo(ctrl.x, ctrl.y, endPt.x, endPt.y)
                        i += 2
                    }
                } else {
                    path.moveTo(start.x, start.y)
                    path.quadTo(controlLocal.x, controlLocal.y, end.x, end.y)
                }

                paint.color = effectiveColor.toArgb()
                if (layer.isAlphaLocked) {
                    paint.style = Paint.Style.FILL
                } else {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 8f
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                }
                canvas.drawPath(path, paint)
            }
            LayerType.FREEHAND_DRAWING -> {
                try {
                    if (layer.brushPoints.isNotEmpty()) {
                        val brushConfig = layer.effects.find { it is StudioEffect.PhotoshopEffect && it.effectType == "BrushConfig" } as? StudioEffect.PhotoshopEffect
                        val bSize = brushConfig?.parameters?.get("Size")?.value ?: 12f
                        val bOpacity = brushConfig?.parameters?.get("Opacity")?.value ?: 1.0f
                        val bSmoothing = (brushConfig?.parameters?.get("Smoothing")?.value ?: 1.0f) > 0.5f
                        val bPreset = brushConfig?.parameters?.get("Preset")?.value?.toInt() ?: 0

                        val strokes = parseFreehandStrokes(
                            brushPoints = layer.brushPoints,
                            defaultColor = effectiveColor,
                            defaultSize = bSize,
                            defaultOpacity = bOpacity * layer.opacity,
                            defaultPreset = bPreset,
                            defaultSmoothing = bSmoothing
                        )

                        for (stroke in strokes) {
                            if (stroke.points.size > 1) {
                                val paintBrush = Paint().apply {
                                    color = stroke.color.toArgb()
                                    strokeWidth = stroke.size
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    isAntiAlias = true
                                    alpha = (stroke.opacity * 255).toInt().coerceIn(0, 255)
                                    if (stroke.isEraser) {
                                        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                    }
                                }

                                val pathBrush = Path()
                                pathBrush.moveTo(stroke.points[0].x, stroke.points[0].y)
                                for (pIdx in 1 until stroke.points.size) {
                                    pathBrush.lineTo(stroke.points[pIdx].x, stroke.points[pIdx].y)
                                }
                                canvas.drawPath(pathBrush, paintBrush)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
            LayerType.TEXT -> {
                val text = if (layer.textContent.isEmpty()) "DOUBLE TAP TO EDIT" else layer.textContent
                val typeface = try {
                    if (!layer.fontPath.isNullOrEmpty() && java.io.File(layer.fontPath).exists()) {
                        val baseTf = android.graphics.Typeface.createFromFile(layer.fontPath)
                        val style = if (layer.fontIsBold && layer.fontIsItalic) {
                            android.graphics.Typeface.BOLD_ITALIC
                        } else if (layer.fontIsBold) {
                            android.graphics.Typeface.BOLD
                        } else if (layer.fontIsItalic) {
                            android.graphics.Typeface.ITALIC
                        } else {
                            android.graphics.Typeface.NORMAL
                        }
                        android.graphics.Typeface.create(baseTf, style)
                    } else {
                        val family = when (layer.fontFamilyName) {
                            "Monospace" -> android.graphics.Typeface.MONOSPACE
                            "Serif" -> android.graphics.Typeface.SERIF
                            "Sans-Serif" -> android.graphics.Typeface.SANS_SERIF
                            else -> android.graphics.Typeface.DEFAULT
                        }
                        val style = if (layer.fontIsBold && layer.fontIsItalic) {
                            android.graphics.Typeface.BOLD_ITALIC
                        } else if (layer.fontIsBold) {
                            android.graphics.Typeface.BOLD
                        } else if (layer.fontIsItalic) {
                            android.graphics.Typeface.ITALIC
                        } else {
                            android.graphics.Typeface.NORMAL
                        }
                        android.graphics.Typeface.create(family, style)
                    }
                } catch (e: Exception) {
                    android.graphics.Typeface.DEFAULT
                }
                val textPaint = TextPaint().apply {
                    color = effectiveColor.toArgb()
                    textSize = layer.fontSize
                    isAntiAlias = true
                    this.typeface = typeface
                }
                val align = when (layer.fontAlign.lowercase(Locale.ROOT)) {
                    "left" -> android.text.Layout.Alignment.ALIGN_NORMAL
                    "right" -> android.text.Layout.Alignment.ALIGN_OPPOSITE
                    else -> android.text.Layout.Alignment.ALIGN_CENTER
                }
                val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxOf(1, layer.width.toInt()))
                        .setAlignment(align)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(text, textPaint, maxOf(1, layer.width.toInt()), align, 1f, 0f, false)
                }
                canvas.save()
                val totalHeight = staticLayout.height
                val startY = maxOf(0f, (layer.height - totalHeight) / 2f)
                canvas.translate(0f, startY)
                staticLayout.draw(canvas)
                canvas.restore()
            }
            LayerType.IMAGE_CARD -> {
                val loadedBitmap = imageBitmapCache[layer.imageUri ?: ""]?.asAndroidBitmap()
                if (loadedBitmap != null) {
                    val destRect = Rect(0, 0, layer.width.toInt(), layer.height.toInt())
                    val imagePaint = Paint().apply {
                        isAntiAlias = true
                        alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                    }
                    canvas.drawBitmap(loadedBitmap, null, destRect, imagePaint)
                }
            }
        }

        canvas.restore()
        return bitmap
    }

    /**
     * Safely calculate layer opacity and color multiplier
     */
    private fun getLayerEffectiveColor(layer: StudioLayer, intensity: Float): Color {
        val base = layer.baseColor
        val op = layer.opacity * intensity
        return base.copy(alpha = base.alpha * op)
    }

    /**
     * Helper subclass for parsing freehand vector strokes.
     */
    private class ParseStroke(
        val points: List<Offset>,
        val color: Color,
        val size: Float,
        val opacity: Float,
        val isEraser: Boolean
    )

    private fun parseFreehandStrokes(
        brushPoints: List<Offset>,
        defaultColor: Color,
        defaultSize: Float,
        defaultOpacity: Float,
        defaultPreset: Int,
        defaultSmoothing: Boolean
    ): List<ParseStroke> {
        val list = mutableListOf<ParseStroke>()
        var curPoints = mutableListOf<Offset>()
        for (pt in brushPoints) {
            if (pt == Offset.Unspecified) {
                if (curPoints.isNotEmpty()) {
                    list.add(ParseStroke(curPoints, defaultColor, defaultSize, defaultOpacity, defaultPreset == 3))
                    curPoints = mutableListOf()
                }
            } else {
                curPoints.add(pt)
            }
        }
        if (curPoints.isNotEmpty()) {
            list.add(ParseStroke(curPoints, defaultColor, defaultSize, defaultOpacity, defaultPreset == 3))
        }
        return list
    }

    /**
     * Performs multi-layer PSD formatting and incrementally writes to Output Stream via Dispatchers.IO.
     */
    suspend fun savePsdToGallery(
        context: Context,
        layers: List<StudioLayer>,
        canvasWidth: Float,
        canvasHeight: Float,
        imageBitmapCache: Map<String, ImageBitmap>,
        filename: String,
        projectDpi: Int = 300
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val details = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.psd")
            put(MediaStore.Images.Media.MIME_TYPE, "image/x-photoshop")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoshopExports")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(imageCollection, details) ?: return@withContext null

        try {
            resolver.openOutputStream(imageUri).use { outStream ->
                if (outStream != null) {
                    writePsdToStream(
                        context = context,
                        outStream = outStream,
                        layers = layers,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        imageBitmapCache = imageBitmapCache,
                        projectDpi = projectDpi
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                details.clear()
                details.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, details, null, null)
            }
            return@withContext imageUri
        } catch (e: Throwable) {
            e.printStackTrace()
            // Delete corrupt file
            try {
                resolver.delete(imageUri, null, null)
            } catch (ignored: Exception) {}
            return@withContext null
        }
    }

    /**
     * Incremental PSD structures writing loops.
     */
    private fun writePsdToStream(
        context: Context,
        outStream: OutputStream,
        layers: List<StudioLayer>,
        canvasWidth: Float,
        canvasHeight: Float,
        imageBitmapCache: Map<String, ImageBitmap>,
        projectDpi: Int
    ) {
        val bos = BufferedOutputStream(outStream)
        val dos = DataOutputStream(bos)

        val cw = canvasWidth.toInt().coerceAtLeast(1)
        val ch = canvasHeight.toInt().coerceAtLeast(1)

        // 1. FILE HEADER SECTION
        dos.writeBytes("8BPS") // Signature
        dos.writeShort(1)     // Version = 1 (PSD)
        dos.write(ByteArray(6)) // Reserved bytes (zeroes)
        dos.writeShort(4)     // Color Channels = 4 (R, G, B, A in PSD)
        dos.writeInt(ch)      // Canvas Height
        dos.writeInt(cw)      // Canvas Width
        dos.writeShort(8)     // Depth = 8 bits per channel
        dos.writeShort(3)     // Color Mode = 3 (RGB Color Mode)

        // 2. COLOR MODE DATA SECTION
        dos.writeInt(0)       // Length = 0 for RGB Mode

        // 3. IMAGE RESOURCES SECTION (Dynamic DPI Density preservation block)
        // Store ResolutionInfo block at key 0x03ED
        val resStream = ByteArrayOutputStream()
        val dataRes = DataOutputStream(resStream)
        
        // Horizontal & Vertical density mapping: Fixed point 16.16 (dpi * 65536)
        val densityFixed = projectDpi * 65536
        dataRes.writeInt(densityFixed) // Horizontal DPI
        dataRes.writeShort(1)          // Unit (1 = pixels/inch)
        dataRes.writeShort(1)          // Width Unit (1 = inches)
        dataRes.writeInt(densityFixed) // Vertical DPI
        dataRes.writeShort(1)          // Unit (1 = pixels/inch)
        dataRes.writeShort(1)          // Height Unit (1 = inches)
        
        val resBytes = resStream.toByteArray()

        val resourceBlockStream = ByteArrayOutputStream()
        val resourceBlockData = DataOutputStream(resourceBlockStream)
        resourceBlockData.writeBytes("8BIM")
        resourceBlockData.writeShort(0x03ED) // ResolutionInfo block ID
        resourceBlockData.writeShort(0)      // Name (Pascal String length 0 -> padded to even = 2 bytes)
        resourceBlockData.writeInt(resBytes.size)
        resourceBlockData.write(resBytes)

        val resourceBlockBytes = resourceBlockStream.toByteArray()
        dos.writeInt(resourceBlockBytes.size) // Image resources content total length
        dos.write(resourceBlockBytes)         // Write image resources data block

        // 4. LAYER AND MASK INFORMATION SECTION (Multi-layer PSD Packaging structure)
        // Order: PSD layers run bottom-up, which is matching the list in app layers reversed
        val psdLayers = layers.filter { it.type != LayerType.IMAGE_CARD || it.imageUri != null }

        // Let's compute sizes and headers for active layers record
        var totalLayerRecordsSize = 0
        val layersInfoList = psdLayers.map { layer ->
            val w = layer.width.toInt().coerceAtLeast(1)
            val h = layer.height.toInt().coerceAtLeast(1)
            val nameClean = layer.name.take(251).replace("[^\\x20-\\x7E]".toRegex(), "")
            val nameLength = nameClean.length
            val paddedPascalLength = ((1 + nameLength + 3) / 4) * 4 // Padding Pascal String to multiple of 4

            // Extra fields size consists of Name string and extra metadata (including 4 bytes for mask data length and 4 bytes for blending ranges length)
            val extraDataSize = 4 + 4 + paddedPascalLength 
            val totalRecordSize = 16 + 2 + (4 * 6) + 4 + 4 + 1 + 1 + 1 + 1 + 4 + extraDataSize
            totalLayerRecordsSize += totalRecordSize

            object {
                val lInstance = layer
                val width = w
                val height = h
                val name = nameClean
                val nameLen = nameLength
                val padNameLen = paddedPascalLength
                val recordSize = totalRecordSize
                val extraSize = extraDataSize
            }
        }

        // Compute total channel image data bytes size inside PSD
        var totalChannelImageDataSize = 0L
        for (info in layersInfoList) {
            // 4 channels (Red, Green, Blue, Alpha)
            // Each channel: 2 bytes (compression) + (width * height) raw byte pixels
            val pixelsCount = info.width.toLong() * info.height.toLong()
            val layerImgDataSize = 4 * (2 + pixelsCount)
            totalChannelImageDataSize += layerImgDataSize
        }

        // Include Layer Count prefix (2 bytes)
        val layerCountAndRecordsTotalSize = 2L + totalLayerRecordsSize + totalChannelImageDataSize
        
        // Padded to even size if odd
        val psdLayersSectionPadding = if (layerCountAndRecordsTotalSize % 2 != 0L) 1 else 0
        val layerInfoLength = layerCountAndRecordsTotalSize + psdLayersSectionPadding

        // Section header specifies the total length (4 bytes) of the whole layers record
        dos.writeInt((4L + layerInfoLength).toInt()) // Layer and Mask section content length (excludes itself)
        dos.writeInt(layerInfoLength.toInt())        // Layer Info list section content length

        // Layer Count (negative means Alpha channel contains transparency metadata, positive count is neat)
        dos.writeShort(psdLayers.size)

        // Write the Layer Headers (Records) for each layer
        for (info in layersInfoList) {
            val lyr = info.lInstance

            // Rect bounds relative to the main canvas
            val top = lyr.positionY.toInt()
            val left = lyr.positionX.toInt()
            val bottom = top + info.height
            val right = left + info.width

            dos.writeInt(top)
            dos.writeInt(left)
            dos.writeInt(bottom)
            dos.writeInt(right)

            dos.writeShort(4) // 4 channels count (RGBA)

            // Dynamic channel entries: Channel IDs: 0 = Red, 1 = Green, 2 = Blue, -1 = Alpha
            val pixelsCount = info.width.toLong() * info.height.toLong()
            val singleChannelDataLength = 2L + pixelsCount

            // Red channel data record
            dos.writeShort(0)
            dos.writeInt(singleChannelDataLength.toInt())
            // Green channel data record
            dos.writeShort(1)
            dos.writeInt(singleChannelDataLength.toInt())
            // Blue channel data record
            dos.writeShort(2)
            dos.writeInt(singleChannelDataLength.toInt())
            // Alpha channel data record
            dos.writeShort(-1)
            dos.writeInt(singleChannelDataLength.toInt())

            dos.writeBytes("8BIM") // Blend mode signature
            dos.writeBytes(getPsdBlendModeKey(lyr.blendMode)) // 4-byte blend mode key

            val opInt = (lyr.opacity * 255f).toInt().coerceIn(0, 255)
            dos.writeByte(opInt) // Scale Opacity directly from layout state

            dos.writeByte(if (lyr.isClippingMask) 1 else 0) // Clipping behavior mapped

            // Visibility flags: Bit 1 holds invisible state
            val visFlag = if (lyr.isVisible) 0 else 2
            dos.writeByte(visFlag)

            dos.writeByte(0) // Filler byte

            // Length of Extra Data field
            dos.writeInt(info.extraSize)

            // Write 0 for mask data length (4 bytes)
            dos.writeInt(0)
            // Write 0 for blending ranges length (4 bytes)
            dos.writeInt(0)

            // Pascal Name String block writing with multiple of 4 padding
            dos.writeByte(info.nameLen)
            dos.writeBytes(info.name)
            val writtenPascalLen = 1 + info.nameLen
            val paddingNeeded = info.padNameLen - writtenPascalLen
            if (paddingNeeded > 0) {
                dos.write(ByteArray(paddingNeeded))
            }
        }

        // Write individual Layer Channel Pixel Bytes sequentially (Red, Green, Blue, Alpha)
        // Memory Optimization: We render layers sequentially AND recycle bitmaps immediately
        for (info in layersInfoList) {
            val lyr = info.lInstance
            val boundsCount = info.width * info.height

            // Render current layer into layout bitmap cache
            val lyrBmp = renderLayerToBitmap(context, lyr, imageBitmapCache)

            val rBytes = ByteArray(boundsCount)
            val gBytes = ByteArray(boundsCount)
            val bBytes = ByteArray(boundsCount)
            val aBytes = ByteArray(boundsCount)

            val rawPixels = IntArray(boundsCount)
            lyrBmp.getPixels(rawPixels, 0, info.width, 0, 0, info.width, info.height)

            for (p in rawPixels.indices) {
                val argb = rawPixels[p]
                aBytes[p] = ((argb shr 24) and 0xFF).toByte()
                rBytes[p] = ((argb shr 16) and 0xFF).toByte()
                gBytes[p] = ((argb shr 8) and 0xFF).toByte()
                bBytes[p] = (argb and 0xFF).toByte()
            }

            // Write Channel 0 (Red)
            dos.writeShort(0) // Raw compression code = 0
            dos.write(rBytes)

            // Write Channel 1 (Green)
            dos.writeShort(0) // Raw compression code = 0
            dos.write(gBytes)

            // Write Channel 2 (Blue)
            dos.writeShort(0) // Raw compression code = 0
            dos.write(bBytes)

            // Write Channel -1 (Alpha)
            dos.writeShort(0) // Raw compression code = 0
            dos.write(aBytes)

            // Low memory requirement check: recycle quickly to free RAM
            lyrBmp.recycle()
        }

        // Section padding if computed length is odd
        if (psdLayersSectionPadding > 0) {
            dos.writeByte(0)
        }

        // 5. IMAGE DATA SECTION (Standard composite background image required)
        dos.writeShort(0) // Raw compression code = 0

        // Produce flat composite render of the active workspace layers
        val compositeBmp = exportCanvasToBitmap(
            context = context,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            layers = layers,
            imageBitmapCache = imageBitmapCache,
            targetWidth = canvasWidth,
            targetHeight = canvasHeight,
            isCmyk = false
        )

        val compSize = cw * ch
        val compR = ByteArray(compSize)
        val compG = ByteArray(compSize)
        val compB = ByteArray(compSize)
        val compA = ByteArray(compSize)

        val compPixels = IntArray(compSize)
        compositeBmp.getPixels(compPixels, 0, cw, 0, 0, cw, ch)

        for (p in compPixels.indices) {
            val argb = compPixels[p]
            compA[p] = ((argb shr 24) and 0xFF).toByte()
            compR[p] = ((argb shr 16) and 0xFF).toByte()
            compG[p] = ((argb shr 8) and 0xFF).toByte()
            compB[p] = (argb and 0xFF).toByte()
        }

        // Write Channel 0 (Composite Red)
        dos.write(compR)
        // Write Channel 1 (Composite Green)
        dos.write(compG)
        // Write Channel 2 (Composite Blue)
        dos.write(compB)
        // Write Channel 3 (Composite Alpha)
        dos.write(compA)

        // Recycle background flat bitmap helper
        compositeBmp.recycle()

        dos.flush()
        dos.close()
    }
}
