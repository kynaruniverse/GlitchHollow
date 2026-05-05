package com.glitchhollow;

public class Enemy {

    public int type;
    public float x, y;
    public float velX;
    public int width, height;
    public boolean movingLeft = false;
    public boolean dead = false;

    private float patrolLeft, patrolRight;
    private float speed = Constants.ENEMY_SPEED;

    public Enemy(int type, float startX, float startY, float patrolRange) {
        this.type = type;
        this.x = startX;
        this.y = startY;
        patrolLeft  = startX;
        patrolRight = startX + patrolRange;
        velX = speed;

        // Size by type
        switch (type) {
            case Constants.ENEMY_DIRECTOR:
                width = 64; height = 64; break;
            case Constants.ENEMY_GLITCH:
            case Constants.ENEMY_STITCHY:
                width = 48; height = 56; break;
            default:
                width = 48; height = 48;
        }
    }

    public void update(TileMap map) {
        if (dead) return;

        x += velX;

        // Patrol bounce
        if (x <= patrolLeft) {
            x = patrolLeft;
            velX = speed;
            movingLeft = false;
        } else if (x + width >= patrolRight) {
            x = patrolRight - width;
            velX = -speed;
            movingLeft = true;
        }

        // Simple gravity snap
        int ts = Constants.TILE_SIZE;
        int botRow   = (int)((y + height + 2) / ts);
        int centerCol = (int)((x + width / 2f) / ts);
        if (!map.isSolid(centerCol, botRow)) {
            y += Constants.GRAVITY * 8; // standardized fall
        } else {
            y = botRow * ts - height;
        }
    }
}