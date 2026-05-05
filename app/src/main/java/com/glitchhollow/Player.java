package com.glitchhollow;

public class Player {

    public float x, y;
    public float velX, velY;
    public boolean onGround  = false;
    public boolean facingLeft = false;
    public int lives;
    public int invincibleFrames = 0;

    // Input flags set by InputHandler
    public boolean wantsLeft  = false;
    public boolean wantsRight = false;
    public boolean wantsJump  = false;

    public Player(float startX, float startY) {
        x = startX;
        y = startY;
        lives = Constants.PLAYER_LIVES;
    }

    public void update(TileMap map) {
        // Horizontal input
        if (wantsLeft) {
            velX -= Constants.PLAYER_ACCEL;
            facingLeft = true;
        } else if (wantsRight) {
            velX += Constants.PLAYER_ACCEL;
            facingLeft = false;
        } else {
            velX *= Constants.FRICTION;
        }

        // Clamp speed
        velX = Math.max(-Constants.PLAYER_SPEED, Math.min(Constants.PLAYER_SPEED, velX));

        // Jump
        if (wantsJump && onGround) {
            velY = Constants.JUMP_FORCE;
            onGround = false;
            wantsJump = false;
        }

        // Gravity
        velY += Constants.GRAVITY;
        if (velY > Constants.MAX_FALL_SPEED) velY = Constants.MAX_FALL_SPEED;

        // Move and collide
        moveX(map);
        moveY(map);

        // Invincibility countdown
        if (invincibleFrames > 0) invincibleFrames--;
    }

    private void moveX(TileMap map) {
        x += velX;
        int ts = Constants.TILE_SIZE;

        // Check left side
        if (velX < 0) {
            int leftCol = (int)(x / ts);
            int topRow  = (int)(y / ts);
            int botRow  = (int)((y + Constants.PLAYER_HEIGHT - 1) / ts);
            if (map.isSolid(leftCol, topRow) || map.isSolid(leftCol, botRow)) {
                x = (leftCol + 1) * ts;
                velX = 0;
            }
        }
        // Check right side
        if (velX > 0) {
            int rightCol = (int)((x + Constants.PLAYER_WIDTH - 1) / ts);
            int topRow   = (int)(y / ts);
            int botRow   = (int)((y + Constants.PLAYER_HEIGHT - 1) / ts);
            if (map.isSolid(rightCol, topRow) || map.isSolid(rightCol, botRow)) {
                x = rightCol * ts - Constants.PLAYER_WIDTH;
                velX = 0;
            }
        }
    }

    private void moveY(TileMap map) {
        y += velY;
        int ts = Constants.TILE_SIZE;
        onGround = false;

        // Falling — check bottom
        if (velY >= 0) {
            int botRow  = (int)((y + Constants.PLAYER_HEIGHT) / ts);
            int leftCol = (int)((x + 4) / ts);
            int rightCol= (int)((x + Constants.PLAYER_WIDTH - 4) / ts);

            if (map.isSolid(leftCol, botRow) || map.isSolid(rightCol, botRow)) {
                y = botRow * ts - Constants.PLAYER_HEIGHT;
                velY = 0;
                onGround = true;
            }
            // Platform — only land if falling onto top
            if (!onGround) {
                if (map.isPlatform(leftCol, botRow) || map.isPlatform(rightCol, botRow)) {
                    y = botRow * ts - Constants.PLAYER_HEIGHT;
                    velY = 0;
                    onGround = true;
                }
            }
        }
        // Rising — check top
        if (velY < 0) {
            int topRow  = (int)(y / ts);
            int leftCol = (int)((x + 4) / ts);
            int rightCol= (int)((x + Constants.PLAYER_WIDTH - 4) / ts);
            if (map.isSolid(leftCol, topRow) || map.isSolid(rightCol, topRow)) {
                y = (topRow + 1) * ts;
                velY = 0;
            }
        }
    }

    public void respawn(float sx, float sy) {
        x = sx; y = sy;
        velX = 0; velY = 0;
        onGround = false;
        invincibleFrames = 120; // 2 seconds invincible
        wantsLeft = false;
        wantsRight = false;
        wantsJump = false;
    }

    public Rect2D getRect() {
        return new Rect2D(x + 4, y + 4,
            Constants.PLAYER_WIDTH - 8,
            Constants.PLAYER_HEIGHT - 4);
    }
}