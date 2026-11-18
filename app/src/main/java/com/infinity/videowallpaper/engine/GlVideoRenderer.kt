package com.infinity.videowallpaper.engine

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import com.infinity.videowallpaper.data.FitMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

data class ShaderParams(
    val fitMode: FitMode = FitMode.CROP,
    val rotationDegrees: Int = 0,
    val dimAmount: Float = 0f,
    val blurAmount: Float = 0f,
    val vignetteAmount: Float = 0f,
    val saturation: Float = 1f,
    val parallaxEnabled: Boolean = false,
    val xOffset: Float = 0.5f,
    val fpsCap: Int = 30,
)

/**
 * Hardware-accelerated OpenGL ES 2.0 renderer attached to a [WallpaperService] [SurfaceHolder].
 *
 * Uses a single EGL producer on a dedicated [HandlerThread] so Media3 ExoPlayer renders frames
 * into an OES [SurfaceTexture] while the shader applies real-time 0/90/180/270 rotation, dimming,
 * blur, vignette, saturation/grayscale, exact Crop/Contain/Stretch scaling, parallax, and FPS cap.
 */
class GlVideoRenderer(
    private val surfaceHolder: SurfaceHolder,
    private val onVideoSurfaceReady: (Surface) -> Unit,
) {
    private val renderThread = HandlerThread("VideoWallpaperGL").apply { start() }
    private val handler = Handler(renderThread.looper)

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var programId = 0
    private var textureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null

    private var surfaceWidth = 1
    private var surfaceHeight = 1
    private var videoWidth = 0
    private var videoHeight = 0

    @Volatile
    private var params = ShaderParams()

    @Volatile
    private var released = false

    private var lastDrawTimeNs = 0L
    private var hasFrame = false

    private val stMatrix = FloatArray(16)

    // Uniform & attribute handles
    private var aPosLoc = -1
    private var aTexLoc = -1
    private var uTexMatrixLoc = -1
    private var uScaleLoc = -1
    private var uOffsetLoc = -1
    private var uRotCosSinLoc = -1
    private var uDimLoc = -1
    private var uBlurLoc = -1
    private var uVignetteLoc = -1
    private var uSaturationLoc = -1

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(QUAD_VERTICES.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_VERTICES)
            position(0)
        }

    fun start(width: Int, height: Int, initialParams: ShaderParams) {
        params = initialParams
        handler.post {
            if (released) return@post
            surfaceWidth = width.coerceAtLeast(1)
            surfaceHeight = height.coerceAtLeast(1)
            if (!initEgl()) return@post
            if (!initGl()) return@post
            val st = SurfaceTexture(textureId).apply {
                setOnFrameAvailableListener({
                    if (!released) {
                        handler.post { onNewFrame() }
                    }
                }, handler)
            }
            surfaceTexture = st
            val surf = Surface(st)
            videoSurface = surf
            drawSolidBlack()
            onVideoSurfaceReady(surf)
        }
    }

    fun updateSurfaceSize(width: Int, height: Int) {
        handler.post {
            if (released) return@post
            surfaceWidth = width.coerceAtLeast(1)
            surfaceHeight = height.coerceAtLeast(1)
            if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglSurface != EGL14.EGL_NO_SURFACE) {
                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
                if (hasFrame) drawCurrentFrame(force = true) else drawSolidBlack()
            }
        }
    }

    fun updateVideoSize(width: Int, height: Int) {
        handler.post {
            if (released) return@post
            videoWidth = width
            videoHeight = height
            if (hasFrame) drawCurrentFrame(force = true)
        }
    }

    fun updateParams(newParams: ShaderParams) {
        params = newParams
        handler.post {
            if (released) return@post
            if (hasFrame) drawCurrentFrame(force = true)
        }
    }

    fun clearToBlack() {
        handler.post {
            if (released) return@post
            hasFrame = false
            drawSolidBlack()
        }
    }

    fun release() {
        if (released) return
        released = true
        handler.post {
            runCatching { surfaceTexture?.setOnFrameAvailableListener(null) }
            runCatching { videoSurface?.release() }
            runCatching { surfaceTexture?.release() }
            videoSurface = null
            surfaceTexture = null

            if (textureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                textureId = 0
            }
            if (programId != 0) {
                GLES20.glDeleteProgram(programId)
                programId = 0
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    eglDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT,
                )
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                }
                EGL14.eglTerminate(eglDisplay)
            }
            eglSurface = EGL14.EGL_NO_SURFACE
            eglContext = EGL14.EGL_NO_CONTEXT
            eglDisplay = EGL14.EGL_NO_DISPLAY
            renderThread.quitSafely()
        }
    }

    private fun onNewFrame() {
        if (released || eglDisplay == EGL14.EGL_NO_DISPLAY) return
        val st = surfaceTexture ?: return
        runCatching {
            st.updateTexImage()
            st.getTransformMatrix(stMatrix)
        }.onFailure { return }
        hasFrame = true
        drawCurrentFrame(force = false)
    }

    private fun drawSolidBlack() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) return
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun drawCurrentFrame(force: Boolean) {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE || programId == 0) {
            return
        }
        val currentParams = params
        val nowNs = System.nanoTime()
        if (!force && currentParams.fpsCap > 0) {
            val minIntervalNs = 1_000_000_000L / currentParams.fpsCap.coerceIn(15, 120)
            if (nowNs - lastDrawTimeNs < minIntervalNs) {
                return
            }
        }
        lastDrawTimeNs = nowNs

        val transform = GlTransformMath.compute(
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            surfaceWidth = surfaceWidth,
            surfaceHeight = surfaceHeight,
            fitMode = currentParams.fitMode,
            parallaxEnabled = currentParams.parallaxEnabled,
            xOffset = currentParams.xOffset,
            rotationDegrees = currentParams.rotationDegrees,
        )

        val rad = Math.toRadians(currentParams.rotationDegrees.toDouble())
        val rotCos = cos(rad).toFloat()
        val rotSin = sin(rad).toFloat()

        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(programId)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(aTexLoc)
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, stMatrix, 0)
        GLES20.glUniform2f(uScaleLoc, transform.scaleX, transform.scaleY)
        GLES20.glUniform2f(uOffsetLoc, transform.offsetX, transform.offsetY)
        GLES20.glUniform2f(uRotCosSinLoc, rotCos, rotSin)
        GLES20.glUniform1f(uDimLoc, currentParams.dimAmount.coerceIn(0f, 0.85f))
        GLES20.glUniform1f(uBlurLoc, currentParams.blurAmount.coerceIn(0f, 1f))
        GLES20.glUniform1f(uVignetteLoc, currentParams.vignetteAmount.coerceIn(0f, 1f))
        GLES20.glUniform1f(uSaturationLoc, currentParams.saturation.coerceIn(0f, 1.5f))

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun initEgl(): Boolean {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "eglGetDisplay failed")
            return false
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            Log.e(TAG, "eglInitialize failed")
            return false
        }

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) ||
            configs[0] == null
        ) {
            Log.e(TAG, "eglChooseConfig failed")
            return false
        }

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE,
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay,
            configs[0],
            EGL14.EGL_NO_CONTEXT,
            contextAttribs,
            0,
        )
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "eglCreateContext failed")
            return false
        }

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            configs[0],
            surfaceHolder.surface,
            surfaceAttribs,
            0,
        )
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "eglCreateWindowSurface failed")
            return false
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            Log.e(TAG, "eglMakeCurrent failed")
            return false
        }
        Matrix.setIdentityM(stMatrix, 0)
        return true
    }

    private fun initGl(): Boolean {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        if (vs == 0 || fs == 0) return false

        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vs)
        GLES20.glAttachShader(programId, fs)
        GLES20.glLinkProgram(programId)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link error: ${GLES20.glGetProgramInfoLog(programId)}")
            GLES20.glDeleteProgram(programId)
            programId = 0
            return false
        }

        aPosLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        aTexLoc = GLES20.glGetAttribLocation(programId, "aTexCoord")
        uTexMatrixLoc = GLES20.glGetUniformLocation(programId, "uTexMatrix")
        uScaleLoc = GLES20.glGetUniformLocation(programId, "uScale")
        uOffsetLoc = GLES20.glGetUniformLocation(programId, "uOffset")
        uRotCosSinLoc = GLES20.glGetUniformLocation(programId, "uRotCosSin")
        uDimLoc = GLES20.glGetUniformLocation(programId, "uDim")
        uBlurLoc = GLES20.glGetUniformLocation(programId, "uBlur")
        uVignetteLoc = GLES20.glGetUniformLocation(programId, "uVignette")
        uSaturationLoc = GLES20.glGetUniformLocation(programId, "uSaturation")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        return true
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        private const val TAG = "GlVideoRenderer"

        // x, y, u, v
        private val QUAD_VERTICES = floatArrayOf(
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f,
        )

        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uTexMatrix;
            uniform vec2 uScale;
            uniform vec2 uOffset;
            uniform vec2 uRotCosSin;
            varying vec2 vTexCoord;
            varying vec2 vScreenUv;

            void main() {
                vec2 pos = aPosition * uScale + uOffset;
                gl_Position = vec4(pos, 0.0, 1.0);

                vec2 centered = aTexCoord - vec2(0.5, 0.5);
                vec2 rotated = vec2(
                    centered.x * uRotCosSin.x - centered.y * uRotCosSin.y,
                    centered.x * uRotCosSin.y + centered.y * uRotCosSin.x
                ) + vec2(0.5, 0.5);

                vTexCoord = (uTexMatrix * vec4(rotated, 0.0, 1.0)).xy;
                vScreenUv = pos * 0.5 + 0.5;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            uniform samplerExternalOES uTexture;
            uniform float uDim;
            uniform float uBlur;
            uniform float uVignette;
            uniform float uSaturation;

            varying vec2 vTexCoord;
            varying vec2 vScreenUv;

            void main() {
                vec4 color;
                if (uBlur > 0.01) {
                    float r = uBlur * 0.012;
                    vec4 sum = texture2D(uTexture, vTexCoord) * 0.24;
                    sum += texture2D(uTexture, vTexCoord + vec2( r,  0.0)) * 0.12;
                    sum += texture2D(uTexture, vTexCoord + vec2(-r,  0.0)) * 0.12;
                    sum += texture2D(uTexture, vTexCoord + vec2(0.0,  r)) * 0.12;
                    sum += texture2D(uTexture, vTexCoord + vec2(0.0, -r)) * 0.12;
                    sum += texture2D(uTexture, vTexCoord + vec2( r,  r)) * 0.07;
                    sum += texture2D(uTexture, vTexCoord + vec2(-r,  r)) * 0.07;
                    sum += texture2D(uTexture, vTexCoord + vec2( r, -r)) * 0.07;
                    sum += texture2D(uTexture, vTexCoord + vec2(-r, -r)) * 0.07;
                    color = sum;
                } else {
                    color = texture2D(uTexture, vTexCoord);
                }

                // Saturation / grayscale adjustment
                float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
                vec3 rgb = mix(vec3(luma), color.rgb, uSaturation);

                // Vignette darkening around edges
                if (uVignette > 0.01) {
                    vec2 centered = vScreenUv - 0.5;
                    float dist = length(centered) * 1.4142;
                    float vig = smoothstep(0.35, 1.15, dist) * uVignette * 0.75;
                    rgb *= (1.0 - vig);
                }

                // Dim brightness
                rgb *= (1.0 - uDim);

                gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), 1.0);
            }
        """
    }
}
