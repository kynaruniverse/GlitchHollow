package com.glitchhollow;

public class Camera {

    public float x, y;
    private float targetX, targetY;
    private int screenWidth, screenHeight;
    private int levelPixelWidth, levelPixelHeight;

    // Screen shake
    private int shakeFrames = 0;
    private float shakeIntensity = 0;

    public Camera(int screenWidth, int screenHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void setLevelSize(int cols, int rows) {
        levelPixelWidth  = cols * Constants.TILE_SIZE;
        levelPixelHeight = rows * Constants.TILE_SIZE;
    }

    public void update(float playerCX, float playerCY) {
        // Target: centre player on screen, accounting for HUD
        int hudH = Constants.HUD_HEIGHT;
        targetX = playerCX - screenWidth  / 2f;
        targetY = playerCY - (screenHeight - hudH) / 2f - hudH;

        // Clamp to level bounds
        targetX = Math.max(0, Math.min(targetX, levelPixelWidth  - screenWidth));
        targetY = Math.max(0, Math.min(targetY, levelPixelHeight - screenHeight + hudH));

        // Smooth follow
        x += (targetX - x) * Constants.CAM_LERP;
        y += (targetY - y) * Constants.CAM_LERP;

        // Shake
        if (shakeFrames > 0) {
            float off = shakeIntensity * ((shakeFrames % 2 == 0) ? 1 : -1);
            x += off;
            y += off * 0.5f;
            shakeFrames--;
        }
    }

    public void shake(float intensity, int frames) {
        shakeIntensity = intensity;
        shakeFrames    = frames;
    }

    /** Convert world X to screen X */
    public float toScreenX(float worldX) { return worldX - x; }

    /** Convert world Y to screen Y */
    public float toScreenY(float worldY) { return worldY - y + Constants.HUD_HEIGHT; }

    /** Is a world rect visible on screen? */
    public boolean isVisible(float wx, float wy, float w, float h) {
        return wx + w > x && wx < x + screenWidth
            && wy + h > y && wy < y + screenHeight;
    }
}