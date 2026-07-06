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
    val gpuImage = jp.co.cyberagent.android.gpuimage.GPUImage(context)
    gpuImage.setImage(source)
    gpuImage.setFilter(filter)
    return gpuImage.bitmapWithFilterApplied
}

object EffectStackManager {
    val filtersByLayer = androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<ZenithFilter>>()
    val texturesByLayer = androidx.compose.runtime.mutableStateMapOf<String, String>() // layerId -> absolute file path of texture
    
    var currentLayerId: String? = null
    var currentLayerImageUri: String? = null
    
    fun getFiltersForLayer(layerId: String): androidx.compose.runtime.snapshots.SnapshotStateList<ZenithFilter> {
        return filtersByLayer.getOrPut(layerId) {
            androidx.compose.runtime.mutableStateListOf()
        }
    }
    
    val activeFilters: androidx.compose.runtime.snapshots.SnapshotStateList<ZenithFilter>
        get() = getFiltersForLayer(currentLayerId ?: "")
    
    fun addFilter(filter: ZenithFilter) {
        val list = activeFilters
        if (!list.any { it.id == filter.id }) {
            list.add(filter)
        }
    }
    
    fun removeFilter(id: String) {
        activeFilters.removeAll { it.id == id }
    }
    
    fun duplicateFilter(id: String) {
        val list = activeFilters
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val original = list[index]
            val suffix = java.util.UUID.randomUUID().toString().take(6)
            val newId = "${original.id}_copy_${suffix}"
            val duplicated = original.duplicate(newId)
            list.add(index + 1, duplicated)
        }
    }
    
    fun toggleFilter(id: String) {
        val list = activeFilters
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            list[index] = list[index].toggleEnabled()
        }
    }
    
    fun updateParameter(filterId: String, paramName: String, newValue: Float) {
        val list = activeFilters
        val index = list.indexOfFirst { it.id == filterId }
        if (index != -1) {
            list[index] = list[index].copyWithParameter(paramName, newValue)
        }
    }
    
    fun clearAll() {
        activeFilters.clear()
    }
}

