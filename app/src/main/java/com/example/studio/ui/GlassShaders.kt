package com.example.studio.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build

object GlassShaders {

    val GLASS_MORPHISM_SHADER_SRC = """
        uniform shader inputShader;
        uniform shader backdropShader;
        uniform float2 size;
        uniform float fractalIntensity;
        uniform float fractalType; // 0.0 = Smooth, 1.0 = Shattered
        uniform float refractionIndex;
        uniform float edgeTorsion;
        uniform float surfaceTension;
        uniform float2 globalOffset;
        uniform float zoomScale;

        float2 hash22(float2 p) {
            p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
            return frac(sin(p) * 43758.5453123);
        }

        float2 voronoiCell(float2 p) {
            float2 ip = floor(p);
            float2 fp = frac(p);
            float d1 = 8.0;
            float2 cellCenter = float2(0.0);
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    float2 l = float2(float(x), float(y));
                    float2 o = hash22(ip + l);
                    float2 r = l + o - fp;
                    float d = dot(r, r);
                    if (d < d1) {
                        d1 = d;
                        cellCenter = ip + l + o;
                    }
                }
            }
            return cellCenter;
        }

        float smoothVoronoi(float2 x) {
            float2 p = floor(x);
            float2 f = frac(x);
            float res = 0.0;
            for (int j = -1; j <= 1; j++) {
                for (int i = -1; i <= 1; i++) {
                    float2 b = float2(float(i), float(j));
                    float2 r = b - f + hash22(p + b);
                    res += exp(-8.0 * dot(r, r));
                }
            }
            return -(1.0 / 8.0) * log(res);
        }

        half4 main(float2 coords) {
            float2 center = size * 0.5;
            float2 norm = (coords - center) / center;
            float dist = length(norm);
            float edgeFactor = smoothstep(0.9, 1.0, dist);

            // 1. Edge Torsion (Twist)
            float twistAngle = edgeTorsion * 0.1 * edgeFactor;
            float cosA = cos(twistAngle);
            float sinA = sin(twistAngle);
            float2 matchedCoords = coords;
            if (edgeFactor > 0.0) {
                float2 local = coords - center;
                float2 rot = float2(
                    local.x * cosA - local.y * sinA,
                    local.x * sinA + local.y * cosA
                );
                matchedCoords = center + rot;
            }

            // 2. Fractal cellular voronoi distortion
            float2 voronoiCoord = matchedCoords * 0.05 * (1.1 - fractalIntensity * 0.08);
            float2 smoothOf = fractalType < 0.5 ? 
                float2(smoothVoronoi(voronoiCoord), smoothVoronoi(voronoiCoord + 5.0)) : 
                voronoiCell(voronoiCoord) * 0.5;

            float2 dispAmt = (smoothOf - (voronoiCoord * 0.5)) * fractalIntensity * refractionIndex * 3.0;
            float2 displacedCoords = matchedCoords + dispAmt;

            // 3. Surface Tension (convex droplet edge push)
            float2 dir = normalize(coords - center);
            float dropletWarp = surfaceTension * 3.0 * edgeFactor * (1.0 - edgeFactor);
            displacedCoords = displacedCoords - dir * dropletWarp;

            // Compute backdrop sampling coordinates aligned with global position and zoom scale
            float2 sourceCoords = (displacedCoords / zoomScale) + globalOffset;

            // Sample from backdropShader (the captured background background)
            half4 color = backdropShader.eval(sourceCoords);

            // Fetch the layer's own content (like translucent fills or borders)
            half4 localColor = inputShader.eval(coords);
            if (localColor.a > 0.0) {
                color = mix(color, localColor, localColor.a * 0.3);
            }

            // 4. Specular highlight ring
            float specRing = exp(-pow((edgeFactor - 0.5) * 5.0, 2.0)) * surfaceTension * 0.08;
            color.rgb += half3(specRing);

            return color;
        }
    """

