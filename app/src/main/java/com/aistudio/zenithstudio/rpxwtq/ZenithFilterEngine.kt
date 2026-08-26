package com.aistudio.zenithstudio.rpxwtq

import android.content.Context
import android.graphics.*
import java.util.UUID
import kotlin.math.*

data class FilterParameter(
    val name: String,
    val currentValue: Float,
    val minValue: Float,
    val maxValue: Float,
    val unit: String = ""
)

sealed interface ZenithFilter {
    val id: String
    val name: String
    val category: String
    val parameters: List<FilterParameter>
    val isEnabled: Boolean

    fun computeShaderEffect(source: Bitmap, context: Context): Bitmap
    fun copyWithParameter(paramName: String, newValue: Float): ZenithFilter
    fun toggleEnabled(): ZenithFilter
    fun duplicate(newId: String): ZenithFilter
}

data class CustomZenithFilter(
    override val id: String,
    override val name: String,
    override val category: String,
    override val parameters: List<FilterParameter>,
    override val isEnabled: Boolean = true,
    val renderBlock: (Bitmap, List<FilterParameter>) -> Bitmap
) : ZenithFilter {
    override fun computeShaderEffect(source: Bitmap, context: Context): Bitmap {
        if (!isEnabled) return source
        return try {
            renderBlock(source, parameters)
        } catch (e: Exception) {
            source
        }
    }

    override fun copyWithParameter(paramName: String, newValue: Float): ZenithFilter {
        val updatedParams = parameters.map {
            if (it.name == paramName) it.copy(currentValue = newValue) else it
        }
        return this.copy(parameters = updatedParams)
    }

    override fun toggleEnabled(): ZenithFilter {
        return this.copy(isEnabled = !isEnabled)
    }

    override fun duplicate(newId: String): ZenithFilter {
        return this.copy(id = newId)
    }
}

data class GPUImageZenithFilter(
    override val id: String,
    override val name: String,
    override val category: String,
    override val parameters: List<FilterParameter>,
    override val isEnabled: Boolean = true,
    val renderBlock: (Bitmap, Context, List<FilterParameter>) -> Bitmap
) : ZenithFilter {
    override fun computeShaderEffect(source: Bitmap, context: Context): Bitmap {
        if (!isEnabled) return source
        return try {
            renderBlock(source, context, parameters)
        } catch (e: Exception) {
            source
        }
    }

    override fun copyWithParameter(paramName: String, newValue: Float): ZenithFilter {
        val updatedParams = parameters.map {
            if (it.name == paramName) it.copy(currentValue = newValue) else it
        }
        return this.copy(parameters = updatedParams)
    }

    override fun toggleEnabled(): ZenithFilter {
        return this.copy(isEnabled = !isEnabled)
    }

    override fun duplicate(newId: String): ZenithFilter {
        return this.copy(id = newId)
    }
}

private fun applySingleGPUImageFilter(context: Context, source: Bitmap, filter: jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter): Bitmap {
    val safeSource = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && source.config == Bitmap.Config.HARDWARE) {
        source.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        source
    }
    val gpuImage = jp.co.cyberagent.android.gpuimage.GPUImage(context).apply {
        setScaleType(jp.co.cyberagent.android.gpuimage.GPUImage.ScaleType.CENTER_INSIDE)
    }
    gpuImage.setFilter(filter)
    return gpuImage.getBitmapWithFilterApplied(safeSource)
}

object EffectStackManager {
    val filtersByLayer = androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<ZenithFilter>>()
    val texturesByLayer = androidx.compose.runtime.mutableStateMapOf<String, String>() // layerId -> absolute file path of texture
    val decodedTexturesCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()
    val patternImagesForFilters = androidx.compose.runtime.mutableStateMapOf<String, String>() // filterId -> absolute file path of texture
    
    val changeCounter = androidx.compose.runtime.mutableStateOf(0)
    val isDraggingSliderState = androidx.compose.runtime.mutableStateOf(false)
    var isDraggingSlider: Boolean
        get() = isDraggingSliderState.value
        set(value) { isDraggingSliderState.value = value }

    private var resetDragRunnable: Runnable? = null
    private val mainHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun notifySliderInteraction(layerId: String? = null) {
        if (!isDraggingSliderState.value) {
            isDraggingSliderState.value = true
        }
        resetDragRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            isDraggingSliderState.value = false
            if (layerId != null) {
                com.example.studio.ui.ParametricLayerCache.invalidate(layerId)
            }
        }
        resetDragRunnable = runnable
        mainHandler.postDelayed(runnable, 150L) // 150ms high-fidelity recalculation debounce
    }

    // Downscale performance settings
    val isDownscaleEnabled = androidx.compose.runtime.mutableStateOf(false)
    val downscaleFactor = androidx.compose.runtime.mutableStateOf(2) // 2=1/2, 3=1/3, etc.
    val downscaleBilinear = androidx.compose.runtime.mutableStateOf(false) // true=Bilinear, false=Nearest Neighbor
    val downscaleOnlyOnDrag = androidx.compose.runtime.mutableStateOf(false) // always downscale vs only on interaction

    // Parallel Undo/Redo stack for professional non-destructive effect pipelines
    private val undoStack = java.util.Stack<Map<String, List<ZenithFilter>>>()
    private val redoStack = java.util.Stack<Map<String, List<ZenithFilter>>>()
    
    private var lastUpdatedParamKey: String? = null
    private var lastUpdateTime = 0L

    private fun createSnapshot(): Map<String, List<ZenithFilter>> {
        return filtersByLayer.mapValues { entry ->
            entry.value.toList()
        }
    }

    fun saveUndoState() {
        undoStack.push(createSnapshot())
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun performUndo(): Boolean {
        if (undoStack.isEmpty()) return false
        val current = createSnapshot()
        redoStack.push(current)
        val previous = undoStack.pop()
        restoreSnapshot(previous)
        return true
    }

    fun performRedo(): Boolean {
        if (redoStack.isEmpty()) return false
        val current = createSnapshot()
        undoStack.push(current)
        val next = redoStack.pop()
        restoreSnapshot(next)
        return true
    }

    private fun restoreSnapshot(snapshot: Map<String, List<ZenithFilter>>) {
        filtersByLayer.clear()
        snapshot.forEach { (layerId, list) ->
            val mutableList = androidx.compose.runtime.mutableStateListOf<ZenithFilter>()
            mutableList.addAll(list)
            filtersByLayer[layerId] = mutableList
        }
        changeCounter.value++
    }
    
    fun getFiltersForLayer(layerId: String): androidx.compose.runtime.snapshots.SnapshotStateList<ZenithFilter> {
        return filtersByLayer.getOrPut(layerId) {
            androidx.compose.runtime.mutableStateListOf()
        }
    }

    fun duplicateFiltersForLayer(sourceLayerId: String, destLayerId: String) {
        saveUndoState()
        val sourceFilters = filtersByLayer[sourceLayerId]
        if (sourceFilters != null && sourceFilters.isNotEmpty()) {
            val destList = getFiltersForLayer(destLayerId)
            destList.clear()
            sourceFilters.forEach { filter ->
                val suffix = java.util.UUID.randomUUID().toString().take(6)
                destList.add(filter.duplicate("${filter.id}_dup_${suffix}"))
            }
            changeCounter.value++
        }
    }
    
    fun addFilter(layerId: String, filter: ZenithFilter) {
        saveUndoState()
        val list = getFiltersForLayer(layerId)
        if (!list.any { it.id == filter.id }) {
            list.add(filter)
            changeCounter.value++
        }
    }
    
    fun removeFilter(layerId: String, id: String) {
        saveUndoState()
        if (getFiltersForLayer(layerId).removeAll { it.id == id }) {
            changeCounter.value++
        }
    }
    
    fun duplicateFilter(layerId: String, id: String) {
        saveUndoState()
        val list = getFiltersForLayer(layerId)
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val original = list[index]
            val suffix = java.util.UUID.randomUUID().toString().take(6)
            val newId = "${original.id}_copy_${suffix}"
            val duplicated = original.duplicate(newId)
            
            // Duplicate pattern image if exists
            patternImagesForFilters[original.id]?.let { imgPath ->
                patternImagesForFilters[newId] = imgPath
            }
            
            list.add(index + 1, duplicated)
            changeCounter.value++
        }
    }
    
    fun toggleFilter(layerId: String, id: String) {
        saveUndoState()
        val list = getFiltersForLayer(layerId)
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = list[index].toggleEnabled()
            changeCounter.value++
        }
    }
    
    fun updateParameter(layerId: String, filterId: String, paramName: String, newValue: Float) {
        notifySliderInteraction()
        val paramKey = "${layerId}_${filterId}_${paramName}"
        val now = System.currentTimeMillis()
        if (paramKey != lastUpdatedParamKey || now - lastUpdateTime > 800L) {
            saveUndoState()
            lastUpdatedParamKey = paramKey
        }
        lastUpdateTime = now

        val list = getFiltersForLayer(layerId)
        val index = list.indexOfFirst { it.id == filterId }
        if (index != -1) {
            list[index] = list[index].copyWithParameter(paramName, newValue)
            changeCounter.value++
        }
    }
    
    fun clearAll(layerId: String) {
        saveUndoState()
        val list = getFiltersForLayer(layerId)
        if (list.isNotEmpty()) {
            list.clear()
            changeCounter.value++
        }
    }
}

object FilterPreviewCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()
    private var baseSample: Bitmap? = null
    private var lastBaseBitmapHash: Int = 0

    fun getBaseSample(customBase: Bitmap?): Bitmap {
        var base = baseSample
        val baseHash = customBase?.hashCode() ?: 0
        if (base == null || (customBase != null && baseHash != lastBaseBitmapHash)) {
            val targetSize = 100
            if (customBase != null) {
                lastBaseBitmapHash = baseHash
                val w = customBase.width
                val h = customBase.height
                val scale = targetSize.toFloat() / maxOf(w, h)
                val nw = (w * scale).toInt().coerceAtLeast(1)
                val nh = (h * scale).toInt().coerceAtLeast(1)
                base = Bitmap.createScaledBitmap(customBase, nw, nh, true)
            } else {
                base = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(base)
                val p = Paint(Paint.ANTI_ALIAS_FLAG)
                
                // Sunset gradient
                val lg = LinearGradient(0f, 0f, 0f, 100f, 
                    Color.parseColor("#FF4E50"), Color.parseColor("#F9D423"), 
                    Shader.TileMode.CLAMP)
                p.shader = lg
                canvas.drawRect(0f, 0f, 100f, 100f, p)
                p.shader = null

                // Sun
                p.color = Color.parseColor("#FFFFFF")
                canvas.drawCircle(70f, 30f, 12f, p)

                // Mountain 1 (dark blue)
                p.color = Color.parseColor("#1B2A47")
                val path1 = android.graphics.Path()
                path1.moveTo(0f, 100f)
                path1.lineTo(30f, 50f)
                path1.lineTo(70f, 100f)
                path1.close()
                canvas.drawPath(path1, p)

                // Mountain 2 (midnight blue)
                p.color = Color.parseColor("#0F172A")
                val path2 = android.graphics.Path()
                path2.moveTo(40f, 100f)
                path2.lineTo(75f, 40f)
                path2.lineTo(100f, 90f)
                path2.lineTo(100f, 100f)
                path2.close()
                canvas.drawPath(path2, p)
            }
            baseSample = base
        }
        return base
    }

    fun getOrCreatePreview(filter: ZenithFilter, context: Context, customBase: Bitmap? = null): Bitmap {
        val baseHash = customBase?.hashCode() ?: 0
        if (customBase != null && baseHash != lastBaseBitmapHash) {
            cache.clear()
        }
        
        val base = getBaseSample(customBase)
        val key = "${filter.id}_preview_v2"
        val cached = cache[key]
        if (cached != null) return cached

        val copy = base.copy(Bitmap.Config.ARGB_8888, true)
        val filtered = try {
            filter.computeShaderEffect(copy, context)
        } catch (e: Exception) {
            copy
        }
        cache[key] = filtered
        return filtered
    }

    fun clear() {
        cache.clear()
        baseSample = null
    }
}

object ZenithFilterFactory {
    private val templatesCached: List<ZenithFilter> by lazy { createFilterList() }
    private val templateMapCached: Map<String, ZenithFilter> by lazy { templatesCached.associateBy { it.id } }

    fun getAllFilters(): List<ZenithFilter> = templatesCached

    fun getFilterTemplate(id: String): ZenithFilter? {
        return templateMapCached[id]
    }

