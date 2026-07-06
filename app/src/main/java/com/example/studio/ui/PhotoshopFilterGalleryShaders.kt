package com.example.studio.ui

import android.opengl.GLES20
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

// -------------------------------------------------------------
// General Vertex Shader (Common structural passthrough)
// -------------------------------------------------------------
const val PSG_VERTEX_SHADER = """
attribute vec4 position;
attribute vec4 inputTextureCoordinate;
varying vec2 textureCoordinate;
void main() {
    gl_Position = position;
    textureCoordinate = inputTextureCoordinate.xy;
}
"""

// -------------------------------------------------------------
// 1. ARTISTIC FILTER IMPLEMENTATIONS
// -------------------------------------------------------------

// Cutout Filter
class GPUImageCutoutFilter(
    var levels: Float = 5.0f,
    var edgeFidelity: Float = 0.5f
) : GPUImageFilter(PSG_VERTEX_SHADER, CUTOUT_FRAGMENT_SHADER) {
    private var uLevelsLocation: Int = -1
    private var uEdgeFidelityLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uLevelsLocation = GLES20.glGetUniformLocation(program, "uLevels")
        uEdgeFidelityLocation = GLES20.glGetUniformLocation(program, "uEdgeFidelity")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(levels: Float, edgeFidelity: Float) {
        this.levels = levels
        this.edgeFidelity = edgeFidelity
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uLevelsLocation, levels)
        setFloat(uEdgeFidelityLocation, edgeFidelity)
    }
}

const val CUTOUT_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uLevels;
uniform highp float uEdgeFidelity;
void main() {
    highp vec4 color = texture2D(inputImageTexture, textureCoordinate);
    highp vec3 rgb = color.rgb;
    // Quantize color regions nicely
    highp float factor = max(2.0, uLevels);
    rgb = floor(rgb * factor + vec3(0.5)) / factor;
    gl_FragColor = vec4(rgb, color.a);
}
"""

// Poster Edges
class GPUImagePosterEdgesFilter(
    var edgeThickness: Float = 2.0f,
    var edgeIntensity: Float = 0.6f,
    var posterization: Float = 6.0f
) : GPUImageFilter(PSG_VERTEX_SHADER, POSTER_EDGES_FRAGMENT_SHADER) {
    private var uThicknessLocation: Int = -1
    private var uIntensityLocation: Int = -1
    private var uPosterizationLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uThicknessLocation = GLES20.glGetUniformLocation(program, "uEdgeThickness")
        uIntensityLocation = GLES20.glGetUniformLocation(program, "uEdgeIntensity")
        uPosterizationLocation = GLES20.glGetUniformLocation(program, "uPosterization")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(thick: Float, intensity: Float, poster: Float) {
        this.edgeThickness = thick
        this.edgeIntensity = intensity
        this.posterization = poster
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uThicknessLocation, edgeThickness)
        setFloat(uIntensityLocation, edgeIntensity)
        setFloat(uPosterizationLocation, posterization)
    }
}

const val POSTER_EDGES_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uEdgeThickness;
uniform highp float uEdgeIntensity;
uniform highp float uPosterization;
void main() {
    highp vec2 texSize = vec2(512.0, 512.0);
    highp float stepX = uEdgeThickness / texSize.x;
    highp float stepY = uEdgeThickness / texSize.y;
    
    highp vec4 c0 = texture2D(inputImageTexture, textureCoordinate);
    highp vec4 cL = texture2D(inputImageTexture, textureCoordinate + vec2(-stepX, 0.0));
    highp vec4 cR = texture2D(inputImageTexture, textureCoordinate + vec2(stepX, 0.0));
    highp vec4 cT = texture2D(inputImageTexture, textureCoordinate + vec2(0.0, -stepY));
    highp vec4 cB = texture2D(inputImageTexture, textureCoordinate + vec2(0.0, stepY));
    
    highp vec3 edge = abs(cR.rgb - cL.rgb) + abs(cB.rgb - cT.rgb);
    highp float edgeIntensity = (edge.r + edge.g + edge.b) * uEdgeIntensity * 2.5;
    
    highp float levels = max(2.0, uPosterization);
    highp vec3 posterRGB = floor(c0.rgb * levels + vec3(0.5)) / levels;
    highp vec3 finalColor = posterRGB * (1.0 - clamp(edgeIntensity, 0.0, 1.0));
    gl_FragColor = vec4(finalColor, c0.a);
}
"""

// Plastic Wrap
class GPUImagePlasticWrapFilter(
    var highlightStrength: Float = 1.2f,
    var detail: Float = 3.0f,
    var smoothness: Float = 0.5f
) : GPUImageFilter(PSG_VERTEX_SHADER, PLASTIC_WRAP_FRAGMENT_SHADER) {
    private var uHighlightLocation: Int = -1
    private var uDetailLocation: Int = -1
    private var uSmoothnessLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uHighlightLocation = GLES20.glGetUniformLocation(program, "uHighlightStrength")
        uDetailLocation = GLES20.glGetUniformLocation(program, "uDetail")
        uSmoothnessLocation = GLES20.glGetUniformLocation(program, "uSmoothness")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(highlight: Float, detail: Float, smoothness: Float) {
        this.highlightStrength = highlight
        this.detail = detail
        this.smoothness = smoothness
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uHighlightLocation, highlightStrength)
        setFloat(uDetailLocation, detail)
        setFloat(uSmoothnessLocation, smoothness)
    }
}

