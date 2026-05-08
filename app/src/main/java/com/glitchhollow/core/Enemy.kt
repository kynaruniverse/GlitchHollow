package com.glitchhollow.core

class Enemy(
    val type:       Int,
    var x:          Float,
    var y:          Float,
    val patrolDist: Float   // pixels
) {
    val width:  Float
    val height: Float

    var velX      = -Constants.ENEMY_SPEED
    var movingLeft = true
    var dead       = false

    // Animation
    var animFrame  = 0
    var animTimer  = 0f

    private val originX = x   // patrol start point

    init {
        // Size varies per enemy type — matches spritesheet frames
        val size = when (type) {
            Constants.ENEMY_DIRECTOR -> 64f to 64f
            Constants.ENEMY_STITCHY,
            Constants.ENEMY_GLITCH   -> 48f to 56f
            else                      -> 48f to 48f   // WOBBLE
        }
        width = size.first; height = size.second
    }

    private var _lastDt = 1f / 60f

    fun update(map: TileMap, dt: Float = 1f / 60f) {
        if (dead) return
        _lastDt = dt

        x += velX

        // Reverse at patrol edges
        if (x <= originX - patrolDist || x >= originX) {
            velX      = -velX
            movingLeft = velX < 0
        }

        // // Advance animation at 8 fps
        animTimer += _lastDt
        if (animTimer >= 1f / 8f) {
            animTimer  = 0f
            animFrame  = (animFrame + 1) % 4
        }
    }

    private val _rect = Rect2D(0f, 0f, 0f, 0f)

    fun getRect(): Rect2D {
        _rect.set(x + 4f, y + 4f, width - 8f, height - 8f)
        return _rect
    }
}