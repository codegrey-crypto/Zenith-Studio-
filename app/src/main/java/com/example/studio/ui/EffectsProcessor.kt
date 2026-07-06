package com.example.studio.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.studio.model.StudioEffect
import com.example.studio.model.StudioLayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Robust EffectsProcessor coordinating high-frequency slider value updates
 * debounced and processed safely off the main UI thread using asynchronous snapshotFlow.
 */
object EffectsProcessor {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lastValidBitmaps = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()

    /**
     * Set up a debounced reactive tracking loop for a selected layer's slider updates.
     * Extracts values via snapshotFlow, filters and recalculates off-thread.
     */
    fun monitorLayerEffects(
        context: Context,
        layerFlow: StateFlow<StudioLayer?>,
        onResult: (ImageBitmap) -> Unit
    ): Job {
        return scope.launch {
            layerFlow
                .filterNotNull()
                .distinctUntilChanged { old, new ->
                    old.effects.hashCode() == new.effects.hashCode()
                }
                .debounce(4) // Conflate high-frequency updates (optimized for ultra-smooth 120 FPS display tracking)
                .mapLatest { layer ->
                    val layerId = layer.id
                    val activeEffects = layer.effects.filter { it.isEnabled }
                    
                    withContext(Dispatchers.Default) {
                        try {
                            val width = layer.width.toInt().coerceAtLeast(1)
                            val height = layer.height.toInt().coerceAtLeast(1)
                            val baseBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            
                            val filtered = applyGPUImageFilters(context, baseBmp, layerId, activeEffects)
                            lastValidBitmaps[layerId] = filtered
                            filtered.asImageBitmap()
                        } catch (e: Exception) {
                            android.util.Log.e("EffectsProcessor", "Calculations failed, falling back to last valid frame", e)
                            val fallback = lastValidBitmaps[layerId]
                            if (fallback != null) {
                                fallback.asImageBitmap()
                            } else {
                                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
                            }
                        }
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect { imageBitmap ->
                    withContext(Dispatchers.Main) {
                        onResult(imageBitmap)
                    }
                }
        }
    }
}