const val PLASTIC_WRAP_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uHighlightStrength;
uniform highp float uDetail;
uniform highp float uSmoothness;
void main() {
    highp vec2 st = textureCoordinate;
    highp vec4 c = texture2D(inputImageTexture, st);
    
    highp float stepSize = (5.0 - uDetail) / 512.0;
    highp float l = (c.r + c.g + c.b) / 3.0;
    highp float lR = (texture2D(inputImageTexture, st + vec2(stepSize, 0.0)).r + texture2D(inputImageTexture, st + vec2(stepSize, 0.0)).g) / 2.0;
    highp float lB = (texture2D(inputImageTexture, st + vec2(0.0, stepSize)).r + texture2D(inputImageTexture, st + vec2(0.0, stepSize)).g) / 2.0;
    
    highp float dx = lR - l;
    highp float dy = lB - l;
    
    highp float specVal = abs(dx - dy) * 5.0;
    specVal = pow(specVal, 1.5 + uSmoothness) * uHighlightStrength * 2.2;
    
    highp vec3 finalCol = c.rgb + vec3(specVal);
    gl_FragColor = vec4(clamp(finalCol, 0.0, 1.0), c.a);
}
"""

// Colored Pencil
class GPUImageColoredPencilFilter(
    var pencilWidth: Float = 1.5f,
    var strokePressure: Float = 0.8f,
    var paperBrightness: Float = 0.9f
) : GPUImageFilter(PSG_VERTEX_SHADER, COLORED_PENCIL_FRAGMENT_SHADER) {
    private var uWidthLocation: Int = -1
    private var uPressureLocation: Int = -1
    private var uBrightnessLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uWidthLocation = GLES20.glGetUniformLocation(program, "uPencilWidth")
        uPressureLocation = GLES20.glGetUniformLocation(program, "uStrokePressure")
        uBrightnessLocation = GLES20.glGetUniformLocation(program, "uPaperBrightness")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(width: Float, pressure: Float, brightness: Float) {
        this.pencilWidth = width
        this.strokePressure = pressure
        this.paperBrightness = brightness
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uWidthLocation, pencilWidth)
        setFloat(uPressureLocation, strokePressure)
        setFloat(uBrightnessLocation, paperBrightness)
    }
}

const val COLORED_PENCIL_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uPencilWidth;
uniform highp float uStrokePressure;
uniform highp float uPaperBrightness;
void main() {
    highp float step = uPencilWidth / 512.0;
    highp vec4 c0 = texture2D(inputImageTexture, textureCoordinate);
    highp vec4 cL = texture2D(inputImageTexture, textureCoordinate + vec2(-step, 0.0));
    highp vec4 cT = texture2D(inputImageTexture, textureCoordinate + vec2(0.0, -step));
    
    highp vec3 edges = abs(c0.rgb - cL.rgb) + abs(c0.rgb - cT.rgb);
    highp float edgeLen = (edges.r + edges.g + edges.b) * uStrokePressure * 3.5;
    edgeLen = clamp(edgeLen, 0.0, 1.0);
    
    highp vec3 strokeCol = c0.rgb * (1.0 - edgeLen) + vec3(uPaperBrightness) * edgeLen;
    gl_FragColor = vec4(clamp(strokeCol, 0.0, 1.0), c0.a);
}
"""


// -------------------------------------------------------------
// 2. BRUSH STROKES FILTER IMPLEMENTATIONS
// -------------------------------------------------------------

// Accented Edges
class GPUImageAccentedEdgesFilter(
    var edgeWidth: Float = 1.8f,
    var edgeBrightness: Float = 0.7f,
    var smoothness: Float = 0.5f
) : GPUImageFilter(PSG_VERTEX_SHADER, ACCENTED_EDGES_FRAGMENT_SHADER) {
    private var uWidthLocation: Int = -1
    private var uBrightnessLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uWidthLocation = GLES20.glGetUniformLocation(program, "uWidth")
        uBrightnessLocation = GLES20.glGetUniformLocation(program, "uBrightness")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(width: Float, brightness: Float) {
        this.edgeWidth = width
        this.edgeBrightness = brightness
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uWidthLocation, edgeWidth)
        setFloat(uBrightnessLocation, edgeBrightness)
    }
}

const val ACCENTED_EDGES_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uWidth;
uniform highp float uBrightness;
void main() {
    highp float step = uWidth / 512.0;
    highp vec4 c = texture2D(inputImageTexture, textureCoordinate);
    highp vec4 cL = texture2D(inputImageTexture, textureCoordinate + vec2(-step, 0.0));
    highp vec4 cT = texture2D(inputImageTexture, textureCoordinate + vec2(0.0, -step));
    
    highp float diff = length(c.rgb - cL.rgb) + length(c.rgb - cT.rgb);
    highp float accent = diff * uBrightness * 2.5;
    accent = clamp(accent, 0.0, 1.0);
    
    highp vec3 finalCol = mix(c.rgb, vec3(1.0, 0.92, 0.45), accent);
    gl_FragColor = vec4(finalCol, c.a);
}
"""

// Crosshatch Ink Filter
class GPUImageCrosshatchFilter(
    var strokeLength: Float = 8.0f,
    var strength: Float = 0.6f,
    var contrast: Float = 0.8f
) : GPUImageFilter(PSG_VERTEX_SHADER, CROSSHATCH_FRAGMENT_SHADER) {
    private var uLengthLocation: Int = -1
    private var uContrastLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uLengthLocation = GLES20.glGetUniformLocation(program, "uStrokeLength")
        uContrastLocation = GLES20.glGetUniformLocation(program, "uContrast")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(length: Float, contrast: Float) {
        this.strokeLength = length
        this.contrast = contrast
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uLengthLocation, strokeLength)
        setFloat(uContrastLocation, contrast)
    }
}

const val CROSSHATCH_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uStrokeLength;
uniform highp float uContrast;
void main() {
    highp vec4 color = texture2D(inputImageTexture, textureCoordinate);
    highp float lum = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
    
    highp vec3 finalColor = color.rgb;
    highp float xPlusY = textureCoordinate.x + textureCoordinate.y;
    highp float xMinusY = textureCoordinate.x - textureCoordinate.y;
    
    highp float frequency = 150.0 / max(1.0, uStrokeLength);
    
    if (lum < 0.72) {
        if (mod(xPlusY * frequency, 1.0) < 0.16) {
            finalColor -= vec3(uContrast * 0.42);
        }
    }
    if (lum < 0.42) {
        if (mod(xMinusY * frequency, 1.0) < 0.16) {
            finalColor -= vec3(uContrast * 0.42);
        }
    }
    
    gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), color.a);
}
"""