    fun createFilterList(): List<ZenithFilter> {
        val list = mutableListOf<ZenithFilter>(
            // ==========================================
            // Category: Color Adjustments
            // ==========================================
            createDitheringFilter("image_toolbox_dither", "Dithering", listOf(
                FilterParameter("Dither Type (FS=0, B4=1, B8=2, Rnd=3)", 0f, 0f, 3f),
                FilterParameter("Quantization Levels", 4f, 2f, 16f),
                FilterParameter("Monochrome (No=0, Yes=1)", 1f, 0f, 1f)
            )),
            createColorAdjustFilter("image_toolbox_exposure", "Exposure", listOf(FilterParameter("EV Amount", 0.0f, -4.0f, 4.0f))),
            createColorAdjustFilter("image_toolbox_brightness", "Brightness", listOf(FilterParameter("Level", 0.0f, -100.0f, 100.0f))),
            createColorAdjustFilter("image_toolbox_contrast", "Contrast", listOf(FilterParameter("Amount", 1.0f, 0.0f, 3.0f))),
            createColorAdjustFilter("image_toolbox_saturation", "Saturation", listOf(FilterParameter("Factor", 1.0f, 0.0f, 3.0f))),
            createColorAdjustFilter("image_toolbox_hue", "Hue Shift", listOf(FilterParameter("Shift degrees", 0.0f, -180.0f, 180.0f, "°"))),
            createColorAdjustFilter("image_toolbox_vignette", "Vignette", listOf(FilterParameter("Radius amount", 0.5f, 0.0f, 1.0f))),

            createColorAdjustFilter("color_fast_adjusting", "Fast Adjusting", listOf(
                FilterParameter("Exposure", 0.0f, -4.0f, 4.0f),
                FilterParameter("Brightness", 0.0f, -100.0f, 100.0f),
                FilterParameter("Contrast", 1.0f, 0.0f, 3.0f),
                FilterParameter("Saturation", 1.0f, 0.0f, 3.0f),
                FilterParameter("Highlights", 0.0f, -100.0f, 100.0f),
                FilterParameter("Shadows", 0.0f, -100.0f, 100.0f),
                FilterParameter("Whites", 0.0f, -100.0f, 100.0f),
                FilterParameter("Blacks", 0.0f, -100.0f, 100.0f),
                FilterParameter("Temp", 0.0f, -100.0f, 100.0f),
                FilterParameter("Tint", 0.0f, -100.0f, 100.0f),
                FilterParameter("Vibrance", 0.0f, -100.0f, 100.0f),
                FilterParameter("Sharpness", 0.0f, 0.0f, 100.0f),
                FilterParameter("Dehaze", 0.0f, -100.0f, 100.0f),
                FilterParameter("Vignette", 0.0f, -100.0f, 100.0f),
                FilterParameter("Texture", 0.0f, -1.0f, 1.0f),
                FilterParameter("Structure", 0.0f, -1.0f, 1.0f)
            )),
            createColorAdjustFilter("color_grayscale", "Grayscale", emptyList()),
            createColorAdjustFilter("color_sepia", "Sepia Tone 2", listOf(FilterParameter("Intensity", 0.8f, 0.0f, 1.0f))),
            createColorAdjustFilter("color_saturation", "Saturation 2", listOf(FilterParameter("Factor", 1.0f, 0.0f, 3.0f))),
            createColorAdjustFilter("color_contrast", "Contrast Correction 2", listOf(FilterParameter("Amount", 1.0f, 0.0f, 3.0f))),
            createColorAdjustFilter("color_brightness", "Brightness Booster 2", listOf(FilterParameter("Level", 0.0f, -100.0f, 100.0f))),
            createColorAdjustFilter("color_exposure", "Exposure Level 2", listOf(FilterParameter("EV Amount", 0.0f, -4.0f, 4.0f))),
            createColorAdjustFilter("color_vignette", "Vignette Focus 2", listOf(FilterParameter("Radius amount", 0.5f, 0.0f, 1.0f))),
            createColorAdjustFilter("color_hue", "Hue Angle Shift 2", listOf(FilterParameter("Shift degrees", 0.0f, -180.0f, 180.0f, "°"))),
            createColorAdjustFilter("color_rgb", "RGB Level Mixer 2", listOf(FilterParameter("Red", 1.0f, 0.0f, 2.0f), FilterParameter("Green", 1.0f, 0.0f, 2.0f), FilterParameter("Blue", 1.0f, 0.0f, 2.0f))),
            createColorAdjustFilter("color_highlights", "Highlight levels 2", listOf(FilterParameter("Level", 0.0f, -1.0f, 1.0f))),
            createColorAdjustFilter("color_shadows", "Shadow recovery 2", listOf(FilterParameter("Level", 0.0f, -1.0f, 1.0f))),
            createColorAdjustFilter("color_temp", "Color Temperature 2", listOf(FilterParameter("Warmth index", 0.0f, -1.0f, 1.0f))),
            createColorAdjustFilter("color_replace_color", "Replace Color 2", listOf(
                FilterParameter("SourceRed", 1.0f, 0.0f, 1.0f),
                FilterParameter("SourceGreen", 0.0f, 0.0f, 1.0f),
                FilterParameter("SourceBlue", 0.0f, 0.0f, 1.0f),
                FilterParameter("TargetRed", 0.0f, 0.0f, 1.0f),
                FilterParameter("TargetGreen", 0.0f, 0.0f, 1.0f),
                FilterParameter("TargetBlue", 1.0f, 0.0f, 1.0f),
                FilterParameter("Tolerance", 0.15f, 0.0f, 1.0f)
            )),

            // ==========================================
            // Category 1: ARTISTIC EFFECTS
            // ==========================================
            createArtisticFilter("image_toolbox_oil_paint", "Oil Paint", listOf(FilterParameter("Brush Curvature", 12f, 2f, 40f))),

            createArtisticFilter("artistic_pencil", "Colored Pencil", listOf(FilterParameter("Scale", 15f, 5f, 40f))),
            createArtisticFilter("artistic_cutout", "Cutout", listOf(FilterParameter("Levels", 4f, 2f, 16f))),
            createArtisticFilter("artistic_drybrush", "Dry Brush", listOf(FilterParameter("Moisture", 3f, 1f, 10f))),
            createArtisticFilter("artistic_fresco", "Fresco", listOf(FilterParameter("Contrast", 12f, 1f, 30f))),
            createArtisticFilter("artistic_neonglow", "Neon Glow", listOf(FilterParameter("Glow Radius", 10f, 2f, 40f))),
            createArtisticFilter("artistic_paintdaubs", "Paint Daubs", listOf(FilterParameter("Brush Size", 8f, 1f, 25f))),
            createArtisticFilter("artistic_paletteknife", "Palette Knife", listOf(FilterParameter("Knife Size", 10f, 2f, 30f))),
            createArtisticFilter("artistic_plasticwrap", "Plastic Wrap", listOf(FilterParameter("Gloss", 15f, 1f, 50f))),
            createArtisticFilter("artistic_posteredges", "Poster Edges", listOf(FilterParameter("Edge Thickness", 2f, 1f, 8f))),
            createArtisticFilter("artistic_roughpastels", "Rough Pastels", listOf(FilterParameter("Roughness", 12f, 1f, 30f))),
            createArtisticFilter("artistic_smudgestick", "Smudge Stick", listOf(FilterParameter("Bleed Space", 8f, 2f, 24f))),
            createArtisticFilter("artistic_sponge", "Sponge", listOf(FilterParameter("Porosity", 10f, 2f, 30f))),
            createArtisticFilter("artistic_underpainting", "Underpainting", listOf(FilterParameter("Glaze opacity", 0.6f, 0.1f, 1.0f))),
            createArtisticFilter("artistic_watercolor", "Watercolor Paint", listOf(FilterParameter("Bleeding", 10f, 2f, 30f))),
            createArtisticFilter("artistic_oil_kuwahara", "Oil Painting Kuwahara 2", listOf(FilterParameter("Kuwahara Radius", 4f, 1f, 10f))),
            createArtisticFilter("artistic_toon", "Comic Book Toon", listOf(FilterParameter("Quantization Levels", 5f, 2f, 10f))),
            createArtisticFilter("artistic_hologram", "Hologram Scanlines", listOf(FilterParameter("Fringe Intensity", 15f, 2f, 50f))),

            // ==========================================
            // Category 2: BLUR & BLUR GALLERY
            // ==========================================
            createBlurFilter("image_toolbox_gaussian_blur", "Gaussian Blur", listOf(FilterParameter("Radius", 15f, 1f, 100f))),
            createBlurFilter("image_toolbox_box_blur", "Box Blur", listOf(FilterParameter("Radius", 12f, 1f, 100f))),
            createBlurFilter("image_toolbox_motion_blur", "Motion Blur", listOf(FilterParameter("Distance", 20f, 1f, 100f), FilterParameter("Angle", 45f, 0f, 360f))),

            createBlurFilter("blur_average", "Average 2", emptyList()),
            createBlurFilter("blur_gaussian", "Gaussian Blur 2", listOf(FilterParameter("Radius", 15f, 1f, 100f))),
            createBlurFilter("blur_box", "Box Blur 2", listOf(FilterParameter("Radius", 12f, 1f, 100f))),
            createBlurFilter("blur_lens", "Lens Blur 2", listOf(FilterParameter("Bokeh Radius", 18f, 1f, 80f))),
            createBlurFilter("blur_motion", "Motion Blur 2", listOf(FilterParameter("Distance", 20f, 1f, 100f), FilterParameter("Angle", 45f, 0f, 360f))),
            createBlurFilter("blur_radial", "Radial Blur 2", listOf(FilterParameter("Factor", 15f, 1f, 50f))),
            createBlurFilter("blur_shape", "Shape Blur 2", listOf(FilterParameter("Scale", 12f, 1f, 40f))),
            createBlurFilter("blur_smart", "Smart Blur 2", listOf(FilterParameter("Threshold", 25f, 1f, 100f), FilterParameter("Radius", 8f, 1f, 30f))),
            createBlurFilter("blur_surface", "Surface Blur 2", listOf(FilterParameter("Threshold", 20f, 1f, 100f), FilterParameter("Radius", 6f, 1f, 25f))),
            createBlurFilter("blur_field", "Field Blur 2", listOf(FilterParameter("Density", 10f, 1f, 50f))),
            createBlurFilter("blur_iris", "Iris Blur 2", listOf(FilterParameter("Radius", 30f, 10f, 120f))),
            createBlurFilter("blur_tiltshift", "Tilt-Shift 2", listOf(FilterParameter("Width", 50f, 10f, 200f))),
            createBlurFilter("blur_path", "Path Blur 2", listOf(FilterParameter("Flow", 15f, 1f, 50f))),
            createBlurFilter("blur_spin", "Spin Blur 2", listOf(FilterParameter("Speed", 20f, 1f, 90f))),
            createBlurFilter("blur_stack", "Fast Stack Blur 2", listOf(FilterParameter("Blur Radius", 15f, 1f, 80f))),
            createBlurFilter("blur_zoom", "Radial Zoom Blur 2", listOf(FilterParameter("Power Strength", 10f, 0f, 50f))),
            createBlurFilter("blur_bilateral", "Bilateral filter 2", listOf(FilterParameter("Spatial Delta", 10f, 1f, 40f), FilterParameter("Color Delta", 25f, 5f, 100f))),
            createBlurFilter("blur_bokeh", "Circle Highlights Bokeh 2", listOf(FilterParameter("Bokeh Radius", 12f, 1f, 60f), FilterParameter("Brightness Threshold", 180f, 100f, 255f))),

            // ==========================================
            // Category 3: BRUSH STROKES
            // ==========================================
            createBrushFilter("brush_accented", "Accented Edges", listOf(FilterParameter("Edge Width", 2f, 1f, 10f))),
            createBrushFilter("brush_angled", "Angled Strokes", listOf(FilterParameter("Angle", 45f, 0f, 180f))),
            createBrushFilter("brush_crosshatch", "Crosshatch", listOf(FilterParameter("Density", 8f, 2f, 20f))),
            createBrushFilter("brush_dark", "Dark Strokes", listOf(FilterParameter("Density", 6f, 1f, 15f))),
            createBrushFilter("brush_ink", "Ink Outlines", listOf(FilterParameter("Ink Weight", 3f, 1f, 10f))),
            createBrushFilter("brush_spatter", "Spatter", listOf(FilterParameter("Spray Radius", 10f, 2f, 40f))),
            createBrushFilter("brush_sprayed", "Sprayed Strokes", listOf(FilterParameter("Scattering", 12f, 2f, 30f))),
            createBrushFilter("brush_sumie", "Sumi-e", listOf(FilterParameter("Saturation", 5f, 1f, 20f))),

            // ==========================================
            // Category 4: DISTORT
            // ==========================================
            createDistortFilter("image_toolbox_ripple", "Ripple", listOf(FilterParameter("Amplitude", 12f, 1f, 50f), FilterParameter("Wavelength", 30f, 5f, 100f))),
            createDistortFilter("image_toolbox_pinch", "Pinch", listOf(FilterParameter("Amount", 0.5f, -1.0f, 1.0f))),
            createDistortFilter("image_toolbox_twirl", "Twirl", listOf(FilterParameter("Angle Degrees", 90f, -360f, 360f))),
            createDistortFilter("image_toolbox_wave", "Wave", listOf(FilterParameter("Amplitude", 15f, 1f, 50f), FilterParameter("Wavelength", 40f, 10f, 150f))),
            createDistortFilter("image_toolbox_spherize", "Spherize", listOf(FilterParameter("Curvature", 0.6f, 0.1f, 1.5f))),
            createDistortFilter("image_toolbox_zigzag", "ZigZag", listOf(FilterParameter("Frequency", 10f, 2f, 40f))),

            createDistortFilter("distort_displace", "Displace 2", listOf(FilterParameter("Offset", 10f, 1f, 50f))),
            createDistortFilter("distort_glass", "Glass 2", listOf(FilterParameter("Distortion", 12f, 1f, 40f))),
            createDistortFilter("distort_ocean", "Ocean Ripple 2", listOf(FilterParameter("Wave Frequency", 15f, 2f, 50f))),
            createDistortFilter("distort_pinch", "Pinch 2", listOf(FilterParameter("Amount", 0.5f, -1.0f, 1.0f))),
            createDistortFilter("distort_polar", "Polar Coordinates 2", listOf(FilterParameter("Intensity", 1f, 0f, 1f))),
            createDistortFilter("distort_ripple", "Ripple 2", listOf(FilterParameter("Amplitude", 12f, 1f, 50f), FilterParameter("Wavelength", 30f, 5f, 100f))),
            createDistortFilter("distort_shear", "Shear 2", listOf(FilterParameter("Skew Angle", 20f, -60f, 60f))),
            createDistortFilter("distort_spherize", "Spherize 2", listOf(FilterParameter("Curvature", 0.6f, 0.1f, 1.5f))),
            createDistortFilter("distort_twirl", "Twirl 2", listOf(FilterParameter("Angle Degrees", 90f, -360f, 360f))),
            createDistortFilter("distort_wave", "Wave 2", listOf(FilterParameter("Amplitude", 15f, 1f, 50f), FilterParameter("Wavelength", 40f, 10f, 150f))),
            createDistortFilter("distort_zigzag", "ZigZag 2", listOf(FilterParameter("Frequency", 10f, 2f, 40f))),
            createDistortFilter("distort_swirl", "Swirl Distortion 2", listOf(FilterParameter("Degrees", 120f, -360f, 360f), FilterParameter("Range", 0.5f, 0.1f, 1.5f))),
            createDistortFilter("distort_bulge", "Bulge Warp 2", listOf(FilterParameter("Scale", 0.6f, -1.0f, 2.0f))),
            createDistortFilter("distort_kaleidoscope", "Kaleidoscope Matrix 2", listOf(FilterParameter("SlicesCount", 6f, 3f, 24f))),
            createDistortFilter("distort_glass_refract", "Refractive Waves 2", listOf(FilterParameter("Scale", 15f, 2f, 60f))),
            createDistortFilter("distort_fractal_glass", "Fractal Glass Effect 2", listOf(
                FilterParameter("Glass Style", 1f, 0f, 5f),
                FilterParameter("Glass Scale", 30f, 10f, 100f),
                FilterParameter("Refraction Index", 15f, 0f, 50f),
                FilterParameter("Frosting/Grain", 10f, 0f, 40f),
                FilterParameter("Light Shine", 30f, 0f, 100f),
                FilterParameter("Angle", 0f, -180f, 180f, "°")
            )),

            // ==========================================
            // Category 5: PIXELATE
            // ==========================================
            createPixelateFilter("image_toolbox_mosaic", "Mosaic", listOf(FilterParameter("Block Size", 16f, 2f, 100f))),

            createPixelateFilter("pixelate_halftone", "Color Halftone 2", listOf(FilterParameter("Dot Radius", 6f, 2f, 20f))),
            createPixelateFilter("pixelate_crystallize", "Crystallize 2", listOf(FilterParameter("Cell Size", 12f, 4f, 40f))),
            createPixelateFilter("pixelate_facet", "Facet 2", listOf(FilterParameter("Clustering", 8f, 2f, 30f))),
            createPixelateFilter("pixelate_fragment", "Fragment 2", listOf(FilterParameter("Interleave", 6f, 2f, 20f))),
            createPixelateFilter("pixelate_mezzotint", "Mezzotint 2", listOf(FilterParameter("Grain Size", 4f, 1f, 15f))),
            createPixelateFilter("pixelate_mosaic", "Mosaic 2", listOf(FilterParameter("Block Size", 16f, 2f, 100f))),
            createPixelateFilter("pixelate_pointillize", "Pointillize 2", listOf(FilterParameter("Dot Size", 8f, 2f, 30f))),

            // ==========================================
            // Category 6: NOISE & RENDER
            // ==========================================
            createNoiseFilter("noise_add", "Add Noise", listOf(FilterParameter("Intensity", 25f, 0f, 100f))),
            createNoiseFilter("noise_despeckle", "Despeckle", listOf(FilterParameter("Threshold", 15f, 1f, 50f))),
            createNoiseFilter("noise_dust", "Dust & Scratches", listOf(FilterParameter("Radius", 3f, 1f, 15f))),
            createNoiseFilter("noise_median", "Median", listOf(FilterParameter("Radius", 2f, 1f, 8f))),
            createNoiseFilter("noise_reduce", "Reduce Noise", listOf(FilterParameter("Passes", 3f, 1f, 8f))),
            createNoiseFilter("noise_clouds", "Clouds / Difference Clouds", listOf(FilterParameter("Octaves", 4f, 1f, 8f))),
            createNoiseFilter("noise_fibers", "Fibers", listOf(FilterParameter("Stretching", 15f, 2f, 40f))),
            createNoiseFilter("noise_lens", "Lens Flare", listOf(FilterParameter("Brightness", 60f, 10f, 150f))),
            createNoiseFilter("noise_lighting", "Lighting Effects", listOf(FilterParameter("Gloss Intensity", 12f, 1f, 40f))),

            // ==========================================
            // Category 7: SKETCH & TEXTURE
            // ==========================================
            createSketchFilter("sketch_basrelief", "Bas Relief", listOf(FilterParameter("Detail", 5f, 1f, 15f))),
            createSketchFilter("sketch_chalkcharcoal", "Chalk & Charcoal", listOf(FilterParameter("Charcoal Density", 8f, 1f, 20f))),
            createSketchFilter("sketch_charcoal", "Charcoal", listOf(FilterParameter("Smudge Level", 6f, 1f, 15f))),
            createSketchFilter("sketch_chrome", "Chrome", listOf(FilterParameter("Mirror shine", 12f, 2f, 30f))),
            createSketchFilter("sketch_conte", "Conte Crayon", listOf(FilterParameter("Toothy Paper", 10f, 2f, 25f))),
            createSketchFilter("sketch_graphicpen", "Graphic Pen", listOf(FilterParameter("Stroke Length", 15f, 5f, 40f))),
            createSketchFilter("sketch_halftonepattern", "Halftone Pattern", listOf(FilterParameter("Pattern Scale", 8f, 2f, 25f))),
            createSketchFilter("sketch_notepaper", "Note Paper", listOf(FilterParameter("Emboss Depth", 4f, 1f, 15f))),
            createSketchFilter("sketch_photocopy", "Photocopy", listOf(FilterParameter("Toner Contrast", 12f, 2f, 30f))),
            createSketchFilter("sketch_plaster", "Plaster", listOf(FilterParameter("raised Depth", 8f, 1f, 20f))),
            createSketchFilter("sketch_reticulation", "Reticulation", listOf(FilterParameter("Curdling Rate", 10f, 2f, 30f))),
            createSketchFilter("sketch_stamp", "Stamp", listOf(FilterParameter("Contrast Threshold", 128f, 20f, 230f))),
            createSketchFilter("sketch_torn", "Torn Edges", listOf(FilterParameter("Roughness", 12f, 2f, 40f))),
            createSketchFilter("sketch_waterpaper", "Water Paper", listOf(FilterParameter("Softness", 6f, 1f, 20f))),
            createSketchFilter("sketch_craquelure", "Craquelure", listOf(FilterParameter("Crack Spacing", 15f, 4f, 40f))),
            createSketchFilter("sketch_grain", "Grain", listOf(FilterParameter("Intensity", 20f, 1f, 80f))),
            createSketchFilter("sketch_mosaictiles", "Mosaic Tiles", listOf(FilterParameter("Grout Width", 4f, 1f, 12f))),
            createSketchFilter("sketch_patchwork", "Patchwork", listOf(FilterParameter("Grid Size", 8f, 2f, 30f))),
            createSketchFilter("sketch_stainedglass", "Stained Glass", listOf(FilterParameter("Pane Size", 18f, 5f, 50f))),
            createSketchFilter("sketch_texturizer", "Texturizer", listOf(FilterParameter("Scaling", 12f, 2f, 30f))),

            // ==========================================
            // Category 8: STYLIZE
            // ==========================================
            createStylizeFilter("image_toolbox_solarize", "Solarize", listOf(FilterParameter("Threshold", 128f, 10f, 240f))),
            createStylizeFilter("image_toolbox_find_edges", "Find Edges", emptyList()),
            createStylizeFilter("image_toolbox_emboss", "Emboss", listOf(FilterParameter("Height", 3f, 1f, 15f))),

            createStylizeFilter("stylize_diffuse", "Diffuse 2", listOf(FilterParameter("Shuffle Range", 3f, 1f, 15f))),
            createStylizeFilter("stylize_emboss", "Emboss 2", listOf(FilterParameter("Height", 3f, 1f, 15f))),
            createStylizeFilter("stylize_extrude", "Extrude 2", listOf(FilterParameter("Pyramid Size", 10f, 2f, 40f))),
            createStylizeFilter("stylize_findedges", "Find Edges 2", emptyList()),
            createStylizeFilter("stylize_glowingedges", "Glowing Edges 2", listOf(FilterParameter("Edge Width", 4f, 1f, 15f))),
            createStylizeFilter("stylize_solarize", "Solarize 2", listOf(FilterParameter("Threshold", 128f, 10f, 240f))),
            createStylizeFilter("stylize_tiles", "Tiles 2", listOf(FilterParameter("Tile Size", 15f, 4f, 50f))),
            createStylizeFilter("stylize_trace", "Trace Contour 2", listOf(FilterParameter("LevelThreshold", 120f, 10f, 240f))),
            createStylizeFilter("stylize_wind", "Wind 2", listOf(FilterParameter("Wind Distance", 18f, 2f, 60f))),
            createStylizeFilter("stylize_oilpaint", "Oil Paint 2", listOf(FilterParameter("Brush Curvature", 12f, 2f, 40f))),

            // ==========================================
            // Category 9: LIGHT EFFECTS
            // ==========================================
            createIbisPaintFilter("image_toolbox_chromatic_aberration", "Chromatic Aberration", listOf(
                FilterParameter("Distance", 16f, 0f, 150f, "px"),
                FilterParameter("Angle", 136f, 0f, 360f, "°")
            )),
            createIbisPaintFilter("image_toolbox_glitch", "Glitch", listOf(
                FilterParameter("Height", 119f, 10f, 500f, "px"),
                FilterParameter("Strength", 23f, 0f, 150f, "px"),
                FilterParameter("Color Shift", 8f, 0f, 100f, "px")
            )),
            createIbisPaintFilter("image_toolbox_bloom", "Bloom", listOf(
                FilterParameter("Area", 100f, 0f, 100f, "%"),
                FilterParameter("Radius", 45f, 1f, 150f, "px"),
                FilterParameter("Brightness", 100f, 0f, 300f, "%"),
                FilterParameter("Balanced Blend", 25f, 0f, 100f, "%")
            )),
            createIbisPaintFilter("image_toolbox_cross_filter", "Cross Filter", listOf(
                FilterParameter("Count", 4f, 2f, 8f),
                FilterParameter("Direction", 45f, 0f, 360f, "°"),
                FilterParameter("Area", 10f, 0f, 100f, "%"),
                FilterParameter("Brightness", 50f, 0f, 300f, "%")
            )),
            createIbisPaintFilter("image_toolbox_inner_glow", "Inner Glow", listOf(
                FilterParameter("Radius", 104f, 5f, 300f, "px"),
                FilterParameter("Red", 1.0f, 0f, 1f),
                FilterParameter("Green", 1.0f, 0f, 1f),
                FilterParameter("Blue", 1.0f, 0f, 1f),
                FilterParameter("Hardness", 0.5f, 0f, 1f),
                FilterParameter("BlendMode", 2f, 0f, 6f)
            )),
            createIbisPaintFilter("image_toolbox_bevel", "Bevel", listOf(
                FilterParameter("Height", 20f, 1f, 100f, "px"),
                FilterParameter("Smoothness", 45f, 0f, 100f, "px"),
                FilterParameter("Highlight Size", 14f, 0f, 100f, "%")
            )),
            createIbisPaintFilter("image_toolbox_waterdrop", "Waterdrop", listOf(
                FilterParameter("Distance", 100f, 10f, 200f, "%"),
                FilterParameter("Flatness", 10f, 0f, 100f, "%"),
                FilterParameter("Height", 3f, 1f, 15f, "px")
            )),
            createIbisPaintFilter("image_toolbox_satin", "Satin", listOf(
                FilterParameter("Distance", 11f, 1f, 100f, "px"),
                FilterParameter("Opacity", 0.5f, 0f, 1f),
                FilterParameter("Red", 0f, 0f, 1f),
                FilterParameter("Green", 0f, 0f, 1f),
                FilterParameter("Blue", 1f, 0f, 1f)
            )),

            createIbisPaintFilter("ibis_chromatic_aberration", "Chromatic Aberration 2", listOf(
                FilterParameter("Distance", 16f, 0f, 150f, "px"),
                FilterParameter("Angle", 136f, 0f, 360f, "°")
            )),
            createIbisPaintFilter("ibis_glitch", "Glitch Distortion 2", listOf(
                FilterParameter("Height", 119f, 10f, 500f, "px"),
                FilterParameter("Strength", 23f, 0f, 150f, "px"),
                FilterParameter("Color Shift", 8f, 0f, 100f, "px")
            )),
            createIbisPaintFilter("ibis_bloom", "Bloom Glow 2", listOf(
                FilterParameter("Area", 100f, 0f, 100f, "%"),
                FilterParameter("Radius", 45f, 1f, 150f, "px"),
                FilterParameter("Brightness", 100f, 0f, 300f, "%"),
                FilterParameter("Balanced Blend", 25f, 0f, 100f, "%")
            )),
            createIbisPaintFilter("ibis_cross_filter", "Cross Filter 2", listOf(
                FilterParameter("Count", 4f, 2f, 8f),
                FilterParameter("Direction", 45f, 0f, 360f, "°"),
                FilterParameter("Area", 10f, 0f, 100f, "%"),
                FilterParameter("Brightness", 50f, 0f, 300f, "%")
            )),
            createIbisPaintFilter("ibis_inner_glow", "Inner Glow Edge 2", listOf(
                FilterParameter("Radius", 104f, 5f, 300f, "px"),
                FilterParameter("Red", 1.0f, 0f, 1f),
                FilterParameter("Green", 1.0f, 0f, 1f),
                FilterParameter("Blue", 1.0f, 0f, 1f),
                FilterParameter("Hardness", 0.5f, 0f, 1f),
                FilterParameter("BlendMode", 2f, 0f, 6f)
            )),
            createIbisPaintFilter("ibis_bevel", "Bevel (Inner/Outer) 2", listOf(
                FilterParameter("Height", 20f, 1f, 100f, "px"),
                FilterParameter("Smoothness", 45f, 0f, 100f, "px"),
                FilterParameter("Highlight Size", 14f, 0f, 100f, "%")
            )),
            createIbisPaintFilter("ibis_emboss", "Emboss Pro 2", listOf(
                FilterParameter("Gray Scale", 0f, 0f, 1f),
                FilterParameter("Height", 1f, 1f, 10f, "px"),
                FilterParameter("Amount", 500f, 10f, 1000f, "%")
            )),
            createIbisPaintFilter("ibis_waterdrop", "Waterdrop (Rounded) 2", listOf(
                FilterParameter("Distance", 100f, 10f, 200f, "%"),
                FilterParameter("Flatness", 10f, 0f, 100f, "%"),
                FilterParameter("Height", 3f, 1f, 15f, "px")
            )),
            createIbisPaintFilter("ibis_satin", "Satin Contour 2", listOf(
                FilterParameter("Distance", 11f, 1f, 100f, "px"),
                FilterParameter("Opacity", 0.5f, 0f, 1f),
                FilterParameter("Red", 0f, 0f, 1f),
                FilterParameter("Green", 0f, 0f, 1f),
                FilterParameter("Blue", 1f, 0f, 1f)
            )),
            createIbisPaintFilter("ibis_grids", "Grids Overlay", listOf(
                FilterParameter("Columns", 8f, 1f, 1000f),
                FilterParameter("Rows", 8f, 1f, 1000f),
                FilterParameter("Width", 1.5f, 0.1f, 20f, "px"),
                FilterParameter("ColorRed", 1.0f, 0.0f, 1.0f),
                FilterParameter("ColorGreen", 1.0f, 0.0f, 1.0f),
                FilterParameter("ColorBlue", 1.0f, 0.0f, 1.0f),
                FilterParameter("ColorAlpha", 1.0f, 0.0f, 1.0f),
                FilterParameter("OffsetX", 0f, -500f, 500f, "px"),
                FilterParameter("OffsetY", 0f, -500f, 500f, "px"),
                FilterParameter("Dashed", 0f, 0f, 1f),
                FilterParameter("Dash Length", 15f, 2f, 100f, "px"),
                FilterParameter("Dash Gap", 10f, 2f, 100f, "px")
            )),

            // ==========================================
            // Category 10: HALFTONE EFFECTS
            // ==========================================
            createHalftoneFilter("image_toolbox_color_halftone", "Color Halftone", listOf(
                FilterParameter("Dot Size", 8f, 2f, 30f, "px"),
                FilterParameter("Opacity", 1.0f, 0.0f, 1.0f),
                FilterParameter("Color Blend", 1f, 0f, 1f)
            )),

            createHalftoneFilter("halftone_standard", "Standard Halftone Dots 2", listOf(
                FilterParameter("Dot Size", 8f, 2f, 30f, "px"),
                FilterParameter("Opacity", 1.0f, 0.0f, 1.0f),
                FilterParameter("Color Blend", 1f, 0f, 1f)
            )),
            createHalftoneFilter("halftone_shaped", "Shaped Halftone 2", listOf(
                FilterParameter("Dot Size", 8f, 3f, 40f, "px"),
                FilterParameter("Shape Type", 0f, 0f, 3f),
                FilterParameter("Contrast", 1.0f, 0.1f, 3.0f),
                FilterParameter("Angle", 45f, 0f, 180f, "°"),
                FilterParameter("Background Style", 1f, 0f, 3f)
            )),
            createHalftoneFilter("halftone_classic", "Classic Dot Halftone 2", listOf(
                FilterParameter("Dot Size", 8f, 3f, 40f, "px"),
                FilterParameter("Contrast", 1.0f, 0.1f, 3.0f),
                FilterParameter("Angle", 45f, 0f, 180f, "°"),
                FilterParameter("Background Style", 1f, 0f, 3f)
            )),
            createHalftoneFilter("halftone_cmyk", "CMYK Press Halftone 2", listOf(
                FilterParameter("Screen Frequency", 12f, 4f, 50f, "px"),
                FilterParameter("Cyan Angle", 15f, 0f, 90f, "°"),
                FilterParameter("Magenta Angle", 75f, 0f, 90f, "°"),
                FilterParameter("Yellow Angle", 0f, 0f, 90f, "°"),
                FilterParameter("Black Angle", 45f, 0f, 90f, "°"),
                FilterParameter("Dot Scale", 1.0f, 0.2f, 2.0f),
                FilterParameter("Paper Style", 1f, 0f, 2f)
            )),
            createHalftoneFilter("halftone_line_screen", "Linear Line Screen 2", listOf(
                FilterParameter("Line Spacing", 8f, 3f, 30f, "px"),
                FilterParameter("Line Angle", 45f, 0f, 180f, "°"),
                FilterParameter("Contrast", 1.0f, 0.1f, 3.0f),
                FilterParameter("Line Width", 1.0f, 0.1f, 3.0f),
                FilterParameter("Line Breaks", 0f, 0f, 1f),
                FilterParameter("Sine Wave", 0f, 0f, 1f),
                FilterParameter("Wave Amplitude", 4f, 0f, 20f, "px"),
                FilterParameter("Background Style", 1f, 0f, 3f)
            )),
            createHalftoneFilter("halftone_crosshatch", "Crosshatch Engraving 2", listOf(
                FilterParameter("Grid Spacing", 10f, 4f, 40f, "px"),
                FilterParameter("Primary Angle", 45f, 0f, 180f, "°"),
                FilterParameter("Cross Angle", 90f, 30f, 120f, "°"),
                FilterParameter("Line Thickness", 1.5f, 0.5f, 3.0f),
                FilterParameter("Ink Type", 0f, 0f, 3f)
            )),
            createHalftoneFilter("halftone_newspaper", "Retro Newspaper Dots 2", listOf(
                FilterParameter("Dot Frequency", 10f, 3f, 30f, "px"),
                FilterParameter("Bleed Amount", 0.3f, 0f, 1.0f),
                FilterParameter("Paper Yellowing", 0.8f, 0f, 1.0f),
                FilterParameter("Contrast", 1.5f, 0.5f, 4.0f),
                FilterParameter("Dot Rotation", 45f, 0f, 90f, "°")
            )),
            createHalftoneFilter("halftone_radial", "Manga Screen Tone 2", listOf(
                FilterParameter("Frequency", 12f, 4f, 40f, "px"),
                FilterParameter("Center X", 0.5f, 0.0f, 1.0f),
                FilterParameter("Center Y", 0.5f, 0.0f, 1.0f),
                FilterParameter("Max Dot Size", 8f, 2f, 30f, "px"),
                FilterParameter("Fade Out", 1.0f, 0.0f, 2.0f)
            )),

            // ==========================================
            // Category 11: PATTERN MAKER
            // ==========================================
            createPatternMakerFilter("pattern_maker", "Creative Pattern Maker", listOf(
                FilterParameter("Rows", 4f, 1f, 30f),
                FilterParameter("Columns", 4f, 1f, 30f),
                FilterParameter("Angle", 0f, -180f, 180f, "°"),
                FilterParameter("Scale", 1.0f, 0.1f, 5.0f),
                FilterParameter("Opacity", 1.0f, 0.0f, 1.0f),
                FilterParameter("X Offset", 0f, -100f, 100f, "%"),
                FilterParameter("Y Offset", 0f, -100f, 100f, "%"),
                FilterParameter("Background Style", 3f, 0f, 3f),
                FilterParameter("ImageTrigger", 0f, 0f, 1000000000f)
            ))
        )

        // Dynamically add all 57/58 Photoshop Non-Destructive effects!
        com.example.studio.model.PhotoshopEffectTemplates.ALL_TYPES_BY_CATEGORY.forEach { (cat, types) ->
            val finalCategoryName = when (cat) {
                "Filter Gallery" -> "Artistic"
                "Advanced & AI Engines" -> "Advanced & AI"
                else -> cat
            }
            types.forEach { type ->
                try {
                    list.add(createPhotoshopBridgeFilter(type, finalCategoryName))
                } catch (e: Exception) {
                    // Ignore gracefully
                }
            }
        }

        return list
    }

