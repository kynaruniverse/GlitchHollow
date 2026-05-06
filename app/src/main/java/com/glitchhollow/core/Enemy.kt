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

    fun update(map: TileMap) {
        if (dead) return

        x += velX

        // Reverse at patrol edges
        if (x <= originX - patrolDist || x >= originX) {
            velX      = -velX
            movingLeft = velX < 0
        }

        // Advance animation at 8 fps
        animTimer += 1f / 60f
        if (animTimer >= 1f / 8f) {
            animTimer  = 0f
            animFrame  = (animFrame + 1) % 4
        }
    }

    fun getRect() = Rect2D(x + 4, y + 4, width - 8f, height - 8f)
}