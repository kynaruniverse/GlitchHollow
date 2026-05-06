package com.glitchhollow.screen

/**
 * Manages which Screen is currently active.
 *
 * Usage:
 *   ScreenManager.set(MainMenuScreen(...))   // replace current screen
 *   ScreenManager.current?.update(dt)
 *   ScreenManager.current?.render()
 *
 * Thread note: set() is called from the UI thread (touch events, Activity lifecycle).
 * update() and render() are called from the GL thread.
 * @Volatile ensures the GL thread always sees the latest screen reference.
 */
object ScreenManager {

    @Volatile
    var current: Screen? = null
        private set

    fun set(screen: Screen) {
        current?.dispose()
        current = screen
    }

    fun dispose() {
        current?.dispose()
        current = null
    }
}