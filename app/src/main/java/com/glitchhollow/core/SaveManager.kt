package com.glitchhollow.core

import android.content.Context
import android.content.SharedPreferences

class SaveManager(context: Context) {

    companion object {
        const val TOTAL_WORLDS     = 4
        const val LEVELS_PER_WORLD = 6
        private const val PREFS_NAME = "glitch_hollow_save"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Stars ─────────────────────────────────────────────────────

    fun saveStars(world: Int, level: Int, stars: BooleanArray) {
        val key = "stars_w${world}_l${level}"
        val packed = stars.foldIndexed(0) { i, acc, v -> if (v) acc or (1 shl i) else acc }
        prefs.edit().putInt(key, packed).apply()
    }

    fun getStars(world: Int, level: Int): BooleanArray {
        val key = "stars_w${world}_l${level}"
        val packed = prefs.getInt(key, 0)
        return BooleanArray(3) { i -> packed and (1 shl i) != 0 }
    }

    fun totalStars(): Int {
        var total = 0
        for (w in 1..TOTAL_WORLDS)
            for (l in 1..LEVELS_PER_WORLD)
                total += getStars(w, l).count { it }
        return total
    }

    fun maxStars() = TOTAL_WORLDS * LEVELS_PER_WORLD * 3

    // ── Coins ─────────────────────────────────────────────────────

    fun saveCoin(world: Int, level: Int, got: Boolean) {
        prefs.edit().putBoolean("coin_w${world}_l${level}", got).apply()
    }

    fun getCoin(world: Int, level: Int): Boolean =
        prefs.getBoolean("coin_w${world}_l${level}", false)

    // ── Progress ──────────────────────────────────────────────────

    fun onLevelComplete(world: Int, level: Int) {
        val next = if (level < LEVELS_PER_WORLD) level + 1 else 1
        val nextWorld = if (level < LEVELS_PER_WORLD) world else world + 1
        if (nextWorld <= TOTAL_WORLDS) {
            val key = "unlocked_w${nextWorld}_l${next}"
            prefs.edit().putBoolean(key, true).apply()
        }
        // Save furthest point reached
        val currentFurthest = prefs.getInt("furthest_world", 1) * 100 +
                              prefs.getInt("furthest_level", 1)
        val thisPoint = world * 100 + level
        if (thisPoint >= currentFurthest) {
            prefs.edit()
                .putInt("furthest_world", if (level < LEVELS_PER_WORLD) world else world + 1)
                .putInt("furthest_level", next)
                .apply()
        }
    }

    fun isUnlocked(world: Int, level: Int): Boolean {
        if (world == 1 && level == 1) return true
        return prefs.getBoolean("unlocked_w${world}_l${level}", false)
    }

    fun getResumePoint(): IntArray {
        val w = prefs.getInt("furthest_world", 1)
        val l = prefs.getInt("furthest_level", 1)
        return intArrayOf(w, l)
    }

    // ── Reset ─────────────────────────────────────────────────────

    fun resetAll() = prefs.edit().clear().apply()
}