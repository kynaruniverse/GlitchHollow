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

    lateinit var batch:    SpriteBatch    private set
    lateinit var assets:   AssetManager  private set
    lateinit var audio:    AudioManager  private set

    private lateinit var shader:    ShaderProgram
    private lateinit var scanlines: ScanlineRenderer

    private var screenWidth  = 0
    private var screenHeight = 0
    private var lastFrameTime = 0L

    private val settings: SharedPreferences by lazy {
        context.getSharedPreferences("gh_settings", Context.MODE_PRIVATE)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.024f, 0.016f, 0.063f, 1.0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        shader    = ShaderProgram(context, "shaders/sprite.vert", "shaders/sprite.frag")
        batch     = SpriteBatch(shader)
        assets    = AssetManager(context)
        audio     = AudioManager(context)
        scanlines = ScanlineRenderer(context)

        lastFrameTime = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth  = width
        screenHeight = height
        GLES20.glViewport(0, 0, width, height)

        if (ScreenManager.current == null) {
            ScreenManager.set(
                MainMenuScreen(context, assets, audio, batch, width, height)
            )
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt  = ((now - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.1f)
        lastFrameTime = now

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val screen = ScreenManager.current ?: return
        screen.update(dt)
        screen.render()

        if (Constants.SCANLINE_ALPHA > 0f) {
            val hm = buildHudMatrix(screenWidth.toFloat(), screenHeight.toFloat())
            scanlines.draw(batch, assets, screenWidth.toFloat(), screenHeight.toFloat(),
                hm, settings.getBoolean("glitch", true))
        }
    }

    fun onPause()  { audio.pauseBgm() }
    fun onResume() { audio.resumeBgm() }
    fun dispose()  { audio.dispose() }

    private fun buildHudMatrix(w: Float, h: Float): FloatArray {
        val m = FloatArray(16)
        val rml = w; val tmb = -h
        m[ 0] =  2f/rml; m[ 4]=0f;      m[ 8]=0f;   m[12]=-1f
        m[ 1] =  0f;      m[ 5]=2f/tmb; m[ 9]=0f;   m[13]=1f
        m[ 2] =  0f;      m[ 6]=0f;     m[10]=-1f;  m[14]=0f
        m[ 3] =  0f;      m[ 7]=0f;     m[11]=0f;   m[15]=1f
        return m
    }
}