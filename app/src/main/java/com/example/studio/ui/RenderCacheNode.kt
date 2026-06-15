package com.example.studio.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.studio.model.StudioLayer
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class RenderCacheNode {
    var paramHash: Int = 0
    var cachedBitmap: Bitmap? = null
    var cachedImageBitmap: ImageBitmap? = null
    var isDirty: Boolean = false

    fun invalidate() {
        isDirty = true
    }
}

object ParametricLayerCache {
    private val nodes = ConcurrentHashMap<String, RenderCacheNode>()

    fun getOrCreateNode(layerId: String): RenderCacheNode {
        return nodes.getOrPut(layerId) { RenderCacheNode() }
    }

    fun invalidate(layerId: String) {
        nodes[layerId]?.invalidate()
    }

    fun clear() {
        nodes.values.forEach {
            it.cachedBitmap?.recycle()
        }
        nodes.clear()
    }

    fun computeParamHash(layer: StudioLayer): Int {
        var result = layer.id.hashCode()
        result = 31 * result + layer.width.hashCode()
        result = 31 * result + layer.height.hashCode()
        result = 31 * result + layer.baseColor.hashCode()
        result = 31 * result + layer.textContent.hashCode()
        result = 31 * result + layer.fontSize.hashCode()
        result = 31 * result + layer.cornerRadius.hashCode()
        result = 31 * result + layer.polygonEdges.hashCode()
        result = 31 * result + layer.strokeThickness.hashCode()
        result = 31 * result + layer.opacity.hashCode()
        result = 31 * result + layer.blendMode.hashCode()
        
        // Hash active effects and their slider parameters
        for (effect in layer.effects) {
            if (effect.isEnabled) {
                result = 31 * result + effect.id.hashCode()
                for ((paramName, param) in effect.parameters) {
                    result = 31 * result + paramName.hashCode()
                    result = 31 * result + param.value.hashCode()
                }
            }
        }

        // Hash brush points count and end values
        result = 31 * result + layer.brushPoints.size
        if (layer.brushPoints.isNotEmpty()) {
            result = 31 * result + layer.brushPoints.first().hashCode()
            result = 31 * result + layer.brushPoints.last().hashCode()
        }
        return result
    }
}

// Infinite Viewport layout behavior: completely eliminate bounds clipping.
// All layout elements are treated as part of an infinite coordinate grid,
// drawing without hardware bounds constraints regardless of zoom/pan state.
fun isLayerVisibleInViewport(
    layer: StudioLayer,
    viewportWidth: Float,
    viewportHeight: Float,
    panX: Float,
    panY: Float,
    scale: Float,
    canvasWidth: Float,
    canvasHeight: Float
): Boolean {
    // Natively bypass bounds checks to prevent boundary-based element clipping in multi-artboard setups
    return true
}

fun getLayerVisibleLocalRect(
    layer: StudioLayer,
    viewportWidth: Float,
    viewportHeight: Float,
    panX: Float,
    panY: Float,
    scale: Float,
    canvasWidth: Float,
    canvasHeight: Float
): android.graphics.Rect {
    // Return the full layer layout dimensions so that pixel processing is never artificially limited or clipped at viewport edges
    return android.graphics.Rect(0, 0, layer.width.toInt(), layer.height.toInt())
}

// Graphite-inspired Unidirectional Asynchronous Message-Passing Event Loop
sealed class CanvasEvent {
    data class UpdateTransform(val panX: Float, val panY: Float, val scale: Float, val rotation: Float) : CanvasEvent()
    data class UpdateSlider(val layerId: String, val effectId: String, val paramName: String, val value: Float) : CanvasEvent()
    data class UpdateLayerProp(val layerId: String, val propName: String, val value: Float) : CanvasEvent()
}

object CanvasEventLoop {
    private val eventChannel = Channel<CanvasEvent>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun emit(event: CanvasEvent) {
        eventChannel.trySend(event)
    }

    fun startProcessing(
        onUpdateTransform: (Float, Float, Float, Float) -> Unit,
        onUpdateSlider: (String, String, String, Float) -> Unit,
        onUpdateLayerProp: (String, String, Float) -> Unit
    ) {
        scope.launch {
            for (event in eventChannel) {
                when (event) {
                    is CanvasEvent.UpdateTransform -> {
                        withContext(Dispatchers.Main) {
                            onUpdateTransform(event.panX, event.panY, event.scale, event.rotation)
                        }
                    }
                    is CanvasEvent.UpdateSlider -> {
                        ParametricLayerCache.invalidate(event.layerId)
                        withContext(Dispatchers.Main) {
                            onUpdateSlider(event.layerId, event.effectId, event.paramName, event.value)
                        }
                    }
                    is CanvasEvent.UpdateLayerProp -> {
                        ParametricLayerCache.invalidate(event.layerId)
                        withContext(Dispatchers.Main) {
                            onUpdateLayerProp(event.layerId, event.propName, event.value)
                        }
                    }
                }
            }
        }
    }
}