    val REEDED_GLASS_SHADER_SRC = """
        uniform shader inputShader;
        uniform shader backdropShader;
        uniform float2 size;
        uniform float lineDensity;
        uniform float refractionStrength;
        uniform float blurMix;
        uniform float specularHighlight;
        uniform float rotation;
        uniform float2 globalOffset;
        uniform float zoomScale;

        half4 main(float2 coords) {
            float coordValue = mix(coords.x, coords.y, rotation);
            float sizeValue = mix(size.x, size.y, rotation);
            
            float angle = (coordValue / sizeValue) * lineDensity * 6.283185;
            float waveValue = sin(angle);
            float slope = cos(angle);
            
            float offset = slope * refractionStrength * 2.0;
            float2 offsetVec = mix(float2(offset, 0.0), float2(0.0, offset), rotation);
            float2 refractedCoords = coords + offsetVec;
            
            // Compute backdrop sampling coordinates aligned with global offset and zoom scale
            float2 sourceCoords = (refractedCoords / zoomScale) + globalOffset;

            // Sample from backdropShader (the captured background backdrop)
            half4 color = backdropShader.eval(sourceCoords);
            
            // Mix with the layer's own translucent style elements
            half4 localColor = inputShader.eval(coords);
            if (localColor.a > 0.0) {
                color = mix(color, localColor, localColor.a * 0.3);
            }
            
            float spec = pow(max(0.0, waveValue), 12.0) * specularHighlight * 0.18;
            color.rgb += half3(spec);
            
            return color;
        }
    """

