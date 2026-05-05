package com.glitchhollow;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SaveManager — persists all player progress to SharedPreferences.
 *
 * Keys used:
 *   unlocked_world   int  (1–4)
 *   unlocked_level   int  (1–6)
 *   stars_wX_lY      int  (0–3, bitmask: bit0=finish, bit1=time, bit2=coin)
 *   coin_wX_lY       bool (hidden coin collected at least once)
 *
 * "Unlocked" means the player may START that level.
 * Level w1l1 is always unlocked; every other level unlocks when the
 * previous one is completed (star 0 earned).
 */
public class SaveManager {

    private static final String PREFS_NAME = "glitch_hollow_save";

    // Keys
    private static final String KEY_UNLOCKED_WORLD = "unlocked_world";
    private static final String KEY_UNLOCKED_LEVEL = "unlocked_level";

    // Totals
    public static final int TOTAL_WORLDS = 4;
    public static final int LEVELS_PER_WORLD = 6;

    private final SharedPreferences prefs;

    public SaveManager(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Unlock tracking ──────────────────────────────────────────

    /** Returns the furthest world the player has reached (1-based). */
    public int getUnlockedWorld() {
        return prefs.getInt(KEY_UNLOCKED_WORLD, 1);
    }

    /** Returns the furthest level inside getUnlockedWorld() (1-based). */
    public int getUnlockedLevel() {
        return prefs.getInt(KEY_UNLOCKED_LEVEL, 1);
    }

    /**
     * Returns true if the given world/level is accessible.
     * w1l1 is always unlocked. Otherwise the player must have
     * reached at least that world+level.
     */
    public boolean isUnlocked(int world, int level) {
        if (world == 1 && level == 1) return true;
        int uw = getUnlockedWorld();
        int ul = getUnlockedLevel();
        if (world < uw) return true;
        if (world == uw) return level <= ul;
        return false;
    }

    /**
     * Called when a level is completed. Advances the unlock pointer
     * to the next level (or next world) if this is further than before.
     */
    public void onLevelComplete(int world, int level) {
        int uw = getUnlockedWorld();
        int ul = getUnlockedLevel();

        // Only advance if this is right at the frontier
        boolean atFrontier = (world == uw && level == ul);
        boolean justBeyond = (world == uw && level > ul)
                          || (world > uw);

        if (!atFrontier && !justBeyond) return; // replaying an old level

        int nextWorld = world;
        int nextLevel = level + 1;

        if (nextLevel > LEVELS_PER_WORLD) {
            nextWorld++;
            nextLevel = 1;
        }

        // Cap at final level
        if (nextWorld > TOTAL_WORLDS) {
            nextWorld = TOTAL_WORLDS;
            nextLevel = LEVELS_PER_WORLD;
        }

        // Only write if it's actually an advance
        if (nextWorld > uw || (nextWorld == uw && nextLevel > ul)) {
            prefs.edit()
                 .putInt(KEY_UNLOCKED_WORLD, nextWorld)
                 .putInt(KEY_UNLOCKED_LEVEL, nextLevel)
                 .apply();
        }
    }

    // ── Stars ────────────────────────────────────────────────────

    /**
     * Returns star bitmask for a level (0–7).
     * Bit 0 = completed, Bit 1 = time bonus, Bit 2 = coin collected.
     */
    public int getStars(int world, int level) {
        return prefs.getInt(starKey(world, level), 0);
    }

    /** Returns count of stars earned (0–3). */
    public int getStarCount(int world, int level) {
        return Integer.bitCount(getStars(world, level));
    }

    /**
     * Saves stars for a level, keeping the best result.
     * starsEarned[] maps to bits 0,1,2.
     */
    public void saveStars(int world, int level, boolean[] starsEarned) {
        int newBits = 0;
        for (int i = 0; i < starsEarned.length && i < 3; i++) {
            if (starsEarned[i]) newBits |= (1 << i);
        }
        int existing = getStars(world, level);
        int merged   = existing | newBits; // keep best
        prefs.edit().putInt(starKey(world, level), merged).apply();
    }

    private String starKey(int world, int level) {
        return "stars_w" + world + "_l" + level;
    }

    // ── Coin ─────────────────────────────────────────────────────

    public boolean isCoinCollected(int world, int level) {
        return prefs.getBoolean(coinKey(world, level), false);
    }

    public void saveCoin(int world, int level, boolean got) {
        if (got) {
            prefs.edit().putBoolean(coinKey(world, level), true).apply();
        }
    }

    private String coinKey(int world, int level) {
        return "coin_w" + world + "_l" + level;
    }

    // ── Aggregate helpers ─────────────────────────────────────────

    /** Total stars collected across all levels. */
    public int totalStars() {
        int total = 0;
        for (int w = 1; w <= TOTAL_WORLDS; w++) {
            for (int l = 1; l <= LEVELS_PER_WORLD; l++) {
                total += getStarCount(w, l);
            }
        }
        return total;
    }

    /** Total possible stars (3 × 24 = 72). */
    public int maxStars() {
        return TOTAL_WORLDS * LEVELS_PER_WORLD * 3;
    }

    // ── Resume point ──────────────────────────────────────────────

    /**
     * Returns {world, level} of the last unlocked (furthest) level,
     * i.e. where PLAY should resume.
     */
    public int[] getResumePoint() {
        return new int[]{ getUnlockedWorld(), getUnlockedLevel() };
    }

    // ── Reset ────────────────────────────────────────────────────

    /** Wipes all save data (for debug / settings screen). */
    public void resetAll() {
        prefs.edit().clear().apply();
    }
}
