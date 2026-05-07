package com.glitchhollow.screen

object ScreenManager {

    @Volatile
    var current: Screen? = null
        private set

    fun set(screen: Screen) {
        val old = current
        current = screen
        old?.dispose()
    }

    fun dispose() {
        current?.dispose()
        current = null
    }

    fun reset() {
        current?.dispose()
        current = null
    }
}