// Sumi-e (Japanese Ink Wash)
class GPUImageSumiEFilter(
    var strokePressure: Float = 0.7f,
    var darkArea: Float = 0.45f
) : GPUImageFilter(PSG_VERTEX_SHADER, SUMI_E_FRAGMENT_SHADER) {
    private var uPressureLocation: Int = -1
    private var uDarkAreaLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uPressureLocation = GLES20.glGetUniformLocation(program, "uPressure")
        uDarkAreaLocation = GLES20.glGetUniformLocation(program, "uDarkArea")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(pressure: Float, darkArea: Float) {
        this.strokePressure = pressure
        this.darkArea = darkArea
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uPressureLocation, strokePressure)
        setFloat(uDarkAreaLocation, darkArea)
    }
}

const val SUMI_E_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uPressure;
uniform highp float uDarkArea;
void main() {
    highp vec4 color = texture2D(inputImageTexture, textureCoordinate);
    highp float lum = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
    
    highp vec3 ink = color.rgb;
    if (lum < uDarkArea) {
        ink = ink * (1.0 - uPressure * 0.75) - vec3(0.12);
    } else {
        ink = ink * 1.12;
    }
    gl_FragColor = vec4(clamp(ink, 0.0, 1.0), color.a);
}
"""


// -------------------------------------------------------------
// 3. DISTORT FILTER IMPLEMENTATIONS
// -------------------------------------------------------------

// Ocean Ripple Distort
class GPUImageOceanRippleFilter(
    var rippleSize: Float = 8.0f,
    var rippleMagnitude: Float = 1.0f
) : GPUImageFilter(PSG_VERTEX_SHADER, OCEAN_RIPPLE_FRAGMENT_SHADER) {
    private var uSizeLocation: Int = -1
    private var uMagnitudeLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uSizeLocation = GLES20.glGetUniformLocation(program, "uRippleSize")
        uMagnitudeLocation = GLES20.glGetUniformLocation(program, "uRippleMagnitude")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(size: Float, mag: Float) {
        this.rippleSize = size
        this.rippleMagnitude = mag
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uSizeLocation, rippleSize)
        setFloat(uMagnitudeLocation, rippleMagnitude)
    }
}

const val OCEAN_RIPPLE_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uRippleSize;
uniform highp float uRippleMagnitude;
void main() {
    highp vec2 uv = textureCoordinate;
    highp float freq = uRippleSize * 15.0;
    highp float amp = uRippleMagnitude * 0.015;
    uv.x += sin(uv.y * freq) * amp;
    uv.y += cos(uv.x * freq) * amp;
    gl_FragColor = texture2D(inputImageTexture, uv);
}
"""

// Glass Distort Filter
class GPUImageGlassFilter(
    var distortion: Float = 5.0f,
    var smoothness: Float = 4.0f
) : GPUImageFilter(PSG_VERTEX_SHADER, GLASS_FRAGMENT_SHADER) {
    private var uDistortionLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uDistortionLocation = GLES20.glGetUniformLocation(program, "uDistortion")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(dist: Float) {
        this.distortion = dist
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uDistortionLocation, distortion)
    }
}

const val GLASS_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uDistortion;
highp float hashValue(highp vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
void main() {
    highp vec2 grain = vec2(hashValue(textureCoordinate * 64.0), hashValue(textureCoordinate * 64.0 + vec2(1.0)));
    highp vec2 dUV = (grain - vec3(0.5).xy) * (uDistortion * 0.024);
    gl_FragColor = texture2D(inputImageTexture, textureCoordinate + dUV);
}
"""


// -------------------------------------------------------------
// 4. SKETCH FILTER IMPLEMENTATIONS
// -------------------------------------------------------------

// Bas Relief
class GPUImageBasReliefFilter(
    var detail: Float = 4.0f,
    var smoothness: Float = 3.0f
) : GPUImageFilter(PSG_VERTEX_SHADER, BAS_RELIEF_FRAGMENT_SHADER) {
    private var uDetailLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uDetailLocation = GLES20.glGetUniformLocation(program, "uDetail")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(detail: Float) {
        this.detail = detail
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uDetailLocation, detail)
    }
}

const val BAS_RELIEF_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uDetail;
void main() {
    highp float step = uDetail / 512.0;
    highp vec4 c0 = texture2D(inputImageTexture, textureCoordinate);
    highp vec4 cL = texture2D(inputImageTexture, textureCoordinate + vec2(-step, -step));
    highp vec4 cR = texture2D(inputImageTexture, textureCoordinate + vec2(step, step));
    
    highp float lL = (cL.r + cL.g + cL.b) / 3.0;
    highp float lR = (cR.r + cR.g + cR.b) / 3.0;
    
    highp float relief = (lR - lL) * 2.8 + 0.5;
    highp vec3 grayPlaster = vec3(relief);
    gl_FragColor = vec4(clamp(grayPlaster, 0.0, 1.0), c0.a);
}
"""

// Halftone Pattern (Circular matrix halftone screen)
class GPUImageHalftonePatternFilter(
    var dotSize: Float = 6.0f,
    var contrast: Float = 0.7f
) : GPUImageFilter(PSG_VERTEX_SHADER, HALFTONE_PATTERN_FRAGMENT_SHADER) {
    private var uSizeLocation: Int = -1
    private var uContrastLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uSizeLocation = GLES20.glGetUniformLocation(program, "uDotSize")
        uContrastLocation = GLES20.glGetUniformLocation(program, "uContrast")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(dotSize: Float, contrast: Float) {
        this.dotSize = dotSize
        this.contrast = contrast
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uSizeLocation, dotSize)
        setFloat(uContrastLocation, contrast)
    }
}

