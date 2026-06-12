package com.example.studio.ui

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.studio.model.StudioEffect
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

// Global mutable trigger to cause Jetpack Compose draw invalidations instantly without full recomposition overhead
val rawFilterRedrawTrigger = androidx.compose.runtime.mutableStateOf(0)

// Vertex Shader for Camera RAW Filtering
const val LIGHTROOM_VERTEX_SHADER = """
attribute vec4 position;
attribute vec4 inputTextureCoordinate;

varying vec2 textureCoordinate;

void main() {
    gl_Position = position;
    textureCoordinate = inputTextureCoordinate.xy;
}
"""

// Fragment Shader with sRGB <-> Linear Conversions, 3x3 Transformations, Contrast around 18% grey, and WB shifts
const val LIGHTROOM_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;

uniform highp float uExposure;
uniform highp float uContrast;
uniform highp float uHighlights;
uniform highp float uShadows;
uniform highp float uWhites;
uniform highp float uBlacks;
uniform highp float uTemp;
uniform highp float uTint;
uniform highp float uVibrance;
uniform highp float uSaturation;
uniform highp float uClarity;
uniform highp float uDehaze;
uniform highp float uProfile; // Camera Profile 3x3 Matrix Key

// sRGB to Linear Space conversion to operate perfectly on radiometric values
highp vec3 srgbToLinear(highp vec3 srgb) {
    highp vec3 linear;
    for (int i = 0; i < 3; i++) {
        if (srgb[i] <= 0.04045) {
            linear[i] = srgb[i] / 12.92;
        } else {
            linear[i] = pow((srgb[i] + 0.055) / 1.055, 2.4);
        }
    }
    return linear;
}

// Linear Space back to sRGB Space mapping
highp vec3 linearToSrgb(highp vec3 linear) {
    highp vec3 srgb;
    for (int i = 0; i < 3; i++) {
        if (linear[i] <= 0.0031308) {
            srgb[i] = linear[i] * 12.92;
        } else {
            srgb[i] = 1.055 * pow(linear[i], 1.0 / 2.4) - 0.055;
        }
    }
    return srgb;
}

