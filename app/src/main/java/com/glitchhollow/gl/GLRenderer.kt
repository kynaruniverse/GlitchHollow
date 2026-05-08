package com.glitchhollow.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.glitchhollow.screen.MainMenuScreen
import com.glitchhollow.screen.ScreenManager
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        const val MAX_STEPS = 3
        private const val FIXED_DT = 1f / 60f
        private const val MAX_DT   = 0.05f   // clamp — prevents spiral of death
    }

    lateinit var batch:  SpriteBatch
    lateinit var assets: AssetManager
    lateinit var audio:  AudioManager

    // Exposed for DebugOverlay
    var actualFps: Float = 0f
        private set
    var physicsTicksLastFrame: Int = 0
        private set

    private lateinit var shader:    ShaderProgram
    lateinit var scanlines: ScanlineRenderer

    // Real-time delta tracking
    private var lastFrameNs = 0L
    private var fpsFrameCount = 0
    private var fpsTimerNs    = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.01f, 0.05f, 1f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        try {
            shader    = ShaderProgram(context, "shaders/sprite.vert", "shaders/sprite.frag")
            batch     = SpriteBatch(shader)
            assets    = AssetManager(context)
            audio     = AudioManager(context)
            scanlines = ScanlineRenderer()
        } catch (e: Exception) {
            throw RuntimeException("GL init failed", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        if (ScreenManager.current == null) {
            ScreenManager.set(
                MainMenuScreen(
                    context, assets, audio, batch,
                    width, height,
                    renderer = this
                )
            )
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()

        // Measure real frame delta; guard against first frame and pauses
        val dt = if (lastFrameNs == 0L) FIXED_DT
                 else ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, MAX_DT)
        lastFrameNs = now

        // FPS counter — averaged over one second
        fpsFrameCount++
        val fpsElapsed = now - fpsTimerNs
        if (fpsElapsed >= 1_000_000_000L) {
            actualFps     = fpsFrameCount * 1_000_000_000f / fpsElapsed
            fpsFrameCount = 0
            fpsTimerNs    = now
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val screen = ScreenManager.current
        if (screen != null) {
            screen.update(dt)
            screen.render()
        }
    }

    // Called from GlitchHollowApp.onResume — reset delta so first frame after
    // resume doesn't produce a huge spike from the paused interval
    fun onResume() {
        lastFrameNs   = 0L
        fpsFrameCount = 0
        fpsTimerNs    = System.nanoTime()
    }

    fun onPause() {
        audio.pauseBgm()
    }

    fun dispose() {
        batch.dispose()
        assets.dispose()
        audio.dispose()
    }
}