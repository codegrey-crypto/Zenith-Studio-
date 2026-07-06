package com.example.studio.render

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RasterExtrudeRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // UI-controlled parameters
    var extrusionDepth: Float = 40f
    var lightX: Float = 0.5f
    var lightY: Float = 0.5f
    var lightZ: Float = 1.0f
    var shadingIntensity: Float = 1.0f
    var rotationX: Float = 25f
    var rotationY: Float = -20f
    var rotationZ: Float = 0f
    var numberOfLayers: Int = 30

    private var inputBitmap: Bitmap? = null
    private var isBitmapChanged = false

    private var programId: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var depthHandle: Int = 0
    private var textureHandle: Int = 0
    private var lightDirHandle: Int = 0
    private var intensityHandle: Int = 0
    private var shadingFadeHandle: Int = 0

    private var glTextureId: Int = 0

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    init {
        // Vertex data covering full screen coordinates
        val quadVertices = floatArrayOf(
            // X, Y, Z, U, V
            -0.8f,  0.8f, 0.0f,  0.0f, 0.0f,
            -0.8f, -0.8f, 0.0f,  0.0f, 1.0f,
             0.8f, -0.8f, 0.0f,  1.0f, 1.0f,
             0.8f,  0.8f, 0.0f,  1.0f, 0.0f
        )

        val quadIndices = shortArrayOf(0, 1, 2, 0, 2, 3)

        vertexBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(quadVertices)
                position(0)
            }

        indexBuffer = ByteBuffer.allocateDirect(quadIndices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(quadIndices)
                position(0)
            }
    }

    fun setInputBitmap(bitmap: Bitmap) {
        inputBitmap = bitmap
        isBitmapChanged = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.08f, 0.08f, 0.1f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)

        programId = GLES30.glCreateProgram().apply {
            GLES30.glAttachShader(this, vertexShader)
            GLES30.glAttachShader(this, fragmentShader)
            GLES30.glLinkProgram(this)

            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(this, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                Log.e("RasterExtrudeRenderer", "Shader linkage failed: " + GLES30.glGetProgramInfoLog(this))
            }
        }

        mvpMatrixHandle = GLES30.glGetUniformLocation(programId, "uMVP")
        depthHandle = GLES30.glGetUniformLocation(programId, "uDepth")
        textureHandle = GLES30.glGetUniformLocation(programId, "uTexture")
        lightDirHandle = GLES30.glGetUniformLocation(programId, "uLightDir")
        intensityHandle = GLES30.glGetUniformLocation(programId, "uIntensity")
        shadingFadeHandle = GLES30.glGetUniformLocation(programId, "uShadingFade")

        setupTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3.2f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        if (programId == 0) return

        if (isBitmapChanged) {
            setupTexture()
            isBitmapChanged = false
        }

        GLES30.glUseProgram(programId)

        // Rotation matrix setup
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTextureId)
        GLES30.glUniform1i(textureHandle, 0)

        GLES30.glUniform3f(lightDirHandle, lightX, lightY, lightZ)
        GLES30.glUniform1f(intensityHandle, shadingIntensity)

        vertexBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, vertexBuffer)
        GLES30.glEnableVertexAttribArray(0)

        vertexBuffer.position(3)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, vertexBuffer)
        GLES30.glEnableVertexAttribArray(1)

        val depthStep = -(extrusionDepth / 1000f) / numberOfLayers.toFloat()

        // Render from back-slice to front-slice to build extrusion layers
        for (i in numberOfLayers - 1 downTo 0) {
            val zOffset = i * depthStep
            val fadeFactor = 1.0f - (i.toFloat() / numberOfLayers.toFloat()) * 0.75f
            
            GLES30.glUniform1f(depthHandle, zOffset)
            GLES30.glUniform1f(shadingFadeHandle, fadeFactor)

            val mvMatrix = FloatArray(16)
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvMatrix, 0, projectionMatrix, 0, mvMatrix, 0)
            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvMatrix, 0)

            GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_SHORT, indexBuffer)
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

    private fun setupTexture() {
        val bitmap = inputBitmap ?: return
        if (glTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(glTextureId), 0)
        }

        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        glTextureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTextureId)
        
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).apply {
            GLES30.glShaderSource(this, shaderCode)
            GLES30.glCompileShader(this)

            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(this, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                Log.e("RasterExtrudeRenderer", "Error compiling shader: " + GLES30.glGetShaderInfoLog(this))
                GLES30.glDeleteShader(this)
            }
        }
    }

    companion object {
        const val VERTEX_SHADER_CODE = """#version 300 es
layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec2 aTexCoord;

uniform float uDepth;
uniform mat4 uMVP;

out vec2 vTexCoord;

void main() {
    vec4 pos = aPosition;
    pos.z += uDepth;
    gl_Position = uMVP * pos;
    vTexCoord = aTexCoord;
}"""

        const val FRAGMENT_SHADER_CODE = """#version 300 es
precision mediump float;

in vec2 vTexCoord;
uniform sampler2D uTexture;
uniform vec3 uLightDir;
uniform float uIntensity;
uniform float uShadingFade;

out vec4 fragColor;

void main() {
    vec4 color = texture(uTexture, vTexCoord);
    if (color.a < 0.05) {
        discard;
    }

    // Normal approximation for the extrude face
    vec3 normal = vec3(0.0, 0.0, 1.0);
    float light = max(dot(normal, normalize(uLightDir)), 0.0);

    float shade = 1.0 + (light * uIntensity * 0.5);
    color.rgb *= shade * uShadingFade;

    fragColor = color;
}"""
    }
}
