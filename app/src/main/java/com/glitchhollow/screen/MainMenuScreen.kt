package com.glitchhollow.screen

import android.content.Context
import android.opengl.GLES20
import android.view.MotionEvent
import com.glitchhollow.core.SaveManager
import com.glitchhollow.gl.AssetManager
import com.glitchhollow.gl.SpriteBatch

class MainMenuScreen(
    private val context: Context,
    private val assets:  AssetManager,
    private val batch:   SpriteBatch,
    private val screenW: Int,
    private val screenH: Int
) : Screen {

    override fun update(dt: Float) {}

    override fun render() {
        // Dark background — Phase 4 adds animated parallax here
        GLES20.glClearColor(0.051f, 0.039f, 0.102f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Draw a simple "tap to play" indicator using a coloured rect
        val atlas = assets.atlas ?: return
        batch.begin(buildHudMatrix(screenW, screenH))

        // Thin cyan line across the middle as a "press start" placeholder
        batch.draw(atlas,
            screenW * 0.2f, screenH * 0.5f - 2f,
            screenW * 0.6f, 4f,
            0f, 0f, 0.001f, 0.001f,
            0f, 0.96f, 1f, 0.8f)

        batch.end()
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_DOWN) {
            val save  = SaveManager(context)
            val point = save.getResumePoint()
            ScreenManager.set(
                GameScreen(context, assets, batch, point[0], point[1], screenW, screenH)
            )
        }
    }

    override fun dispose() {}

    private fun buildHudMatrix(w: Int, h: Int): FloatArray {
        val m = FloatArray(16)
        val rml = w.toFloat(); val tmb = -h.toFloat()
        m[ 0] =  2f/rml;  m[ 4]=0f;        m[ 8]=0f;   m[12]=-1f
        m[ 1] =  0f;       m[ 5]=2f/tmb;   m[ 9]=0f;   m[13]=1f
        m[ 2] =  0f;       m[ 6]=0f;       m[10]=-1f;  m[14]=0f
        m[ 3] =  0f;       m[ 7]=0f;       m[11]=0f;   m[15]=1f
        return m
    }
}