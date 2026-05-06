package com.glitchhollow.core

/**
 * Typed sound event queue — decouples game logic from audio system.
 *
 * Design: GameEngine never imports AudioManager. Instead it pushes
 * SoundEvent values into a queue each update tick. GameScreen drains
 * the queue and passes each event to AudioManager.
 *
 * This means:
 *   - Core logic has zero Android dependencies (stays testable)
 *   - Audio can be mocked/disabled without touching game logic
 *   - Events naturally batch per-frame (no double-playing)
 */
enum class SoundEvent {
    JUMP,
    LAND,
    SHARD_COLLECT,
    COIN_COLLECT,
    ENEMY_STOMP,
    PLAYER_HURT,
    PLAYER_DEATH,
    LEVEL_WIN,
    MENU_SELECT,
    MENU_BACK,
    GLITCH_PULSE     // short glitch noise on shard collect / screen transition
}