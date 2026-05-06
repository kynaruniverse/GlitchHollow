package com.glitchhollow.core

/** Axis-aligned bounding box for collision detection. Pure math, no Android deps. */
data class Rect2D(var x: Float, var y: Float, var w: Float, var h: Float) {

    fun set(x: Float, y: Float, w: Float, h: Float) {
        this.x = x; this.y = y; this.w = w; this.h = h
    }

    fun intersects(other: Rect2D): Boolean =
        x < other.x + other.w && x + w > other.x &&
        y < other.y + other.h && y + h > other.y

    val right  get() = x + w
    val bottom get() = y + h
}