    fun compileGlassMorphismEffect(
        effect: com.example.studio.model.StudioEffect.PhotoshopEffect,
        width: Float,
        height: Float,
        backdrop: Bitmap?,
        globalX: Float,
        globalY: Float,
        zoomScale: Float = 1.0f
    ): Any? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        
        try {
            val shader = RuntimeShader(GLASS_MORPHISM_SHADER_SRC)
            
            val fracIntensity = effect.parameters["FractalIntensity"]?.value ?: 5f
            val fracType = effect.parameters["FractalType"]?.value ?: 0f
            val refrIndex = effect.parameters["RefractionIndex"]?.value ?: 3f
            val torsion = effect.parameters["EdgeTorsion"]?.value ?: 2f
            val tension = effect.parameters["SurfaceTension"]?.value ?: 4f
            val blurRad = effect.parameters["Radius"]?.value ?: 20f

            shader.setFloatUniform("size", width, height)
            shader.setFloatUniform("fractalIntensity", fracIntensity)
            shader.setFloatUniform("fractalType", fracType)
            shader.setFloatUniform("refractionIndex", refrIndex)
            shader.setFloatUniform("edgeTorsion", torsion)
            shader.setFloatUniform("surfaceTension", tension)
            
            val actualBmp = backdrop ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val backdropShader = BitmapShader(actualBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setInputShader("backdropShader", backdropShader)
            shader.setFloatUniform("globalOffset", globalX, globalY)
            shader.setFloatUniform("zoomScale", zoomScale)

            val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "inputShader")
            return if (blurRad > 0.1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurEffect = RenderEffect.createBlurEffect(blurRad, blurRad, Shader.TileMode.CLAMP)
                RenderEffect.createChainEffect(shaderEffect, blurEffect)
            } else {
                shaderEffect
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun compileReededGlassEffect(
        effect: com.example.studio.model.StudioEffect.PhotoshopEffect,
        width: Float,
        height: Float,
        backdrop: Bitmap?,
        globalX: Float,
        globalY: Float,
        zoomScale: Float = 1.0f
    ): Any? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null

        try {
            val shader = RuntimeShader(REEDED_GLASS_SHADER_SRC)

            val dns = effect.parameters["LineDensity"]?.value ?: 33f
            val str = effect.parameters["RefractionStrength"]?.value ?: 8f
            val blurM = effect.parameters["BlurMix"]?.value ?: 12f
            val spec = effect.parameters["SpecularHighlight"]?.value ?: 5f
            val rot = effect.parameters["Rotation"]?.value ?: 0f

            shader.setFloatUniform("size", width, height)
            shader.setFloatUniform("lineDensity", dns)
            shader.setFloatUniform("refractionStrength", str)
            shader.setFloatUniform("blurMix", blurM)
            shader.setFloatUniform("specularHighlight", spec)
            shader.setFloatUniform("rotation", rot)

            val actualBmp = backdrop ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val backdropShader = BitmapShader(actualBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setInputShader("backdropShader", backdropShader)
            shader.setFloatUniform("globalOffset", globalX, globalY)
            shader.setFloatUniform("zoomScale", zoomScale)

            val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "inputShader")
            return if (blurM > 0.1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurEffect = RenderEffect.createBlurEffect(blurM, blurM, Shader.TileMode.CLAMP)
                RenderEffect.createChainEffect(shaderEffect, blurEffect)
            } else {
                shaderEffect
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    val BEVEL_EMBOSS_SHADER_SRC = """
        uniform shader inputShader;
        uniform float depth;
        uniform float size;
        uniform float soften;
        uniform float angle;
        uniform float altitude;

        half4 main(float2 coords) {
            half4 color = inputShader.eval(coords);
            if (color.a <= 0.0) return color;

            float d = max(1.0, size * 0.15);
            float aL = inputShader.eval(coords + float2(-d, 0.0)).a;
            float aR = inputShader.eval(coords + float2(d, 0.0)).a;
            float aT = inputShader.eval(coords + float2(0.0, -d)).a;
            float aB = inputShader.eval(coords + float2(0.0, d)).a;

            float aTL = inputShader.eval(coords + float2(-d * 0.707, -d * 0.707)).a;
            float aTR = inputShader.eval(coords + float2(d * 0.707, -d * 0.707)).a;
            float aBL = inputShader.eval(coords + float2(-d * 0.707, d * 0.707)).a;
            float aBR = inputShader.eval(coords + float2(d * 0.707, d * 0.707)).a;

            float gx = (aTR + 2.0 * aR + aBR) - (aTL + 2.0 * aL + aBL);
            float gy = (aBL + 2.0 * aB + aBR) - (aTL + 2.0 * aT + aTR);

            float radA = angle * 3.14159265 / 180.0;
            float radH = altitude * 3.14159265 / 180.0;
            float3 L = float3(cos(radH) * cos(radA), cos(radH) * sin(radA), sin(radH));

            float slopeScale = (depth / 100.0) * 8.0;
            float3 N = normalize(float3(-gx * slopeScale, -gy * slopeScale, 1.0));

            float diffuse = dot(N, L);

            float3 V = float3(0.0, 0.0, 1.0);
            float3 H_vec = normalize(L + V);
            float specular = pow(max(0.0, dot(N, H_vec)), 16.0) * (depth / 100.0);

            half3 rgb = color.rgb;
            if (diffuse > 0.0) {
                float hlIntensity = diffuse * 0.4 * (depth / 100.0) + specular * 0.6;
                rgb = rgb + half3(hlIntensity) - rgb * half3(hlIntensity);
            } else {
                float shIntensity = -diffuse * 0.5 * (depth / 100.0);
                rgb = rgb * (half3(1.0) - half3(shIntensity));
            }

            return half4(clamp(rgb, 0.0, 1.0), color.a);
        }
    """

    fun compileBevelEmbossEffect(
        effect: com.example.studio.model.StudioEffect.PhotoshopEffect,
        width: Float,
        height: Float
    ): Any? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        try {
            val shader = RuntimeShader(BEVEL_EMBOSS_SHADER_SRC)
            val dp = effect.parameters["Depth"]?.value ?: 100f
            val sz = effect.parameters["Size"]?.value ?: 5f
            val sf = effect.parameters["Soften"]?.value ?: 0f
            val ang = effect.parameters["Angle"]?.value ?: 120f
            val alt = effect.parameters["Altitude"]?.value ?: 30f

            shader.setFloatUniform("depth", dp)
            shader.setFloatUniform("size", sz)
            shader.setFloatUniform("soften", sf)
            shader.setFloatUniform("angle", ang)
            shader.setFloatUniform("altitude", alt)

            val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "inputShader")
            return if (sf > 0.1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurEffect = RenderEffect.createBlurEffect(sf, sf, Shader.TileMode.CLAMP)
                RenderEffect.createChainEffect(shaderEffect, blurEffect)
            } else {
                shaderEffect
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
