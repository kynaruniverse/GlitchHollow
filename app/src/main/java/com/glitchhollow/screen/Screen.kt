package com.glitchhollow.screen

/**
 * Every full-screen game state implements this interface.
 *
 * Threading model:
 * - update() runs on game loop thread
 * - render() runs on OpenGL (GLSurfaceView) thread
 * - onTouch() runs on Android UI thread
 */
interface Screen {
    fun update(dt: Float)
    fun render()
    fun dispose()
    fun onTouch(x: Float, y: Float, action: Int) {}
}