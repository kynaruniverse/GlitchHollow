package com.glitchhollow.core

class Player(startX: Float, startY: Float) {

    var x = startX; var y = startY
    var velX = 0f;  var velY = 0f
    var onGround     = false
    var facingLeft   = false
    var lives        = Constants.PLAYER_LIVES
    var invincible   = 0   // frames remaining

    // Set by VirtualControls each frame
    var wantsLeft  = false
    var wantsRight = false
    var wantsJump  = false

    // Animation state
    enum class Anim { IDLE, RUN, JUMP, DEAD }
    var anim       = Anim.IDLE
    var animFrame  = 0
    var animTimer  = 0f   // seconds since last frame advance

    fun update(map: TileMap) {
        applyInput()
        applyGravity()
        moveX(map)
        moveY(map)
        updateAnim()
        if (invincible > 0) invincible--
    }

    private fun applyInput() {
        when {
            wantsLeft  -> { velX -= Constants.PLAYER_ACCEL; facingLeft = true  }
            wantsRight -> { velX += Constants.PLAYER_ACCEL; facingLeft = false }
            else       -> velX *= Constants.FRICTION
        }
        velX = velX.coerceIn(-Constants.PLAYER_SPEED, Constants.PLAYER_SPEED)

        if (wantsJump && onGround) {
            velY = Constants.JUMP_FORCE
            onGround  = false
            wantsJump = false
        }
    }

    private fun applyGravity() {
        velY = (velY + Constants.GRAVITY).coerceAtMost(Constants.MAX_FALL_SPEED)
    }

    private fun moveX(map: TileMap) {
        x += velX
        val ts = Constants.TILE_SIZE
        if (velX < 0) {
            val col = (x / ts).toInt()
            val t   = (y / ts).toInt()
            val b   = ((y + Constants.PLAYER_HEIGHT - 1) / ts).toInt()
            if (map.isSolid(col, t) || map.isSolid(col, b)) {
                x = (col + 1) * ts.toFloat(); velX = 0f
            }
        } else if (velX > 0) {
            val col = ((x + Constants.PLAYER_WIDTH - 1) / ts).toInt()
            val t   = (y / ts).toInt()
            val b   = ((y + Constants.PLAYER_HEIGHT - 1) / ts).toInt()
            if (map.isSolid(col, t) || map.isSolid(col, b)) {
                x = col * ts.toFloat() - Constants.PLAYER_WIDTH; velX = 0f
            }
        }
    }

    private fun moveY(map: TileMap) {
        y += velY
        val ts = Constants.TILE_SIZE
        onGround = false

        if (velY >= 0f) {
            val row  = ((y + Constants.PLAYER_HEIGHT) / ts).toInt()
            val lCol = ((x + 4) / ts).toInt()
            val rCol = ((x + Constants.PLAYER_WIDTH - 4) / ts).toInt()

            if (map.isSolid(lCol, row) || map.isSolid(rCol, row)) {
                y = row * ts.toFloat() - Constants.PLAYER_HEIGHT
                velY = 0f; onGround = true
            }
            if (!onGround && (map.isPlatform(lCol, row) || map.isPlatform(rCol, row))) {
                y = row * ts.toFloat() - Constants.PLAYER_HEIGHT
                velY = 0f; onGround = true
            }
        } else {
            val row  = (y / ts).toInt()
            val lCol = ((x + 4) / ts).toInt()
            val rCol = ((x + Constants.PLAYER_WIDTH - 4) / ts).toInt()
            if (map.isSolid(lCol, row) || map.isSolid(rCol, row)) {
                y = (row + 1) * ts.toFloat(); velY = 0f
            }
        }
    }

    private fun updateAnim() {
        val target = when {
            !onGround                    -> Anim.JUMP
            Math.abs(velX) > 0.5f        -> Anim.RUN
            else                         -> Anim.IDLE
        }
        if (target != anim) { anim = target; animFrame = 0; animTimer = 0f }

        // Advance frame — speeds vary per animation
        val fps = when (anim) { Anim.RUN -> 10f; Anim.IDLE -> 6f; else -> 8f }
        animTimer += 1f / 60f
        if (animTimer >= 1f / fps) {
            animTimer = 0f
            animFrame = (animFrame + 1) % frameCount(anim)
        }
    }

    private fun frameCount(a: Anim) = when (a) {
        Anim.IDLE -> 4; Anim.RUN -> 6; Anim.JUMP -> 2; Anim.DEAD -> 4
    }

    fun respawn(sx: Float, sy: Float) {
        x = sx; y = sy; velX = 0f; velY = 0f
        onGround = false; invincible = 120
        wantsLeft = false; wantsRight = false; wantsJump = false
        anim = Anim.IDLE; animFrame = 0; animTimer = 0f
    }

    fun getRect() = Rect2D(x + 4, y + 4,
        Constants.PLAYER_WIDTH - 8f,
        Constants.PLAYER_HEIGHT - 4f)
}