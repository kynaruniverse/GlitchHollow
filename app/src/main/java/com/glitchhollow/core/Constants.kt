package com.glitchhollow.core

object Constants {
    // Tile
    const val TILE_SIZE       = 48
    const val TILE_AIR        = 0
    const val TILE_FLOOR      = 1
    const val TILE_WALL       = 2
    const val TILE_PLATFORM   = 3
    const val TILE_HAZARD     = 4
    const val TILE_EXIT       = 5

    // Items
    const val ITEM_SHARD      = 10
    const val ITEM_COIN       = 11

    // Enemies
    const val ENEMY_WOBBLE    = 20
    const val ENEMY_STITCHY   = 21
    const val ENEMY_GLITCH    = 22
    const val ENEMY_DIRECTOR  = 23

    // Physics
    const val GRAVITY         = 0.55f
    const val MAX_FALL_SPEED  = 18f
    const val JUMP_FORCE      = -14f
    const val PLAYER_SPEED    = 5f
    const val PLAYER_ACCEL    = 1.8f
    const val FRICTION        = 0.78f
    const val ENEMY_SPEED     = 2.2f

    // Player
    const val PLAYER_WIDTH    = 36
    const val PLAYER_HEIGHT   = 52
    const val PLAYER_LIVES    = 3

    // Stars
    const val STAR_TIME_BONUS = 60   // seconds — finish within this for star 2
    const val STAR_FAST_TIME  = 30

    // Camera
    const val CAM_LERP        = 0.12f

    // Game states
    const val STATE_PLAYING   = 0
    const val STATE_PAUSED    = 1
    const val STATE_WIN       = 2
    const val STATE_DEAD      = 3
    const val STATE_GAMEOVER  = 4

    // Gesture
    const val SWIPE_THRESHOLD = 40   // px
    const val SWIPE_MAX_TIME  = 300L // ms

    // HUD
    const val HUD_HEIGHT      = 60
    const val GLITCH_DURATION = 8    // frames
}