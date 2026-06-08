package com.example.studio.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.example.studio.model.LayerType
import com.example.studio.model.StudioLayer
import com.example.studio.model.ZenithBlendMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

object PsdImportEngine {

    class ParsedPsd(
        val canvasWidth: Float,
        val canvasHeight: Float,
        val layers: List<StudioLayer>,
        val projectDpi: Int,
        val rawWidth: Int,
        val rawHeight: Int
    )

    /**
     * Parse ZenithBlendMode from PSD 4-byte blend mode keys
     */
    private fun parsePsdBlendMode(key: String): ZenithBlendMode {
        return when (key.trim()) {
            "norm" -> ZenithBlendMode.NORMAL
            "add " -> ZenithBlendMode.ADD
            "mul " -> ZenithBlendMode.MULTIPLY
            "scrn" -> ZenithBlendMode.SCREEN
            "over" -> ZenithBlendMode.OVERLAY
            "dark" -> ZenithBlendMode.DARKEN
            "lite" -> ZenithBlendMode.LIGHTEN
            "idiv" -> ZenithBlendMode.COLOR_BURN
            "div " -> ZenithBlendMode.COLOR_DODGE
            "diff" -> ZenithBlendMode.DIFFERENCE
            "excl" -> ZenithBlendMode.EXCLUSION
            "hue " -> ZenithBlendMode.HUE
            "sat " -> ZenithBlendMode.SATURATION
            "colr" -> ZenithBlendMode.COLOR
            "lum " -> ZenithBlendMode.LUMINOSITY
            else -> ZenithBlendMode.NORMAL
        }
    }