const val HALFTONE_PATTERN_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uDotSize;
uniform highp float uContrast;
void main() {
    highp vec4 color = texture2D(inputImageTexture, textureCoordinate);
    highp float lum = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
    
    highp vec2 tc = textureCoordinate * vec2(512.0, 512.0) / max(1.0, uDotSize);
    highp vec2 f = fract(tc) - vec2(0.5);
    highp float dist = length(f);
    
    highp float targetRadius = (1.0 - lum) * 0.65;
    highp float pattern = smoothstep(targetRadius - 0.12, targetRadius + 0.12, dist);
    
    highp vec3 finalColor = mix(vec3(0.0), vec3(1.0), pattern);
    finalColor = mix(finalColor, color.rgb, 1.0 - uContrast);
    gl_FragColor = vec4(finalColor, color.a);
}
"""

// Photocopy Outline Filter
class GPUImagePhotocopyFilter(
    var detail: Float = 4.0f,
    var darkness: Float = 0.8f
) : GPUImageFilter(PSG_VERTEX_SHADER, PHOTOCOPY_FRAGMENT_SHADER) {
    private var uDetailLocation: Int = -1
    private var uDarknessLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uDetailLocation = GLES20.glGetUniformLocation(program, "uDetail")
        uDarknessLocation = GLES20.glGetUniformLocation(program, "uDarkness")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(detail: Float, darkness: Float) {
        this.detail = detail
        this.darkness = darkness
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uDetailLocation, detail)
        setFloat(uDarknessLocation, darkness)
    }
}

const val PHOTOCOPY_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uDetail;
uniform highp float uDarkness;
void main() {
    highp float step = uDetail / 512.0;
    highp vec4 c = texture2D(inputImageTexture, textureCoordinate);
    highp vec4 cL = texture2D(inputImageTexture, textureCoordinate + vec2(-step, 0.0));
    highp vec4 cR = texture2D(inputImageTexture, textureCoordinate + vec2(step, 0.0));
    highp vec4 cT = texture2D(inputImageTexture, textureCoordinate + vec2(0.0, -step));
    highp vec4 cB = texture2D(inputImageTexture, textureCoordinate + vec2(0.0, step));
    
    highp vec4 blurred = (cL + cR + cT + cB) / 4.0;
    highp vec4 diff = c - blurred;
    
    highp float mask = (diff.r + diff.g + diff.b) / 3.0;
    highp float edge = 1.0 - smoothstep(0.0, 0.06 / max(0.01, uDarkness), abs(mask));
    
    gl_FragColor = vec4(vec3(edge), c.a);
}
"""

// Chrome reflective liquid look
class GPUImageChromeFilter(
    var detail: Float = 4.0f,
    var smoothness: Float = 3.0f
) : GPUImageFilter(PSG_VERTEX_SHADER, CHROME_FRAGMENT_SHADER) {
    override fun onInitialized() {
        super.onInitialized()
    }
}

const val CHROME_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
void main() {
    highp vec4 color = texture2D(inputImageTexture, textureCoordinate);
    highp float lum = (color.r + color.g + color.b) / 3.0;
    
    // Metallic reflection mapping
    highp float reflection = sin(lum * 6.28318) * 0.5 + 0.5;
    highp vec3 chrome = mix(vec3(0.08, 0.12, 0.18), vec3(0.96, 0.98, 1.0), reflection);
    gl_FragColor = vec4(chrome, color.a);
}
"""


// -------------------------------------------------------------
// 5. TEXTURE FILTER IMPLEMENTATIONS
// -------------------------------------------------------------

// Stained Glass Layout
class GPUImageStainedGlassFilter(
    var cellSize: Float = 14.0f,
    var borderThickness: Float = 1.5f
) : GPUImageFilter(PSG_VERTEX_SHADER, STAINED_GLASS_FRAGMENT_SHADER) {
    private var uCellLocation: Int = -1
    private var uBorderLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uCellLocation = GLES20.glGetUniformLocation(program, "uCellSize")
        uBorderLocation = GLES20.glGetUniformLocation(program, "uBorderThickness")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(cellSize: Float, border: Float) {
        this.cellSize = cellSize
        this.borderThickness = border
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uCellLocation, cellSize)
        setFloat(uBorderLocation, borderThickness)
    }
}

const val STAINED_GLASS_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uCellSize;
uniform highp float uBorderThickness;
highp vec2 hash2(highp vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453123);
}
highp vec3 stainedVoronoi(highp vec2 x) {
    highp vec2 n = floor(x);
    highp vec2 f = fract(x);
    
    highp vec2 mg, mr;
    highp float md = 8.0;
    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            highp vec2 g = vec2(float(i), float(j));
            highp vec2 o = hash2(n + g);
            highp vec2 r = g + o - f;
            highp float d = dot(r, r);
            if (d < md) {
                md = d;
                mr = r;
                mg = g;
            }
        }
    }
    
    md = 8.0;
    for (int j = -2; j <= 2; j++) {
        for (int i = -2; i <= 2; i++) {
            highp vec2 g = mg + vec2(float(i), float(j));
            highp vec2 o = hash2(n + g);
            highp vec2 r = mg + g + o - f;
            if (dot(mr - r, mr - r) > 0.00001) {
                md = min(md, dot(0.5*(mr + r), normalize(r - mr)));
            }
        }
    }
    return vec3(md, n + mg);
}
void main() {
    highp float safetyCell = max(2.0, uCellSize);
    highp vec2 uv = textureCoordinate * (1000.0 / safetyCell);
    highp vec3 v = stainedVoronoi(uv);
    
    highp vec2 cellUV = (v.yz + vec2(0.5)) / (1000.0 / safetyCell);
    highp vec4 baseColor = texture2D(inputImageTexture, cellUV);
    
    highp float border = smoothstep(0.0, uBorderThickness * 0.12, v.x);
    highp vec3 finalColor = baseColor.rgb * border;
    gl_FragColor = vec4(finalColor, baseColor.a);
}
"""

