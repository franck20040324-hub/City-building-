package com.example.engine3d

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.example.engine3d.CityMeshGenerators.TILE_SIZE
import com.example.model.GameState
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CityGLSurfaceView(
    context: Context,
    private val onTileTapped: (Int, Int) -> Unit
) : GLSurfaceView(context) {

    private val renderer: City3DRenderer
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var isDragging = false
    private var isOrbitMode = false
    private var activePointers = 0

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.zoomBy(1f / detector.scaleFactor)
                return true
            }
        }
    )

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        renderer = City3DRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updateGameState(state: GameState) {
        renderer.setGameState(state)
    }

    fun rotateCameraBy(deltaDeg: Float) {
        renderer.rotateYaw(deltaDeg)
    }

    fun adjustPitchBy(deltaDeg: Float) {
        renderer.adjustPitch(deltaDeg)
    }

    fun resetCamera() {
        renderer.resetCamera()
    }

    fun setOrbitMode(enabled: Boolean) {
        isOrbitMode = enabled
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        activePointers = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                downTime = System.currentTimeMillis()
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    if (!isDragging && (abs(event.x - downX) > 14f || abs(event.y - downY) > 14f)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        if (isOrbitMode) {
                            renderer.rotateYaw(dx * 0.35f)
                            renderer.adjustPitch(dy * 0.25f)
                        } else {
                            renderer.panByScreenDelta(dx, dy)
                        }
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            MotionEvent.ACTION_UP -> {
                val dt = System.currentTimeMillis() - downTime
                if (!isDragging && dt < 320 && event.pointerCount == 1) {
                    val hit = renderer.screenToGrid(event.x, event.y, width, height)
                    if (hit != null) {
                        onTileTapped(hit.first, hit.second)
                    }
                }
                isDragging = false
            }
        }
        return true
    }

    private class City3DRenderer : Renderer {

        private var programId = 0
        private var uMvpLoc = 0
        private var uLightDirLoc = 0
        private var uSunColorLoc = 0
        private var uAmbientColorLoc = 0
        private var uNightFactorLoc = 0
        private var uFogColorLoc = 0
        private var uBrightnessLoc = 0

        private var aPosLoc = 0
        private var aNormLoc = 0
        private var aColorLoc = 0
        private var aEmissiveLoc = 0

        private val staticBuilder = MeshBuilder(131072)
        private val dynamicBuilder = MeshBuilder(32768)
        private val animRenderer = AnimatedEntitiesRenderer()

        @Volatile
        private var latestState: GameState? = null
        private var staticDirty = true
        private var staticSignature = -1L
        private var staticBuffer: FloatBuffer? = null
        private var staticVertexCount = 0

        private val projMatrix = FloatArray(16)
        private val viewMatrix = FloatArray(16)
        private val vpMatrix = FloatArray(16)
        private val invVpMatrix = FloatArray(16)

        // Camera state
        @Volatile private var camTargetX = 0f
        @Volatile private var camTargetZ = 0f
        @Volatile private var camDistance = 24f
        @Volatile private var camYawDeg = 42f
        @Volatile private var camPitchDeg = 44f

        private val startNanos = System.nanoTime()

        fun setGameState(state: GameState) {
            val sig = computeStaticSignature(state)
            latestState = state
            if (sig != staticSignature) {
                staticSignature = sig
                staticDirty = true
            }
        }

        private fun computeStaticSignature(state: GameState): Long {
            var h = state.season.ordinal.toLong() * 31L + state.gridSize
            for (c in state.cells) {
                h = h * 131 + c.terrain.ordinal
                val b = c.building
                if (b != null) {
                    h = h * 31 + b.type.ordinal + (b.level * 17) + (b.rotation * 7) +
                        (if (b.isConstructing) 1 else 0) +
                        (if (b.isConstructing) (b.constructionRemainingSec / 5) else 0)
                }
            }
            return h
        }

        fun zoomBy(factor: Float) {
            camDistance = (camDistance * factor).coerceIn(10f, 52f)
        }

        fun rotateYaw(deltaDeg: Float) {
            camYawDeg = (camYawDeg + deltaDeg) % 360f
        }

        fun adjustPitch(deltaDeg: Float) {
            camPitchDeg = (camPitchDeg + deltaDeg).coerceIn(22f, 74f)
        }

        fun resetCamera() {
            camTargetX = 0f
            camTargetZ = 0f
            camDistance = 24f
            camYawDeg = 42f
            camPitchDeg = 44f
        }

        fun panByScreenDelta(dx: Float, dy: Float) {
            val yawRad = Math.toRadians(camYawDeg.toDouble()).toFloat()
            val rightX = cos(yawRad)
            val rightZ = -sin(yawRad)
            val fwdX = -sin(yawRad)
            val fwdZ = -cos(yawRad)
            val scale = camDistance * 0.00145f
            camTargetX = (camTargetX - (rightX * dx - fwdX * dy) * scale).coerceIn(-25f, 25f)
            camTargetZ = (camTargetZ - (rightZ * dx - fwdZ * dy) * scale).coerceIn(-25f, 25f)
        }

        fun screenToGrid(sx: Float, sy: Float, w: Int, h: Int): Pair<Int, Int>? {
            val state = latestState ?: return null
            if (w <= 0 || h <= 0) return null
            val nx = (2f * sx) / w - 1f
            val ny = 1f - (2f * sy) / h

            val inv = FloatArray(16)
            if (!Matrix.invertM(inv, 0, vpMatrix, 0)) return null

            val nearPt = FloatArray(4)
            val farPt = FloatArray(4)
            Matrix.multiplyMV(nearPt, 0, inv, 0, floatArrayOf(nx, ny, -1f, 1f), 0)
            Matrix.multiplyMV(farPt, 0, inv, 0, floatArrayOf(nx, ny, 1f, 1f), 0)
            if (abs(nearPt[3]) < 1e-5f || abs(farPt[3]) < 1e-5f) return null

            val x0 = nearPt[0] / nearPt[3]; val y0 = nearPt[1] / nearPt[3]; val z0 = nearPt[2] / nearPt[3]
            val x1 = farPt[0] / farPt[3]; val y1 = farPt[1] / farPt[3]; val z1 = farPt[2] / farPt[3]
            val dy = y1 - y0
            if (abs(dy) < 1e-5f) return null
            val t = -y0 / dy
            if (t < 0f) return null
            val hitX = x0 + (x1 - x0) * t
            val hitZ = z0 + (z1 - z0) * t

            val halfSpan = (state.gridSize * TILE_SIZE) * 0.5f
            val gx = ((hitX + halfSpan) / TILE_SIZE).toInt()
            val gz = ((hitZ + halfSpan) / TILE_SIZE).toInt()
            if (gx in 0 until state.gridSize && gz in 0 until state.gridSize) {
                return gx to gz
            }
            return null
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_CULL_FACE)
            GLES20.glCullFace(GLES20.GL_BACK)

            val vertSrc = """
                uniform mat4 uMVPMatrix;
                attribute vec3 aPosition;
                attribute vec3 aNormal;
                attribute vec3 aColor;
                attribute float aEmissive;
                varying vec3 vNormal;
                varying vec3 vColor;
                varying float vEmissive;
                varying float vDepth;
                void main() {
                    vec4 pos = uMVPMatrix * vec4(aPosition, 1.0);
                    gl_Position = pos;
                    vNormal = aNormal;
                    vColor = aColor;
                    vEmissive = aEmissive;
                    vDepth = pos.w;
                }
            """.trimIndent()

            val fragSrc = """
                precision mediump float;
                uniform vec3 uLightDir;
                uniform vec3 uSunColor;
                uniform vec3 uAmbientColor;
                uniform float uNightFactor;
                uniform vec3 uFogColor;
                uniform float uBrightness;
                varying vec3 vNormal;
                varying vec3 vColor;
                varying float vEmissive;
                varying float vDepth;
                void main() {
                    vec3 n = normalize(vNormal);
                    float diff = max(dot(n, normalize(uLightDir)), 0.0);
                    vec3 lit = vColor * (uAmbientColor + uSunColor * diff);
                    float glowStrength = 0.0;
                    if (vEmissive > 1.45) {
                        glowStrength = 1.0;
                    } else if (vEmissive > 0.35) {
                        glowStrength = smoothstep(0.20, 0.62, uNightFactor) * clamp(vEmissive, 0.0, 1.0);
                    }
                    float boost = clamp(max(vEmissive, 1.0), 1.0, 1.38);
                    vec3 finalCol = mix(lit, vColor * (1.15 * boost), glowStrength);
                    float fogFactor = clamp((vDepth - 38.0) / 55.0, 0.0, 0.65);
                    finalCol = mix(finalCol, uFogColor, fogFactor);
                    finalCol *= uBrightness;
                    gl_FragColor = vec4(clamp(finalCol, 0.0, 1.0), 1.0);
                }
            """.trimIndent()

            val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
            val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
            programId = GLES20.glCreateProgram().also {
                GLES20.glAttachShader(it, vs)
                GLES20.glAttachShader(it, fs)
                GLES20.glLinkProgram(it)
            }

            uMvpLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
            uLightDirLoc = GLES20.glGetUniformLocation(programId, "uLightDir")
            uSunColorLoc = GLES20.glGetUniformLocation(programId, "uSunColor")
            uAmbientColorLoc = GLES20.glGetUniformLocation(programId, "uAmbientColor")
            uNightFactorLoc = GLES20.glGetUniformLocation(programId, "uNightFactor")
            uFogColorLoc = GLES20.glGetUniformLocation(programId, "uFogColor")
            uBrightnessLoc = GLES20.glGetUniformLocation(programId, "uBrightness")

            aPosLoc = GLES20.glGetAttribLocation(programId, "aPosition")
            aNormLoc = GLES20.glGetAttribLocation(programId, "aNormal")
            aColorLoc = GLES20.glGetAttribLocation(programId, "aColor")
            aEmissiveLoc = GLES20.glGetAttribLocation(programId, "aEmissive")
            staticDirty = true
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            val ratio = width.toFloat() / height.coerceAtLeast(1).toFloat()
            Matrix.perspectiveM(projMatrix, 0, 48f, ratio, 0.5f, 160f)
        }

        override fun onDrawFrame(gl: GL10?) {
            val state = latestState ?: return
            val elapsedSec = (System.nanoTime() - startNanos) / 1_000_000_000f

            if (staticDirty) {
                CityMeshGenerators.buildStaticWorld(state, staticBuilder)
                staticBuffer = staticBuilder.toFloatBuffer()
                staticVertexCount = staticBuilder.vertexCount()
                staticDirty = false
            }

            animRenderer.buildDynamicFrame(state, elapsedSec, dynamicBuilder)
            val dynBuffer = dynamicBuilder.toFloatBuffer()
            val dynCount = dynamicBuilder.vertexCount()

            // Compute Day/Night sky, sun & ambient colors
            val tod = state.timeOfDay
            // Day peak at 0.5, night at 0.0 / 1.0
            val dayWave = ((cos((tod - 0.5f) * 2f * PI.toFloat()) + 1f) * 0.5f).coerceIn(0f, 1f)
            val nightFactor = 1f - dayWave
            val sunsetFactor = (1f - abs(dayWave - 0.35f) * 3.2f).coerceIn(0f, 1f)

            val skyR = 0.06f + dayWave * 0.42f + sunsetFactor * 0.38f
            val skyG = 0.09f + dayWave * 0.66f + sunsetFactor * 0.14f
            val skyB = 0.18f + dayWave * 0.78f - sunsetFactor * 0.08f
            val brightness = state.settings.brightness.coerceIn(0.35f, 1.25f)

            GLES20.glClearColor(
                (skyR * brightness).coerceIn(0f, 1f),
                (skyG * brightness).coerceIn(0f, 1f),
                (skyB * brightness).coerceIn(0f, 1f),
                1f
            )
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            // Camera View Matrix
            val yawR = Math.toRadians(camYawDeg.toDouble()).toFloat()
            val pitchR = Math.toRadians(camPitchDeg.toDouble()).toFloat()
            val horizDist = camDistance * cos(pitchR)
            val eyeX = camTargetX + horizDist * sin(yawR)
            val eyeY = camDistance * sin(pitchR)
            val eyeZ = camTargetZ + horizDist * cos(yawR)
            Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, camTargetX, 0f, camTargetZ, 0f, 1f, 0f)
            Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

            GLES20.glUseProgram(programId)
            GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, vpMatrix, 0)

            val sunAngle = (tod - 0.25f) * 2f * PI.toFloat()
            val lx = cos(sunAngle)
            val ly = abs(sin(sunAngle)).coerceAtLeast(0.25f)
            val len = sqrt(lx * lx + ly * ly + 0.35f * 0.35f)
            GLES20.glUniform3f(uLightDirLoc, lx / len, ly / len, 0.35f / len)

            val sunR = 0.25f + dayWave * 0.75f + sunsetFactor * 0.2f
            val sunG = 0.28f + dayWave * 0.68f
            val sunB = 0.45f + dayWave * 0.45f - sunsetFactor * 0.15f
            GLES20.glUniform3f(uSunColorLoc, sunR, sunG, sunB)
            GLES20.glUniform3f(uAmbientColorLoc, 0.22f + dayWave * 0.26f, 0.24f + dayWave * 0.26f, 0.34f + dayWave * 0.22f)
            val emissiveNight = if (state.settings.brightRoadLinesAlways) 1.0f else nightFactor
            GLES20.glUniform1f(uNightFactorLoc, emissiveNight)
            GLES20.glUniform3f(uFogColorLoc, skyR, skyG, skyB)
            GLES20.glUniform1f(uBrightnessLoc, brightness)

            staticBuffer?.let { drawBuffer(it, staticVertexCount) }
            if (dynCount > 0) drawBuffer(dynBuffer, dynCount)
        }

        private fun drawBuffer(buf: FloatBuffer, count: Int) {
            val stride = 10 * 4
            buf.position(0)
            GLES20.glEnableVertexAttribArray(aPosLoc)
            GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, buf)

            buf.position(3)
            GLES20.glEnableVertexAttribArray(aNormLoc)
            GLES20.glVertexAttribPointer(aNormLoc, 3, GLES20.GL_FLOAT, false, stride, buf)

            buf.position(6)
            GLES20.glEnableVertexAttribArray(aColorLoc)
            GLES20.glVertexAttribPointer(aColorLoc, 3, GLES20.GL_FLOAT, false, stride, buf)

            buf.position(9)
            GLES20.glEnableVertexAttribArray(aEmissiveLoc)
            GLES20.glVertexAttribPointer(aEmissiveLoc, 1, GLES20.GL_FLOAT, false, stride, buf)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
        }

        private fun compileShader(type: Int, src: String): Int {
            val s = GLES20.glCreateShader(type)
            GLES20.glShaderSource(s, src)
            GLES20.glCompileShader(s)
            return s
        }
    }
}
