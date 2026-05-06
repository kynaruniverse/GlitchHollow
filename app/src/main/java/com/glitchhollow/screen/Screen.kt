package com.glitchhollow.screen

/**
 * Every full-screen game state implements this interface.
 *
 * update(dt) — advance logic. dt = delta time in seconds since last frame.
 * render()   — draw everything. Called on the GL thread.
 * dispose()  — release any resources (textures, buffers) when screen is removed.
 * onTouch()  — forward touch events from the Activity.
 */
interface Screen {
    fun update(dt: Float)
    fun render()
    fun dispose()
    fun onTouch(x: Float, y: Float, action: Int) {}
}