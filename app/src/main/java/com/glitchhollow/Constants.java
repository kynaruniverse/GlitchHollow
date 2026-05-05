package com.glitchhollow;

public final class Constants {

    private Constants() {}

    // Tile
    public static final int TILE_SIZE       = 48;   // pixels at base resolution
    public static final int TILE_AIR        = 0;
    public static final int TILE_FLOOR      = 1;
    public static final int TILE_WALL       = 2;
    public static final int TILE_PLATFORM   = 3;
    public static final int TILE_HAZARD     = 4;
    public static final int TILE_EXIT       = 5;

    // Items
    public static final int ITEM_SHARD      = 10;
    public static final int ITEM_COIN       = 11;

    // Enemies
    public static final int ENEMY_WOBBLE    = 20;
    public static final int ENEMY_STITCHY   = 21;
    public static final int ENEMY_GLITCH    = 22;
    public static final int ENEMY_DIRECTOR  = 23;

    // Physics
    public static final float GRAVITY           = 0.55f;
    public static final float MAX_FALL_SPEED    = 18f;
    public static final float JUMP_FORCE        = -14f;
    public static final float PLAYER_SPEED      = 5f;
    public static final float FRICTION          = 0.78f;

    // Player
    public static final int PLAYER_WIDTH    = 36;
    public static final int PLAYER_HEIGHT   = 52;
    public static final int PLAYER_LIVES    = 3;
    public static final int SHARDS_PER_LEVEL = 3;

    // Stars
    public static final int STAR_TIME_BONUS = 60;   // seconds for 2nd star
    public static final int STAR_FAST_TIME  = 30;   // seconds for time bonus

    // Camera
    public static final float CAM_LERP     = 0.12f;

    // Game states
    public static final int STATE_PLAYING   = 0;
    public static final int STATE_PAUSED    = 1;
    public static final int STATE_WIN       = 2;
    public static final int STATE_DEAD      = 3;
    public static final int STATE_GAMEOVER  = 4;

    // Swipe
    public static final int SWIPE_THRESHOLD = 40;  // px
    public static final long SWIPE_MAX_TIME = 300;  // ms

    // HUD
    public static final int HUD_HEIGHT      = 0; // drawn relative to screen, set dynamically
    public static final int HUD_HEIGHT_DP   = 56; // design height in dp, resolved at runtime

    // Glitch effect
    public static final int GLITCH_DURATION = 8;   // frames
}