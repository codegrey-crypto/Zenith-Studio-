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
    var paddingPx: Float = 0f

    fun invalidate() {
        isDirty = true
    }
}

object ParametricLayerCache {
    val nodes = object : java.util.LinkedHashMap<String, RenderCacheNode>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RenderCacheNode>?): Boolean {
            if (size > 40) {
                eldest?.value?.cachedBitmap?.recycle()
                return true
            }
            return false
        }
    }
    
    val exportNodes = object : java.util.LinkedHashMap<String, RenderCacheNode>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RenderCacheNode>?): Boolean {
            if (size > 20) {
                eldest?.value?.cachedBitmap?.recycle()
                return true
            }
            return false
        }
    }

    @Synchronized
    fun getOrCreateNode(layerId: String, isExporting: Boolean = false): RenderCacheNode {
        return if (isExporting) {
            exportNodes.getOrPut(layerId) { RenderCacheNode() }
        } else {
            nodes.getOrPut(layerId) { RenderCacheNode() }
        }
    }

    @Synchronized
    fun invalidate(layerId: String) {
        nodes[layerId]?.invalidate()
        exportNodes[layerId]?.invalidate()
    }

    @Synchronized
    fun clear() {
        nodes.values.forEach {
            it.cachedBitmap?.recycle()
        }
        nodes.clear()
        clearExportCache()
    }

    @Synchronized
    fun clearExportCache() {
        exportNodes.values.forEach {
            it.cachedBitmap?.recycle()
        }
        exportNodes.clear()
    }

    @Synchronized
    fun duplicateNode(originalId: String, newId: String) {
        val origNode = nodes[originalId]
        if (origNode != null) {
            val newNode = RenderCacheNode()
            newNode.paramHash = origNode.paramHash
            newNode.cachedBitmap = origNode.cachedBitmap
            newNode.cachedImageBitmap = origNode.cachedImageBitmap
            newNode.paddingPx = origNode.paddingPx
            newNode.isDirty = origNode.isDirty
            nodes[newId] = newNode
        }
    }

    @Synchronized
    fun canRecycleBitmap(bitmap: Bitmap?): Boolean {
        if (bitmap == null) return true
        var count = 0
        nodes.values.forEach { if (it.cachedBitmap === bitmap) count++ }
        exportNodes.values.forEach { if (it.cachedBitmap === bitmap) count++ }
        return count <= 1
    }

    fun computeParamHash(layer: StudioLayer): Int {
        var result = layer.type.hashCode()
        result = 31 * result + layer.width.hashCode()
        result = 31 * result + layer.height.hashCode()
        result = 31 * result + layer.baseColor.hashCode()
        result = 31 * result + (layer.imageUri?.hashCode() ?: 0)
        result = 31 * result + (layer.threeDStateJson?.hashCode() ?: 0)
        result = 31 * result + (layer.imageResourceId ?: 0)
        result = 31 * result + layer.textContent.hashCode()
        result = 31 * result + layer.richTextSpansJson.hashCode()
        result = 31 * result + layer.fontSize.hashCode()
        result = 31 * result + layer.letterSpacing.hashCode()
        result = 31 * result + layer.lineSpacing.hashCode()
        result = 31 * result + layer.fontFamilyName.hashCode()
        result = 31 * result + layer.fontIsBold.hashCode()
        result = 31 * result + layer.fontIsItalic.hashCode()
        result = 31 * result + layer.fontAlign.hashCode()
        result = 31 * result + (layer.fontPath?.hashCode() ?: 0)
        result = 31 * result + layer.cornerRadius.hashCode()
        result = 31 * result + layer.polygonEdges.hashCode()
        result = 31 * result + layer.starInnerRadiusRatio.hashCode()
        result = 31 * result + layer.strokeThickness.hashCode()
        result = 31 * result + layer.opacity.hashCode()
        result = 31 * result + layer.blendMode.hashCode()
        result = 31 * result + layer.perspWarpEnabled.hashCode()
        result = 31 * result + layer.perspWarpSplitY.hashCode()
        result = 31 * result + layer.perspWarpWidth.hashCode()
        result = 31 * result + layer.perspWarpHeight.hashCode()
        
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
    private val transformChannel = Channel<CanvasEvent.UpdateTransform>(Channel.CONFLATED)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeSliderJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    @Volatile private var isStarted = false

    fun emit(event: CanvasEvent) {
        if (event is CanvasEvent.UpdateTransform) {
            transformChannel.trySend(event)
        } else {
            eventChannel.trySend(event)
        }
    }

    @Synchronized
    fun startProcessing(
        onUpdateTransform: (Float, Float, Float, Float) -> Unit,
        onUpdateSlider: (String, String, String, Float) -> Unit,
        onUpdateLayerProp: (String, String, Float) -> Unit
    ) {
        if (isStarted) return
        isStarted = true
        // Collect transformChannel synchronized with hardware display VSYNC via Choreographer to guarantee perfect 120 FPS
        scope.launch(Dispatchers.Main) {
            val callback = object : android.view.Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    val transform = transformChannel.tryReceive().getOrNull()
                    if (transform != null) {
                        var latest = transform
                        while (true) {
                            val next = transformChannel.tryReceive().getOrNull() ?: break
                            latest = next
                        }
                        onUpdateTransform(latest.panX, latest.panY, latest.scale, latest.rotation)
                    }
                    if (isActive) {
                        android.view.Choreographer.getInstance().postFrameCallback(this)
                    }
                }
            }
            android.view.Choreographer.getInstance().postFrameCallback(callback)
        }

        scope.launch {
            for (event in eventChannel) {
                when (event) {
                    is CanvasEvent.UpdateTransform -> {
                        withContext(Dispatchers.Main) {
                            onUpdateTransform(event.panX, event.panY, event.scale, event.rotation)
                        }
                    }
                    is CanvasEvent.UpdateSlider -> {
                        com.aistudio.zenithstudio.rpxwtq.EffectStackManager.notifySliderInteraction(event.layerId)
                        ParametricLayerCache.invalidate(event.layerId)
                        withContext(Dispatchers.Main) {
                            onUpdateSlider(event.layerId, event.effectId, event.paramName, event.value)
                        }
                    }
                    is CanvasEvent.UpdateLayerProp -> {
                        com.aistudio.zenithstudio.rpxwtq.EffectStackManager.notifySliderInteraction(event.layerId)
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