    private fun createPhotoshopBridgeFilter(effectType: String, category: String): ZenithFilter {
        val studioEffect = try {
            com.example.studio.model.PhotoshopEffectTemplates.create(effectType = effectType)
        } catch (e: Exception) {
            com.example.studio.model.StudioEffect.GaussianBlur()
        }
        val name = studioEffect.name
        val params = studioEffect.parameters.values.map { param ->
            FilterParameter(param.name, param.value, param.rangeMin, param.rangeMax, param.unit)
        }
        val keyMap = studioEffect.parameters.map { it.value.name to it.key }.toMap()
        return GPUImageZenithFilter(
            id = "ps_${category.lowercase().replace(" ", "_").replace("(", "").replace(")", "").replace("&", "and")}_${effectType.lowercase()}",
            name = name,
            category = category,
            parameters = params
        ) { source, context, updatedParams ->
            var currentEffect = studioEffect
            updatedParams.forEach { param ->
                val realKey = keyMap[param.name] ?: param.name
                currentEffect = currentEffect.updateParameter(realKey, param.currentValue)
            }
            val singleEffectList = listOf(currentEffect)
            com.example.studio.ui.applyGPUImageFilters(context, source, "bridge_layer", singleEffectList)
        }
    }

    private fun createPatternMakerFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return CustomZenithFilter(id, name, "Pattern Maker", params) { source, parameters ->
            val rows = parameters.find { it.name == "Rows" }?.currentValue ?: 4f
            val cols = parameters.find { it.name == "Columns" }?.currentValue ?: 4f
            val angle = parameters.find { it.name == "Angle" }?.currentValue ?: 0f
            val scale = parameters.find { it.name == "Scale" }?.currentValue ?: 1.0f
            val opacity = parameters.find { it.name == "Opacity" }?.currentValue ?: 1.0f
            val xOffset = parameters.find { it.name == "X Offset" }?.currentValue ?: 0f
            val yOffset = parameters.find { it.name == "Y Offset" }?.currentValue ?: 0f
            val bgStyle = parameters.find { it.name == "Background Style" }?.currentValue ?: 0f // Default 0: Transparent/Pattern Only (hides source mesh shape)
            
            // Get pattern image
            val patternImgPath = EffectStackManager.patternImagesForFilters[id]
            val patternBmp = if (patternImgPath != null) {
                EffectStackManager.decodedTexturesCache.getOrPut(patternImgPath) {
                    try {
                        BitmapFactory.decodeFile(patternImgPath)
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                null
            }

            val finalPatternBmp = patternBmp ?: try {
                Bitmap.createScaledBitmap(source, 150, 150, true)
            } catch (e: Exception) {
                source
            }

            val w = source.width
            val h = source.height
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)

            when (bgStyle.toInt()) {
                1 -> canvas.drawColor(Color.WHITE)
                2 -> canvas.drawColor(Color.BLACK)
                3 -> canvas.drawBitmap(source, 0f, 0f, null)
                else -> canvas.drawColor(Color.TRANSPARENT)
            }

            if (finalPatternBmp != null && finalPatternBmp.width > 0 && finalPatternBmp.height > 0) {
                canvas.save()
                val cx = w / 2f
                val cy = h / 2f
                canvas.rotate(angle, cx, cy)

                val tileW = (w / cols.coerceAtLeast(1f)) * scale
                val tileH = (h / rows.coerceAtLeast(1f)) * scale

                if (tileW > 1f && tileH > 1f) {
                    val diagonal = sqrt((w * w + h * h).toDouble()).toFloat()
                    val gridExtent = diagonal * 2.0f

                    val startX = cx - gridExtent / 2f + (xOffset / 100f * tileW)
                    val startY = cy - gridExtent / 2f + (yOffset / 100f * tileH)

                    val endX = cx + gridExtent / 2f
                    val endY = cy + gridExtent / 2f

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    paint.alpha = (opacity * 255f).toInt().coerceIn(0, 255)

                    var x = startX
                    while (x < endX) {
                        var y = startY
                        while (y < endY) {
                            val destRect = RectF(x, y, x + tileW, y + tileH)
                            canvas.drawBitmap(finalPatternBmp, null, destRect, paint)
                            y += tileH
                        }
                        x += tileW
                    }
                }
                canvas.restore()
            }

            out
        }
    }