object ZenithFilterFactory {
    fun createFilterList(): List<ZenithFilter> {
        return listOf(
            // Category: Color Adjustments
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
            createColorAdjustFilter("color_sepia", "Sepia Tone", listOf(FilterParameter("Intensity", 0.8f, 0.0f, 1.0f))),
            createColorAdjustFilter("color_saturation", "Saturation", listOf(FilterParameter("Factor", 1.0f, 0.0f, 3.0f))),
            createColorAdjustFilter("color_contrast", "Contrast Correction", listOf(FilterParameter("Amount", 1.0f, 0.0f, 3.0f))),
            createColorAdjustFilter("color_brightness", "Brightness Booster", listOf(FilterParameter("Level", 0.0f, -100.0f, 100.0f))),
            createColorAdjustFilter("color_exposure", "Exposure Level", listOf(FilterParameter("EV Amount", 0.0f, -4.0f, 4.0f))),
            createColorAdjustFilter("color_vignette", "Vignette Focus", listOf(FilterParameter("Radius amount", 0.5f, 0.0f, 1.0f))),
            createColorAdjustFilter("color_hue", "Hue Angle Shift", listOf(FilterParameter("Shift degrees", 0.0f, -180.0f, 180.0f, "°"))),
            createColorAdjustFilter("color_rgb", "RGB Level Mixer", listOf(FilterParameter("Red", 1.0f, 0.0f, 2.0f), FilterParameter("Green", 1.0f, 0.0f, 2.0f), FilterParameter("Blue", 1.0f, 0.0f, 2.0f))),
            createColorAdjustFilter("color_highlights", "Highlight levels", listOf(FilterParameter("Level", 0.0f, -1.0f, 1.0f))),
            createColorAdjustFilter("color_shadows", "Shadow recovery", listOf(FilterParameter("Level", 0.0f, -1.0f, 1.0f))),
            createColorAdjustFilter("color_temp", "Color Temperature", listOf(FilterParameter("Warmth index", 0.0f, -1.0f, 1.0f))),

            // Category 1: ARTISTIC EFFECTS
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
            createArtisticFilter("artistic_oil_kuwahara", "Oil Painting Kuwahara", listOf(FilterParameter("Kuwahara Radius", 4f, 1f, 10f))),
            createArtisticFilter("artistic_toon", "Comic Book Toon", listOf(FilterParameter("Quantization Levels", 5f, 2f, 10f))),
            createArtisticFilter("artistic_hologram", "Hologram Scanlines", listOf(FilterParameter("Fringe Intensity", 15f, 2f, 50f))),

            // Category 2: BLUR & BLUR GALLERY
            createBlurFilter("blur_average", "Average", emptyList()),
            createBlurFilter("blur_gaussian", "Gaussian Blur", listOf(FilterParameter("Radius", 15f, 1f, 100f))),
            createBlurFilter("blur_box", "Box Blur", listOf(FilterParameter("Radius", 12f, 1f, 100f))),
            createBlurFilter("blur_lens", "Lens Blur", listOf(FilterParameter("Bokeh Radius", 18f, 1f, 80f))),
            createBlurFilter("blur_motion", "Motion Blur", listOf(FilterParameter("Distance", 20f, 1f, 100f), FilterParameter("Angle", 45f, 0f, 360f))),
            createBlurFilter("blur_radial", "Radial Blur", listOf(FilterParameter("Factor", 15f, 1f, 50f))),
            createBlurFilter("blur_shape", "Shape Blur", listOf(FilterParameter("Scale", 12f, 1f, 40f))),
            createBlurFilter("blur_smart", "Smart Blur", listOf(FilterParameter("Threshold", 25f, 1f, 100f), FilterParameter("Radius", 8f, 1f, 30f))),
            createBlurFilter("blur_surface", "Surface Blur", listOf(FilterParameter("Threshold", 20f, 1f, 100f), FilterParameter("Radius", 6f, 1f, 25f))),
            createBlurFilter("blur_field", "Field Blur", listOf(FilterParameter("Density", 10f, 1f, 50f))),
            createBlurFilter("blur_iris", "Iris Blur", listOf(FilterParameter("Radius", 30f, 10f, 120f))),
            createBlurFilter("blur_tiltshift", "Tilt-Shift", listOf(FilterParameter("Width", 50f, 10f, 200f))),
            createBlurFilter("blur_path", "Path Blur", listOf(FilterParameter("Flow", 15f, 1f, 50f))),
            createBlurFilter("blur_spin", "Spin Blur", listOf(FilterParameter("Speed", 20f, 1f, 90f))),
            createBlurFilter("blur_stack", "Fast Stack Blur", listOf(FilterParameter("Blur Radius", 15f, 1f, 80f))),
            createBlurFilter("blur_zoom", "Radial Zoom Blur", listOf(FilterParameter("Power Strength", 10f, 0f, 50f))),
            createBlurFilter("blur_bilateral", "Bilateral filter", listOf(FilterParameter("Spatial Delta", 10f, 1f, 40f), FilterParameter("Color Delta", 25f, 5f, 100f))),
            createBlurFilter("blur_bokeh", "Circle Highlights Bokeh", listOf(FilterParameter("Bokeh Radius", 12f, 1f, 60f), FilterParameter("Brightness Threshold", 180f, 100f, 255f))),

            // Category 3: BRUSH STROKES
            createBrushFilter("brush_accented", "Accented Edges", listOf(FilterParameter("Edge Width", 2f, 1f, 10f))),
            createBrushFilter("brush_angled", "Angled Strokes", listOf(FilterParameter("Angle", 45f, 0f, 180f))),
            createBrushFilter("brush_crosshatch", "Crosshatch", listOf(FilterParameter("Density", 8f, 2f, 20f))),
            createBrushFilter("brush_dark", "Dark Strokes", listOf(FilterParameter("Density", 6f, 1f, 15f))),
            createBrushFilter("brush_ink", "Ink Outlines", listOf(FilterParameter("Ink Weight", 3f, 1f, 10f))),
            createBrushFilter("brush_spatter", "Spatter", listOf(FilterParameter("Spray Radius", 10f, 2f, 40f))),
            createBrushFilter("brush_sprayed", "Sprayed Strokes", listOf(FilterParameter("Scattering", 12f, 2f, 30f))),
            createBrushFilter("brush_sumie", "Sumi-e", listOf(FilterParameter("Saturation", 5f, 1f, 20f))),

            // Category 4: DISTORT
            createDistortFilter("distort_displace", "Displace", listOf(FilterParameter("Offset", 10f, 1f, 50f))),
            createDistortFilter("distort_glass", "Glass", listOf(FilterParameter("Distortion", 12f, 1f, 40f))),
            createDistortFilter("distort_ocean", "Ocean Ripple", listOf(FilterParameter("Wave Frequency", 15f, 2f, 50f))),
            createDistortFilter("distort_pinch", "Pinch", listOf(FilterParameter("Amount", 0.5f, -1.0f, 1.0f))),
            createDistortFilter("distort_polar", "Polar Coordinates", listOf(FilterParameter("Intensity", 1f, 0f, 1f))),
            createDistortFilter("distort_ripple", "Ripple", listOf(FilterParameter("Amplitude", 12f, 1f, 50f), FilterParameter("Wavelength", 30f, 5f, 100f))),
            createDistortFilter("distort_shear", "Shear", listOf(FilterParameter("Skew Angle", 20f, -60f, 60f))),
            createDistortFilter("distort_spherize", "Spherize", listOf(FilterParameter("Curvature", 0.6f, 0.1f, 1.5f))),
            createDistortFilter("distort_twirl", "Twirl", listOf(FilterParameter("Angle Degrees", 90f, -360f, 360f))),
            createDistortFilter("distort_wave", "Wave", listOf(FilterParameter("Amplitude", 15f, 1f, 50f), FilterParameter("Wavelength", 40f, 10f, 150f))),
            createDistortFilter("distort_zigzag", "ZigZag", listOf(FilterParameter("Frequency", 10f, 2f, 40f))),
            createDistortFilter("distort_swirl", "Swirl Distortion", listOf(FilterParameter("Degrees", 120f, -360f, 360f), FilterParameter("Range", 0.5f, 0.1f, 1.5f))),
            createDistortFilter("distort_bulge", "Bulge Warp", listOf(FilterParameter("Scale", 0.6f, -1.0f, 2.0f))),
            createDistortFilter("distort_kaleidoscope", "Kaleidoscope Matrix", listOf(FilterParameter("SlicesCount", 6f, 3f, 24f))),
            createDistortFilter("distort_glass_refract", "Refractive Waves", listOf(FilterParameter("Scale", 15f, 2f, 60f))),

            // Category 5: PIXELATE
            createPixelateFilter("pixelate_halftone", "Color Halftone", listOf(FilterParameter("Dot Radius", 6f, 2f, 20f))),
            createPixelateFilter("pixelate_crystallize", "Crystallize", listOf(FilterParameter("Cell Size", 12f, 4f, 40f))),
            createPixelateFilter("pixelate_facet", "Facet", listOf(FilterParameter("Clustering", 8f, 2f, 30f))),
            createPixelateFilter("pixelate_fragment", "Fragment", listOf(FilterParameter("Interleave", 6f, 2f, 20f))),
            createPixelateFilter("pixelate_mezzotint", "Mezzotint", listOf(FilterParameter("Grain Size", 4f, 1f, 15f))),
            createPixelateFilter("pixelate_mosaic", "Mosaic", listOf(FilterParameter("Block Size", 16f, 2f, 100f))),
            createPixelateFilter("pixelate_pointillize", "Pointillize", listOf(FilterParameter("Dot Size", 8f, 2f, 30f))),

            // Category 6: NOISE & RENDER
            createNoiseFilter("noise_add", "Add Noise", listOf(FilterParameter("Intensity", 25f, 0f, 100f))),
            createNoiseFilter("noise_despeckle", "Despeckle", listOf(FilterParameter("Threshold", 15f, 1f, 50f))),
            createNoiseFilter("noise_dust", "Dust & Scratches", listOf(FilterParameter("Radius", 3f, 1f, 15f))),
            createNoiseFilter("noise_median", "Median", listOf(FilterParameter("Radius", 2f, 1f, 8f))),
            createNoiseFilter("noise_reduce", "Reduce Noise", listOf(FilterParameter("Passes", 3f, 1f, 8f))),
            createNoiseFilter("noise_clouds", "Clouds / Difference Clouds", listOf(FilterParameter("Octaves", 4f, 1f, 8f))),
            createNoiseFilter("noise_fibers", "Fibers", listOf(FilterParameter("Stretching", 15f, 2f, 40f))),
            createNoiseFilter("noise_lens", "Lens Flare", listOf(FilterParameter("Brightness", 60f, 10f, 150f))),
            createNoiseFilter("noise_lighting", "Lighting Effects", listOf(FilterParameter("Gloss Intensity", 12f, 1f, 40f))),

            // Category 7: SKETCH & TEXTURE
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

            // Category 8: STYLIZE
            createStylizeFilter("stylize_diffuse", "Diffuse", listOf(FilterParameter("Shuffle Range", 3f, 1f, 15f))),
            createStylizeFilter("stylize_emboss", "Emboss", listOf(FilterParameter("Height", 3f, 1f, 15f))),
            createStylizeFilter("stylize_extrude", "Extrude", listOf(FilterParameter("Pyramid Size", 10f, 2f, 40f))),
            createStylizeFilter("stylize_findedges", "Find Edges", emptyList()),
            createStylizeFilter("stylize_glowingedges", "Glowing Edges", listOf(FilterParameter("Edge Width", 4f, 1f, 15f))),
            createStylizeFilter("stylize_solarize", "Solarize", listOf(FilterParameter("Threshold", 128f, 10f, 240f))),
            createStylizeFilter("stylize_tiles", "Tiles", listOf(FilterParameter("Tile Size", 15f, 4f, 50f))),
            createStylizeFilter("stylize_trace", "Trace Contour", listOf(FilterParameter("LevelThreshold", 120f, 10f, 240f))),
            createStylizeFilter("stylize_wind", "Wind", listOf(FilterParameter("Wind Distance", 18f, 2f, 60f))),
            createStylizeFilter("stylize_oilpaint", "Oil Paint", listOf(FilterParameter("Brush Curvature", 12f, 2f, 40f))),

            // Category 9: 3D MODULE
            create3DModuleFilter(),

            // Category 10: LIGHT EFFECTS
            createIbisPaintFilter("ibis_chromatic_aberration", "Chromatic Aberration", listOf(
                FilterParameter("Distance", 16f, 0f, 150f, "px"),
                FilterParameter("Angle", 136f, 0f, 360f, "°")
            )),
            createIbisPaintFilter("ibis_glitch", "Glitch Distortion", listOf(
                FilterParameter("Height", 119f, 10f, 500f, "px"),
                FilterParameter("Strength", 23f, 0f, 150f, "px"),
                FilterParameter("Color Shift", 8f, 0f, 100f, "px")
            )),
            createIbisPaintFilter("ibis_bloom", "Bloom Glow", listOf(
                FilterParameter("Area", 100f, 0f, 100f, "%"),
                FilterParameter("Radius", 45f, 1f, 150f, "px"),
                FilterParameter("Brightness", 100f, 0f, 300f, "%"),
                FilterParameter("Balanced Blend", 25f, 0f, 100f, "%")
            )),
            createIbisPaintFilter("ibis_cross_filter", "Cross Filter", listOf(
                FilterParameter("Count", 4f, 2f, 8f),
                FilterParameter("Direction", 45f, 0f, 360f, "°"),
                FilterParameter("Area", 10f, 0f, 100f, "%"),
                FilterParameter("Brightness", 50f, 0f, 300f, "%")
            )),
            createIbisPaintFilter("ibis_inner_glow", "Inner Glow Edge", listOf(
                FilterParameter("Radius", 104f, 5f, 300f, "px"),
                FilterParameter("Red", 1.0f, 0f, 1f),
                FilterParameter("Green", 1.0f, 0f, 1f),
                FilterParameter("Blue", 1.0f, 0f, 1f)
            )),
            createIbisPaintFilter("ibis_bevel", "Bevel (Inner/Outer)", listOf(
                FilterParameter("Height", 20f, 1f, 100f, "px"),
                FilterParameter("Smoothness", 45f, 0f, 100f, "px"),
                FilterParameter("Highlight Size", 14f, 0f, 100f, "%")
            )),
            createIbisPaintFilter("ibis_emboss", "Emboss Pro", listOf(
                FilterParameter("Gray Scale", 0f, 0f, 1f),
                FilterParameter("Height", 1f, 1f, 10f, "px"),
                FilterParameter("Amount", 500f, 10f, 1000f, "%")
            )),
            createIbisPaintFilter("ibis_waterdrop", "Waterdrop (Rounded)", listOf(
                FilterParameter("Distance", 100f, 10f, 200f, "%"),
                FilterParameter("Flatness", 10f, 0f, 100f, "%"),
                FilterParameter("Height", 3f, 1f, 15f, "px")
            )),
            createIbisPaintFilter("ibis_satin", "Satin Contour", listOf(
                FilterParameter("Distance", 11f, 1f, 100f, "px"),
                FilterParameter("Opacity", 0.5f, 0f, 1f),
                FilterParameter("Red", 0f, 0f, 1f),
                FilterParameter("Green", 0f, 0f, 1f),
                FilterParameter("Blue", 1f, 0f, 1f)
            ))
        )
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
                "color_saturation" -> {
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
                "color_contrast" -> {
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
                "color_brightness" -> {
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
                "color_exposure" -> {
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
                "color_vignette" -> {
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
                "color_hue" -> {
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
                "artistic_oil_kuwahara" -> {
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
        return CustomZenithFilter(id, name, "Blur & Blur Gallery", params) { source, parameters ->
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
                "blur_gaussian", "blur_box" -> {
                    val r = parameters.firstOrNull()?.currentValue?.toInt() ?: 12
                    applyBoxBlur(source, r)
                }
                "blur_motion" -> {
                    val distance = (parameters.firstOrNull()?.currentValue ?: 20f).toInt().coerceIn(1, 100)
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        // Sample along directional angle
                        val stepX = x - distance / 2f
                        val stepY = y - distance / 2f
                        Pair(stepX, stepY)
                    }
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
                    val radius = (parameters.find { it.name == "Blur Radius" }?.currentValue ?: 15f).toInt()
                    applyBoxBlur(source, radius)
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
        return CustomZenithFilter(id, name, "Distort", params) { source, parameters ->
            when (id) {
                "distort_ripple" -> {
                    val amp = parameters.find { it.name == "Amplitude" }?.currentValue ?: 12f
                    val wave = parameters.find { it.name == "Wavelength" }?.currentValue ?: 30f
                    applyCoordinateDistortion(source) { x, y, _, _ ->
                        Pair(x + sin(y / wave) * amp, y)
                    }
                }
                "distort_twirl" -> {
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
                "distort_pinch" -> {
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
                "distort_spherize" -> {
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
                "distort_wave" -> {
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
                "pixelate_mosaic" -> {
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
                "stylize_findedges" -> {
                    applySobel(source, 90f, Color.BLUE, Color.WHITE, drawEdgesOnly = true)
                }
                "stylize_emboss" -> {
                    val height = parameters.firstOrNull()?.currentValue ?: 3f
                    val edge = applySobel(source, 80f, Color.GRAY, colorQuantize(Color.GRAY, 2), drawEdgesOnly = true)
                    applyCoordinateDistortion(edge) { x, y, _, _ ->
                        Pair(x + height, y + height)
                    }
                }
                "stylize_solarize" -> {
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

    private fun create3DModuleFilter(): ZenithFilter {
        return GPUImageZenithFilter(
            id = "3d_raster_extrude",
            name = "3D Raster Extrude",
            category = "3D Module",
            parameters = listOf(
                FilterParameter("Extrusion Depth", 120f, 0f, 300f, "px"),
                FilterParameter("Rotation X", 45f, 0f, 360f, "°"),
                FilterParameter("Rotation Y", 30f, 0f, 360f, "°"),
                FilterParameter("Rotation Z", 0f, 0f, 360f, "°"),
                FilterParameter("Bevel Radius", 8f, 0f, 50f, "px"),
                FilterParameter("Bevel Segments", 4f, 1f, 10f),
                FilterParameter("Specular Intensity", 0.8f, 0f, 1f),
                FilterParameter("Roughness", 0.2f, 0.01f, 1f),
                FilterParameter("Ambient Occlusion", 0.5f, 0f, 1f),
                FilterParameter("Metallic", 0.0f, 0f, 1f),
                FilterParameter("Light Type", 1f, 0f, 2f), // 0=FLAT, 1=DIRECTIONAL, 2=POINT
                FilterParameter("Light Azimuth", 135f, 0f, 360f, "°"),
                FilterParameter("Light Elevation", 45f, -90f, 90f, "°"),
                FilterParameter("Light Intensity", 1.2f, 0f, 5f),
                FilterParameter("Light Color R", 255f, 0f, 255f),
                FilterParameter("Light Color G", 255f, 0f, 255f),
                FilterParameter("Light Color B", 255f, 0f, 255f),
                FilterParameter("UV Scale X", 1.0f, 0.1f, 10f),
                FilterParameter("UV Scale Y", 1.0f, 0.1f, 10f),
                FilterParameter("UV Offset X", 0.0f, -5f, 5f),
                FilterParameter("UV Offset Y", 0.0f, -5f, 5f),
                FilterParameter("Use Texture", 0.0f, 0f, 1f)
            )
        ) { source, context, parameters ->
            val depth = parameters.find { it.name == "Extrusion Depth" }?.currentValue ?: 120f
            val rx = parameters.find { it.name == "Rotation X" }?.currentValue ?: 45f
            val ry = parameters.find { it.name == "Rotation Y" }?.currentValue ?: 30f
            val rz = parameters.find { it.name == "Rotation Z" }?.currentValue ?: 0f
            val bevelRadius = parameters.find { it.name == "Bevel Radius" }?.currentValue ?: 8f
            val bevelSegments = parameters.find { it.name == "Bevel Segments" }?.currentValue ?: 4f
            val specularIntensity = parameters.find { it.name == "Specular Intensity" }?.currentValue ?: 0.8f
            val roughness = parameters.find { it.name == "Roughness" }?.currentValue ?: 0.2f
            val ao = parameters.find { it.name == "Ambient Occlusion" }?.currentValue ?: 0.5f
            val metallic = parameters.find { it.name == "Metallic" }?.currentValue ?: 0.0f
            val lightType = parameters.find { it.name == "Light Type" }?.currentValue ?: 1f
            val azimuth = parameters.find { it.name == "Light Azimuth" }?.currentValue ?: 135f
            val elevation = parameters.find { it.name == "Light Elevation" }?.currentValue ?: 45f
            val intensity = parameters.find { it.name == "Light Intensity" }?.currentValue ?: 1.2f
            val lightR = parameters.find { it.name == "Light Color R" }?.currentValue ?: 255f
            val lightG = parameters.find { it.name == "Light Color G" }?.currentValue ?: 255f
            val lightB = parameters.find { it.name == "Light Color B" }?.currentValue ?: 255f
            val scaleX = parameters.find { it.name == "UV Scale X" }?.currentValue ?: 1.0f
            val scaleY = parameters.find { it.name == "UV Scale Y" }?.currentValue ?: 1.0f
            val offsetX = parameters.find { it.name == "UV Offset X" }?.currentValue ?: 0.0f
            val offsetY = parameters.find { it.name == "UV Offset Y" }?.currentValue ?: 0.0f
            val useTex = parameters.find { it.name == "Use Texture" }?.currentValue ?: 0.0f
            
            val normalizer = source.width.toFloat().coerceAtLeast(100f)
            
            // Try loading material texture
            var textureBmp: Bitmap? = null
            if (useTex > 0.5f) {
                val layerId = EffectStackManager.currentLayerId
                val texPath = if (layerId != null) EffectStackManager.texturesByLayer[layerId] else null
                if (texPath != null) {
                    try {
                        textureBmp = BitmapFactory.decodeFile(texPath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            applySingleGPUImageFilter(
                context,
                source,
                com.example.studio.ui.GPUImageRasterExtrudeFilter(
                    rotX = rx,
                    rotY = ry,
                    rotZ = rz,
                    depth = depth / normalizer,
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
                    lightColorR = lightR,
                    lightColorG = lightG,
                    lightColorB = lightB,
                    useTexture = useTex,
                    uvScaleX = scaleX,
                    uvScaleY = scaleY,
                    uvOffsetX = offsetX,
                    uvOffsetY = offsetY,
                    textureBitmap = textureBmp
                )
            )
        }
    }

    private fun createIbisPaintFilter(
        id: String,
        name: String,
        params: List<FilterParameter>
    ): ZenithFilter {
        return GPUImageZenithFilter(id, name, "Light Effects", params) { source, context, parameters ->
            when (id) {
                "ibis_chromatic_aberration" -> {
                    val distance = parameters.find { it.name == "Distance" }?.currentValue ?: 16f
                    val angle = parameters.find { it.name == "Angle" }?.currentValue ?: 136f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageChromaticAberrationFilter(distance, angle))
                }
                "ibis_glitch" -> {
                    val height = parameters.find { it.name == "Height" }?.currentValue ?: 119f
                    val strength = parameters.find { it.name == "Strength" }?.currentValue ?: 23f
                    val colorShift = parameters.find { it.name == "Color Shift" }?.currentValue ?: 8f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageGlitchFilter(height, strength, colorShift))
                }
                "ibis_bloom" -> {
                    val area = parameters.find { it.name == "Area" }?.currentValue ?: 100f
                    val radius = parameters.find { it.name == "Radius" }?.currentValue ?: 45f
                    val brightness = parameters.find { it.name == "Brightness" }?.currentValue ?: 100f
                    val balanced = parameters.find { it.name == "Balanced Blend" }?.currentValue ?: 25f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageBloomFilter(area, radius, brightness, balanced))
                }
                "ibis_cross_filter" -> {
                    val count = parameters.find { it.name == "Count" }?.currentValue ?: 4f
                    val direction = parameters.find { it.name == "Direction" }?.currentValue ?: 45f
                    val area = parameters.find { it.name == "Area" }?.currentValue ?: 10f
                    val brightness = parameters.find { it.name == "Brightness" }?.currentValue ?: 50f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageCrossFilterFilter(count, direction, area, brightness))
                }
                "ibis_inner_glow" -> {
                    val radius = parameters.find { it.name == "Radius" }?.currentValue ?: 104f
                    val r = parameters.find { it.name == "Red" }?.currentValue ?: 1f
                    val g = parameters.find { it.name == "Green" }?.currentValue ?: 1f
                    val b = parameters.find { it.name == "Blue" }?.currentValue ?: 1f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageInnerGlowFilter(radius, r, g, b))
                }
                "ibis_bevel" -> {
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
                "ibis_waterdrop" -> {
                    val dist = parameters.find { it.name == "Distance" }?.currentValue ?: 100f
                    val flat = parameters.find { it.name == "Flatness" }?.currentValue ?: 10f
                    val h = parameters.find { it.name == "Height" }?.currentValue ?: 3f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageWaterdropFilter(dist, flat, h))
                }
                "ibis_satin" -> {
                    val dist = parameters.find { it.name == "Distance" }?.currentValue ?: 11f
                    val op = parameters.find { it.name == "Opacity" }?.currentValue ?: 0.5f
                    val r = parameters.find { it.name == "Red" }?.currentValue ?: 0f
                    val g = parameters.find { it.name == "Green" }?.currentValue ?: 0f
                    val b = parameters.find { it.name == "Blue" }?.currentValue ?: 1f
                    applySingleGPUImageFilter(context, source, com.example.studio.ui.GPUImageSatinFilter(dist, op, r, g, b))
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
}