// Craquelure painted crack surface
class GPUImageCraquelureFilter(
    var crackSpacing: Float = 15.0f,
    var crackDepth: Float = 1.0f
) : GPUImageFilter(PSG_VERTEX_SHADER, CRAQUELURE_FRAGMENT_SHADER) {
    private var uSpacingLocation: Int = -1
    private var uDepthLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uSpacingLocation = GLES20.glGetUniformLocation(program, "uCrackSpacing")
        uDepthLocation = GLES20.glGetUniformLocation(program, "uCrackDepth")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(spacing: Float, depth: Float) {
        this.crackSpacing = spacing
        this.crackDepth = depth
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uSpacingLocation, crackSpacing)
        setFloat(uDepthLocation, crackDepth)
    }
}

const val CRAQUELURE_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uCrackSpacing;
uniform highp float uCrackDepth;
highp float crackRand(highp vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}
void main() {
    highp vec4 color = texture2D(inputImageTexture, textureCoordinate);
    highp float spacing = max(1.0, uCrackSpacing);
    highp vec2 uv = textureCoordinate * (500.0 / spacing);
    highp vec2 ip = floor(uv);
    highp vec2 fp = fract(uv);
    
    highp float dMin = 1.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            highp vec2 l = vec2(float(x), float(y));
            highp vec2 o = vec2(crackRand(ip + l), crackRand(ip + l + vec2(1.0)));
            highp vec2 r = l + o - fp;
            dMin = min(dMin, dot(r, r));
        }
    }
    
    highp float crack = smoothstep(0.0, 0.08 / max(0.1, uCrackDepth), abs(dMin - 0.12));
    highp vec3 carved = color.rgb * crack;
    gl_FragColor = vec4(clamp(carved, 0.0, 1.0), color.a);
}
"""

// Texturizer Shader
class GPUImageTexturizerFilter(
    var scaling: Float = 8.0f,
    var relief: Float = 2.0f
) : GPUImageFilter(PSG_VERTEX_SHADER, TEXTURIZER_FRAGMENT_SHADER) {
    private var uScalingLocation: Int = -1
    private var uReliefLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uScalingLocation = GLES20.glGetUniformLocation(program, "uScaling")
        uReliefLocation = GLES20.glGetUniformLocation(program, "uRelief")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(scaling: Float, relief: Float) {
        this.scaling = scaling
        this.relief = relief
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uScalingLocation, scaling)
        setFloat(uReliefLocation, relief)
    }
}

const val TEXTURIZER_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uScaling;
uniform highp float uRelief;
void main() {
    highp vec4 color = texture2D(inputImageTexture, textureCoordinate);
    highp float pattern = sin(textureCoordinate.x * uScaling * 12.0) * cos(textureCoordinate.y * uScaling * 12.0);
    highp vec3 retCol = color.rgb + vec3(pattern * uRelief * 0.14);
    gl_FragColor = vec4(clamp(retCol, 0.0, 1.0), color.a);
}
"""

// -------------------------------------------------------------
// IBIS PAINT STYLE FX FILTERS
// -------------------------------------------------------------

class GPUImageChromaticAberrationFilter(
    var distance: Float = 16f,
    var angle: Float = 136f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(PSG_VERTEX_SHADER, CHROMATIC_ABERRATION_FRAGMENT_SHADER) {
    private var uDistanceLocation: Int = -1
    private var uAngleLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uDistanceLocation = GLES20.glGetUniformLocation(program, "uDistance")
        uAngleLocation = GLES20.glGetUniformLocation(program, "uAngle")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(distance: Float, angle: Float) {
        this.distance = distance
        this.angle = angle
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uDistanceLocation, distance / 1000f)
        setFloat(uAngleLocation, angle)
    }
}