    /**
     * Read a layered PSD document sequentially via a DataInputStream.
     */
    suspend fun parsePsdStream(
        context: Context,
        inputStream: InputStream,
        onProgress: (String) -> Unit
    ): ParsedPsd = withContext(Dispatchers.IO) {
        android.util.Log.d("PSD_IMPORT", "Starting parsePsdStream...")
        onProgress("Reading file header...")
        val bis = BufferedInputStream(inputStream)
        val dis = DataInputStream(bis)

        // 1. FILE HEADER SECTION
        val signature = readString(dis, 4)
        if (signature != "8BPS") {
            android.util.Log.e("PSD_IMPORT", "Header signature mismatch: expected 8BPS but found '$signature'")
            throw IllegalArgumentException("Invalid file format: Header signature must be 8BPS (found '$signature')")
        }
        val version = dis.readUnsignedShort()
        if (version != 1) {
            android.util.Log.e("PSD_IMPORT", "Unsupported version: expected 1 but found $version")
            throw IllegalArgumentException("Unsupported PSD version: $version (only standard PSD v1 is supported)")
        }
        // Skip 6 reserved bytes
        dis.skipBytes(6)
        val projectChannels = dis.readUnsignedShort()
        val ch = dis.readInt()
        val cw = dis.readInt()
        val depth = dis.readUnsignedShort()
        val colorMode = dis.readUnsignedShort()

        android.util.Log.d("PSD_IMPORT", "Parsed file header successfully: dimensions=${cw}x${ch}, channels=$projectChannels, colorMode=$colorMode, depth=$depth")
        onProgress("Header parsed: ${cw}x${ch} (${projectChannels} channels)...")

        // 2. COLOR MODE DATA SECTION
        val colorModeLen = dis.readInt()
        if (colorModeLen > 0) {
            android.util.Log.d("PSD_IMPORT", "Skipping color mode data block: size=$colorModeLen")
            dis.skipBytes(colorModeLen)
        }

        // 3. IMAGE RESOURCES SECTION
        val imageResLen = dis.readInt()
        var projectDpi = 300
        if (imageResLen > 0) {
            android.util.Log.d("PSD_IMPORT", "Parsing image resources block: size=$imageResLen")
            onProgress("Parsing project resolution metadata...")
            val resData = ByteArray(imageResLen)
            dis.readFully(resData)
            projectDpi = parseDpiFromImageResources(resData)
            android.util.Log.d("PSD_IMPORT", "Extracted DPI from image resources: $projectDpi DPI")
        }

        // 4. LAYER AND MASK INFORMATION SECTION
        val layerAndMaskSectionLen = dis.readInt()
        val layersToCreate = mutableListOf<StudioLayer>()

        android.util.Log.d("PSD_IMPORT", "Layer & mask section length: $layerAndMaskSectionLen bytes")
        if (layerAndMaskSectionLen > 0) {
            onProgress("Parsing layer structure metadata...")
            // We read the entire layer and mask section into a safe sub-stream or parse sequentially
            val layerInfoLen = dis.readInt()
            android.util.Log.d("PSD_IMPORT", "Layer records sub-section length: $layerInfoLen bytes")
            if (layerInfoLen > 0) {
                val layerCountRaw = dis.readShort().toInt()
                val isAbsoluteCount = layerCountRaw < 0
                val layerCount = if (isAbsoluteCount) -layerCountRaw else layerCountRaw
                android.util.Log.d("PSD_IMPORT", "Found $layerCount layers in the PSD document structure specification.")
                onProgress("Found $layerCount layers. Parsing records...")

                // Track layer temp headers
                val layerHeaders = mutableListOf<LayerTempHeader>()

                for (i in 0 until layerCount) {
                    val top = dis.readInt()
                    val left = dis.readInt()
                    val bottom = dis.readInt()
                    val right = dis.readInt()
                    val channelCount = dis.readUnsignedShort()

                    val channelInfos = mutableListOf<ChannelTempInfo>()
                    for (c in 0 until channelCount) {
                        val channelId = dis.readShort().toInt()
                        val channelDataLen = dis.readInt()
                        channelInfos.add(ChannelTempInfo(channelId, channelDataLen))
                    }

                    val blendSignature = readString(dis, 4)
                    if (blendSignature != "8BIM" && blendSignature != "8BPS") {
                        throw IllegalArgumentException("Unsupported blend mode signature: $blendSignature")
                    }
                    val blendKey = readString(dis, 4)
                    val opacityByte = dis.readUnsignedByte()
                    val clipping = dis.readUnsignedByte()
                    val flags = dis.readUnsignedByte()
                    dis.skipBytes(1) // Filler

                    val extraDataLen = dis.readInt()
                    var layerName = "Layer $i"
                    var isTextType = false
                    var textString = ""

                    if (extraDataLen > 0) {
                        // Let's load extra data block into memory to parse naming safely
                        val extraBytes = ByteArray(extraDataLen)
                        dis.readFully(extraBytes)
                        val extraStream = DataInputStream(ByteArrayInputStream(extraBytes))

                        // Mask Data length
                        val maskDataLen = extraStream.readInt()
                        if (maskDataLen > 0) {
                            extraStream.skipBytes(maskDataLen)
                        }

                        // Blending ranges length
                        val blendRangesLen = extraStream.readInt()
                        if (blendRangesLen > 0) {
                            extraStream.skipBytes(blendRangesLen)
                        }

                        // Pascal Naming String
                        if (extraStream.available() > 0) {
                            val nameLen = extraStream.readUnsignedByte()
                            if (nameLen > 0) {
                                val nameBytes = ByteArray(nameLen)
                                extraStream.readFully(nameBytes)
                                layerName = String(nameBytes, StandardCharsets.UTF_8).trim()
                            }
                            // Skip padding bytes (multiple of 4 relative to start of naming field)
                            val writtenPascalLen = 1 + nameLen
                            val paddedPascalLength = ((writtenPascalLen + 3) / 4) * 4
                            val paddingNeeded = paddedPascalLength - writtenPascalLen
                            if (paddingNeeded > 0 && extraStream.available() >= paddingNeeded) {
                                extraStream.skipBytes(paddingNeeded)
                            }
                        }

                        // Scan remaining additional info blocks (8BIM) for text descriptors (e.g. "TySh" or "TxTo")
                        while (extraStream.available() >= 12) {
                            val chunkSig = readString(extraStream, 4)
                            if (chunkSig == "8BIM" || chunkSig == "8BPS") {
                                val chunkKey = readString(extraStream, 4)
                                val chunkSize = extraStream.readInt()
                                val paddedSize = ((chunkSize + 1) / 2) * 2 // Padded to even size

                                if (chunkKey == "TySh" && paddedSize <= extraStream.available()) {
                                    isTextType = true
                                    val textBlockBytes = ByteArray(paddedSize)
                                    extraStream.readFully(textBlockBytes)
                                    textString = extractTextFromTyShBlock(textBlockBytes) ?: ""
                                } else if (paddedSize <= extraStream.available()) {
                                    extraStream.skipBytes(paddedSize)
                                } else {
                                    break
                                }
                            } else {
                                // Skip one byte and retry syncing to 8BIM signatures
                                extraStream.skipBytes(1)
                            }
                        }
                    }

                    layerHeaders.add(
                        LayerTempHeader(
                            top = top,
                            left = left,
                            bottom = bottom,
                            right = right,
                            layerName = layerName,
                            opacity = opacityByte / 255f,
                            blendMode = parsePsdBlendMode(blendKey),
                            visible = (flags and 0x02) == 0,
                            isClippingMask = clipping == 1,
                            channels = channelInfos,
                            isText = isTextType,
                            textValue = textString
                        )
                    )
                }

                // 2. Read individual Channel Image Packets sequentially
                // Memory Optimization: Decode sequentially to keep RAM safe
                var layerIdx = 0
                for (header in layerHeaders) {
                    layerIdx++
                    val lw = (header.right - header.left).coerceAtLeast(1)
                    val lh = (header.bottom - header.top).coerceAtLeast(1)

                    android.util.Log.d("PSD_IMPORT", "Decoding layer $layerIdx/${layerHeaders.size}: '${header.layerName}' size=${lw}x${lh}")
                    onProgress("Decoding layer $layerIdx/${layerHeaders.size}: ${header.layerName} (${lw}x${lh})...")

                    // Containers for channel bytes
                    var rBytes = ByteArray(lw * lh) { 255.toByte() }
                    var gBytes = ByteArray(lw * lh) { 255.toByte() }
                    var bBytes = ByteArray(lw * lh) { 255.toByte() }
                    var aBytes = ByteArray(lw * lh) { 255.toByte() }

                    var hasAlpha = false

                    for (chanInfo in header.channels) {
                        val compression = dis.readShort().toInt()
                        val rawDataSize = chanInfo.dataLen - 2L

                        android.util.Log.d("PSD_IMPORT", "  Channel ID ${chanInfo.channelId}: compression=$compression, length=$rawDataSize bytes")

                        val uncompressedChannelBytes = ByteArray(lw * lh)

                        if (compression == 0) { // RAW Compression
                            dis.readFully(uncompressedChannelBytes, 0, (lw * lh).coerceAtMost(rawDataSize.toInt()))
                        } else if (compression == 1) { // RLE Compression
                            // Read line lengths (lh * 2 bytes)
                            val lineLengths = IntArray(lh)
                            for (y in 0 until lh) {
                                lineLengths[y] = dis.readUnsignedShort()
                            }

                            // Decompress line by line
                            var destOffset = 0
                            for (y in 0 until lh) {
                                val compressedLen = lineLengths[y]
                                val compressedBytes = ByteArray(compressedLen)
                                dis.readFully(compressedBytes)
                                decompressRleLine(compressedBytes, uncompressedChannelBytes, destOffset, lw)
                                destOffset += lw
                            }
                        } else {
                            // Unsupported ZIP, skip bytes safely
                            dis.skipBytes(rawDataSize.toInt())
                        }

                        // Map decoded channel to respective RGBA containers
                        // Channel ID: 0 = Red, 1 = Green, 2 = Blue, -1 = Transparency Mask
                        when (chanInfo.channelId) {
                            0 -> rBytes = uncompressedChannelBytes
                            1 -> gBytes = uncompressedChannelBytes
                            2 -> bBytes = uncompressedChannelBytes
                            -1 -> {
                                aBytes = uncompressedChannelBytes
                                hasAlpha = true
                            }
                        }
                    }

                    // Create final ARGB Bitmap
                    val layerBitmap = Bitmap.createBitmap(lw, lh, Bitmap.Config.ARGB_8888)
                    val rawPixels = IntArray(lw * lh)
                    for (p in rawPixels.indices) {
                        val r = rBytes[p].toInt() and 0xFF
                        val g = gBytes[p].toInt() and 0xFF
                        val b = bBytes[p].toInt() and 0xFF
                        val a = if (hasAlpha) (aBytes[p].toInt() and 0xFF) else 255
                        rawPixels[p] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    layerBitmap.setPixels(rawPixels, 0, lw, 0, 0, lw, lh)

                    // Write the imported bitmap as a local file inside context persistent dir to assign imageUri
                    val localImageFile = File(context.filesDir, "imports/psd_assets")
                    if (!localImageFile.exists()) {
                        localImageFile.mkdirs()
                    }
                    val assetFile = File(localImageFile, "layer_${UUID.randomUUID()}.png")
                    FileOutputStream(assetFile).use { fos ->
                        layerBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                    layerBitmap.recycle()

                    // Build StudioLayer matching the decoded parameters
                    val layerStructure = StudioLayer(
                        id = UUID.randomUUID().toString(),
                        name = header.layerName,
                        type = if (header.isText && header.textValue.isNotEmpty()) LayerType.TEXT else LayerType.IMAGE_CARD,
                        positionX = header.left.toFloat(),
                        positionY = header.top.toFloat(),
                        width = lw.toFloat(),
                        height = lh.toFloat(),
                        baseColor = Color.White,
                        opacity = header.opacity,
                        isVisible = header.visible,
                        isClippingMask = header.isClippingMask,
                        blendMode = header.blendMode,
                        imageUri = assetFile.absolutePath,
                        textContent = header.textValue
                    )

                    layersToCreate.add(layerStructure)
                }
            }
        }

        // Return completed result (PSD layers run bottom-up, top of list is top of drawer layout, matches Zenith reverse logic)
        android.util.Log.d("PSD_IMPORT", "Successfully decoded all layered elements! Total layers: ${layersToCreate.size}")
        onProgress("Reassembling layout layers...")
        ParsedPsd(
            canvasWidth = cw.toFloat(),
            canvasHeight = ch.toFloat(),
            layers = layersToCreate.reversed(), // Re-align to bottom-to-top drawing stacking inside the app
            projectDpi = projectDpi,
            rawWidth = cw,
            rawHeight = ch
        )
    }

    /**
     * Parse resolution info (0x03ED) from Image Resources payload block to retrieve DPI successfully.
     */
    private fun parseDpiFromImageResources(data: ByteArray): Int {
        try {
            val dis = DataInputStream(ByteArrayInputStream(data))
            while (dis.available() >= 12) {
                val sig = readString(dis, 4)
                if (sig == "8BIM") {
                    val id = dis.readUnsignedShort()
                    val nameLen = dis.readUnsignedByte()
                    val paddedNameLen = ((nameLen + 2) / 2) * 2
                    dis.skipBytes(paddedNameLen - 1) // Read already 1 byte

                    val dataSize = dis.readInt()
                    val paddedDataSize = ((dataSize + 1) / 2) * 2

                    if (id == 0x03ED && paddedDataSize <= dis.available()) {
                        // Horiz density fixed float (16.16)
                        val horizDensityFixed = dis.readInt()
                        val dpi = horizDensityFixed / 65536
                        return if (dpi in 10..2400) dpi else 300
                    } else if (paddedDataSize <= dis.available()) {
                        dis.skipBytes(paddedDataSize)
                    } else {
                        break
                    }
                } else {
                    dis.skipBytes(1)
                }
            }
        } catch (ignored: Exception) {}
        return 300
    }

    /**
     * Unescape text block data in Photoshop TySh block structures.
     */
    private fun extractTextFromTyShBlock(bytes: ByteArray): String? {
        try {
            // Locate unicode markers or raw string characters inside Adobe text engine descriptor blocks
            val content = String(bytes, StandardCharsets.ISO_8859_1)
            // Photoshop usually hosts texts inside /Text (text) or similar structures
            if (content.contains("/Text (")) {
                val sub = content.substringAfter("/Text (")
                val text = sub.substringBefore(")")
                val cleanText = text.replace("\\(", "(").replace("\\)", ")")
                // Clean octal sequences if any like \376\377
                if (cleanText.startsWith("\\376\\377")) {
                    return ""
                }
                return cleanText
            }
        } catch (ignored: Exception) {}
        return null
    }

    /**
     * UnpackBits decompression algorithm.
     */
    private fun decompressRleLine(src: ByteArray, dest: ByteArray, destOffset: Int, expectedLength: Int) {
        var srcIndex = 0
        var destIndex = destOffset
        val destEnd = destOffset + expectedLength

        while (destIndex < destEnd && srcIndex < src.size) {
            val b = src[srcIndex++].toInt()
            if (b >= 0) { // (0 <= b <= 127) -> Copy b + 1 literal data bytes
                val count = b + 1
                for (i in 0 until count) {
                    if (destIndex < destEnd && srcIndex < src.size) {
                        dest[destIndex++] = src[srcIndex++]
                    }
                }
            } else if (b != -128) { // (-127 <= b <= -1) -> Repeat byte (-b + 1) times
                val count = -b + 1
                if (srcIndex < src.size) {
                    val value = src[srcIndex++]
                    for (i in 0 until count) {
                        if (destIndex < destEnd) {
                            dest[destIndex++] = value
                        }
                    }
                }
            }
        }
    }

    private fun readString(dis: DataInputStream, len: Int): String {
        val bytes = ByteArray(len)
        dis.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private data class ChannelTempInfo(
        val channelId: Int,
        val dataLen: Int
    )

    private data class LayerTempHeader(
        val top: Int,
        val left: Int,
        val bottom: Int,
        val right: Int,
        val layerName: String,
        val opacity: Float,
        val blendMode: ZenithBlendMode,
        val visible: Boolean,
        val isClippingMask: Boolean,
        val channels: List<ChannelTempInfo>,
        val isText: Boolean,
        val textValue: String
    )
}
