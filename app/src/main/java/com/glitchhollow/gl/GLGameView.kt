package com.glitchhollow.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.glitchhollow.screen.ScreenManager

/**
 * The GLSurfaceView subclass.
 *
 * Responsibilities:
 *   1. Own the GLRenderer
 *   2. Forward touch events to the current Screen (on the GL thread,
 *      so the screen doesn't need synchronisation)
 */
class GLGameView(context: Context) : GLSurfaceView(context) {

    val renderer: GLRenderer = GLRenderer(context)

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val x = event.getX(event.actionIndex)
        val y = event.getY(event.actionIndex)

        // queueEvent posts to the GL thread — safe to call GL/game state here
        queueEvent {
            ScreenManager.current?.onTouch(x, y, action)
        }
        return true
    }
}