const val CHROMATIC_ABERRATION_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uDistance;
uniform highp float uAngle;
void main() {
    highp float rad = uAngle * 3.14159265 / 180.0;
    highp vec2 offset = vec2(cos(rad), sin(rad)) * uDistance;
    highp float r = texture2D(inputImageTexture, textureCoordinate - offset).r;
    highp float g = texture2D(inputImageTexture, textureCoordinate).g;
    highp float b = texture2D(inputImageTexture, textureCoordinate + offset).b;
    highp float a = texture2D(inputImageTexture, textureCoordinate).a;
    gl_FragColor = vec4(r, g, b, a);
}
"""

class GPUImageGlitchFilter(
    var height: Float = 119f,
    var strength: Float = 23f,
    var colorShift: Float = 8f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(PSG_VERTEX_SHADER, GLITCH_FRAGMENT_SHADER) {
    private var uHeightLocation: Int = -1
    private var uStrengthLocation: Int = -1
    private var uColorShiftLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uHeightLocation = GLES20.glGetUniformLocation(program, "uHeight")
        uStrengthLocation = GLES20.glGetUniformLocation(program, "uStrength")
        uColorShiftLocation = GLES20.glGetUniformLocation(program, "uColorShift")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(height: Float, strength: Float, colorShift: Float) {
        this.height = height
        this.strength = strength
        this.colorShift = colorShift
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uHeightLocation, height)
        setFloat(uStrengthLocation, strength / 1000f)
        setFloat(uColorShiftLocation, colorShift / 1000f)
    }
}

const val GLITCH_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uHeight;
uniform highp float uStrength;
uniform highp float uColorShift;
void main() {
    highp float sliceY = floor(textureCoordinate.y * uHeight);
    highp float wave = sin(sliceY * 13.0) * cos(sliceY * 3.7);
    highp float dispX = wave * uStrength;
    
    highp float r = texture2D(inputImageTexture, vec2(textureCoordinate.x + dispX - uColorShift, textureCoordinate.y)).r;
    highp float g = texture2D(inputImageTexture, vec2(textureCoordinate.x + dispX, textureCoordinate.y)).g;
    highp float b = texture2D(inputImageTexture, vec2(textureCoordinate.x + dispX + uColorShift, textureCoordinate.y)).b;
    highp float a = texture2D(inputImageTexture, vec2(textureCoordinate.x + dispX, textureCoordinate.y)).a;
    
    gl_FragColor = vec4(r, g, b, a);
}
"""

class GPUImageBloomFilter(
    var area: Float = 100f,
    var radius: Float = 45f,
    var brightness: Float = 100f,
    var balanced: Float = 25f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(PSG_VERTEX_SHADER, BLOOM_FRAGMENT_SHADER) {
    private var uAreaLocation: Int = -1
    private var uRadiusLocation: Int = -1
    private var uBrightnessLocation: Int = -1
    private var uBalancedLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uAreaLocation = GLES20.glGetUniformLocation(program, "uArea")
        uRadiusLocation = GLES20.glGetUniformLocation(program, "uRadius")
        uBrightnessLocation = GLES20.glGetUniformLocation(program, "uBrightness")
        uBalancedLocation = GLES20.glGetUniformLocation(program, "uBalanced")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(area: Float, radius: Float, brightness: Float, balanced: Float) {
        this.area = area
        this.radius = radius
        this.brightness = brightness
        this.balanced = balanced
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uAreaLocation, area / 100f)
        setFloat(uRadiusLocation, radius / 1000f)
        setFloat(uBrightnessLocation, brightness / 100f)
        setFloat(uBalancedLocation, balanced / 100f)
    }
}

const val BLOOM_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uArea;
uniform highp float uRadius;
uniform highp float uBrightness;
uniform highp float uBalanced;
void main() {
    highp vec4 baseColor = texture2D(inputImageTexture, textureCoordinate);
    highp vec3 blurred = vec3(0.0);
    highp float total = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            highp vec2 offset = vec2(float(x), float(y)) * uRadius;
            highp vec3 sampleCol = texture2D(inputImageTexture, textureCoordinate + offset).rgb;
            highp float luma = dot(sampleCol, vec3(0.299, 0.587, 0.114));
            if (luma > (1.0 - uArea)) {
                blurred += sampleCol;
                total += 1.0;
            }
        }
    }
    if (total > 0.0) {
        blurred = (blurred / total) * uBrightness;
    } else {
        blurred = vec3(0.0);
    }
    highp vec3 finalColor = mix(baseColor.rgb + blurred, vec3(1.0) - (vec3(1.0) - baseColor.rgb) * (vec3(1.0) - blurred), uBalanced);
    gl_FragColor = vec4(finalColor, baseColor.a);
}
"""

class GPUImageCrossFilterFilter(
    var count: Float = 4f,
    var direction: Float = 45f,
    var area: Float = 10f,
    var brightness: Float = 50f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(PSG_VERTEX_SHADER, CROSS_FILTER_FRAGMENT_SHADER) {
    private var uCountLocation: Int = -1
    private var uDirectionLocation: Int = -1
    private var uAreaLocation: Int = -1
    private var uBrightnessLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uCountLocation = GLES20.glGetUniformLocation(program, "uCount")
        uDirectionLocation = GLES20.glGetUniformLocation(program, "uDirection")
        uAreaLocation = GLES20.glGetUniformLocation(program, "uArea")
        uBrightnessLocation = GLES20.glGetUniformLocation(program, "uBrightness")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(count: Float, direction: Float, area: Float, brightness: Float) {
        this.count = count
        this.direction = direction
        this.area = area
        this.brightness = brightness
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uCountLocation, count)
        setFloat(uDirectionLocation, direction)
        setFloat(uAreaLocation, area / 100f)
        setFloat(uBrightnessLocation, brightness / 100f)
    }
}

const val CROSS_FILTER_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uCount;
uniform highp float uDirection;
uniform highp float uArea;
uniform highp float uBrightness;
void main() {
    highp vec4 baseColor = texture2D(inputImageTexture, textureCoordinate);
    highp vec3 flare = vec3(0.0);
    highp float steps = 6.0;
    highp float numSpokes = max(2.0, uCount);
    for (float i = 0.0; i < 8.0; i++) {
        if (i >= numSpokes) break;
        highp float angle = uDirection * 3.14159 / 180.0 + (i * 3.14159 / numSpokes);
        highp vec2 dir = vec2(cos(angle), sin(angle)) * 0.004;
        for (float j = 1.0; j <= steps; j++) {
            highp vec2 sampleCoord1 = textureCoordinate + dir * j;
            highp vec2 sampleCoord2 = textureCoordinate - dir * j;
            highp vec3 col1 = texture2D(inputImageTexture, sampleCoord1).rgb;
            highp vec3 col2 = texture2D(inputImageTexture, sampleCoord2).rgb;
            highp float l1 = dot(col1, vec3(0.299, 0.587, 0.114));
            highp float l2 = dot(col2, vec3(0.299, 0.587, 0.114));
            if (l1 > (1.0 - uArea)) {
                flare += col1 * (1.0 - j/steps);
            }
            if (l2 > (1.0 - uArea)) {
                flare += col2 * (1.0 - j/steps);
            }
        }
    }
    highp vec3 finalColor = baseColor.rgb + flare * uBrightness;
    gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), baseColor.a);
}
"""

