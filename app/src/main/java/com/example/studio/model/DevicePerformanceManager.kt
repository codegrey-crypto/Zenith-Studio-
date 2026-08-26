package com.example.studio.model

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.studio.ui.ParametricLayerCache
import com.example.studio.ui.TypefaceCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PerformanceStats(
    val isLowRamDevice: Boolean = false,
    val totalMemoryMb: Long = 0,
    val availableMemoryMb: Long = 0,
    val usedMemoryMb: Long = 0,
    val freeStorageMb: Long = 0,
    val isLowStorage: Boolean = false,
    val isLowRamModeActive: Boolean = false,
    val maxUndoLimit: Int = 20,
    val renderScaleFactor: Float = 1.0f
)

object DevicePerformanceManager {
    private val _statsState = MutableStateFlow(PerformanceStats())
    val statsState: StateFlow<PerformanceStats> = _statsState.asStateFlow()

    private var isUserForcedLowRamMode: Boolean? = null

    fun initialize(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        val usedMb = (totalMb - availMb).coerceAtLeast(0)

        val isSystemLowRam = (am?.isLowRamDevice == true) || (totalMb > 0 && totalMb < 3000)
        val freeStorageMb = getFreeStorageMb(context)
        val isLowStorage = freeStorageMb < 100 // Less than 100MB free

        val activeLowRamMode = isUserForcedLowRamMode ?: isSystemLowRam

        val maxUndo = if (activeLowRamMode) 15 else 40
        val scaleFactor = if (activeLowRamMode) 0.5f else 1.0f

        _statsState.value = PerformanceStats(
            isLowRamDevice = isSystemLowRam,
            totalMemoryMb = totalMb,
            availableMemoryMb = availMb,
            usedMemoryMb = usedMb,
            freeStorageMb = freeStorageMb,
            isLowStorage = isLowStorage,
            isLowRamModeActive = activeLowRamMode,
            maxUndoLimit = maxUndo,
            renderScaleFactor = scaleFactor
        )
    }

    fun toggleLowRamMode(context: Context, enabled: Boolean) {
        isUserForcedLowRamMode = enabled
        initialize(context)
        if (enabled) {
            trimAllMemoryCaches()
        }
    }

    fun getFreeStorageMb(context: Context): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            bytesAvailable / (1024 * 1024)
        } catch (e: Exception) {
            0L
        }
    }

    fun getRenderScaleFactor(): Float {
        return _statsState.value.renderScaleFactor
    }

    fun getMaxUndoLimit(): Int {
        return _statsState.value.maxUndoLimit
    }

    fun getMaxBitmapDimension(): Int {
        return if (_statsState.value.isLowRamModeActive) 1200 else 2560
    }

    /**
     * Memory-aware bitmap loading decoder that downsamples large bitmaps to prevent OOM
     */
    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: android.net.Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val maxDim = getMaxBitmapDimension()
            val targetW = reqWidth.coerceAtMost(maxDim)
            val targetH = reqHeight.coerceAtMost(maxDim)

            options.inSampleSize = calculateInSampleSize(options, targetW, targetH)
            options.inJustDecodeBounds = false
            if (_statsState.value.isLowRamModeActive) {
                options.inPreferredConfig = Bitmap.Config.RGB_565 // 16-bit color consumes 50% less RAM than ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Clears all non-essential bitmap and font memory caches immediately
     */
    fun trimAllMemoryCaches() {
        try {
            ParametricLayerCache.clear()
            TypefaceCache.clear()
            com.example.studio.ui.OpenGLBrushRenderer.clearCaches()
            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.decodedTexturesCache.clear()
            com.aistudio.zenithstudio.rpxwtq.FilterPreviewCache.clear()
            System.gc()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes temporary history files, backup frames, and cached font temp files
     */
    fun cleanTemporaryStorage(context: Context): Long {
        var bytesFreed = 0L
        try {
            fun cleanDir(dir: File?): Long {
                if (dir == null || !dir.exists()) return 0L
                var total = 0L
                dir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        total += cleanDir(file)
                        if (file.listFiles()?.isEmpty() == true) {
                            file.delete()
                        }
                    } else if (file.isFile) {
                        val length = file.length()
                        if (file.delete()) {
                            total += length
                        }
                    }
                }
                return total
            }

            bytesFreed += cleanDir(context.cacheDir)
            context.externalCacheDir?.let { bytesFreed += cleanDir(it) }

            val historyDir = File(context.filesDir, "history")
            if (historyDir.exists()) {
                historyDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".tmp") || file.name.endsWith(".bak")) {
                        val len = file.length()
                        if (file.delete()) bytesFreed += len
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        trimAllMemoryCaches()
        initialize(context)
        return bytesFreed / (1024 * 1024)
    }
}
