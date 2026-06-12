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

// Viewport calculations for hardware clipping & Zoom Isolation
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
    if (viewportWidth <= 0f || viewportHeight <= 0f) return true
    val centerX = viewportWidth / 2f + panX
    val centerY = viewportHeight / 2f + panY

    val layerLeftScreen = centerX + (-canvasWidth / 2f + layer.positionX) * scale
    val layerTopScreen = centerY + (-canvasHeight / 2f + layer.positionY) * scale
    val layerRightScreen = layerLeftScreen + (layer.width * scale)
    val layerBottomScreen = layerTopScreen + (layer.height * scale)

    return !(layerLeftScreen > viewportWidth || 
             layerRightScreen < 0f || 
             layerTopScreen > viewportHeight || 
             layerBottomScreen < 0f)
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
    if (viewportWidth <= 0f || viewportHeight <= 0f) {
        return android.graphics.Rect(0, 0, layer.width.toInt(), layer.height.toInt())
    }
    val centerX = viewportWidth / 2f + panX
    val centerY = viewportHeight / 2f + panY

    val layerLeftScreen = centerX + (-canvasWidth / 2f + layer.positionX) * scale
    val layerTopScreen = centerY + (-canvasHeight / 2f + layer.positionY) * scale

    val overlapLeftScreen = maxOf(0f, layerLeftScreen)
    val overlapRightScreen = minOf(viewportWidth, layerLeftScreen + layer.width * scale)
    val overlapTopScreen = maxOf(0f, layerTopScreen)
    val overlapBottomScreen = minOf(viewportHeight, layerTopScreen + layer.height * scale)

    if (overlapLeftScreen >= overlapRightScreen || overlapTopScreen >= overlapBottomScreen) {
        return android.graphics.Rect(0, 0, 0, 0)
    }

    val localLeft = ((overlapLeftScreen - layerLeftScreen) / scale).toInt().coerceIn(0, layer.width.toInt())
    val localTop = ((overlapTopScreen - layerTopScreen) / scale).toInt().coerceIn(0, layer.height.toInt())
    val localRight = ((overlapRightScreen - layerLeftScreen) / scale).toInt().coerceIn(0, layer.width.toInt())
    val localBottom = ((overlapBottomScreen - layerTopScreen) / scale).toInt().coerceIn(0, layer.height.toInt())

    return android.graphics.Rect(localLeft, localTop, localRight, localBottom)
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
