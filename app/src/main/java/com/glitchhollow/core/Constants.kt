package com.glitchhollow.core

object Constants {
    // ── Existing values (unchanged) ───────────────────────────────
    const val TILE_SIZE       = 48
    const val TILE_AIR        = 0
    const val TILE_FLOOR      = 1
    const val TILE_WALL       = 2
    const val TILE_PLATFORM   = 3
    const val TILE_HAZARD     = 4
    const val TILE_EXIT       = 5
    const val ITEM_SHARD      = 10
    const val ITEM_COIN       = 11
    const val ENEMY_WOBBLE    = 20
    const val ENEMY_STITCHY   = 21
    const val ENEMY_GLITCH    = 22
    const val ENEMY_DIRECTOR  = 23
    const val GRAVITY         = 0.55f
    const val MAX_FALL_SPEED  = 18f
    const val JUMP_FORCE      = -14f
    const val PLAYER_SPEED    = 5f
    const val PLAYER_ACCEL    = 1.8f
    const val FRICTION        = 0.78f
    const val ENEMY_SPEED     = 2.2f
    const val PLAYER_WIDTH    = 36
    const val PLAYER_HEIGHT   = 52
    const val PLAYER_LIVES    = 3
    const val STAR_TIME_BONUS = 60
    const val STAR_FAST_TIME  = 30
    const val CAM_LERP        = 0.12f
    const val STATE_PLAYING   = 0
    const val STATE_PAUSED    = 1
    const val STATE_WIN       = 2
    const val STATE_DEAD      = 3
    const val STATE_GAMEOVER  = 4
    const val SWIPE_THRESHOLD = 40
    const val SWIPE_MAX_TIME  = 300L
    const val HUD_HEIGHT      = 60
    const val GLITCH_DURATION = 8

    // ── Phase 5 additions ─────────────────────────────────────────

    // Particles
    const val PARTICLE_POOL_SIZE    = 256   // max live particles at once
    const val SHARD_BURST_COUNT     = 12    // particles per shard pickup
    const val COIN_BURST_COUNT      = 8
    const val STOMP_DUST_COUNT      = 6
    const val DEATH_BURST_COUNT     = 20
    const val PARTICLE_GRAVITY      = 0.18f
    const val PARTICLE_DRAG         = 0.92f // velocity multiplied each frame

    // Glow
    const val GLOW_SHARD_INTENSITY  = 0.85f
    const val GLOW_COIN_INTENSITY   = 0.65f
    const val GLOW_EXIT_INTENSITY   = 0.55f
    const val GLOW_PLAYER_INTENSITY = 0.30f // subtle — just edge light
    const val GLOW_SCALE            = 1.6f  // how much bigger the glow quad is

    // Scanlines
    const val SCANLINE_ALPHA        = 0.08f // keep subtle — 0 to disable
    const val VIGNETTE_ALPHA        = 0.35f

    // Screen shake
    const val SHAKE_SHARD           = 2f    // px intensity
    const val SHAKE_DEATH           = 6f
    const val SHAKE_STOMP           = 3f
    const val SHAKE_FRAMES_SHORT    = 6
    const val SHAKE_FRAMES_LONG     = 14
}