void main() {
    highp vec4 rawColor = texture2D(inputImageTexture, textureCoordinate);
    highp vec3 linearColor = srgbToLinear(clamp(rawColor.rgb, 0.0, 1.0));
    
    // 1. Color Profile 3x3 Transformations (Directly on GPU Plane)
    highp mat3 profileMatrix;
    if (uProfile < 0.5) {
        // Standard (Adobe Color Profile Reference)
        profileMatrix = mat3(
            1.0624, -0.0123, -0.0401,
            -0.1012,  1.1215, -0.0103,
            -0.0152, -0.0901,  1.1053
        );
    } else if (uProfile < 1.5) {
        // Portrait (Skin tone warm enhancements)
        profileMatrix = mat3(
            1.0921, -0.0423, -0.0398,
            -0.0521,  1.0824, -0.0303,
            -0.0111, -0.0712,  1.0823
        );
    } else if (uProfile < 2.5) {
        // Landscape (Vibrant greens and foliage)
        profileMatrix = mat3(
            0.9823,  0.0521, -0.0244,
            -0.0821,  1.1824, -0.0903,
            -0.0211, -0.1512,  1.2623
        );
    } else if (uProfile < 3.5) {
        // Vivid (Punchy highlights & color expansion)
        profileMatrix = mat3(
            1.1524, -0.0823, -0.0501,
            -0.1212,  1.2215, -0.0803,
            -0.0352, -0.1201,  1.2453
        );
    } else {
        // Monochrome (RAW classic black-and-white tonal scale)
        profileMatrix = mat3(
            0.2126,  0.7152,  0.0722,
            0.2126,  0.7152,  0.0722,
            0.2126,  0.7152,  0.0722
        );
    }
    linearColor = clamp(profileMatrix * linearColor, 0.0, 1.0);
    
    // 2. White Balance (Temp & Tint Shifts in Linear Color Space)
    // Map -100 to +100 range to safe multiplicative scales
    highp float tMultiplier = uTemp * 0.0025;
    highp float tintMultiplier = uTint * 0.0022;
    linearColor.r *= (1.0 + tMultiplier);
    linearColor.g *= (1.0 + tintMultiplier);
    linearColor.b *= (1.0 - tMultiplier - tintMultiplier);
    linearColor = clamp(linearColor, 0.0, 1.0);

    // 3. Exposure Adjustment (Linear Multiplication)
    linearColor *= pow(2.0, uExposure);

    // Radiometric Luminance check
    highp float rawLuma = dot(linearColor, vec3(0.2126, 0.7152, 0.0722));

    // 4. Highlight & Shadow Recovery in Linear Space
    highp float highlightWeight = smoothstep(0.42, 0.98, rawLuma);
    highp float shadowWeight = 1.0 - smoothstep(0.02, 0.58, rawLuma);

    if (uHighlights != 0.0) {
        highp float hFactor = uHighlights * 0.0055;
        if (hFactor < 0.0) {
            linearColor = mix(linearColor, linearColor / (1.0 - hFactor), highlightWeight);
        } else {
            linearColor = mix(linearColor, linearColor * (1.0 + hFactor), highlightWeight);
        }
    }

    if (uShadows != 0.0) {
        highp float sFactor = uShadows * 0.0065;
        if (sFactor > 0.0) {
            linearColor = mix(linearColor, linearColor + (vec3(1.0) - linearColor) * sFactor * 0.25, shadowWeight);
        } else {
            linearColor = mix(linearColor, linearColor * (1.0 + sFactor), shadowWeight);
        }
    }

    // 5. Whites & Blacks Adjustments (Whites compress near clipping, Blacks anchor shadows)
    highp float whiteWeight = smoothstep(0.65, 1.0, rawLuma);
    highp float blackWeight = 1.0 - smoothstep(0.0, 0.32, rawLuma);

    if (uWhites != 0.0) {
        linearColor += linearColor * (uWhites * 0.0062) * whiteWeight;
    }
    if (uBlacks != 0.0) {
        linearColor += linearColor * (uBlacks * 0.0068) * blackWeight;
    }

    // 6. Contrast adjustment centered around standard 18% Middle Grey
    if (uContrast != 0.0) {
        highp float cFactor = (uContrast + 100.0) / 100.0;
        linearColor = (linearColor - 0.18) * cFactor + 0.18;
    }

    // 7. Clarity (Localized midtone contrast adjustment)
    highp float midtoneWeight = 1.0 - smoothstep(0.0, 0.5, abs(rawLuma - 0.18));
    if (uClarity != 0.0) {
        highp float clarityFactor = uClarity * 0.007;
        linearColor += (linearColor - 0.18) * clarityFactor * midtoneWeight;
    }

    // 8. Dehaze
    if (uDehaze != 0.0) {
        highp float dehazeFactor = uDehaze * 0.0045;
        linearColor = (linearColor - 0.04) * (1.0 + dehazeFactor) + 0.04;
    }

    // Conversion back to standard non-linear display coordinate system
    highp vec3 srgbColor = linearToSrgb(clamp(linearColor, 0.0, 1.0));

    // Display Luma for display saturation shifts
    highp float srgbLuma = dot(srgbColor, vec3(0.299, 0.587, 0.114));

    // 9. Saturation Adjustment (sRGB display space)
    if (uSaturation != 0.0) {
        highp float satFactor = (uSaturation + 100.0) / 100.0;
        if (satFactor < 1.0) {
            satFactor = mix(0.0, 1.0, satFactor);
        } else {
            satFactor = mix(1.0, 2.3, satFactor - 1.0);
        }
        srgbColor = mix(vec3(srgbLuma), srgbColor, satFactor);
    }

    // 10. Vibrance (Protects saturated pixels while enriching low-saturation tones)
    if (uVibrance != 0.0) {
        highp float maxSample = max(srgbColor.r, max(srgbColor.g, srgbColor.b));
        highp float minSample = min(srgbColor.r, min(srgbColor.g, srgbColor.b));
        highp float currentSat = maxSample - minSample;
        highp float vibFactor = uVibrance * 0.015 * (1.0 - currentSat);
        srgbColor = mix(srgbColor, srgbColor * (1.0 + vibFactor), clamp(vibFactor, -1.0, 1.0));
    }

    gl_FragColor = vec4(clamp(srgbColor, 0.0, 1.0), rawColor.a);
}
"""

// Custom Camera RAW Matrix Filter representing hardware accelerated adjustment
class LightroomFilter(
    var exposure: Float = 0f,
    var contrast: Float = 0f,
    var highlights: Float = 0f,
    var shadows: Float = 0f,
    var whites: Float = 0f,
    var blacks: Float = 0f,
    var temp: Float = 0f,
    var tint: Float = 0f,
    var vibrance: Float = 0f,
    var saturation: Float = 0f,
    var clarity: Float = 0f,
    var dehaze: Float = 0f,
    var profile: Float = 0f // 0f = Standard, 1f = Portrait, 2f = Landscape, 3f = Vivid, 4f = Monochrome
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(
    LIGHTROOM_VERTEX_SHADER,
    LIGHTROOM_FRAGMENT_SHADER
) {
    private var uExposureLocation: Int = -1
    private var uContrastLocation: Int = -1
    private var uHighlightsLocation: Int = -1
    private var uShadowsLocation: Int = -1
    private var uWhitesLocation: Int = -1
    private var uBlacksLocation: Int = -1
    private var uTempLocation: Int = -1
    private var uTintLocation: Int = -1
    private var uVibranceLocation: Int = -1
    private var uSaturationLocation: Int = -1
    private var uClarityLocation: Int = -1
    private var uDehazeLocation: Int = -1
    private var uProfileLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uExposureLocation = GLES20.glGetUniformLocation(program, "uExposure")
        uContrastLocation = GLES20.glGetUniformLocation(program, "uContrast")
        uHighlightsLocation = GLES20.glGetUniformLocation(program, "uHighlights")
        uShadowsLocation = GLES20.glGetUniformLocation(program, "uShadows")
        uWhitesLocation = GLES20.glGetUniformLocation(program, "uWhites")
        uBlacksLocation = GLES20.glGetUniformLocation(program, "uBlacks")
        uTempLocation = GLES20.glGetUniformLocation(program, "uTemp")
        uTintLocation = GLES20.glGetUniformLocation(program, "uTint")
        uVibranceLocation = GLES20.glGetUniformLocation(program, "uVibrance")
        uSaturationLocation = GLES20.glGetUniformLocation(program, "uSaturation")
        uClarityLocation = GLES20.glGetUniformLocation(program, "uClarity")
        uDehazeLocation = GLES20.glGetUniformLocation(program, "uDehaze")
        uProfileLocation = GLES20.glGetUniformLocation(program, "uProfile")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun updateParams(
        exposure: Float, contrast: Float, highlights: Float, shadows: Float,
        whites: Float, blacks: Float, temp: Float, tint: Float,
        vibrance: Float, saturation: Float, clarity: Float, dehaze: Float,
        profile: Float = 0f
    ) {
        this.exposure = exposure
        this.contrast = contrast
        this.highlights = highlights
        this.shadows = shadows
        this.whites = whites
        this.blacks = blacks
        this.temp = temp
        this.tint = tint
        this.vibrance = vibrance
        this.saturation = saturation
        this.clarity = clarity
        this.dehaze = dehaze
        this.profile = profile
        applyParameters()
    }

    private fun applyParameters() {
        if (uExposureLocation != -1) setFloat(uExposureLocation, exposure)
        if (uContrastLocation != -1) setFloat(uContrastLocation, contrast)
        if (uHighlightsLocation != -1) setFloat(uHighlightsLocation, highlights)
        if (uShadowsLocation != -1) setFloat(uShadowsLocation, shadows)
        if (uWhitesLocation != -1) setFloat(uWhitesLocation, whites)
        if (uBlacksLocation != -1) setFloat(uBlacksLocation, blacks)
        if (uTempLocation != -1) setFloat(uTempLocation, temp)
        if (uTintLocation != -1) setFloat(uTintLocation, tint)
        if (uVibranceLocation != -1) setFloat(uVibranceLocation, vibrance)
        if (uSaturationLocation != -1) setFloat(uSaturationLocation, saturation)
        if (uClarityLocation != -1) setFloat(uClarityLocation, clarity)
        if (uDehazeLocation != -1) setFloat(uDehazeLocation, dehaze)
        if (uProfileLocation != -1) setFloat(uProfileLocation, profile)
    }
}

// CameraRAWFilterEngine manages zero-lag background GLES/GPUImage rendering jobs
object CameraRAWFilterEngine {
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Memory-pooled bitmap list to reuse space and avoid allocations
    private val bitmapPool = ConcurrentHashMap<String, Bitmap>()

    /**
     * Executes the filter pipeline off-main-thread with automatic cancellation
     * of redundant/stale slider inputs for responsive live rendering.
     */
    fun triggerBackgroundFilterRender(
        processedKey: String,
        layerId: String,
        loadedBitmap: ImageBitmap,
        activeEffects: List<StudioEffect>,
        context: Context?
    ) {
        // If the processed version already exists, skip
        if (processedImageBitmapCache.containsKey(processedKey)) {
            return
        }

        // Cancel previous pending jobs for this layer to save GPU resources
        val previousJobKeyPrefix = "render_${layerId}_"
        activeJobs.keys.forEach { key ->
            if (key.startsWith(previousJobKeyPrefix) && key != processedKey) {
                activeJobs.remove(key)?.cancel()
            }
        }

        // Keep track of this job
        val jobKey = "render_${layerId}_${processedKey}"
        val renderJob = engineScope.launch {
            try {
                if (context == null) return@launch

                // Read bitmap safely under Dispatchers.IO
                val androidBmp = withContext(Dispatchers.IO) {
                    loadedBitmap.asAndroidBitmap()
                }

                // Check cancel before proceeding to GLES allocation
                ensureActive()

                // Process GLES pixel shaders synchronously inside threads isolated Dispatchers.IO
                val filteredBmp = withContext(Dispatchers.IO) {
                    applyGPUImageFilters(context, androidBmp, activeEffects)
                }

                // Check cancel before updating cache
                ensureActive()

                val imageBmp = filteredBmp.asImageBitmap()
                processedImageBitmapCache[processedKey] = imageBmp

                // Notify main thread to trigger Compose redraw
                withContext(Dispatchers.Main) {
                    rawFilterRedrawTrigger.value++
                }
            } catch (c: CancellationException) {
                // Job was cancelled because parameters changed (slider dragged). Completely normal and expected.
            } catch (t: Throwable) {
                Log.e("CameraRAWFilterEngine", "Error processing background RAW filters", t)
            } finally {
                activeJobs.remove(jobKey)
            }
        }

        activeJobs[jobKey] = renderJob
    }
}
