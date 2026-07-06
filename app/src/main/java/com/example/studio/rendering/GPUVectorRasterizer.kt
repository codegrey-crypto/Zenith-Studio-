package com.example.studio.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Color
import android.os.Build
import kotlin.math.max

data class Vertex(val x: Float, val y: Float)

data class Triangle(val a: Vertex, val b: Vertex, val c: Vertex)

class PathTessellator {
    /**
     * Approximates any complex Path outline by linear steps, translating the outline
     * into dynamic triangle fans for high-speed hardware-friendly rasterization.
     */
    fun tessellate(path: Path, precision: Float = 4f): List<Triangle> {
        val pm = PathMeasure(path, false)
        val length = pm.length
        if (length <= 0) return emptyList()

        val points = mutableListOf<Vertex>()
        var distance = 0f
        val pos = FloatArray(2)

        while (distance < length) {
            pm.getPosTan(distance, pos, null)
            points.add(Vertex(pos[0], pos[1]))
            distance += max(precision, 1f)
        }

        // Add final point
        pm.getPosTan(length, pos, null)
        points.add(Vertex(pos[0], pos[1]))

        if (points.size < 3) return emptyList()

        // Fan Triangulate
        val tris = mutableListOf<Triangle>()
        val origin = points[0]
        for (i in 1 until points.size - 1) {
            tris.add(Triangle(origin, points[i], points[i + 1]))
        }
        return tris
    }
}

class GPUVectorRasterizer {
    private val tessellator = PathTessellator()

    companion object {
        private const val VECTOR_SHADER_SRC = """
            uniform float4 color;
            uniform float opacity;

            half4 main(float2 fragCoord) {
                return half4(color.rgb, color.a * opacity);
            }
        """
    }

    /**
     * Tessellate and shade vectors on the GPU. Batching vertices into paths
     * and filling them using specialized shaders.
     */
    fun render(
        path: Path,
        width: Int,
        height: Int,
        color: Int,
        opacity: Float
    ): Bitmap {
        val output = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val triangles = tessellator.tessellate(path)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && triangles.isNotEmpty()) {
            try {
                val shader = android.graphics.RuntimeShader(VECTOR_SHADER_SRC)
                shader.setFloatUniform(
                    "color",
                    Color.red(color) / 255f,
                    Color.green(color) / 255f,
                    Color.blue(color) / 255f,
                    Color.alpha(color) / 255f
                )
                shader.setFloatUniform("opacity", opacity)

                val paint = Paint().apply {
                    setShader(shader)
                    isAntiAlias = true
                }

                for (t in triangles) {
                    drawTriangle(canvas, t, paint)
                }
                return output
            } catch (e: Exception) {
                // fallback to hardware raster path
            }
        }

        // Optimized standard path fill
        val paint = Paint().apply {
            this.color = color
            this.alpha = (opacity * Color.alpha(color)).toInt().coerceIn(0, 255)
            this.style = Paint.Style.FILL
            this.isAntiAlias = true
        }
        canvas.drawPath(path, paint)
        return output
    }

    private fun drawTriangle(canvas: Canvas, t: Triangle, paint: Paint) {
        val path = Path().apply {
            moveTo(t.a.x, t.a.y)
            lineTo(t.b.x, t.b.y)
            lineTo(t.c.x, t.c.y)
            close()
        }
        canvas.drawPath(path, paint)
    }
}