class GPUImageInnerGlowFilter(
    var radius: Float = 104f,
    var r: Float = 1.0f,
    var g: Float = 1.0f,
    var b: Float = 1.0f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(PSG_VERTEX_SHADER, INNER_GLOW_FRAGMENT_SHADER) {
    private var uRadiusLocation: Int = -1
    private var uColorRLocation: Int = -1
    private var uColorGLocation: Int = -1
    private var uColorBLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uRadiusLocation = GLES20.glGetUniformLocation(program, "uRadius")
        uColorRLocation = GLES20.glGetUniformLocation(program, "uColorR")
        uColorGLocation = GLES20.glGetUniformLocation(program, "uColorG")
        uColorBLocation = GLES20.glGetUniformLocation(program, "uColorB")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(radius: Float, r: Float, g: Float, b: Float) {
        this.radius = radius
        this.r = r
        this.g = g
        this.b = b
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uRadiusLocation, radius / 2000f)
        setFloat(uColorRLocation, r)
        setFloat(uColorGLocation, g)
        setFloat(uColorBLocation, b)
    }
}

const val INNER_GLOW_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uRadius;
uniform highp float uColorR;
uniform highp float uColorG;
uniform highp float uColorB;
void main() {
    highp vec3 uColor = vec3(uColorR, uColorG, uColorB);
    highp vec4 baseColor = texture2D(inputImageTexture, textureCoordinate);
    highp float minAlpha = 1.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            highp vec2 offset = vec2(float(x), float(y)) * uRadius;
            highp float a = texture2D(inputImageTexture, textureCoordinate + offset).a;
            minAlpha = min(minAlpha, a);
        }
    }
    highp float edgeAmount = (baseColor.a - minAlpha);
    highp vec3 finalColor = mix(baseColor.rgb, uColor, edgeAmount * 0.8);
    gl_FragColor = vec4(finalColor, baseColor.a);
}
"""

class GPUImageBevelFilter(
    var height: Float = 20f,
    var smoothness: Float = 45f,
    var highlightSize: Float = 14f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(PSG_VERTEX_SHADER, BEVEL_FRAGMENT_SHADER) {
    private var uHeightLocation: Int = -1
    private var uSmoothnessLocation: Int = -1
    private var uHighlightLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uHeightLocation = GLES20.glGetUniformLocation(program, "uHeight")
        uSmoothnessLocation = GLES20.glGetUniformLocation(program, "uSmoothness")
        uHighlightLocation = GLES20.glGetUniformLocation(program, "uHighlightSize")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(height: Float, smoothness: Float, highlightSize: Float) {
        this.height = height
        this.smoothness = smoothness
        this.highlightSize = highlightSize
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uHeightLocation, height / 100f)
        setFloat(uSmoothnessLocation, smoothness / 100f)
        setFloat(uHighlightLocation, highlightSize / 100f)
    }
}

const val BEVEL_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uHeight;
uniform highp float uSmoothness;
uniform highp float uHighlightSize;
void main() {
    highp vec4 baseColor = texture2D(inputImageTexture, textureCoordinate);
    highp float step = 0.002 * (1.0 + uSmoothness * 3.0);
    highp float aL = texture2D(inputImageTexture, textureCoordinate + vec2(-step, 0.0)).a;
    highp float aR = texture2D(inputImageTexture, textureCoordinate + vec2(step, 0.0)).a;
    highp float aD = texture2D(inputImageTexture, textureCoordinate + vec2(0.0, -step)).a;
    highp float aU = texture2D(inputImageTexture, textureCoordinate + vec2(0.0, step)).a;
    highp vec2 norm2d = vec2(aR - aL, aU - aD) * uHeight;
    highp vec3 normal = normalize(vec3(norm2d, 1.0));
    highp vec3 lightDir = normalize(vec3(-0.7, 0.7, 1.2));
    highp float diffuse = dot(normal, lightDir);
    highp float specular = pow(max(0.0, diffuse), 4.0) * uHighlightSize * 4.0;
    highp float shadow = (1.0 - smoothstep(0.0, 0.6, diffuse)) * uHeight * 0.5;
    highp vec3 litColor = baseColor.rgb + vec3(specular) - vec3(shadow);
    gl_FragColor = vec4(clamp(litColor, 0.0, 1.0), baseColor.a);
}
"""