    private fun createDitheringFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return CustomZenithFilter(id, name, "Color Adjustments", params) { source, parameters ->
            val type = (parameters.find { it.name.startsWith("Dither") }?.currentValue ?: 0.0f).toInt()
            val quantLevels = (parameters.find { it.name.startsWith("Quantization") }?.currentValue ?: 4.0f).toInt().coerceIn(2, 256)
            val monochrome = (parameters.find { it.name.startsWith("Monochrome") }?.currentValue ?: 1.0f).toInt() == 1

            val w = source.width
            val h = source.height
            val pixels = IntArray(w * h)
            source.getPixels(pixels, 0, w, 0, 0, w, h)

            fun quantizeValue(v: Int, levels: Int): Int {
                val step = 255 / (levels - 1)
                val bucket = (v + step / 2) / step
                return (bucket * step).coerceIn(0, 255)
            }

            if (type == 0) {
                val rErr = FloatArray(w * h)
                val gErr = FloatArray(w * h)
                val bErr = FloatArray(w * h)

                val rChan = FloatArray(w * h)
                val gChan = FloatArray(w * h)
                val bChan = FloatArray(w * h)
                for (i in pixels.indices) {
                    val p = pixels[i]
                    if (monochrome) {
                        val g = (((p shr 16) and 0xff) * 0.299f + ((p shr 8) and 0xff) * 0.587f + (p and 0xff) * 0.114f)
                        rChan[i] = g; gChan[i] = g; bChan[i] = g
                    } else {
                        rChan[i] = ((p shr 16) and 0xff).toFloat()
                        gChan[i] = ((p shr 8) and 0xff).toFloat()
                        bChan[i] = (p and 0xff).toFloat()
                    }
                }

                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val idx = y * w + x
                        val oldR = (rChan[idx] + rErr[idx]).coerceIn(0f, 255f).toInt()
                        val oldG = (gChan[idx] + gErr[idx]).coerceIn(0f, 255f).toInt()
                        val oldB = (bChan[idx] + bErr[idx]).coerceIn(0f, 255f).toInt()

                        val newR = quantizeValue(oldR, quantLevels)
                        val newG = quantizeValue(oldG, quantLevels)
                        val newB = quantizeValue(oldB, quantLevels)

                        pixels[idx] = (pixels[idx] and -0x1000000) or (newR shl 16) or (newG shl 8) or newB

                        val errR = oldR - newR
                        val errG = oldG - newG
                        val errB = oldB - newB

                        fun addErr(nx: Int, ny: Int, factor: Float) {
                            if (nx in 0 until w && ny in 0 until h) {
                                val nidx = ny * w + nx
                                rErr[nidx] += errR * factor
                                gErr[nidx] += errG * factor
                                bErr[nidx] += errB * factor
                            }
                        }
                        addErr(x + 1, y, 7f / 16f)
                        addErr(x - 1, y + 1, 3f / 16f)
                        addErr(x, y + 1, 5f / 16f)
                        addErr(x + 1, y + 1, 1f / 16f)
                    }
                }
            } else {
                val bayerMatrix4x4 = arrayOf(
                    floatArrayOf( 0f,  8f,  2f, 10f),
                    floatArrayOf(12f,  4f, 14f,  6f),
                    floatArrayOf( 3f, 11f,  1f,  9f),
                    floatArrayOf(15f,  7f, 13f,  5f)
                )
                val bayerMatrix8x8 = arrayOf(
                    floatArrayOf( 0f, 48f, 12f, 60f,  3f, 51f, 15f, 63f),
                    floatArrayOf(32f, 16f, 44f, 28f, 35f, 19f, 47f, 31f),
                    floatArrayOf( 8f, 56f,  4f, 52f, 11f, 59f,  7f, 55f),
                    floatArrayOf(40f, 24f, 36f, 20f, 43f, 27f, 39f, 23f),
                    floatArrayOf( 2f, 50f, 14f, 62f,  1f, 49f, 13f, 61f),
                    floatArrayOf(34f, 18f, 46f, 30f, 33f, 17f, 45f, 29f),
                    floatArrayOf(10f, 58f,  6f, 54f,  9f, 57f,  5f, 53f),
                    floatArrayOf(42f, 26f, 38f, 22f, 41f, 25f, 37f, 21f)
                )

                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val idx = y * w + x
                        val p = pixels[idx]
                        
                        var r = ((p shr 16) and 0xff).toFloat()
                        var g = ((p shr 8) and 0xff).toFloat()
                        var b = (p and 0xff).toFloat()

                        if (monochrome) {
                            val grey = r * 0.299f + g * 0.587f + b * 0.114f
                            r = grey; g = grey; b = grey
                        }

                        val threshold = if (type == 1) {
                            val mx = x % 4
                            val my = y % 4
                            (bayerMatrix4x4[my][mx] / 16f) - 0.5f
                        } else if (type == 2) {
                            val mx = x % 8
                            val my = y % 8
                            (bayerMatrix8x8[my][mx] / 64f) - 0.5f
                        } else {
                            (Math.random().toFloat() - 0.5f)
                        }

                        val step = 255f / (quantLevels - 1)
                        val noise = threshold * step

                        val newR = quantizeValue((r + noise).coerceIn(0f, 255f).toInt(), quantLevels)
                        val newG = quantizeValue((g + noise).coerceIn(0f, 255f).toInt(), quantLevels)
                        val newB = quantizeValue((b + noise).coerceIn(0f, 255f).toInt(), quantLevels)

                        pixels[idx] = (p and -0x1000000) or (newR shl 16) or (newG shl 8) or newB
                    }
                }
            }

            val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            result.setPixels(pixels, 0, w, 0, 0, w, h)
            result
        }
    }

    private fun createColorAdjustFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return CustomZenithFilter(id, name, "Color Adjustments", params) { source, parameters ->
            when (id) {
                "color_fast_adjusting" -> {
                    val ev = parameters.find { it.name == "Exposure" }?.currentValue ?: 0.0f
                    val bLevel = parameters.find { it.name == "Brightness" }?.currentValue ?: 0.0f
                    val contrast = parameters.find { it.name == "Contrast" }?.currentValue ?: 1.0f
                    val sat = parameters.find { it.name == "Saturation" }?.currentValue ?: 1.0f

                    val hl = (parameters.find { it.name == "Highlights" }?.currentValue ?: 0.0f) / 100.0f
                    val sh = (parameters.find { it.name == "Shadows" }?.currentValue ?: 0.0f) / 100.0f
                    val whites = (parameters.find { it.name == "Whites" }?.currentValue ?: 0.0f) / 100.0f
                    val blacks = (parameters.find { it.name == "Blacks" }?.currentValue ?: 0.0f) / 100.0f
                    val temp = (parameters.find { it.name == "Temp" }?.currentValue ?: 0.0f) / 100.0f
                    val tint = (parameters.find { it.name == "Tint" }?.currentValue ?: 0.0f) / 100.0f
                    val vibrance = (parameters.find { it.name == "Vibrance" }?.currentValue ?: 0.0f) / 100.0f
                    val sharpness = (parameters.find { it.name == "Sharpness" }?.currentValue ?: 0.0f) / 100.0f
                    val dehaze = (parameters.find { it.name == "Dehaze" }?.currentValue ?: 0.0f) / 100.0f
                    val vignette = (parameters.find { it.name == "Vignette" }?.currentValue ?: 0.0f) / 100.0f

                    val texture = parameters.find { it.name == "Texture" }?.currentValue ?: 0.0f
                    val structure = parameters.find { it.name == "Structure" }?.currentValue ?: 0.0f

                    val expFactor = 2.0f.pow(ev)
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)

                    // Core Pixel Adjustment Pass
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val rOrg = (p shr 16) and 0xff
                        val gOrg = (p shr 8) and 0xff
                        val bOrg = p and 0xff

                        // 1. Temperature & Tint (White Balance)
                        var r = rOrg.toFloat()
                        var g = gOrg.toFloat()
                        var b = bOrg.toFloat()

                        if (temp != 0.0f) {
                            if (temp > 0f) {
                                r += temp * 30f
                                b -= temp * 15f
                            } else {
                                r += temp * 15f
                                b -= temp * 30f
                            }
                        }
                        if (tint != 0.0f) {
                            if (tint > 0f) {
                                r += tint * 15f
                                g -= tint * 20f
                                b += tint * 15f
                            } else {
                                g -= tint * 25f
                                r += tint * 10f
                                b += tint * 10f
                            }
                        }

                        // 2. Exposure
                        if (ev != 0.0f) {
                            r *= expFactor
                            g *= expFactor
                            b *= expFactor
                        }

                        // 3. Brightness
                        if (bLevel != 0.0f) {
                            r += bLevel
                            g += bLevel
                            b += bLevel
                        }

                        var luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

                        // 4. Highlights & Shadows
                        var hlFactor = 1.0f
                        if (hl != 0.0f && luma > 0.5f) {
                            hlFactor = 1.0f + hl * (luma - 0.5f) * 2.0f
                        }
                        var shFactor = 1.0f
                        if (sh != 0.0f && luma < 0.5f) {
                            shFactor = 1.0f + sh * (0.5f - luma) * 2.0f
                        }
                        r *= hlFactor * shFactor
                        g *= hlFactor * shFactor
                        b *= hlFactor * shFactor

                        // Recalculate luma for whites, blacks, and contrast
                        luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

                        // 5. Whites & Blacks adjustments
                        if (whites != 0.0f && luma > 0.6f) {
                            val wAmt = whites * (luma - 0.6f) * 2.5f * 50f
                            r += wAmt
                            g += wAmt
                            b += wAmt
                        }
                        if (blacks != 0.0f && luma < 0.4f) {
                            val bAmt = blacks * (0.4f - luma) * 2.5f * 50f
                            r += bAmt
                            g += bAmt
                            b += bAmt
                        }

                        // 6. Contrast pivot around 0.5
                        if (contrast != 1.0f) {
                            r = (((r / 255f - 0.5f) * contrast + 0.5f) * 255f)
                            g = (((g / 255f - 0.5f) * contrast + 0.5f) * 255f)
                            b = (((b / 255f - 0.5f) * contrast + 0.5f) * 255f)
                        }

                        // 7. Dehaze simulation
                        if (dehaze != 0.0f) {
                            if (dehaze > 0f) {
                                val dehazeFactor = 1.0f + dehaze * 0.3f
                                r = (((r / 255f - 0.45f) * dehazeFactor + 0.45f) * 255f) - dehaze * 15f
                                g = (((g / 255f - 0.45f) * dehazeFactor + 0.45f) * 255f) - dehaze * 15f
                                b = (((b / 255f - 0.45f) * dehazeFactor + 0.45f) * 255f) - dehaze * 10f
                            } else {
                                val dehazeFactor = 1.0f + dehaze * 0.2f
                                r = (((r / 255f - 0.5f) * dehazeFactor + 0.5f) * 255f) - dehaze * 20f
                                g = (((g / 255f - 0.5f) * dehazeFactor + 0.5f) * 255f) - dehaze * 20f
                                b = (((b / 255f - 0.5f) * dehazeFactor + 0.5f) * 255f) - dehaze * 20f
                            }
                        }

                        r = r.coerceIn(0f, 255f)
                        g = g.coerceIn(0f, 255f)
                        b = b.coerceIn(0f, 255f)

                        // 8. Saturation & Vibrance (smart saturation of less-saturated regions)
                        val currentLuma = 0.299f * r + 0.587f * g + 0.114f * b
                        val maxVal = max(r, max(g, b))
                        val minVal = min(r, min(g, b))
                        val satVal = if (maxVal == 0f) 0f else (maxVal - minVal) / maxVal

                        var finalSatFactor = sat
                        if (vibrance != 0.0f) {
                            val vibAmt = vibrance * (1.0f - satVal) * 1.2f
                            finalSatFactor += vibAmt
                        }

                        if (finalSatFactor != 1.0f) {
                            r = currentLuma + (r - currentLuma) * finalSatFactor
                            g = currentLuma + (g - currentLuma) * finalSatFactor
                            b = currentLuma + (b - currentLuma) * finalSatFactor
                        }

                        pixels[i] = (a shl 24) or (r.toInt().coerceIn(0, 255) shl 16) or (g.toInt().coerceIn(0, 255) shl 8) or b.toInt().coerceIn(0, 255)
                    }

                    // 9. Vignette (edge shading effect)
                    if (vignette != 0.0f) {
                        val cx = w / 2f
                        val cy = h / 2f
                        val maxDist = sqrt(cx * cx + cy * cy)
                        if (maxDist > 0f) {
                            for (yIdx in 0 until h) {
                                val rowOffset = yIdx * w
                                val dy = yIdx - cy
                                val dySq = dy * dy
                                for (xIdx in 0 until w) {
                                    val idx = rowOffset + xIdx
                                    val dx = xIdx - cx
                                    val dist = sqrt(dx * dx + dySq)
                                    val normDist = dist / maxDist
                                    
                                    if (normDist > 0.1f) {
                                        val vignImpact = (normDist - 0.1f) / 0.9f
                                        // negative vignette: dark border, positive vignette: white border
                                        val factor = 1.0f + vignette * (vignImpact * vignImpact) * 0.6f
                                        val p = pixels[idx]
                                        val a = p ushr 24
                                        val rVal = (((p shr 16) and 0xff) * factor).toInt().coerceIn(0, 255)
                                        val gVal = (((p shr 8) and 0xff) * factor).toInt().coerceIn(0, 255)
                                        val bVal = ((p and 0xff) * factor).toInt().coerceIn(0, 255)
                                        pixels[idx] = (a shl 24) or (rVal shl 16) or (gVal shl 8) or bVal
                                    }
                                }
                            }
                        }
                    }

                    val adjustedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    adjustedBitmap.setPixels(pixels, 0, w, 0, 0, w, h)

                    // 10. Texture, Structure & Sharpness adjustments (local neighborhood / high pass filtering)
                    if (texture != 0.0f || structure != 0.0f || sharpness > 0.0f) {
                        val blur1 = applyBoxBlur(adjustedBitmap, 2)
                        val blur2 = applyBoxBlur(adjustedBitmap, 6)

                        val pBuf = IntArray(w * h)
                        adjustedBitmap.getPixels(pBuf, 0, w, 0, 0, w, h)

                        val pBlur1 = IntArray(w * h)
                        blur1.getPixels(pBlur1, 0, w, 0, 0, w, h)

                        val pBlur2 = IntArray(w * h)
                        blur2.getPixels(pBlur2, 0, w, 0, 0, w, h)

                        val highFreqFactor = texture + sharpness * 1.5f

                        for (i in pBuf.indices) {
                            val p = pBuf[i]
                            val px1 = pBlur1[i]
                            val px2 = pBlur2[i]

                            val a = p ushr 24

                            // Red
                            val r = (p shr 16) and 0xff
                            val r1 = (px1 shr 16) and 0xff
                            val r2 = (px2 shr 16) and 0xff
                            val d1r = r - r1
                            val d2r = r1 - r2
                            val nr = (r + highFreqFactor * d1r + structure * d2r).toInt().coerceIn(0, 255)

                            // Green
                            val g = (p shr 8) and 0xff
                            val g1 = (px1 shr 8) and 0xff
                            val g2 = (px2 shr 8) and 0xff
                            val d1g = g - g1
                            val d2g = g1 - g2
                            val ng = (g + highFreqFactor * d1g + structure * d2g).toInt().coerceIn(0, 255)

                            // Blue
                            val b = p and 0xff
                            val b1 = px1 and 0xff
                            val b2 = px2 and 0xff
                            val d1b = b - b1
                            val d2b = b1 - b2
                            val nb = (b + highFreqFactor * d1b + structure * d2b).toInt().coerceIn(0, 255)

                            pBuf[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
                        }

                        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        result.setPixels(pBuf, 0, w, 0, 0, w, h)
                        adjustedBitmap.recycle()
                        blur1.recycle()
                        blur2.recycle()
                        result
                    } else {
                        adjustedBitmap
                    }
                }
                "color_grayscale" -> {
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
                        pixels[i] = (a shl 24) or (gray shl 16) or (gray shl 8) or gray
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_sepia" -> {
                    val intensity = parameters.firstOrNull()?.currentValue ?: 0.8f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val tr = ((0.393f * r + 0.769f * g + 0.189f * b) * intensity + r * (1f - intensity)).toInt().coerceIn(0, 255)
                        val tg = ((0.349f * r + 0.686f * g + 0.168f * b) * intensity + g * (1f - intensity)).toInt().coerceIn(0, 255)
                        val tb = ((0.272f * r + 0.534f * g + 0.131f * b) * intensity + b * (1f - intensity)).toInt().coerceIn(0, 255)
                        pixels[i] = (a shl 24) or (tr shl 16) or (tg shl 8) or tb
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_saturation", "image_toolbox_saturation" -> {
                    val sat = parameters.firstOrNull()?.currentValue ?: 1.0f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val gray = 0.299f * r + 0.587f * g + 0.114f * b
                        val nr = (gray + (r - gray) * sat).toInt().coerceIn(0, 255)
                        val ng = (gray + (g - gray) * sat).toInt().coerceIn(0, 255)
                        val nb = (gray + (b - gray) * sat).toInt().coerceIn(0, 255)
                        pixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_contrast", "image_toolbox_contrast" -> {
                    val contrast = parameters.firstOrNull()?.currentValue ?: 1.0f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val nr = (((r / 255f - 0.5f) * contrast + 0.5f) * 255f).toInt().coerceIn(0, 255)
                        val ng = (((g / 255f - 0.5f) * contrast + 0.5f) * 255f).toInt().coerceIn(0, 255)
                        val nb = (((b / 255f - 0.5f) * contrast + 0.5f) * 255f).toInt().coerceIn(0, 255)
                        pixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_brightness", "image_toolbox_brightness" -> {
                    val level = parameters.firstOrNull()?.currentValue ?: 0.0f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = ((p shr 16) and 0xff) + level.toInt()
                        val g = ((p shr 8) and 0xff) + level.toInt()
                        val b = (p and 0xff) + level.toInt()
                        pixels[i] = (a shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_exposure", "image_toolbox_exposure" -> {
                    val ev = parameters.firstOrNull()?.currentValue ?: 0.0f
                    val factor = 2.0f.pow(ev)
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (((p shr 16) and 0xff) * factor).toInt().coerceIn(0, 255)
                        val g = (((p shr 8) and 0xff) * factor).toInt().coerceIn(0, 255)
                        val b = ((p and 0xff) * factor).toInt().coerceIn(0, 255)
                        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_vignette", "image_toolbox_vignette" -> {
                    val radius = parameters.firstOrNull()?.currentValue ?: 0.5f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val cx = w / 2f
                    val cy = h / 2f
                    val maxDist = sqrt(cx * cx + cy * cy)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val dx = x - cx
                            val dy = y - cy
                            val dist = sqrt(dx * dx + dy * dy)
                            val normDist = dist / maxDist
                            val vignetteFactor = (1f - normDist * radius).coerceIn(0f, 1f)
                            val a = p ushr 24
                            val r = (((p shr 16) and 0xff) * vignetteFactor).toInt()
                            val g = (((p shr 8) and 0xff) * vignetteFactor).toInt()
                            val b = ((p and 0xff) * vignetteFactor).toInt()
                            pixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_hue", "image_toolbox_hue" -> {
                    val shift = parameters.firstOrNull()?.currentValue ?: 0.0f
                    val hsv = FloatArray(3)
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        Color.RGBToHSV(r, g, b, hsv)
                        hsv[0] = (hsv[0] + shift + 360f) % 360f
                        val newColor = Color.HSVToColor(a, hsv)
                        pixels[i] = newColor
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_rgb" -> {
                    val rScale = parameters.find { it.name == "Red" }?.currentValue ?: 1.0f
                    val gScale = parameters.find { it.name == "Green" }?.currentValue ?: 1.0f
                    val bScale = parameters.find { it.name == "Blue" }?.currentValue ?: 1.0f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (((p shr 16) and 0xff) * rScale).toInt().coerceIn(0, 255)
                        val g = (((p shr 8) and 0xff) * gScale).toInt().coerceIn(0, 255)
                        val b = ((p and 0xff) * bScale).toInt().coerceIn(0, 255)
                        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_highlights" -> {
                    val hl = parameters.firstOrNull()?.currentValue ?: 0.0f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                        val factor = if (luma > 0.5f) 1f + hl * (luma - 0.5f) * 2f else 1f
                        pixels[i] = (a shl 24) or ((r * factor).toInt().coerceIn(0, 255) shl 16) or ((g * factor).toInt().coerceIn(0, 255) shl 8) or (b * factor).toInt().coerceIn(0, 255)
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_shadows" -> {
                    val sh = parameters.firstOrNull()?.currentValue ?: 0.0f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                        val factor = if (luma < 0.5f) 1f + sh * (0.5f - luma) * 2f else 1f
                        pixels[i] = (a shl 24) or ((r * factor).toInt().coerceIn(0, 255) shl 16) or ((g * factor).toInt().coerceIn(0, 255) shl 8) or (b * factor).toInt().coerceIn(0, 255)
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_replace_color" -> {
                    val sR = parameters.find { it.name == "SourceRed" }?.currentValue ?: 1.0f
                    val sG = parameters.find { it.name == "SourceGreen" }?.currentValue ?: 0.0f
                    val sB = parameters.find { it.name == "SourceBlue" }?.currentValue ?: 0.0f
                    val tR = parameters.find { it.name == "TargetRed" }?.currentValue ?: 0.0f
                    val tG = parameters.find { it.name == "TargetGreen" }?.currentValue ?: 0.0f
                    val tB = parameters.find { it.name == "TargetBlue" }?.currentValue ?: 1.0f
                    val tolerance = parameters.find { it.name == "Tolerance" }?.currentValue ?: 0.15f

                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)

                    val sourceHsv = FloatArray(3)
                    android.graphics.Color.RGBToHSV((sR * 255f).toInt(), (sG * 255f).toInt(), (sB * 255f).toInt(), sourceHsv)

                    val targetHsv = FloatArray(3)
                    android.graphics.Color.RGBToHSV((tR * 255f).toInt(), (tG * 255f).toInt(), (tB * 255f).toInt(), targetHsv)

                    val pixelHsv = FloatArray(3)
                    val resultHsv = FloatArray(3)

                    // Tolerance translates to Hue angle range (max distance 180 degrees)
                    val hueToleranceDegrees = tolerance * 180f

                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        if (a == 0) continue

                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff

                        android.graphics.Color.RGBToHSV(r, g, b, pixelHsv)

                        // 1. Shortest angular distance between hues
                        var diffH = kotlin.math.abs(pixelHsv[0] - sourceHsv[0])
                        if (diffH > 180f) {
                            diffH = 360f - diffH
                        }

                        // 2. Distance in saturation and value to prevent gray/neutral matches
                        val diffS = kotlin.math.abs(pixelHsv[1] - sourceHsv[1])
                        val diffV = kotlin.math.abs(pixelHsv[2] - sourceHsv[2])

                        val hDist = if (hueToleranceDegrees > 0f) diffH / hueToleranceDegrees else if (diffH == 0f) 0f else 100f
                        val sDist = if (tolerance > 0f) diffS / tolerance else if (diffS == 0f) 0f else 100f
                        
                        if (hDist <= 1.0f && sDist <= 1.5f) {
                            val factor = (1.0f - maxOf(hDist, sDist * 0.5f)).coerceIn(0f, 1f)

                            // Replace Hue and Saturation, preserve the pixel's luminosity/brightness (Value)
                            resultHsv[0] = targetHsv[0]
                            resultHsv[1] = targetHsv[1]
                            resultHsv[2] = pixelHsv[2]

                            val interpS = pixelHsv[1] * (1f - factor) + resultHsv[1] * factor
                            val interpV = pixelHsv[2] * (1f - factor) + resultHsv[2] * factor

                            var interpH = pixelHsv[0]
                            if (factor > 0f) {
                                val h0 = pixelHsv[0]
                                val h1 = resultHsv[0]
                                var d = h1 - h0
                                if (d > 180f) {
                                    d -= 360f
                                } else if (d < -180f) {
                                    d += 360f
                                }
                                interpH = (h0 + d * factor + 360f) % 360f
                            }

                            val outHsv = floatArrayOf(interpH, interpS, interpV)
                            pixels[i] = android.graphics.Color.HSVToColor(a, outHsv)
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "color_temp" -> {
                    val warmth = parameters.firstOrNull()?.currentValue ?: 0.0f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        var r = (p shr 16) and 0xff
                        var g = (p shr 8) and 0xff
                        var b = p and 0xff
                        if (warmth >= 0f) {
                            r = (r + warmth * 40f).toInt().coerceIn(0, 255)
                            b = (b - warmth * 20f).toInt().coerceIn(0, 255)
                        } else {
                            r = (r + warmth * 20f).toInt().coerceIn(0, 255)
                            b = (b - warmth * 40f).toInt().coerceIn(0, 255)
                        }
                        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                else -> source
            }
        }
    }

    private fun createArtisticFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return CustomZenithFilter(id, name, "Artistic Effects", params) { source, parameters ->
            when (id) {
                "artistic_pencil" -> {
                    val scale = parameters.firstOrNull()?.currentValue ?: 15f
                    applySobel(source, scale * 3f, Color.BLACK, Color.WHITE, drawEdgesOnly = true)
                }
                "artistic_cutout" -> {
                    val levels = parameters.firstOrNull()?.currentValue?.toInt() ?: 4
                    applyColorQuantizeAndCluster(source, levels)
                }
                "artistic_paintdaubs" -> {
                    val size = parameters.firstOrNull()?.currentValue?.toInt() ?: 8
                    applyGridTransformer(source, size) { pixels ->
                        // Sample a pseudo-random bristle pixel to create paint daubs look
                        val index = (pixels.size * 0.73f).toInt() % pixels.size
                        pixels[index]
                    }
                }
                "artistic_paletteknife" -> {
                    val size = parameters.firstOrNull()?.currentValue?.toInt() ?: 10
                    applyGridTransformer(source, size) { pixels ->
                        // Simulate flat color matching
                        if (pixels.isEmpty()) Color.GRAY else pixels[pixels.size / 2]
                    }
                }
                "artistic_posteredges" -> {
                    val thick = parameters.firstOrNull()?.currentValue ?: 2f
                    val quantized = applyColorQuantizeAndCluster(source, 6)
                    applySobel(quantized, 50f + thick * 10f, Color.BLACK, Color.TRANSPARENT, drawEdgesOnly = false)
                }
                "artistic_neonglow" -> {
                    val r = parameters.firstOrNull()?.currentValue?.toInt() ?: 10
                    val edges = applySobel(source, 80f, Color.CYAN, Color.BLACK, drawEdgesOnly = true)
                    applyBoxBlur(edges, r)
                }
                "artistic_watercolor" -> {
                    val bleeding = (parameters.firstOrNull()?.currentValue ?: 10f).toInt()
                    applyBoxBlur(source, bleeding / 2)
                }
                "artistic_oil_kuwahara", "image_toolbox_oil_paint" -> {
                    val radius = parameters.firstOrNull()?.currentValue?.toInt() ?: 4
                    applyColorQuantizeAndCluster(applyBoxBlur(source, radius), 6)
                }
                "artistic_toon" -> {
                    val levels = parameters.firstOrNull()?.currentValue?.toInt() ?: 5
                    val quantizedColors = applyColorQuantizeAndCluster(source, levels)
                    applySobel(quantizedColors, 80f, Color.BLACK, Color.TRANSPARENT, drawEdgesOnly = false)
                }
                "artistic_hologram" -> {
                    val fringe = parameters.firstOrNull()?.currentValue ?: 15f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        val factor = sin(y * 0.5f) * fringe
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val a = p ushr 24
                            var r = (p shr 16) and 0xff
                            var g = (p shr 8) and 0xff
                            var b = p and 0xff
                            if (y % 4 < 2) {
                                r = (r + factor).toInt().coerceIn(0, 255)
                                g = (g - factor / 2).toInt().coerceIn(0, 255)
                            } else {
                                b = (b + factor).toInt().coerceIn(0, 255)
                                g = (g - factor / 2).toInt().coerceIn(0, 255)
                            }
                            pixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                else -> {
                    // Fallback baseline contrast modification representing artistic filter response
                    val contrastFactor = 1.2f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val a = p ushr 24
                        val r = (((((p shr 16) and 0xff) / 255f - 0.5f) * contrastFactor + 0.5f) * 255f).toInt().coerceIn(0, 255)
                        val g = (((((p shr 8) and 0xff) / 255f - 0.5f) * contrastFactor + 0.5f) * 255f).toInt().coerceIn(0, 255)
                        val b = (((((p) and 0xff) / 255f - 0.5f) * contrastFactor + 0.5f) * 255f).toInt().coerceIn(0, 255)
                        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
            }
        }
    }

    private fun createBlurFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return GPUImageZenithFilter(id, name, "Blur & Blur Gallery", params) { source, context, parameters ->
            when (id) {
                "blur_average" -> {
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    var sumR = 0L
                    var sumG = 0L
                    var sumB = 0L
                    var sumA = 0L
                    for (p in pixels) {
                        sumA += p ushr 24
                        sumR += (p shr 16) and 0xff
                        sumG += (p shr 8) and 0xff
                        sumB += p and 0xff
                    }
                    val count = pixels.size
                    val avgColor = ((sumA / count).toInt() shl 24) or
                                   ((sumR / count).toInt() shl 16) or
                                   ((sumG / count).toInt() shl 8) or
                                   ((sumB / count).toInt())
                    val result = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
                    result.eraseColor(avgColor)
                    result
                }
                "blur_gaussian", "blur_box", "image_toolbox_gaussian_blur", "image_toolbox_box_blur" -> {
                    val r = parameters.firstOrNull()?.currentValue ?: 15f
                    applyBoxBlur(source, r.toInt().coerceAtLeast(1))
                }
                "blur_radial" -> {
                    val factor = parameters.firstOrNull()?.currentValue ?: 15f
                    applyCoordinateDistortion(source) { x, y, width, height ->
                        val cx = width / 2f
                        val cy = height / 2f
                        val dx = x - cx
                        val dy = y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        val angle = atan2(dy, dx)
                        val newDist = dist * (1f - factor / 100f)
                        val sx = cx + cos(angle) * newDist
                        val sy = cy + sin(angle) * newDist
                        Pair(sx, sy)
                    }
                }
                "blur_stack", "blur_fast" -> {
                    val radius = parameters.find { it.name == "Blur Radius" }?.currentValue ?: 15f
                    applyBoxBlur(source, radius.toInt().coerceAtLeast(1))
                }
                "blur_zoom" -> {
                    val strength = parameters.find { it.name == "Power Strength" }?.currentValue ?: 10f
                    applyCoordinateDistortion(source) { x, y, width, height ->
                        val cx = width / 2f
                        val cy = height / 2f
                        val dx = x - cx
                        val dy = y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        val multiplier = 1f - (strength / 100f) * (dist / max(cx, cy))
                        Pair(cx + dx * multiplier.coerceIn(0.1f, 1f), cy + dy * multiplier.coerceIn(0.1f, 1f))
                    }
                }
                "blur_bilateral" -> {
                    val spatial = (parameters.find { it.name == "Spatial Delta" }?.currentValue ?: 10f).toInt()
                    applyBoxBlur(source, spatial / 2)
                }
                "blur_bokeh" -> {
                    val radius = (parameters.find { it.name == "Bokeh Radius" }?.currentValue ?: 12f).toInt()
                    applyBoxBlur(source, radius)
                }
                else -> {
                    val radius = 10
                    applyBoxBlur(source, radius)
                }
            }
        }
    }

    private fun createBrushFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return CustomZenithFilter(id, name, "Brush Strokes", params) { source, parameters ->
            when (id) {
                "brush_accented" -> {
                    val width = parameters.firstOrNull()?.currentValue ?: 2f
                    applySobel(source, 120f - width * 10f, Color.WHITE, Color.BLACK, drawEdgesOnly = false)
                }
                "brush_angled" -> {
                    val angle = parameters.find { it.name == "Angle" }?.currentValue ?: 45f
                    val rad = Math.toRadians(angle.toDouble())
                    val dx = cos(rad).toFloat() * 3f
                    val dy = sin(rad).toFloat() * 3f
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        if ((x + y).toInt() % 6 < 2) {
                            Pair(x + dx, y + dy)
                        } else {
                            Pair(x, y)
                        }
                    }
                }
                "brush_crosshatch" -> {
                    val density = (parameters.find { it.name == "Density" }?.currentValue ?: 8f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val r = (p shr 16) and 0xff
                            val g = (p shr 8) and 0xff
                            val b = p and 0xff
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            if (luma < 150f) {
                                if ((x + y) % density == 0 || (x - y) % density == 0) {
                                    pixels[idx] = (p and -0x1000000) or 0x000000
                                }
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "brush_dark" -> {
                    val density = (parameters.find { it.name == "Density" }?.currentValue ?: 6f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val r = (p shr 16) and 0xff
                            val g = (p shr 8) and 0xff
                            val b = p and 0xff
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            if (luma < 110f && (x * y) % density == 0) {
                                pixels[idx] = (p and -0x1000000) or 0x0c0c0c
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "brush_ink" -> {
                    val weight = parameters.firstOrNull()?.currentValue ?: 3f
                    applySobel(source, 100f - weight * 10f, Color.BLACK, Color.WHITE, drawEdgesOnly = false)
                }
                "brush_spatter" -> {
                    val radius = (parameters.firstOrNull()?.currentValue ?: 10f).toInt()
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        val rx = x + (sin(x * 0.9f) * radius)
                        val ry = y + (cos(y * 0.9f) * radius)
                        Pair(rx, ry)
                    }
                }
                "brush_sprayed" -> {
                    val scattering = (parameters.find { it.name == "Scattering" }?.currentValue ?: 12f).toInt()
                    val rObj = java.util.Random()
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        if (rObj.nextFloat() < 0.4f) {
                            Pair(x + rObj.nextInt(scattering) - scattering / 2f, y + rObj.nextInt(scattering) - scattering / 2f)
                        } else {
                            Pair(x, y)
                        }
                    }
                }
                "brush_sumie" -> {
                    val sat = parameters.find { it.name == "Saturation" }?.currentValue ?: 5f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val luma = (0.299f * r + 0.587f * g + 0.114f * b)
                        val gray = if (luma < 120) (luma * 0.7f).toInt() else ((luma - 120) * 0.5f + 120).toInt()
                        pixels[i] = (p and -0x1000000) or (gray shl 16) or (gray shl 8) or gray
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    applyBoxBlur(result, (sat / 2f).toInt().coerceAtLeast(1))
                }
                else -> {
                    val strokeLength = 8f
                    applySobel(source, 150f - strokeLength * 5f, Color.BLACK, Color.TRANSPARENT, drawEdgesOnly = false)
                }
            }
        }
    }

    private fun createDistortFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        if (id == "distort_fractal_glass") {
            return GPUImageZenithFilter(id, name, "Distort", params) { source, context, parameters ->
                val style = parameters.find { it.name == "Glass Style" }?.currentValue ?: 1f
                val scale = parameters.find { it.name == "Glass Scale" }?.currentValue ?: 30f
                val refraction = parameters.find { it.name == "Refraction Index" }?.currentValue ?: 15f
                val frosting = parameters.find { it.name == "Frosting/Grain" }?.currentValue ?: 10f
                val shine = parameters.find { it.name == "Light Shine" }?.currentValue ?: 30f
                val angle = parameters.find { it.name == "Angle" }?.currentValue ?: 0f

                applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageFractalGlassFilter(
                    style = style,
                    scale = scale,
                    refraction = refraction,
                    frosting = frosting,
                    shine = shine,
                    angle = angle
                ))
            }
        }
        return CustomZenithFilter(id, name, "Distort", params) { source, parameters ->
            when (id) {
                "distort_ripple", "image_toolbox_ripple" -> {
                    val amp = parameters.find { it.name == "Amplitude" }?.currentValue ?: 12f
                    val wave = parameters.find { it.name == "Wavelength" }?.currentValue ?: 30f
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        Pair(x + sin(y / wave) * amp, y)
                    }
                }
                "distort_twirl", "image_toolbox_twirl" -> {
                    val angleDeg = parameters.firstOrNull()?.currentValue ?: 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                    applyCoordinateDistortion(source) { x, y, width, height ->
                        val cx = width / 2f
                        val cy = height / 2f
                        val dx = x - cx
                        val dy = y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        val maxR = sqrt(cx * cx + cy * cy)
                        val factor = (maxR - r) / maxR
                        if (factor > 0f) {
                            val theta = atan2(dy, dx) + angleRad * factor
                            Pair(cx + r * cos(theta), cy + r * sin(theta))
                        } else {
                            Pair(x, y)
                        }
                    }
                }
                "distort_pinch", "image_toolbox_pinch" -> {
                    val amount = parameters.firstOrNull()?.currentValue ?: 0.5f
                    applyCoordinateDistortion(source) { x, y, width, height ->
                        val cx = width / 2f
                        val cy = height / 2f
                        val dx = x - cx
                        val dy = y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        val maxR = sqrt(cx * cx + cy * cy)
                        if (r < maxR) {
                            val scale = if (amount >= 0f) {
                                1f + amount * (r / maxR)
                            } else {
                                1f - abs(amount) * (1f - r / maxR)
                            }
                            Pair(cx + dx * scale, cy + dy * scale)
                        } else {
                            Pair(x, y)
                        }
                    }
                }
                "distort_spherize", "image_toolbox_spherize" -> {
                    val curvature = parameters.firstOrNull()?.currentValue ?: 0.6f
                    applyCoordinateDistortion(source) { x, y, width, height ->
                        val cx = width / 2f
                        val cy = height / 2f
                        val dx = (x - cx) / cx
                        val dy = (y - cy) / cy
                        val r = dx * dx + dy * dy
                        if (r < 1f) {
                            val factor = sin(PI * 0.5 * Math.pow(r.toDouble(), curvature.toDouble())).toFloat()
                            Pair(cx + dx * cx * factor, cy + dy * cy * factor)
                        } else {
                            Pair(x, y)
                        }
                    }
                }
                "distort_shear" -> {
                    val angle = parameters.firstOrNull()?.currentValue ?: 20f
                    val factor = tan(Math.toRadians(angle.toDouble())).toFloat()
                    applyCoordinateDistortion(source) { x, y, _, height ->
                        Pair(x + (y - height / 2f) * factor, y)
                    }
                }
                "distort_wave", "image_toolbox_wave" -> {
                    val amp = parameters.find { it.name == "Amplitude" }?.currentValue ?: 15f
                    val wave = parameters.find { it.name == "Wavelength" }?.currentValue ?: 40f
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        Pair(x + sin(y / wave) * amp, y + cos(x / wave) * amp)
                    }
                }
                "distort_swirl" -> {
                    val degrees = parameters.find { it.name == "Degrees" }?.currentValue ?: 120f
                    val rAmt = parameters.find { it.name == "Range" }?.currentValue ?: 0.5f
                    applyCoordinateDistortion(source) { x, y, width, height ->
                        val cx = width / 2f
                        val cy = height / 2f
                        val dx = x - cx
                        val dy = y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        val maxR = sqrt(cx * cx + cy * cy) * rAmt
                        if (r < maxR) {
                            val factor = (maxR - r) / maxR
                            val theta = atan2(dy, dx) + Math.toRadians(degrees.toDouble()).toFloat() * factor
                            Pair(cx + r * cos(theta), cy + r * sin(theta))
                        } else {
                            Pair(x, y)
                        }
                    }
                }
                "distort_bulge" -> {
                    val scale = parameters.find { it.name == "Scale" }?.currentValue ?: 0.6f
                    applyCoordinateDistortion(source) { x, y, width, height ->
                        val cx = width / 2f
                        val cy = height / 2f
                        val dx = x - cx
                        val dy = y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        val maxR = sqrt(cx * cx + cy * cy)
                        if (r < maxR) {
                            val factor = r / maxR
                            val term = if (scale >= 0) 1f + scale * (1f - factor) else 1f - abs(scale) * factor
                            Pair(cx + dx * term, cy + dy * term)
                        } else {
                            Pair(x, y)
                        }
                    }
                }
                "distort_kaleidoscope" -> {
                    val slices = parameters.find { it.name == "SlicesCount" }?.currentValue ?: 6f
                    applyCoordinateDistortion(source) { x, y, width, height ->
                        val cx = width / 2f
                        val cy = height / 2f
                        val dx = x - cx
                        val dy = y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        var theta = atan2(dy, dx)
                        val sliceAngle = (PI * 2 / slices).toFloat()
                        theta = (theta % sliceAngle + sliceAngle) % sliceAngle
                        if (theta > sliceAngle / 2f) {
                            theta = sliceAngle - theta
                        }
                        Pair(cx + r * cos(theta).toFloat(), cy + r * sin(theta).toFloat())
                    }
                }
                "distort_glass_refract" -> {
                    val scale = parameters.find { it.name == "Scale" }?.currentValue ?: 15f
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        Pair(x + sin(y / 10f) * scale, y + cos(x / 10f) * scale)
                    }
                }
                "distort_zigzag", "image_toolbox_zigzag" -> {
                    val freq = parameters.find { it.name == "Frequency" }?.currentValue ?: 10f
                    applyCoordinateDistortion(source) { x, y, width, height ->
                        val cx = width / 2f
                        val cy = height / 2f
                        val dx = x - cx
                        val dy = y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        val theta = atan2(dy, dx)
                        val offset = sin(r / freq) * 10f
                        Pair(cx + r * cos(theta + offset), cy + r * sin(theta + offset))
                    }
                }
                "distort_fractal_glass" -> {
                    applyFractalGlass(source, parameters)
                }
                else -> {
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        // Tiny baseline distortion (glass / ocean ripple noise)
                        Pair(x + (sin(y * 0.1f) * 2f), y + (cos(x * 0.1f) * 2f))
                    }
                }
            }
        }
    }

    private fun createPixelateFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return CustomZenithFilter(id, name, "Pixelate", params) { source, parameters ->
            when (id) {
                "pixelate_mosaic", "image_toolbox_mosaic" -> {
                    val size = parameters.firstOrNull()?.currentValue?.toInt() ?: 16
                    applyGridTransformer(source, size) { pixels ->
                        if (pixels.isEmpty()) return@applyGridTransformer Color.BLACK
                        var r = 0L; var g = 0L; var b = 0L; var a = 0L
                        for (p in pixels) {
                            a += p ushr 24
                            r += (p shr 16) and 0xff
                            g += (p shr 8) and 0xff
                            b += p and 0xff
                        }
                        val cnt = pixels.size
                        ((a / cnt).toInt() shl 24) or ((r / cnt).toInt() shl 16) or ((g / cnt).toInt() shl 8) or (b / cnt).toInt()
                    }
                }
                "pixelate_crystallize" -> {
                    val size = parameters.firstOrNull()?.currentValue?.toInt() ?: 12
                    applyGridTransformer(source, size) { pixels ->
                        if (pixels.isEmpty()) return@applyGridTransformer Color.GRAY
                        pixels.maxByOrNull {
                            val r = (it shr 16) and 0xff
                            val g = (it shr 8) and 0xff
                            val b = it and 0xff
                            r * r + g * g + b * b
                        } ?: pixels[pixels.size / 2]
                    }
                }
                "pixelate_pointillize" -> {
                    val size = parameters.firstOrNull()?.currentValue?.toInt() ?: 8
                    applyGridTransformer(source, size) { pixels ->
                        if (pixels.isEmpty()) return@applyGridTransformer Color.GRAY
                        pixels[pixels.size / 2]
                    }
                }
                "pixelate_halftone" -> {
                    val radius = (parameters.find { it.name == "Dot Radius" }?.currentValue ?: 6f).toInt()
                    val w = source.width
                    val h = source.height
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val rCanvas = android.graphics.Canvas(out)
                    rCanvas.drawColor(Color.WHITE)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                    val step = (radius * 2).coerceAtLeast(4)
                    for (cy in 0 until h step step) {
                        for (cx in 0 until w step step) {
                            val px = source.getPixel(cx.coerceIn(0, w - 1), cy.coerceIn(0, h - 1))
                            val r = (px shr 16) and 0xff
                            val g = (px shr 8) and 0xff
                            val b = px and 0xff
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            val dR = radius * (1.0f - luma / 255.0f)
                            paint.color = px
                            rCanvas.drawCircle(cx.toFloat() + step / 2f, cy.toFloat() + step / 2f, dR, paint)
                        }
                    }
                    out
                }
                "pixelate_facet" -> {
                    val clustering = (parameters.find { it.name == "Clustering" }?.currentValue ?: 8f).toInt()
                    applyGridTransformer(source, clustering) { pixels ->
                        if (pixels.isEmpty()) return@applyGridTransformer Color.GRAY
                        val avg = pixels[pixels.size / 2]
                        val r = (((avg shr 16) and 0xff) / 32) * 32
                        val g = (((avg shr 8) and 0xff) / 32) * 32
                        val b = ((avg and 0xff) / 32) * 32
                        (avg and -0x1000000) or (r shl 16) or (g shl 8) or b
                    }
                }
                "pixelate_fragment" -> {
                    val interleave = (parameters.find { it.name == "Interleave" }?.currentValue ?: 6f).toInt()
                    val w = source.width
                    val h = source.height
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val outCanvas = android.graphics.Canvas(out)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { alpha = 130 }
                    outCanvas.drawBitmap(source, -interleave.toFloat(), -interleave.toFloat(), paint)
                    outCanvas.drawBitmap(source, interleave.toFloat(), interleave.toFloat(), paint)
                    out
                }
                "pixelate_mezzotint" -> {
                    val grainSize = (parameters.find { it.name == "Grain Size" }?.currentValue ?: 4f).toInt()
                    val w = source.width
                    val h = source.height
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val rObj = java.util.Random()
                    for (y in 0 until h step grainSize) {
                        for (x in 0 until w step grainSize) {
                            val px = pixels[y.coerceIn(0, h - 1) * w + x.coerceIn(0, w - 1)]
                            val r = (px shr 16) and 0xff
                            val g = (px shr 8) and 0xff
                            val b = px and 0xff
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            val dColor = if (rObj.nextFloat() * 255.0f > luma) Color.BLACK else Color.WHITE
                            for (gy in 0 until grainSize) {
                                for (gx in 0 until grainSize) {
                                    val tx = x + gx
                                    val ty = y + gy
                                    if (tx < w && ty < h) {
                                        pixels[ty * w + tx] = dColor
                                    }
                                }
                            }
                        }
                    }
                    out.setPixels(pixels, 0, w, 0, 0, w, h)
                    out
                }
                else -> {
                    applyColorQuantizeAndCluster(source, 5)
                }
            }
        }
    }

    private fun createNoiseFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return CustomZenithFilter(id, name, "Noise & Render", params) { source, parameters ->
            when (id) {
                "noise_add" -> {
                    val intensity = parameters.firstOrNull()?.currentValue ?: 25f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val rObj = java.util.Random()
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val rVal = (rObj.nextGaussian() * intensity).toInt()
                        val a = p ushr 24
                        val r = (((p shr 16) and 0xff) + rVal).coerceIn(0, 255)
                        val g = (((p shr 8) and 0xff) + rVal).coerceIn(0, 255)
                        val b = ((p and 0xff) + rVal).coerceIn(0, 255)
                        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "noise_despeckle" -> {
                    val thresh = (parameters.find { it.name == "Threshold" }?.currentValue ?: 15f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val outPixels = IntArray(w * h) { index -> pixels[index] }
                    for (y in 1 until h - 1) {
                        for (x in 1 until w - 1) {
                            val idx = y * w + x
                            val current = pixels[idx]
                            val r = (current shr 16) and 0xff
                            val g = (current shr 8) and 0xff
                            val b = current and 0xff
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            var neighborLumaSum = 0f
                            for (dy in -1..1) {
                                for (dx in -1..1) {
                                    val np = pixels[(y + dy) * w + (x + dx)]
                                    neighborLumaSum += 0.299f * ((np shr 16) and 0xff) + 0.587f * ((np shr 8) and 0xff) + 0.114f * (np and 0xff)
                                }
                            }
                            val avgLuma = neighborLumaSum / 9f
                            if (abs(luma - avgLuma) > thresh * 2) {
                                outPixels[idx] = pixels[(y - 1) * w + x]
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(outPixels, 0, w, 0, 0, w, h)
                    result
                }
                "noise_dust" -> {
                    val radius = (parameters.find { it.name == "Radius" }?.currentValue ?: 3f).toInt()
                    val w = source.width
                    val h = source.height
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val outCanvas = android.graphics.Canvas(out)
                    outCanvas.drawBitmap(source, 0f, 0f, null)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(230, 222, 210)
                        style = android.graphics.Paint.Style.FILL
                    }
                    val rObj = java.util.Random(42)
                    val counts = (w * h / 1000).coerceAtLeast(50)
                    for (i in 0 until counts) {
                        val cx = rObj.nextInt(w).toFloat()
                        val cy = rObj.nextInt(h).toFloat()
                        val r = rObj.nextFloat() * radius + 1f
                        if (rObj.nextBoolean()) {
                            paint.color = Color.rgb(20, 20, 20)
                        } else {
                            paint.color = Color.rgb(240, 235, 230)
                        }
                        outCanvas.drawCircle(cx, cy, r, paint)
                    }
                    out
                }
                "noise_median" -> {
                    val radius = (parameters.find { it.name == "Radius" }?.currentValue ?: 2f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val outPixels = IntArray(w * h)
                    val windowSize = (radius * 2 + 1) * (radius * 2 + 1)
                    val rArr = IntArray(windowSize)
                    val gArr = IntArray(windowSize)
                    val bArr = IntArray(windowSize)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            var count = 0
                            for (dy in -radius..radius) {
                                val ny = (y + dy).coerceIn(0, h - 1)
                                for (dx in -radius..radius) {
                                    val nx = (x + dx).coerceIn(0, w - 1)
                                    val p = pixels[ny * w + nx]
                                    rArr[count] = (p shr 16) and 0xff
                                    gArr[count] = (p shr 8) and 0xff
                                    bArr[count] = p and 0xff
                                    count++
                                }
                            }
                            rArr.sort(0, count)
                            gArr.sort(0, count)
                            bArr.sort(0, count)
                            val mid = count / 2
                            outPixels[y * w + x] = (0xff shl 24) or (rArr[mid] shl 16) or (gArr[mid] shl 8) or bArr[mid]
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(outPixels, 0, w, 0, 0, w, h)
                    result
                }
                "noise_reduce" -> {
                    val passes = (parameters.find { it.name == "Passes" }?.currentValue ?: 3f).toInt()
                    var current = source
                    for (i in 0 until passes.coerceIn(1, 4)) {
                        current = applyBoxBlur(current, 2)
                    }
                    current
                }
                "noise_clouds" -> {
                    val octaves = (parameters.find { it.name == "Octaves" }?.currentValue ?: 4f).toInt()
                    val w = source.width
                    val h = source.height
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val a = p ushr 24
                            var r = (p shr 16) and 0xff
                            var g = (p shr 8) and 0xff
                            var b = p and 0xff
                            var sum = 0f
                            var scale = 0.05f
                            var amplitude = 1f
                            var ampSum = 0f
                            for (o in 0 until octaves) {
                                sum += (sin(x * scale) * cos(y * scale) + 1f) * 0.5f * amplitude
                                ampSum += amplitude
                                scale *= 1.8f
                                amplitude *= 0.5f
                            }
                            val factor = sum / ampSum
                            r = (r * factor).toInt().coerceIn(0, 255)
                            g = (g * factor).toInt().coerceIn(0, 255)
                            b = (b * factor).toInt().coerceIn(0, 255)
                            pixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    out.setPixels(pixels, 0, w, 0, 0, w, h)
                    out
                }
                "noise_fibers" -> {
                    val stretching = (parameters.find { it.name == "Stretching" }?.currentValue ?: 15f).toInt()
                    val w = source.width
                    val h = source.height
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val rObj = java.util.Random(1337)
                    for (x in 0 until w) {
                        val baseFiberValue = rObj.nextFloat() * stretching
                        for (y in 0 until h) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val rVal = (baseFiberValue + sin(y * 0.2f) * 5f).toInt()
                            val r = (((p shr 16) and 0xff) + rVal).coerceIn(0, 255)
                            val g = (((p shr 8) and 0xff) + rVal).coerceIn(0, 255)
                            val b = ((p and 0xff) + rVal).coerceIn(0, 255)
                            pixels[idx] = (p and -0x1000000) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    out.setPixels(pixels, 0, w, 0, 0, w, h)
                    out
                }
                "noise_lens" -> {
                    val bAmount = parameters.find { it.name == "Brightness" }?.currentValue ?: 60f
                    val w = source.width
                    val h = source.height
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val outCanvas = android.graphics.Canvas(out)
                    outCanvas.drawBitmap(source, 0f, 0f, null)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 3f
                    }
                    val cx = w / 2f
                    val cy = h / 2f
                    val radialPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb((bAmount * 1.5f).toInt().coerceIn(0, 255), 255, 235, 180)
                    }
                    outCanvas.drawCircle(cx, cy, 30f + bAmount / 2f, radialPaint)
                    paint.color = Color.argb(40, 255, 100, 100)
                    outCanvas.drawCircle(cx, cy, w * 0.15f + bAmount, paint)
                    paint.color = Color.argb(30, 100, 255, 100)
                    outCanvas.drawCircle(cx, cy, w * 0.22f + bAmount, paint)
                    paint.color = Color.argb(20, 100, 100, 255)
                    outCanvas.drawCircle(cx, cy, w * 0.35f + bAmount, paint)
                    out
                }
                "noise_lighting" -> {
                    val gloss = parameters.find { it.name == "Gloss Intensity" }?.currentValue ?: 12f
                    val w = source.width
                    val h = source.height
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val cx = w / 2f
                    val cy = h/2f
                    val maxRadius = sqrt(cx * cx + cy * cy)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val dx = x - cx
                            val dy = y - cy
                            val dist = sqrt(dx * dx + dy * dy)
                            val factor = (1f - dist / maxRadius * (gloss / 20f)).coerceIn(0.1f, 1.4f)
                            val p = pixels[idx]
                            val r = (((p shr 16) and 0xff) * factor).toInt().coerceIn(0, 255)
                            val g = (((p shr 8) and 0xff) * factor).toInt().coerceIn(0, 255)
                            val b = ((p and 0xff) * factor).toInt().coerceIn(0, 255)
                            pixels[idx] = (p and -0x1000000) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    out.setPixels(pixels, 0, w, 0, 0, w, h)
                    out
                }
                else -> {
                    source
                }
            }
        }
    }

    private fun createSketchFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return CustomZenithFilter(id, name, "Sketch & Texture", params) { source, parameters ->
            when (id) {
                "sketch_stamp" -> {
                    val thresh = parameters.firstOrNull()?.currentValue ?: 128f
                    applySobel(source, thresh, Color.BLACK, Color.WHITE, drawEdgesOnly = true)
                }
                "sketch_charcoal" -> {
                    val edge = applySobel(source, 90f, Color.DKGRAY, Color.WHITE, drawEdgesOnly = true)
                    applyBoxBlur(edge, 4)
                }
                "sketch_basrelief" -> {
                    val detail = (parameters.find { it.name == "Detail" }?.currentValue ?: 5f).toInt()
                    val edge = applySobel(source, 100f - detail * 5f, Color.LTGRAY, Color.GRAY, drawEdgesOnly = true)
                    applyCoordinateDistortion(edge) { x, y, _, _ ->
                        Pair(x + 2f, y + 2f)
                    }
                }
                "sketch_chalkcharcoal" -> {
                    val density = (parameters.find { it.name == "Charcoal Density" }?.currentValue ?: 8f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val r = (p shr 16) and 0xff
                            val g = (p shr 8) and 0xff
                            val b = p and 0xff
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            pixels[idx] = if (luma < 90f && (x + y) % density < 2) {
                                (p and -0x1000000) or 0x222222
                            } else if (luma > 170f && (x - y) % density < 2) {
                                (p and -0x1000000) or 0xffffff
                            } else {
                                (p and -0x1000000) or 0x909090
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_chrome" -> {
                    val shine = parameters.find { it.name == "Mirror shine" }?.currentValue ?: 12f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val luma = 0.299f * r + 0.587f * g + 0.114f * b
                        val factor = (sin(luma / 255f * PI * (shine / 5f)) * 127 + 128).toInt().coerceIn(0, 255)
                        pixels[i] = (p and -0x1000000) or (factor shl 16) or (factor shl 8) or factor
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_conte" -> {
                    val toothy = (parameters.find { it.name == "Toothy Paper" }?.currentValue ?: 10f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val rObj = java.util.Random()
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val r = (p shr 16) and 0xff
                            val g = (p shr 8) and 0xff
                            val b = p and 0xff
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            val noise = rObj.nextInt(toothy * 4) - toothy * 2
                            if (luma + noise < 110f) {
                                pixels[idx] = (p and -0x1000000) or 0x6e3c23
                            } else {
                                pixels[idx] = (p and -0x1000000) or 0xece6cb
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_graphicpen" -> {
                    val strokeLength = (parameters.find { it.name == "Stroke Length" }?.currentValue ?: 15f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val r = (p shr 16) and 0xff
                            val g = (p shr 8) and 0xff
                            val b = p and 0xff
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            pixels[idx] = if (luma < 128f && x % strokeLength < strokeLength / 2) {
                                (p and -0x1000000)
                            } else {
                                (p and -0x1000000) or 0xffffff
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_halftonepattern" -> {
                    val scale = (parameters.find { it.name == "Pattern Scale" }?.currentValue ?: 8f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val idx = y * w + x
                            val p = pixels[idx]
                            val r = (p shr 16) and 0xff
                            val g = (p shr 8) and 0xff
                            val b = p and 0xff
                            val luma = 0.299f * r + 0.587f * g + 0.114f * b
                            val gridMatch = (y % scale == 0) || (x % scale == 0)
                            pixels[idx] = if (gridMatch && luma < 150f) {
                                (p and -0x1000000)
                            } else {
                                (p and -0x1000000) or 0xffffff
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_notepaper" -> {
                    val depth = (parameters.find { it.name == "Emboss Depth" }?.currentValue ?: 4f).toInt()
                    val embossed = applySobel(source, 100f, Color.BLACK, Color.LTGRAY, drawEdgesOnly = true)
                    applyCoordinateDistortion(embossed) { x, y, _, _ ->
                        Pair(x + depth, y + depth)
                    }
                }
                "sketch_photocopy" -> {
                    val contrast = parameters.find { it.name == "Toner Contrast" }?.currentValue ?: 12f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val luma = 0.299f * r + 0.587f * g + 0.114f * b
                        val binary = if (luma > 128f + contrast * 5f) 255 else 0
                        pixels[i] = (p and -0x1000000) or (binary shl 16) or (binary shl 8) or binary
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_plaster" -> {
                    val depth = (parameters.find { it.name == "raised Depth" }?.currentValue ?: 8f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val luma = 0.299f * r + 0.587f * g + 0.114f * b
                        val outputValue = if (luma < 120f) 60 else 240
                        pixels[i] = (p and -0x1000000) or (outputValue shl 16) or (outputValue shl 8) or outputValue
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    applyBoxBlur(result, depth / 2)
                }
                "sketch_reticulation" -> {
                    val curdling = (parameters.find { it.name == "Curdling Rate" }?.currentValue ?: 10f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val rObj = java.util.Random()
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val luma = 0.299f * r + 0.587f * g + 0.114f * b
                        val threshold = 128f + (rObj.nextInt(curdling * 8) - curdling * 4)
                        val outColor = if (luma > threshold) 230 else 30
                        pixels[i] = (p and -0x1000000) or (outColor shl 16) or (outColor shl 8) or outColor
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_torn" -> {
                    val roughness = (parameters.find { it.name == "Roughness" }?.currentValue ?: 12f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val rObj = java.util.Random()
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        val b = p and 0xff
                        val luma = 0.299f * r + 0.587f * g + 0.114f * b
                        val outValue = if (luma > 100f + rObj.nextInt(roughness * 4)) 255 else 0
                        pixels[i] = (p and -0x1000000) or (outValue shl 16) or (outValue shl 8) or outValue
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_waterpaper" -> {
                    val softness = (parameters.find { it.name == "Softness" }?.currentValue ?: 6f).toInt()
                    val desat = applyColorQuantizeAndCluster(source, 6)
                    applyBoxBlur(desat, softness)
                }
                "sketch_craquelure" -> {
                    val spacing = (parameters.find { it.name == "Crack Spacing" }?.currentValue ?: 15f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            if (x % spacing == 0 || y % spacing == 0 || (x + y) % (spacing * 3) == 0) {
                                val idx = y * w + x
                                val p = pixels[idx]
                                pixels[idx] = (p and -0x1000000) or 0x221100
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_grain" -> {
                    val intensity = parameters.find { it.name == "Intensity" }?.currentValue ?: 20f
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    val rObj = java.util.Random()
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val grainNoise = (rObj.nextGaussian() * intensity).toInt()
                        var r = ((p shr 16) and 0xff) + grainNoise
                        var g = ((p shr 8) and 0xff) + grainNoise
                        var b = (p and 0xff) + grainNoise
                        r = r.coerceIn(0, 255)
                        g = g.coerceIn(0, 255)
                        b = b.coerceIn(0, 255)
                        pixels[i] = (p and -0x1000000) or (r shl 16) or (g shl 8) or b
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_mosaictiles" -> {
                    val gW = (parameters.find { it.name == "Grout Width" }?.currentValue ?: 4f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            if (x % 20 < gW || y % 20 < gW) {
                                pixels[y * w + x] = (pixels[y * w + x] and -0x1000000) or 0x444444
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "sketch_patchwork" -> {
                    val size = (parameters.find { it.name == "Grid Size" }?.currentValue ?: 8f).toInt()
                    applyGridTransformer(source, size) { pixels ->
                        if (pixels.isEmpty()) return@applyGridTransformer Color.GRAY
                        val avg = pixels[pixels.size / 2]
                        val r = if (((avg shr 16) and 0xff) > 128) 255 else 100
                        val g = if (((avg shr 8) and 0xff) > 128) 255 else 100
                        val b = if ((avg and 0xff) > 128) 255 else 100
                        (avg and -0x1000000) or (r shl 16) or (g shl 8) or b
                    }
                }
                "sketch_stainedglass" -> {
                    val pane = (parameters.find { it.name == "Pane Size" }?.currentValue ?: 18f).toInt()
                    applyGridTransformer(source, pane) { pixels ->
                        if (pixels.isEmpty()) return@applyGridTransformer Color.GRAY
                        val avg = pixels[pixels.size / 2]
                        val r = (avg shr 16) and 0xff
                        val g = (avg shr 8) and 0xff
                        val b = avg and 0xff
                        val rSat = (r * 1.3f).toInt().coerceIn(0, 255)
                        val gSat = (g * 1.3f).toInt().coerceIn(0, 255)
                        val bSat = (b * 1.3f).toInt().coerceIn(0, 255)
                        (avg and -0x1000000) or (rSat shl 16) or (gSat shl 8) or bSat
                    }
                }
                "sketch_texturizer" -> {
                    val scaling = (parameters.find { it.name == "Scaling" }?.currentValue ?: 12f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val factor = if ((x + y) % scaling < scaling / 2) 0.85f else 1.15f
                            val idx = y * w + x
                            val p = pixels[idx]
                            val r = (((p shr 16) and 0xff) * factor).toInt().coerceIn(0, 255)
                            val g = (((p shr 8) and 0xff) * factor).toInt().coerceIn(0, 255)
                            val b = ((p and 0xff) * factor).toInt().coerceIn(0, 255)
                            pixels[idx] = (p and -0x1000000) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                else -> {
                    applySobel(source, 100f, Color.BLACK, Color.WHITE, drawEdgesOnly = true)
                }
            }
        }
    }

    private fun createStylizeFilter(id: String, name: String, params: List<FilterParameter>): ZenithFilter {
        return CustomZenithFilter(id, name, "Stylize", params) { source, parameters ->
            when (id) {
                "stylize_glowingedges" -> {
                    val w = parameters.firstOrNull()?.currentValue ?: 4f
                    applySobel(source, 150f - w * 10f, Color.GREEN, Color.BLACK, drawEdgesOnly = true)
                }
                "stylize_findedges", "image_toolbox_find_edges" -> {
                    applySobel(source, 90f, Color.BLUE, Color.WHITE, drawEdgesOnly = true)
                }
                "stylize_emboss", "image_toolbox_emboss" -> {
                    val height = parameters.firstOrNull()?.currentValue ?: 3f
                    val edge = applySobel(source, 80f, Color.GRAY, colorQuantize(Color.GRAY, 2), drawEdgesOnly = true)
                    applyCoordinateDistortion(edge) { x, y, _, _ ->
                        Pair(x + height, y + height)
                    }
                }
                "stylize_solarize", "image_toolbox_solarize" -> {
                    val thresh = (parameters.firstOrNull()?.currentValue ?: 128f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (i in pixels.indices) {
                        val p = pixels[i]
                        val rVal = (p shr 16) and 0xff
                        val gVal = (p shr 8) and 0xff
                        val bVal = p and 0xff
                        val r = if (rVal > thresh) 255 - rVal else rVal
                        val g = if (gVal > thresh) 255 - gVal else gVal
                        val b = if (bVal > thresh) 255 - bVal else bVal
                        pixels[i] = (p and -0x1000000) or (r shl 16) or (g shl 8) or b
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "stylize_diffuse" -> {
                    val shuffle = (parameters.find { it.name == "Shuffle Range" }?.currentValue ?: 3f).toInt()
                    val rObj = java.util.Random()
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        Pair(x + rObj.nextInt(shuffle * 2 + 1) - shuffle, y + rObj.nextInt(shuffle * 2 + 1) - shuffle)
                    }
                }
                "stylize_extrude" -> {
                    val pyr = (parameters.find { it.name == "Pyramid Size" }?.currentValue ?: 10f).toInt()
                    applyGridTransformer(source, pyr) { pixels ->
                        if (pixels.isEmpty()) return@applyGridTransformer Color.BLACK
                        pixels[pixels.size / 2]
                    }
                }
                "stylize_tiles" -> {
                    val tileSize = (parameters.find { it.name == "Tile Size" }?.currentValue ?: 15f).toInt()
                    val w = source.width
                    val h = source.height
                    val pixels = IntArray(w * h)
                    source.getPixels(pixels, 0, w, 0, 0, w, h)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            if (x % tileSize == 0 || y % tileSize == 0) {
                                pixels[y * w + x] = (pixels[y * w + x] and -0x1000000) or 0x000000
                            }
                        }
                    }
                    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    result.setPixels(pixels, 0, w, 0, 0, w, h)
                    result
                }
                "stylize_trace" -> {
                    val level = (parameters.find { it.name == "LevelThreshold" }?.currentValue ?: 120f).toInt()
                    applySobel(source, level.toFloat(), Color.RED, Color.TRANSPARENT, drawEdgesOnly = false)
                }
                "stylize_wind" -> {
                    val windDist = (parameters.find { it.name == "Wind Distance" }?.currentValue ?: 18f).toInt()
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        Pair(x + (windDist * (sin(y * 0.1f) + 1f) / 2f), y)
                    }
                }
                "stylize_oilpaint" -> {
                    val curvature = (parameters.find { it.name == "Brush Curvature" }?.currentValue ?: 12f).toInt()
                    val quanted = applyColorQuantizeAndCluster(source, 6)
                    applyBoxBlur(quanted, curvature / 3 + 1)
                }
                else -> {
                    applyColorQuantizeAndCluster(source, 4)
                }
            }
        }
    }

    private fun createIbisPaintFilter(
        id: String,
        name: String,
        params: List<FilterParameter>
    ): ZenithFilter {
        return GPUImageZenithFilter(id, name, "Light Effects", params) { source, context, parameters ->
            when (id) {
                "ibis_chromatic_aberration", "image_toolbox_chromatic_aberration" -> {
                    val distance = parameters.find { it.name == "Distance" }?.currentValue ?: 16f
                    val angle = parameters.find { it.name == "Angle" }?.currentValue ?: 136f
                    try {
                        applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageChromaticAberrationFilter(distance, angle))
                    } catch (e: Throwable) {
                        applyChromaticAberrationCPU(source, distance, angle)
                    }
                }
                "ibis_glitch", "image_toolbox_glitch" -> {
                    val height = parameters.find { it.name == "Height" }?.currentValue ?: 119f
                    val strength = parameters.find { it.name == "Strength" }?.currentValue ?: 23f
                    val colorShift = parameters.find { it.name == "Color Shift" }?.currentValue ?: 8f
                    try {
                        applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageGlitchFilter(height, strength, colorShift))
                    } catch (e: Throwable) {
                        applyGlitchCPU(source, height, strength, colorShift)
                    }
                }
                "ibis_bloom", "image_toolbox_bloom" -> {
                    val area = parameters.find { it.name == "Area" }?.currentValue ?: 100f
                    val radius = parameters.find { it.name == "Radius" }?.currentValue ?: 45f
                    val brightness = parameters.find { it.name == "Brightness" }?.currentValue ?: 100f
                    val balanced = parameters.find { it.name == "Balanced Blend" }?.currentValue ?: 25f
                    try {
                        applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageBloomFilter(area, radius, brightness, balanced))
                    } catch (e: Throwable) {
                        applyBloomCPU(source, area, radius, brightness)
                    }
                }
                "ibis_cross_filter", "image_toolbox_cross_filter" -> {
                    val count = parameters.find { it.name == "Count" }?.currentValue ?: 4f
                    val direction = parameters.find { it.name == "Direction" }?.currentValue ?: 45f
                    val area = parameters.find { it.name == "Area" }?.currentValue ?: 10f
                    val brightness = parameters.find { it.name == "Brightness" }?.currentValue ?: 50f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageCrossFilterFilter(count, direction, area, brightness))
                }
                "ibis_inner_glow", "image_toolbox_inner_glow" -> {
                    val radius = parameters.find { it.name == "Radius" }?.currentValue ?: 104f
                    val r = parameters.find { it.name == "Red" }?.currentValue ?: 1f
                    val g = parameters.find { it.name == "Green" }?.currentValue ?: 1f
                    val b = parameters.find { it.name == "Blue" }?.currentValue ?: 1f
                    val hardness = parameters.find { it.name == "Hardness" }?.currentValue ?: 0.5f
                    val blendMode = parameters.find { it.name == "BlendMode" }?.currentValue ?: 2.0f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageInnerGlowFilter(radius, r, g, b, hardness, blendMode))
                }
                "ibis_bevel", "image_toolbox_bevel" -> {
                    val h = parameters.find { it.name == "Height" }?.currentValue ?: 20f
                    val s = parameters.find { it.name == "Smoothness" }?.currentValue ?: 45f
                    val hs = parameters.find { it.name == "Highlight Size" }?.currentValue ?: 14f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageBevelFilter(h, s, hs))
                }
                "ibis_emboss" -> {
                    val gs = parameters.find { it.name == "Gray Scale" }?.currentValue ?: 0f
                    val h = parameters.find { it.name == "Height" }?.currentValue ?: 1f
                    val amt = parameters.find { it.name == "Amount" }?.currentValue ?: 500f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageEmboss2Filter(gs, h, amt))
                }
                "ibis_waterdrop", "image_toolbox_waterdrop" -> {
                    val dist = parameters.find { it.name == "Distance" }?.currentValue ?: 100f
                    val flat = parameters.find { it.name == "Flatness" }?.currentValue ?: 10f
                    val h = parameters.find { it.name == "Height" }?.currentValue ?: 3f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageWaterdropFilter(dist, flat, h))
                }
                "ibis_satin", "image_toolbox_satin" -> {
                    val dist = parameters.find { it.name == "Distance" }?.currentValue ?: 11f
                    val op = parameters.find { it.name == "Opacity" }?.currentValue ?: 0.5f
                    val r = parameters.find { it.name == "Red" }?.currentValue ?: 0f
                    val g = parameters.find { it.name == "Green" }?.currentValue ?: 0f
                    val b = parameters.find { it.name == "Blue" }?.currentValue ?: 1f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageSatinFilter(dist, op, r, g, b))
                }
                "ibis_grids" -> {
                    val columns = (parameters.find { it.name == "Columns" }?.currentValue ?: 8f).toInt().coerceIn(1, 1000)
                    val rows = (parameters.find { it.name == "Rows" }?.currentValue ?: 8f).toInt().coerceIn(1, 1000)
                    val width = parameters.find { it.name == "Width" }?.currentValue ?: 1.5f
                    val r = parameters.find { it.name == "ColorRed" }?.currentValue ?: 1.0f
                    val g = parameters.find { it.name == "ColorGreen" }?.currentValue ?: 1.0f
                    val b = parameters.find { it.name == "ColorBlue" }?.currentValue ?: 1.0f
                    val a = parameters.find { it.name == "ColorAlpha" }?.currentValue ?: 1.0f

                    val offsetX = parameters.find { it.name == "OffsetX" }?.currentValue ?: 0f
                    val offsetY = parameters.find { it.name == "OffsetY" }?.currentValue ?: 0f
                    val dashed = (parameters.find { it.name == "Dashed" }?.currentValue ?: 0f) > 0.5f
                    val dashLength = parameters.find { it.name == "Dash Length" }?.currentValue ?: 15f
                    val dashGap = parameters.find { it.name == "Dash Gap" }?.currentValue ?: 10f

                    val mutableBmp = if (source.isMutable) {
                        source
                    } else {
                        source.copy(Bitmap.Config.ARGB_8888, true)
                    }
                    val canvas = Canvas(mutableBmp)
                    val paint = Paint().apply {
                        color = Color.argb((a * 255).toInt(), (r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
                        style = Paint.Style.STROKE
                        strokeWidth = width
                        if (dashed && dashLength > 0f && dashGap > 0f) {
                            pathEffect = DashPathEffect(floatArrayOf(dashLength, dashGap), 0f)
                        }
                    }
                    val colStep = mutableBmp.width.toFloat() / columns
                    for (i in 0..columns) {
                        val x = i * colStep + offsetX
                        if (x in 0f..mutableBmp.width.toFloat()) {
                            canvas.drawLine(x, 0f, x, mutableBmp.height.toFloat(), paint)
                        }
                    }
                    val rowStep = mutableBmp.height.toFloat() / rows
                    for (j in 0..rows) {
                        val y = j * rowStep + offsetY
                        if (y in 0f..mutableBmp.height.toFloat()) {
                            canvas.drawLine(0f, y, mutableBmp.width.toFloat(), y, paint)
                        }
                    }
                    mutableBmp
                }
                else -> source
            }
        }
    }

    // --- HELPER RENDERING PIPELINE METHODS ---
    private fun colorQuantize(color: Int, levels: Int): Int {
        val div = if (levels <= 1) 255 else (256 / levels)
        val r = (((color shr 16) and 0xff) / div) * div
        val g = (((color shr 8) and 0xff) / div) * div
        val b = ((color and 0xff) / div) * div
        return (color and -0x1000000) or (r shl 16) or (g shl 8) or b
    }

    private fun applySobel(
        source: Bitmap,
        threshold: Float,
        edgeColor: Int = Color.WHITE,
        bgColor: Int = Color.BLACK,
        drawEdgesOnly: Boolean = true
    ): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        val inPixels = IntArray(w * h)
        source.getPixels(inPixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)
        
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var gx = 0f; var gy = 0f
                val coeffsX = arrayOf(
                    floatArrayOf(-1f, 0f, 1f),
                    floatArrayOf(-2f, 0f, 2f),
                    floatArrayOf(-1f, 0f, 1f)
                )
                val coeffsY = arrayOf(
                    floatArrayOf(-1f, -2f, -1f),
                    floatArrayOf(0f, 0f, 0f),
                    floatArrayOf(1f, 2f, 1f)
                )
                
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val c = inPixels[(y + dy) * w + (x + dx)]
                        val r = ((c shr 16) and 0xff).toFloat()
                        val g = ((c shr 8) and 0xff).toFloat()
                        val b = (c and 0xff).toFloat()
                        val gray = 0.299f * r + 0.587f * g + 0.114f * b
                        
                        gx += gray * coeffsX[dy + 1][dx + 1]
                        gy += gray * coeffsY[dy + 1][dx + 1]
                    }
                }
                
                val valEdge = sqrt(gx * gx + gy * gy)
                if (valEdge > threshold) {
                    outPixels[y * w + x] = edgeColor
                } else {
                    if (drawEdgesOnly) {
                        outPixels[y * w + x] = bgColor
                    } else {
                        outPixels[y * w + x] = inPixels[y * w + x]
                    }
                }
            }
        }
        
        // Edge boundaries padding
        for (x in 0 until w) {
            outPixels[x] = if (drawEdgesOnly) bgColor else inPixels[x]
            outPixels[(h - 1) * w + x] = if (drawEdgesOnly) bgColor else inPixels[(h - 1) * w + x]
        }
        for (y in 0 until h) {
            outPixels[y * w] = if (drawEdgesOnly) bgColor else inPixels[y * w]
            outPixels[y * w + (w - 1)] = if (drawEdgesOnly) bgColor else inPixels[y * w + (w - 1)]
        }
        
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyColorQuantizeAndCluster(source: Bitmap, levels: Int): Bitmap {
        val actualLevels = levels.coerceIn(2, 16)
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        val inPixels = IntArray(w * h)
        source.getPixels(inPixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)
        
        for (i in inPixels.indices) {
            val c = inPixels[i]
            val a = c and -0x1000000
            val r = (c shr 16) and 0xff
            val g = (c shr 8) and 0xff
            val b = c and 0xff
            val div = 256 / actualLevels
            val qr = (r / div) * div
            val qg = (g / div) * div
            val qb = (b / div) * div
            outPixels[i] = a or (qr shl 16) or (qg shl 8) or qb
        }
        
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyGridTransformer(
        source: Bitmap,
        cellSize: Int,
        transformer: (IntArray) -> Int
    ): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)
        
        val actualCell = cellSize.coerceIn(1, 128)
        for (cy in 0 until h step actualCell) {
            val ey = (cy + actualCell).coerceAtMost(h)
            for (cx in 0 until w step actualCell) {
                val ex = (cx + actualCell).coerceAtMost(w)
                val blockW = ex - cx
                val blockH = ey - cy
                val cellPixels = IntArray(blockW * blockH)
                var idx = 0
                for (ly in cy until ey) {
                    for (lx in cx until ex) {
                        cellPixels[idx++] = pixels[ly * w + lx]
                    }
                }
                
                val targetColor = transformer(cellPixels)
                for (ly in cy until ey) {
                    for (lx in cx until ex) {
                        outPixels[ly * w + lx] = targetColor
                    }
                }
            }
        }
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyBoxBlur(source: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 100)
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        val inPixels = IntArray(w * h)
        source.getPixels(inPixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)
        val tempPixels = IntArray(w * h)
        
        for (y in 0 until h) {
            for (x in 0 until w) {
                var rSum = 0L; var gSum = 0L; var bSum = 0L; var aSum = 0L
                var count = 0
                for (dx in -r..r) {
                    val nx = (x + dx).coerceIn(0, w - 1)
                    val c = inPixels[y * w + nx]
                    aSum += (c shr 24) and 0xff
                    rSum += (c shr 16) and 0xff
                    gSum += (c shr 8) and 0xff
                    bSum += c and 0xff
                    count++
                }
                tempPixels[y * w + x] = ((aSum / count).toInt() shl 24) or
                                        ((rSum / count).toInt() shl 16) or
                                        ((gSum / count).toInt() shl 8) or
                                        (bSum / count).toInt()
            }
        }
        
        for (x in 0 until w) {
            for (y in 0 until h) {
                var rSum = 0L; var gSum = 0L; var bSum = 0L; var aSum = 0L
                var count = 0
                for (dy in -r..r) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    val c = tempPixels[ny * w + x]
                    aSum += (c shr 24) and 0xff
                    rSum += (c shr 16) and 0xff
                    gSum += (c shr 8) and 0xff
                    bSum += c and 0xff
                    count++
                }
                outPixels[y * w + x] = ((aSum / count).toInt() shl 24) or
                                       ((rSum / count).toInt() shl 16) or
                                       ((gSum / count).toInt() shl 8) or
                                       (bSum / count).toInt()
            }
        }
        
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyCoordinateDistortion(
        source: Bitmap,
        transformer: (x: Float, y: Float, width: Float, height: Float) -> Pair<Float, Float>
    ): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        val inPixels = IntArray(w * h)
        source.getPixels(inPixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)
        val wf = w.toFloat()
        val hf = h.toFloat()
        
        for (y in 0 until h) {
            val yf = y.toFloat()
            for (x in 0 until w) {
                val xf = x.toFloat()
                val coord = transformer(xf, yf, wf, hf)
                val sx = coord.first.toInt().coerceIn(0, w - 1)
                val sy = coord.second.toInt().coerceIn(0, h - 1)
                outPixels[y * w + x] = inPixels[sy * w + sx]
            }
        }
        
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun compute3DProjection(
        source: Bitmap,
        alpha: Float,
        beta: Float,
        rx: Float,
        ry: Float,
        rz: Float,
        depth: Float
    ): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        val outCanvas = android.graphics.Canvas(out)
        outCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        
        val thH = Math.toRadians(alpha.toDouble())
        val thV = Math.toRadians(beta.toDouble())
        val dx = (cos(thH) * cos(thV)).toFloat()
        val dy = (sin(thV)).toFloat()
        val steps = depth.toInt().coerceIn(1, 100)
        
        for (i in steps downTo 1) {
            val shiftX = dx * i * 0.5f
            val shiftY = dy * i * 0.5f
            val colorFilter = android.graphics.PorterDuffColorFilter(
                android.graphics.Color.argb((180 - (i * 100 / steps).coerceIn(0, 100)), 0, 0, 0),
                android.graphics.PorterDuff.Mode.SRC_ATOP
            )
            paint.colorFilter = colorFilter
            outCanvas.drawBitmap(source, shiftX, shiftY, paint)
        }
        
        paint.colorFilter = null
        outCanvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    private fun createHalftoneFilter(
        id: String,
        name: String,
        params: List<FilterParameter>
    ): ZenithFilter {
        return CustomZenithFilter(id, name, "Halftone Effects", params) { source, parameters ->
            when (id) {
                "halftone_standard", "image_toolbox_color_halftone" -> applyStandardHalftone(source, parameters)
                "halftone_shaped" -> applyShapedHalftone(source, parameters)
                "halftone_classic" -> applyClassicHalftone(source, parameters)
                "halftone_cmyk" -> applyCMYKHalftone(source, parameters)
                "halftone_line_screen" -> applyLineScreenHalftone(source, parameters)
                "halftone_crosshatch" -> applyCrosshatchHalftone(source, parameters)
                "halftone_newspaper" -> applyNewspaperHalftone(source, parameters)
                "halftone_radial" -> applyRadialHalftone(source, parameters)
                else -> source
            }
        }
    }

    private fun applyStandardHalftone(source: Bitmap, parameters: List<FilterParameter>): Bitmap {
        val dotSize = parameters.find { it.name == "Dot Size" }?.currentValue ?: 8f
        val opacity = parameters.find { it.name == "Opacity" }?.currentValue ?: 1.0f
        val colorBlend = parameters.find { it.name == "Color Blend" }?.currentValue ?: 1.0f

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val step = dotSize.coerceAtLeast(3f)
        var finalStep = step
        while ((w / finalStep) * (h / finalStep) > 30000) {
            finalStep += 1f
        }

        val stepI = finalStep.toInt().coerceAtLeast(3)
        for (cy in 0 until h step stepI) {
            for (cx in 0 until w step stepI) {
                val srcX = cx + stepI / 2
                val srcY = cy + stepI / 2
                if (srcX in 0 until w && srcY in 0 until h) {
                    val px = source.getPixel(srcX, srcY)
                    val r = (px shr 16) and 0xff
                    val g = (px shr 8) and 0xff
                    val b = px and 0xff
                    val a = (px ushr 24) and 0xff
                    
                    if (a == 0) continue
                    
                    val luma = 0.299f * r + 0.587f * g + 0.114f * b
                    val normLuma = luma / 255.0f
                    val dR = (finalStep / 2f) * (1.0f - normLuma)
                    
                    if (dR > 0.1f) {
                        val blendedColor = if (colorBlend >= 1.0f) {
                            px
                        } else {
                            val blendedR = (luma * (1f - colorBlend) + r * colorBlend).toInt().coerceIn(0, 255)
                            val blendedG = (luma * (1f - colorBlend) + g * colorBlend).toInt().coerceIn(0, 255)
                            val blendedB = (luma * (1f - colorBlend) + b * colorBlend).toInt().coerceIn(0, 255)
                            Color.argb((a * opacity).toInt().coerceIn(0, 255), blendedR, blendedG, blendedB)
                        }
                        
                        paint.color = blendedColor
                        if (opacity < 1.0f) {
                            paint.alpha = (a * opacity * ((paint.color ushr 24) / 255f)).toInt().coerceIn(0, 255)
                        }
                        canvas.drawCircle(cx.toFloat() + finalStep / 2f, cy.toFloat() + finalStep / 2f, dR, paint)
                    }
                }
            }
        }
        return out
    }

    private fun applyShapedHalftone(source: Bitmap, parameters: List<FilterParameter>): Bitmap {
        val dotSize = parameters.find { it.name == "Dot Size" }?.currentValue ?: 8f
        val shapeType = (parameters.find { it.name == "Shape Type" }?.currentValue ?: 0f).toInt()
        val contrast = parameters.find { it.name == "Contrast" }?.currentValue ?: 1.0f
        val angleDeg = parameters.find { it.name == "Angle" }?.currentValue ?: 45f
        val bgStyle = (parameters.find { it.name == "Background Style" }?.currentValue ?: 1f).toInt()

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        when (bgStyle) {
            1 -> canvas.drawColor(Color.WHITE)
            2 -> canvas.drawColor(Color.BLACK)
            3 -> canvas.drawBitmap(source, 0f, 0f, null)
            else -> { /* transparent */ }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.save()
        canvas.rotate(angleDeg, w / 2f, h / 2f)

        val center = PointF(w / 2f, h / 2f)
        val radiusBounding = sqrt((w * w + h * h).toFloat()) / 2f

        val step = dotSize.coerceAtLeast(3f)
        var finalStep = step
        while (((radiusBounding * 2) / finalStep) * ((radiusBounding * 2) / finalStep) > 30000) {
            finalStep += 1f
        }

        val startX = center.x - radiusBounding
        val endX = center.x + radiusBounding
        val startY = center.y - radiusBounding
        val endY = center.y + radiusBounding

        val matrix = Matrix()
        matrix.setRotate(-angleDeg, w / 2f, h / 2f)

        val pts = FloatArray(2)

        val stepI = finalStep.toInt().coerceAtLeast(3)
        for (cy in startY.toInt()..endY.toInt() step stepI) {
            for (cx in startX.toInt()..endX.toInt() step stepI) {
                pts[0] = cx.toFloat() + finalStep / 2f
                pts[1] = cy.toFloat() + finalStep / 2f
                matrix.mapPoints(pts)
                val srcX = pts[0].toInt()
                val srcY = pts[1].toInt()
                
                if (srcX in 0 until w && srcY in 0 until h) {
                    val px = source.getPixel(srcX, srcY)
                    val r = (px shr 16) and 0xff
                    val g = (px shr 8) and 0xff
                    val b = px and 0xff
                    val a = (px ushr 24) and 0xff
                    
                    if (a == 0) continue
                    
                    val luma = 0.299f * r + 0.587f * g + 0.114f * b
                    val normLuma = luma / 255.0f
                    val adjustedLuma = ((normLuma - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
                    
                    val dR = (finalStep / 2f) * (1.0f - adjustedLuma)
                    if (dR > 0.1f) {
                        paint.color = px
                        val centerXPos = cx.toFloat() + finalStep / 2f
                        val centerYPos = cy.toFloat() + finalStep / 2f
                        when (shapeType) {
                            0 -> { // Circle
                                canvas.drawCircle(centerXPos, centerYPos, dR, paint)
                            }
                            1 -> { // Square
                                canvas.drawRect(
                                    centerXPos - dR,
                                    centerYPos - dR,
                                    centerXPos + dR,
                                    centerYPos + dR,
                                    paint
                                )
                            }
                            2 -> { // Diamond
                                val path = Path().apply {
                                    moveTo(centerXPos, centerYPos - dR)
                                    lineTo(centerXPos + dR, centerYPos)
                                    lineTo(centerXPos, centerYPos + dR)
                                    lineTo(centerXPos - dR, centerYPos)
                                    close()
                                }
                                canvas.drawPath(path, paint)
                            }
                            3 -> { // Cross
                                val wW = dR * 0.35f
                                canvas.drawRect(centerXPos - dR, centerYPos - wW, centerXPos + dR, centerYPos + wW, paint)
                                canvas.drawRect(centerXPos - wW, centerYPos - dR, centerXPos + wW, centerYPos + dR, paint)
                            }
                        }
                    }
                }
            }
        }
        canvas.restore()
        return out
    }

    private fun applyClassicHalftone(source: Bitmap, parameters: List<FilterParameter>): Bitmap {
        val dotSize = parameters.find { it.name == "Dot Size" }?.currentValue ?: 8f
        val contrast = parameters.find { it.name == "Contrast" }?.currentValue ?: 1.0f
        val angleDeg = parameters.find { it.name == "Angle" }?.currentValue ?: 45f
        val bgStyle = (parameters.find { it.name == "Background Style" }?.currentValue ?: 1f).toInt()

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        when (bgStyle) {
            1 -> canvas.drawColor(Color.WHITE)
            2 -> canvas.drawColor(Color.BLACK)
            3 -> canvas.drawBitmap(source, 0f, 0f, null)
            else -> { /* transparent */ }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.save()
        canvas.rotate(angleDeg, w / 2f, h / 2f)

        val center = PointF(w / 2f, h / 2f)
        val radiusBounding = sqrt((w * w + h * h).toFloat()) / 2f

        val step = dotSize.coerceAtLeast(3f)
        var finalStep = step
        val gridWidth = (radiusBounding * 2)
        val gridHeight = (radiusBounding * 2)
        while ((gridWidth / finalStep) * (gridHeight / finalStep) > 30000) {
            finalStep += 1f
        }

        val startX = center.x - radiusBounding
        val endX = center.x + radiusBounding
        val startY = center.y - radiusBounding
        val endY = center.y + radiusBounding

        val matrix = Matrix()
        matrix.setRotate(-angleDeg, w / 2f, h / 2f)

        val pts = FloatArray(2)

        val stepI = finalStep.toInt().coerceAtLeast(3)
        for (cy in startY.toInt()..endY.toInt() step stepI) {
            for (cx in startX.toInt()..endX.toInt() step stepI) {
                pts[0] = cx.toFloat() + finalStep / 2f
                pts[1] = cy.toFloat() + finalStep / 2f
                matrix.mapPoints(pts)
                val srcX = pts[0].toInt()
                val srcY = pts[1].toInt()
                
                if (srcX in 0 until w && srcY in 0 until h) {
                    val px = source.getPixel(srcX, srcY)
                    val r = (px shr 16) and 0xff
                    val g = (px shr 8) and 0xff
                    val b = px and 0xff
                    val a = (px ushr 24) and 0xff
                    
                    if (a == 0) continue
                    
                    val luma = 0.299f * r + 0.587f * g + 0.114f * b
                    val normLuma = luma / 255.0f
                    val adjustedLuma = ((normLuma - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
                    
                    val dR = (finalStep / 2f) * (1.0f - adjustedLuma)
                    if (dR > 0.1f) {
                        paint.color = px
                        canvas.drawCircle(cx.toFloat() + finalStep / 2f, cy.toFloat() + finalStep / 2f, dR, paint)
                    }
                }
            }
        }
        canvas.restore()
        return out
    }

    private fun applyCMYKHalftone(source: Bitmap, parameters: List<FilterParameter>): Bitmap {
        val freq = parameters.find { it.name == "Screen Frequency" }?.currentValue ?: 12f
        val cAngle = parameters.find { it.name == "Cyan Angle" }?.currentValue ?: 15f
        val mAngle = parameters.find { it.name == "Magenta Angle" }?.currentValue ?: 75f
        val yAngle = parameters.find { it.name == "Yellow Angle" }?.currentValue ?: 0f
        val kAngle = parameters.find { it.name == "Black Angle" }?.currentValue ?: 45f
        val dotScale = parameters.find { it.name == "Dot Scale" }?.currentValue ?: 1.0f
        val paperStyle = (parameters.find { it.name == "Paper Style" }?.currentValue ?: 1f).toInt()

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        when (paperStyle) {
            1 -> canvas.drawColor(Color.WHITE)
            2 -> canvas.drawColor(Color.argb(255, 252, 245, 225)) // vintage warm
            else -> { /* transparent */ }
        }

        val getC = { px: Int ->
            val r = ((px shr 16) and 0xff) / 255f
            val g = ((px shr 8) and 0xff) / 255f
            val b = (px and 0xff) / 255f
            val k = 1f - maxOf(r, g, b)
            if (k < 1f) (1f - r - k) / (1f - k) else 0f
        }
        val getM = { px: Int ->
            val r = ((px shr 16) and 0xff) / 255f
            val g = ((px shr 8) and 0xff) / 255f
            val b = (px and 0xff) / 255f
            val k = 1f - maxOf(r, g, b)
            if (k < 1f) (1f - g - k) / (1f - k) else 0f
        }
        val getY = { px: Int ->
            val r = ((px shr 16) and 0xff) / 255f
            val g = ((px shr 8) and 0xff) / 255f
            val b = (px and 0xff) / 255f
            val k = 1f - maxOf(r, g, b)
            if (k < 1f) (1f - b - k) / (1f - k) else 0f
        }
        val getK = { px: Int ->
            val r = ((px shr 16) and 0xff) / 255f
            val g = ((px shr 8) and 0xff) / 255f
            val b = (px and 0xff) / 255f
            1f - maxOf(r, g, b)
        }

        // Draw CMYK passes with Multiply blend mode
        drawCMYKChannel(canvas, source, yAngle, freq, dotScale, Color.argb(255, 255, 230, 0), getY)
        drawCMYKChannel(canvas, source, mAngle, freq, dotScale, Color.argb(255, 235, 0, 130), getM)
        drawCMYKChannel(canvas, source, cAngle, freq, dotScale, Color.argb(255, 0, 175, 230), getC)
        drawCMYKChannel(canvas, source, kAngle, freq, dotScale, Color.argb(255, 25, 25, 28), getK)

        return out
    }

    private fun drawCMYKChannel(
        canvas: Canvas,
        source: Bitmap,
        angle: Float,
        step: Float,
        dotScale: Float,
        channelColor: Int,
        getChannelValue: (Int) -> Float
    ) {
        val w = source.width
        val h = source.height
        canvas.save()
        canvas.rotate(angle, w / 2f, h / 2f)
        val center = PointF(w / 2f, h / 2f)
        val radiusBounding = sqrt((w * w + h * h).toFloat()) / 2f
        
        val startX = center.x - radiusBounding
        val endX = center.x + radiusBounding
        val startY = center.y - radiusBounding
        val endY = center.y + radiusBounding

        val matrix = Matrix()
        matrix.setRotate(-angle, w / 2f, h / 2f)
        val pts = FloatArray(2)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = channelColor
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }

        var finalStep = step
        while (((radiusBounding * 2) / finalStep) * ((radiusBounding * 2) / finalStep) > 12000) {
            finalStep += 0.5f
        }

        val stepI = finalStep.toInt().coerceAtLeast(3)
        for (cy in startY.toInt()..endY.toInt() step stepI) {
            for (cx in startX.toInt()..endX.toInt() step stepI) {
                pts[0] = cx.toFloat() + finalStep / 2f
                pts[1] = cy.toFloat() + finalStep / 2f
                matrix.mapPoints(pts)
                val srcX = pts[0].toInt()
                val srcY = pts[1].toInt()
                if (srcX in 0 until w && srcY in 0 until h) {
                    val px = source.getPixel(srcX, srcY)
                    val cVal = getChannelValue(px)
                    if (cVal > 0.02f) {
                        val dR = (finalStep / 2f) * cVal * dotScale
                        canvas.drawCircle(cx.toFloat() + finalStep / 2f, cy.toFloat() + finalStep / 2f, dR, paint)
                    }
                }
            }
        }
        canvas.restore()
    }

    private fun applyLineScreenHalftone(source: Bitmap, parameters: List<FilterParameter>): Bitmap {
        val lineSpacing = parameters.find { it.name == "Line Spacing" }?.currentValue ?: 8f
        val lineAngle = parameters.find { it.name == "Line Angle" }?.currentValue ?: 45f
        val contrast = parameters.find { it.name == "Contrast" }?.currentValue ?: 1.0f
        val lineWidth = parameters.find { it.name == "Line Width" }?.currentValue ?: 1.0f
        val lineBreaks = parameters.find { it.name == "Line Breaks" }?.currentValue ?: 0f
        val sineWave = parameters.find { it.name == "Sine Wave" }?.currentValue ?: 0f
        val waveAmp = parameters.find { it.name == "Wave Amplitude" }?.currentValue ?: 4f
        val bgStyle = (parameters.find { it.name == "Background Style" }?.currentValue ?: 1f).toInt()

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        when (bgStyle) {
            1 -> canvas.drawColor(Color.WHITE)
            2 -> canvas.drawColor(Color.BLACK)
            3 -> canvas.drawBitmap(source, 0f, 0f, null)
            else -> { /* transparent */ }
        }

        drawSingleLineScreen(
            canvas = canvas,
            source = source,
            angle = lineAngle,
            spacing = lineSpacing,
            thicknessScale = 1.0f,
            inkType = 3, // Source Color
            sineWave = (sineWave > 0.5f),
            waveAmp = waveAmp,
            contrast = contrast,
            lineWidthScale = lineWidth,
            lineBreaks = lineBreaks
        )

        return out
    }

    private fun drawSingleLineScreen(
        canvas: Canvas,
        source: Bitmap,
        angle: Float,
        spacing: Float,
        thicknessScale: Float,
        inkType: Int,
        sineWave: Boolean,
        waveAmp: Float,
        contrast: Float,
        lineWidthScale: Float = 1.0f,
        lineBreaks: Float = 0f
    ) {
        val w = source.width
        val h = source.height
        canvas.save()
        canvas.rotate(angle, w / 2f, h / 2f)
        
        val center = PointF(w / 2f, h / 2f)
        val radiusBounding = sqrt((w * w + h * h).toFloat()) / 2f
        val startX = center.x - radiusBounding
        val endX = center.x + radiusBounding
        val startY = center.y - radiusBounding
        val endY = center.y + radiusBounding

        val matrix = Matrix()
        matrix.setRotate(-angle, w / 2f, h / 2f)
        val pts = FloatArray(2)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        var finalStep = spacing
        while (((radiusBounding * 2) / finalStep) * ((radiusBounding * 2) / 8f) > 20000) {
            finalStep += 1f
        }

        val stepI = finalStep.toInt().coerceAtLeast(3)
        val segmentLength = 8f
        for (cx in startX.toInt()..endX.toInt() step stepI) {
            for (cy in startY.toInt()..endY.toInt() step segmentLength.toInt()) {
                if (lineBreaks > 0.01f) {
                    val randVal = ((cx * 1013 + cy * 313) % 1000) / 1000f
                    if (randVal < lineBreaks) continue
                }

                pts[0] = cx.toFloat()
                pts[1] = cy.toFloat()
                matrix.mapPoints(pts)
                val srcX = pts[0].toInt()
                val srcY = pts[1].toInt()
                if (srcX in 0 until w && srcY in 0 until h) {
                    val px = source.getPixel(srcX, srcY)
                    val r = (px shr 16) and 0xff
                    val g = (px shr 8) and 0xff
                    val b = px and 0xff
                    val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                    val adjustedLuma = ((luma - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
                    val inkDensity = 1.0f - adjustedLuma
                    
                    if (inkDensity > 0.05f) {
                        paint.strokeWidth = finalStep * inkDensity * thicknessScale * lineWidthScale * 0.9f
                        paint.color = when (inkType) {
                            1 -> Color.argb(255, 10, 45, 120) // retro blue
                            2 -> Color.argb(255, 90, 50, 25)  // deep sepia
                            3 -> px // source color
                            else -> Color.BLACK
                        }
                        
                        var xOffset = 0f
                        if (sineWave) {
                            xOffset = sin(cy.toFloat() * 0.04f) * waveAmp
                        }
                        canvas.drawLine(
                            cx.toFloat() + xOffset,
                            cy.toFloat(),
                            cx.toFloat() + xOffset,
                            cy.toFloat() + segmentLength,
                            paint
                        )
                    }
                }
            }
        }
        canvas.restore()
    }

    private fun applyCrosshatchHalftone(source: Bitmap, parameters: List<FilterParameter>): Bitmap {
        val gridSpacing = parameters.find { it.name == "Grid Spacing" }?.currentValue ?: 10f
        val priAngle = parameters.find { it.name == "Primary Angle" }?.currentValue ?: 45f
        val crossAngle = parameters.find { it.name == "Cross Angle" }?.currentValue ?: 90f
        val lineThick = parameters.find { it.name == "Line Thickness" }?.currentValue ?: 1.5f
        val inkType = (parameters.find { it.name == "Ink Type" }?.currentValue ?: 0f).toInt()

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        // Pass 1: Primary line screen
        drawSingleLineScreen(
            canvas = canvas,
            source = source,
            angle = priAngle,
            spacing = gridSpacing,
            thicknessScale = lineThick,
            inkType = inkType,
            sineWave = false,
            waveAmp = 0f,
            contrast = 1.0f
        )

        // Pass 2: Crossed line screen
        drawSingleLineScreen(
            canvas = canvas,
            source = source,
            angle = priAngle + crossAngle,
            spacing = gridSpacing,
            thicknessScale = lineThick,
            inkType = inkType,
            sineWave = false,
            waveAmp = 0f,
            contrast = 1.0f
        )

        return out
    }

    private fun applyNewspaperHalftone(source: Bitmap, parameters: List<FilterParameter>): Bitmap {
        val freq = parameters.find { it.name == "Dot Frequency" }?.currentValue ?: 10f
        val bleed = parameters.find { it.name == "Bleed Amount" }?.currentValue ?: 0.3f
        val yellowing = parameters.find { it.name == "Paper Yellowing" }?.currentValue ?: 0.8f
        val contrast = parameters.find { it.name == "Contrast" }?.currentValue ?: 1.5f
        val rotation = parameters.find { it.name == "Dot Rotation" }?.currentValue ?: 45f

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Yellowing vintage newspaper base
        val yFactor = yellowing.coerceIn(0f, 1f)
        val paperColor = Color.argb(
            255,
            (255 - 12 * yFactor).toInt(),
            (250 - 25 * yFactor).toInt(),
            (230 - 50 * yFactor).toInt()
        )
        canvas.drawColor(paperColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 18, 18, 22) // Newspaper dark ink
        }

        canvas.save()
        canvas.rotate(rotation, w / 2f, h / 2f)

        val center = PointF(w / 2f, h / 2f)
        val radiusBounding = sqrt((w * w + h * h).toFloat()) / 2f

        val step = freq.coerceAtLeast(3f)
        var finalStep = step
        while (((radiusBounding * 2) / finalStep) * ((radiusBounding * 2) / finalStep) > 25000) {
            finalStep += 1f
        }

        val startX = center.x - radiusBounding
        val endX = center.x + radiusBounding
        val startY = center.y - radiusBounding
        val endY = center.y + radiusBounding

        val matrix = Matrix()
        matrix.setRotate(-rotation, w / 2f, h / 2f)
        val pts = FloatArray(2)

        val stepI = finalStep.toInt().coerceAtLeast(3)
        for (cy in startY.toInt()..endY.toInt() step stepI) {
            for (cx in startX.toInt()..endX.toInt() step stepI) {
                pts[0] = cx.toFloat() + finalStep / 2f
                pts[1] = cy.toFloat() + finalStep / 2f
                matrix.mapPoints(pts)
                val srcX = pts[0].toInt()
                val srcY = pts[1].toInt()
                
                if (srcX in 0 until w && srcY in 0 until h) {
                    val px = source.getPixel(srcX, srcY)
                    val r = (px shr 16) and 0xff
                    val g = (px shr 8) and 0xff
                    val b = px and 0xff
                    val a = (px ushr 24) and 0xff
                    if (a == 0) continue

                    val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                    val adjustedLuma = ((luma - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
                    val dR = (finalStep / 2f) * (1.0f - adjustedLuma)

                    if (dR > 0.1f) {
                        paint.alpha = 255
                        canvas.drawCircle(cx.toFloat() + finalStep / 2f, cy.toFloat() + finalStep / 2f, dR, paint)
                        
                        if (bleed > 0.02f) {
                            paint.alpha = (bleed * 110).toInt()
                            canvas.drawCircle(
                                cx.toFloat() + finalStep / 2f,
                                cy.toFloat() + finalStep / 2f,
                                dR * (1.0f + bleed * 0.6f),
                                paint
                            )
                        }
                    }
                }
            }
        }
        canvas.restore()
        return out
    }

    private fun applyRadialHalftone(source: Bitmap, parameters: List<FilterParameter>): Bitmap {
        val freq = parameters.find { it.name == "Frequency" }?.currentValue ?: 12f
        val cXFrac = parameters.find { it.name == "Center X" }?.currentValue ?: 0.5f
        val cYFrac = parameters.find { it.name == "Center Y" }?.currentValue ?: 0.5f
        val maxDot = parameters.find { it.name == "Max Dot Size" }?.currentValue ?: 8f
        val fadeOut = parameters.find { it.name == "Fade Out" }?.currentValue ?: 1.0f

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        val centerX = w * cXFrac
        val centerY = h * cYFrac
        val maxDist = sqrt(maxOf(centerX, w - centerX) * maxOf(centerX, w - centerX) + maxOf(centerY, h - centerY) * maxOf(centerY, h - centerY))

        val step = freq.coerceAtLeast(4f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        var rRing = step
        while (rRing < maxDist) {
            val circumference = 2f * PI.toFloat() * rRing
            val numDots = (circumference / step).toInt().coerceAtLeast(1)
            
            if (rRing / step > 350) break
            
            val dTheta = (2f * PI.toFloat()) / numDots
            for (i in 0 until numDots) {
                val theta = i * dTheta
                val cx = centerX + rRing * cos(theta)
                val cy = centerY + rRing * sin(theta)
                
                val srcX = cx.toInt()
                val srcY = cy.toInt()
                if (srcX in 0 until w && srcY in 0 until h) {
                    val px = source.getPixel(srcX, srcY)
                    val red = (px shr 16) and 0xff
                    val g = (px shr 8) and 0xff
                    val b = px and 0xff
                    val luma = (0.299f * red + 0.587f * g + 0.114f * b) / 255f
                    
                    val distFactor = rRing / maxDist
                    val fade = distFactor.pow(fadeOut).coerceIn(0f, 1f)
                    val inkDensity = (1.0f - luma) * (1.0f - fade)
                    
                    if (inkDensity > 0.05f) {
                        val dSize = (maxDot / 2f) * inkDensity
                        paint.color = px
                        canvas.drawCircle(cx, cy, dSize, paint)
                    }
                }
            }
            rRing += step
        }
        return out
    }

    private fun sampleBilinear(pixels: IntArray, w: Int, h: Int, x: Float, y: Float): Int {
        val xFloor = floor(x)
        val yFloor = floor(y)
        
        val x0 = xFloor.toInt().coerceIn(0, w - 1)
        val x1 = (x0 + 1).coerceIn(0, w - 1)
        val y0 = yFloor.toInt().coerceIn(0, h - 1)
        val y1 = (y0 + 1).coerceIn(0, h - 1)

        val xWeight = x - xFloor
        val yWeight = y - yFloor

        val p00 = pixels[y0 * w + x0]
        val p10 = pixels[y0 * w + x1]
        val p01 = pixels[y1 * w + x0]
        val p11 = pixels[y1 * w + x1]

        val a00 = (p00 ushr 24) and 0xff
        val r00 = (p00 ushr 16) and 0xff
        val g00 = (p00 ushr 8) and 0xff
        val b00 = p00 and 0xff

        val a10 = (p10 ushr 24) and 0xff
        val r10 = (p10 ushr 16) and 0xff
        val g10 = (p10 ushr 8) and 0xff
        val b10 = p10 and 0xff

        val a01 = (p01 ushr 24) and 0xff
        val r01 = (p01 ushr 16) and 0xff
        val g01 = (p01 ushr 8) and 0xff
        val b01 = p01 and 0xff

        val a11 = (p11 ushr 24) and 0xff
        val r11 = (p11 ushr 16) and 0xff
        val g11 = (p11 ushr 8) and 0xff
        val b11 = p11 and 0xff

        val a0 = a00 + xWeight * (a10 - a00)
        val a1 = a01 + xWeight * (a11 - a01)
        val a = (a0 + yWeight * (a1 - a0)).toInt().coerceIn(0, 255)

        val r0 = r00 + xWeight * (r10 - r00)
        val r1 = r01 + xWeight * (r11 - r01)
        val r = (r0 + yWeight * (r1 - r0)).toInt().coerceIn(0, 255)

        val g0 = g00 + xWeight * (g10 - g00)
        val g1 = g01 + xWeight * (g11 - g01)
        val g = (g0 + yWeight * (g1 - g0)).toInt().coerceIn(0, 255)

        val b0 = b00 + xWeight * (b10 - b00)
        val b1 = b01 + xWeight * (b11 - b01)
        val b = (b0 + yWeight * (b1 - b0)).toInt().coerceIn(0, 255)

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun applyFractalGlass(source: Bitmap, parameters: List<FilterParameter>): Bitmap {
        val originalW = source.width
        val originalH = source.height
        
        // Optimize maximum dimension to 2400f for crystal clear professional print quality
        val maxDimension = 2400f
        val needsDownscale = originalW > maxDimension || originalH > maxDimension
        
        val scaleFactor = if (needsDownscale) {
            maxDimension / maxOf(originalW, originalH)
        } else {
            1.0f
        }
        
        val workingSource = if (needsDownscale) {
            val dw = (originalW * scaleFactor).toInt().coerceAtLeast(2)
            val dh = (originalH * scaleFactor).toInt().coerceAtLeast(2)
            Bitmap.createScaledBitmap(source, dw, dh, true)
        } else {
            source
        }

        val style = parameters.find { it.name == "Glass Style" }?.currentValue ?: 1f
        val scale = ((parameters.find { it.name == "Glass Scale" }?.currentValue ?: 30f) * scaleFactor).coerceAtLeast(4f)
        val refraction = (parameters.find { it.name == "Refraction Index" }?.currentValue ?: 15f) * scaleFactor
        val frosting = (parameters.find { it.name == "Frosting/Grain" }?.currentValue ?: 10f) * scaleFactor
        val shine = parameters.find { it.name == "Light Shine" }?.currentValue ?: 30f
        val angle = parameters.find { it.name == "Angle" }?.currentValue ?: 0f

        val w = workingSource.width
        val h = workingSource.height
        val out = Bitmap.createBitmap(w, h, workingSource.config ?: Bitmap.Config.ARGB_8888)
        val inPixels = IntArray(w * h)
        workingSource.getPixels(inPixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)

        val rad = Math.toRadians(angle.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        val cx = w / 2f
        val cy = h / 2f

        val styleInt = style.toInt()

        // Distribute row rendering tasks dynamically across all available CPU cores
        val numThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val rowBatch = (h + numThreads - 1) / numThreads

        val executor = java.util.concurrent.Executors.newFixedThreadPool(numThreads)
        val futures = java.util.ArrayList<java.util.concurrent.Future<*>>()

        for (threadIdx in 0 until numThreads) {
            futures.add(executor.submit {
                val startY = threadIdx * rowBatch
                val endY = minOf(startY + rowBatch, h)
                for (y in startY until endY) {
                    val ry = y - cy
                    val yOffset = y * w
                    for (x in 0 until w) {
                        val rx = x - cx
                        
                        // 1. Rotate to pattern coordinates (px, py)
                        val px = rx * cosA - ry * sinA + cx
                        val py = rx * sinA + ry * cosA + cy

                        var dx = 0f
                        var dy = 0f
                        var boundary = 0.0

                        when (styleInt) {
                            0 -> { // Ribbed / Linear Flutes
                                val frequency = 2.0 * PI / scale
                                val angleArg = px * frequency
                                val wave = sin(angleArg)
                                val slope = cos(angleArg)
                                dx = (slope * refraction).toFloat()
                                dy = 0f
                                boundary = max(0.0, 1.0 - abs(wave))
                            }
                            1 -> { // Hexagonal/Voronoi cells
                                val s = scale
                                val cellX = floor((px / s).toDouble()).toInt()
                                val cellY = floor((py / s).toDouble()).toInt()
                                var minDist = 1e9
                                var secondMinDist = 1e9
                                var closestCellX = 0f
                                var closestCellY = 0f
                                for (offsetY in -1..1) {
                                    for (offsetX in -1..1) {
                                        val cxCell = cellX + offsetX
                                        val cyCell = cellY + offsetY
                                        
                                        // Super fast LCG integer hashing to avoid sin/cos inside 9x loop
                                        var h1 = cxCell * 374761393 + cyCell * 668265263
                                        h1 = (h1 xor (h1 ushr 13)) * 1274126177
                                        val hash1 = (h1 xor (h1 ushr 16)) and 0xffff
                                        
                                        var h2 = cxCell * -1640531527 + cyCell * -2048144777
                                        h2 = (h2 xor (h2 ushr 13)) * 1274126177
                                        val hash2 = (h2 xor (h2 ushr 16)) and 0xffff
                                        
                                        val rxCell = hash1 / 65535.0f
                                        val ryCell = hash2 / 65535.0f

                                        val centerX = (cxCell + rxCell) * s
                                        val centerY = (cyCell + ryCell) * s
                                        val dxCell = px - centerX
                                        val dyCell = py - centerY
                                        val dist = (dxCell * dxCell + dyCell * dyCell).toDouble()
                                        if (dist < minDist) {
                                            secondMinDist = minDist
                                            minDist = dist
                                            closestCellX = centerX
                                            closestCellY = centerY
                                        } else if (dist < secondMinDist) {
                                            secondMinDist = dist
                                        }
                                    }
                                }
                                dx = (closestCellX - px) * (refraction / s)
                                dy = (closestCellY - py) * (refraction / s)
                                boundary = (1.0 - sqrt(minDist) / s).coerceIn(0.0, 1.0)
                            }
                            2 -> { // Wavy/Sinusoidal
                                val frequency = 2.0 * PI / scale
                                val waveX = sin(px * frequency)
                                val waveY = sin(py * frequency)
                                dx = (cos(py * frequency) * refraction).toFloat()
                                dy = (cos(px * frequency) * refraction).toFloat()
                                boundary = (abs(waveX) + abs(waveY)) / 2.0
                            }
                            3 -> { // Triangular / Crystallized Facets
                                val s = scale
                                val tx = floor((px / s).toDouble()).toInt()
                                val ty = floor((py / s).toDouble()).toInt()
                                val fx = (px / s) - tx
                                val fy = (py / s) - ty
                                val inUpperTriangle = fx + fy < 1.0f
                                dx = if (inUpperTriangle) -refraction * fx else refraction * (1.0f - fx)
                                dy = if (inUpperTriangle) -refraction * fy else refraction * (1.0f - fy)
                                boundary = abs((fx + fy - 1.0f).toDouble())
                            }
                            4 -> { // Frosted glass micro-texture (using ultra-fast integer hash)
                                var h1 = x * 374761393 + y * 668265263
                                h1 = (h1 xor (h1 ushr 13)) * 1274126177
                                val hash1 = (h1 xor (h1 ushr 16)) and 0xffff
                                
                                var h2 = x * -1640531527 + y * -2048144777
                                h2 = (h2 xor (h2 ushr 13)) * 1274126177
                                val hash2 = (h2 xor (h2 ushr 16)) and 0xffff

                                val rx1 = hash1 / 65535.0f
                                val ry1 = hash2 / 65535.0f

                                dx = (rx1 - 0.5f) * refraction * 0.4f
                                dy = (ry1 - 0.5f) * refraction * 0.4f
                                boundary = 0.0
                            }
                            else -> { // Glass bricks
                                val s = scale
                                val bx = floor((px / s).toDouble()).toInt()
                                val by = floor((py / (s * 0.6f)).toDouble()).toInt()
                                val fx = (px / s) - bx
                                val fy = (py / (s * 0.6f)) - by
                                val borderDistX = min(fx, 1.0f - fx)
                                val borderDistY = min(fy, 1.0f - fy)
                                val edgeDist = min(borderDistX, borderDistY)
                                dx = (0.5f - fx) * refraction
                                dy = (0.5f - fy) * refraction
                                boundary = (1.0 - (edgeDist / 0.15f)).coerceIn(0.0, 1.0)
                            }
                        }

                        // 2. Rotate displacement vector back to image coords
                        val sdx = dx * cosA + dy * sinA
                        val sdy = -dx * sinA + dy * cosA

                        var sxFloat = x + sdx
                        var syFloat = y + sdy

                        // 3. Add Frosting / Grain
                        if (frosting > 0f) {
                            var h1 = x * 374761393 + y * 668265263
                            h1 = (h1 xor (h1 ushr 13)) * 1274126177
                            val hash1 = (h1 xor (h1 ushr 16)) and 0xffff
                            
                            var h2 = x * -1640531527 + y * -2048144777
                            h2 = (h2 xor (h2 ushr 13)) * 1274126177
                            val hash2 = (h2 xor (h2 ushr 16)) and 0xffff

                            val rx1 = hash1 / 65535.0f
                            val ry1 = hash2 / 65535.0f

                            sxFloat += (rx1 - 0.5f) * frosting * 0.35f
                            syFloat += (ry1 - 0.5f) * frosting * 0.35f
                        }

                        // 4. Sample using smooth bilinear filtering to preserve high-res boundaries and PNG alpha
                        val p = sampleBilinear(inPixels, w, h, sxFloat, syFloat)

                        // 5. Apply Light Shine (specular bright edges)
                        if (shine > 0f && boundary > 0.0) {
                            val aVal = (p ushr 24) and 0xff
                            val rVal = (p shr 16) and 0xff
                            val gVal = (p shr 8) and 0xff
                            val bVal = p and 0xff

                            val hl = (Math.pow(boundary, 8.0) * (shine / 100.0) * 110.0).toFloat()
                            val r = (rVal + hl).toInt().coerceIn(0, 255)
                            val g = (gVal + hl).toInt().coerceIn(0, 255)
                            val b = (bVal + hl).toInt().coerceIn(0, 255)

                            outPixels[yOffset + x] = (aVal shl 24) or (r shl 16) or (g shl 8) or b
                        } else {
                            outPixels[yOffset + x] = p
                        }
                    }
                }
            })
        }

        // Wait for all execution chunks to finish
        for (future in futures) {
            future.get()
        }
        executor.shutdown()

        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        
        val finalOut = if (needsDownscale) {
            val scaledBack = Bitmap.createScaledBitmap(out, originalW, originalH, true)
            out.recycle()
            workingSource.recycle()
            scaledBack
        } else {
            out
        }
        
        return finalOut
    }

    private fun applyChromaticAberrationCPU(source: Bitmap, distance: Float, angleDegrees: Float): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        
        val outPixels = IntArray(w * h)
        val angleRad = Math.toRadians(angleDegrees.toDouble())
        val dx = (Math.cos(angleRad) * distance).toInt()
        val dy = (Math.sin(angleRad) * distance).toInt()
        
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val p = pixels[idx]
                val a = (p ushr 24) and 0xff
                val g = (p shr 8) and 0xff
                
                val rx = (x - dx).coerceIn(0, w - 1)
                val ry = (y - dy).coerceIn(0, h - 1)
                val rColor = pixels[ry * w + rx]
                val r = (rColor shr 16) and 0xff
                
                val bx = (x + dx).coerceIn(0, w - 1)
                val by = (y + dy).coerceIn(0, h - 1)
                val bColor = pixels[by * w + bx]
                val b = bColor and 0xff
                
                outPixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyGlitchCPU(source: Bitmap, height: Float, strength: Float, colorShift: Float): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val outPixels = pixels.clone()
        
        val random = java.util.Random(1337)
        val numBands = (h / height.coerceAtLeast(5f)).toInt().coerceIn(2, 40)
        
        for (i in 0 until numBands) {
            if (random.nextFloat() < 0.4f) {
                val bandYStart = random.nextInt(h)
                val bandHeight = (random.nextFloat() * height).toInt().coerceIn(2, 50)
                val bandYEnd = (bandYStart + bandHeight).coerceAtMost(h - 1)
                val shiftX = ((random.nextFloat() - 0.5f) * strength * 2f).toInt()
                
                for (y in bandYStart..bandYEnd) {
                    for (x in 0 until w) {
                        val targetX = (x + shiftX).coerceIn(0, w - 1)
                        val srcIdx = y * w + targetX
                        val destIdx = y * w + x
                        
                        val p = pixels[srcIdx]
                        val a = p and 0xff000000.toInt()
                        
                        var r = (p shr 16) and 0xff
                        val g = (p shr 8) and 0xff
                        var b = p and 0xff
                        if (random.nextFloat() < 0.3f) {
                            r = (r + colorShift.toInt()).coerceIn(0, 255)
                            b = (b - colorShift.toInt()).coerceIn(0, 255)
                        }
                        
                        outPixels[destIdx] = a or (r shl 16) or (g shl 8) or b
                    }
                }
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyBloomCPU(source: Bitmap, threshold: Float, blurRadius: Float, intensity: Float): Bitmap {
        val w = source.width
        val h = source.height
        
        val brightBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val brightPixels = IntArray(w * h)
        
        val threshInt = (threshold * 2.55f).toInt()
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt()
            if (lum > threshInt) {
                brightPixels[i] = p
            } else {
                brightPixels[i] = p and 0xff000000.toInt()
            }
        }
        brightBmp.setPixels(brightPixels, 0, w, 0, 0, w, h)
        
        val blurredBright = applyBoxBlur(brightBmp, (blurRadius / 2f).toInt().coerceAtLeast(1))
        
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val blurPixels = IntArray(w * h)
        blurredBright.getPixels(blurPixels, 0, w, 0, 0, w, h)
        
        val outPixels = IntArray(w * h)
        val blendFactor = (intensity / 100f).coerceIn(0f, 2f)
        
        for (i in pixels.indices) {
            val p = pixels[i]
            val bp = blurPixels[i]
            
            val a = (p ushr 24) and 0xff
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            
            val br = (bp shr 16) and 0xff
            val bg = (bp shr 8) and 0xff
            val bb = bp and 0xff
            
            val nr = (r + br * blendFactor).toInt().coerceIn(0, 255)
            val ng = (g + bg * blendFactor).toInt().coerceIn(0, 255)
            val nb = (b + bb * blendFactor).toInt().coerceIn(0, 255)
            
            outPixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
        }
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }
}

class GPUImageMotionBlurFilter(
    var distance: Float = 20.0f,
    var angle: Float = 45.0f,
    var width: Float = 1000f,
    var height: Float = 1000f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(
    """
    attribute vec4 position;
    attribute vec4 inputTextureCoordinate;
    varying vec2 textureCoordinate;
    void main() {
        gl_Position = position;
        textureCoordinate = inputTextureCoordinate.xy;
    }
    """.trimIndent(),
    """
    precision highp float;
    varying highp vec2 textureCoordinate;
    uniform sampler2D inputImageTexture;
    uniform highp float uDirectionX;
    uniform highp float uDirectionY;
    void main() {
        highp vec4 color = vec4(0.0);
        highp float totalWeight = 0.0;
        highp vec2 dir = vec2(uDirectionX, uDirectionY);
        for (int i = -10; i <= 10; i++) {
            highp float offset = float(i) / 10.0;
            color += texture2D(inputImageTexture, textureCoordinate + dir * offset);
            totalWeight += 1.0;
        }
        gl_FragColor = color / totalWeight;
    }
    """.trimIndent()
) {
    private var uDirectionXLocation: Int = -1
    private var uDirectionYLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uDirectionXLocation = android.opengl.GLES20.glGetUniformLocation(program, "uDirectionX")
        uDirectionYLocation = android.opengl.GLES20.glGetUniformLocation(program, "uDirectionY")
    }

    override fun onInitialized() {
        super.onInitialized()
        updateDirection()
    }

    fun updateParams(newDistance: Float, newAngle: Float, newWidth: Float, newHeight: Float) {
        distance = newDistance
        angle = newAngle
        width = newWidth
        height = newHeight
        updateDirection()
    }

    private fun updateDirection() {
        val rad = Math.toRadians(angle.toDouble())
        val dx = (Math.cos(rad) * distance / maxOf(1f, width)).toFloat()
        val dy = (Math.sin(rad) * distance / maxOf(1f, height)).toFloat()
        setFloat(uDirectionXLocation, dx)
        setFloat(uDirectionYLocation, dy)
    }
}
