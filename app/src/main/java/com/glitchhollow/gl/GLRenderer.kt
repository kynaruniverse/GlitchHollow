package com.glitchhollow.gl

import android.content.Context
import android.content.SharedPreferences
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.glitchhollow.core.Constants
import com.glitchhollow.screen.MainMenuScreen
import com.glitchhollow.screen.ScreenManager
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    lateinit var batch: SpriteBatch
    private set

    lateinit var assets: AssetManager
    private set

    lateinit var audio: AudioManager
    private set
    
    private lateinit var shader:    ShaderProgram
    private lateinit var scanlines: ScanlineRenderer

    private var screenWidth  = 0
    private var screenHeight = 0

    // ── Fixed timestep ────────────────────────────────────────────

    companion object {
        const val FIXED_STEP      = 1f / 60f
        const val FIXED_STEP_MS = (FIXED_STEP * 1000f).toLong()
        const val MAX_STEPS       = 5
        const val MAX_ACCUMULATED = FIXED_STEP * MAX_STEPS
    }

    private var accumulator = 0f
    private var lastFrameNs = 0L

    // ── Debug stats ───────────────────────────────────────────────

    var actualFps             = 0f;  private set
    var physicsTicksLastFrame = 0;   private set
    private var frameCount    = 0
    private var fpsTimer      = 0f

    private val settings: SharedPreferences by lazy {
        context.getSharedPreferences("gh_settings", Context.MODE_PRIVATE)
    }

    // ── GLSurfaceView.Renderer ────────────────────────────────────

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.024f, 0.016f, 0.063f, 1.0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        shader    = ShaderProgram(context, "shaders/sprite.vert", "shaders/sprite.frag")
        batch     = SpriteBatch(shader)
        assets    = AssetManager(context)
        audio     = AudioManager(context)
        scanlines = ScanlineRenderer(context)

        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        screenWidth  = width
        screenHeight = height
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
        // ── Real elapsed time ─────────────────────────────────────
        val now     = System.nanoTime()
        val elapsed = ((now - lastFrameNs) / 1_000_000_000f)
            .coerceIn(0.0001f, MAX_ACCUMULATED)

        lastFrameNs = now

        // ── FPS counter ───────────────────────────────────────────
        frameCount++
        fpsTimer += elapsed
        if (fpsTimer >= 1f) {
            actualFps  = frameCount / fpsTimer
            frameCount = 0
            fpsTimer   = 0f
        }

        // ── Fixed timestep accumulator ────────────────────────────
        accumulator += elapsed
        if (accumulator > MAX_ACCUMULATED) {
            accumulator = MAX_ACCUMULATED
        }
        var ticks = 0

        while (accumulator >= FIXED_STEP && ticks < MAX_STEPS) {
        
        ScreenManager.current?.update(FIXED_STEP)
            accumulator -= FIXED_STEP
            ticks++
        }

        physicsTicksLastFrame = ticks

        // ── Render ────────────────────────────────────────────────
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT
        )
        ScreenManager.current?.render()

        // Scanlines + vignette — absolute last pass
        if (Constants.SCANLINE_ALPHA > 0f) {
            val hm = Camera2D(screenWidth, screenHeight).buildHudMatrix()
            scanlines.draw(
                batch, assets,
                screenWidth.toFloat(), screenHeight.toFloat(),
                hm,
                settings.getBoolean("glitch", true)
            )
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    fun onPause() {
        audio.pauseBgm()
    }

    fun onResume() {
        audio.resumeBgm()
        lastFrameNs = System.nanoTime()
        accumulator = 0f
    }

    fun dispose() {
        audio.dispose()
    }
}