class GPUImageEmboss2Filter(
    var grayScale: Float = 0f,
    var height: Float = 1f,
    var amount: Float = 500f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(PSG_VERTEX_SHADER, EMBOSS2_FRAGMENT_SHADER) {
    private var uGrayScaleLocation: Int = -1
    private var uHeightLocation: Int = -1
    private var uAmountLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uGrayScaleLocation = GLES20.glGetUniformLocation(program, "uGrayScale")
        uHeightLocation = GLES20.glGetUniformLocation(program, "uHeight")
        uAmountLocation = GLES20.glGetUniformLocation(program, "uAmount")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(grayScale: Float, height: Float, amount: Float) {
        this.grayScale = grayScale
        this.height = height
        this.amount = amount
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uGrayScaleLocation, grayScale)
        setFloat(uHeightLocation, height / 1000f)
        setFloat(uAmountLocation, amount / 100f)
    }
}

const val EMBOSS2_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uGrayScale;
uniform highp float uHeight;
uniform highp float uAmount;
void main() {
    highp vec4 baseColor = texture2D(inputImageTexture, textureCoordinate);
    highp vec3 colOffset = texture2D(inputImageTexture, textureCoordinate - vec2(uHeight)).rgb;
    highp vec3 diff = baseColor.rgb - colOffset;
    highp float lumaDiff = dot(diff, vec3(0.299, 0.587, 0.114)) * uAmount;
    highp vec3 embossedColor = baseColor.rgb + vec3(lumaDiff);
    if (uGrayScale > 0.5) {
        highp float gray = dot(embossedColor, vec3(0.299, 0.587, 0.114));
        gl_FragColor = vec4(vec3(gray), baseColor.a);
    } else {
        gl_FragColor = vec4(clamp(embossedColor, 0.0, 1.0), baseColor.a);
    }
}
"""

class GPUImageWaterdropFilter(
    var distance: Float = 100f,
    var flatness: Float = 10f,
    var height: Float = 3f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(PSG_VERTEX_SHADER, WATERDROP_FRAGMENT_SHADER) {
    private var uDistanceLocation: Int = -1
    private var uFlatnessLocation: Int = -1
    private var uHeightLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uDistanceLocation = GLES20.glGetUniformLocation(program, "uDistance")
        uFlatnessLocation = GLES20.glGetUniformLocation(program, "uFlatness")
        uHeightLocation = GLES20.glGetUniformLocation(program, "uHeight")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(distance: Float, flatness: Float, height: Float) {
        this.distance = distance
        this.flatness = flatness
        this.height = height
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uDistanceLocation, distance / 100f)
        setFloat(uFlatnessLocation, flatness / 100f)
        setFloat(uHeightLocation, height / 100f)
    }
}

const val WATERDROP_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uDistance;
uniform highp float uFlatness;
uniform highp float uHeight;
void main() {
    highp vec2 center = vec2(0.5, 0.5);
    highp vec2 toCenter = textureCoordinate - center;
    highp float r = length(toCenter);
    if (r < (0.4 * uDistance)) {
        highp float d = r / (0.4 * uDistance);
        highp float bulge = (1.0 - d * d) * uHeight * (1.0 - uFlatness);
        highp vec2 distCoord = textureCoordinate - toCenter * bulge;
        highp vec4 c = texture2D(inputImageTexture, distCoord);
        highp float spec = pow(max(0.0, 1.0 - d), 3.0) * 0.15;
        gl_FragColor = vec4(clamp(c.rgb + vec3(spec), 0.0, 1.0), c.a);
    } else {
        gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
    }
}
"""

class GPUImageSatinFilter(
    var distance: Float = 11f,
    var opacity: Float = 0.5f,
    var r: Float = 0f,
    var g: Float = 0f,
    var b: Float = 0f
) : jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(PSG_VERTEX_SHADER, SATIN_FRAGMENT_SHADER) {
    private var uDistanceLocation: Int = -1
    private var uOpacityLocation: Int = -1
    private var uColorRLocation: Int = -1
    private var uColorGLocation: Int = -1
    private var uColorBLocation: Int = -1

    override fun onInit() {
        super.onInit()
        uDistanceLocation = GLES20.glGetUniformLocation(program, "uDistance")
        uOpacityLocation = GLES20.glGetUniformLocation(program, "uOpacity")
        uColorRLocation = GLES20.glGetUniformLocation(program, "uColorR")
        uColorGLocation = GLES20.glGetUniformLocation(program, "uColorG")
        uColorBLocation = GLES20.glGetUniformLocation(program, "uColorB")
    }

    override fun onInitialized() {
        super.onInitialized()
        applyParameters()
    }

    fun setParams(distance: Float, opacity: Float, r: Float, g: Float, b: Float) {
        this.distance = distance
        this.opacity = opacity
        this.r = r
        this.g = g
        this.b = b
        applyParameters()
    }

    private fun applyParameters() {
        setFloat(uDistanceLocation, distance / 1000f)
        setFloat(uOpacityLocation, opacity)
        setFloat(uColorRLocation, r)
        setFloat(uColorGLocation, g)
        setFloat(uColorBLocation, b)
    }
}

const val SATIN_FRAGMENT_SHADER = """
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float uDistance;
uniform highp float uOpacity;
uniform highp float uColorR;
uniform highp float uColorG;
uniform highp float uColorB;
void main() {
    highp vec3 uColor = vec3(uColorR, uColorG, uColorB);
    highp vec4 baseColor = texture2D(inputImageTexture, textureCoordinate);
    highp float aOffset1 = texture2D(inputImageTexture, textureCoordinate + vec2(uDistance)).a;
    highp float aOffset2 = texture2D(inputImageTexture, textureCoordinate - vec2(uDistance)).a;
    highp float wave = sin(aOffset1 * 3.14159) * cos(aOffset2 * 3.14159);
    highp float satinVal = (1.0 - wave) * uOpacity;
    highp vec3 finalColor = mix(baseColor.rgb, uColor, satinVal * baseColor.a);
    gl_FragColor = vec4(finalColor, baseColor.a);
}
"""

