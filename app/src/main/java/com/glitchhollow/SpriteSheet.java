package com.glitchhollow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.Map;

public class SpriteSheet {

    private Map<Integer, Bitmap> cache = new HashMap<>();
    private Context context;

    // Sprite IDs
    public static final int PIP_IDLE       = 0;
    public static final int PIP_RUN        = 1;
    public static final int PIP_JUMP       = 2;
    public static final int TILE_FLOOR     = 10;
    public static final int TILE_WALL      = 11;
    public static final int TILE_PLATFORM  = 12;
    public static final int TILE_HAZARD    = 13;
    public static final int TILE_EXIT      = 14;
    public static final int ENEMY_WOBBLE   = 20;
    public static final int ENEMY_STITCHY  = 21;
    public static final int ENEMY_GLITCH   = 22;
    public static final int ENEMY_DIRECTOR = 23;
    public static final int ITEM_SHARD     = 30;
    public static final int ITEM_COIN      = 31;
    public static final int UI_STAR_ON     = 40;
    public static final int UI_STAR_OFF    = 41;
    public static final int UI_HEART       = 42;

    public SpriteSheet(Context context) {
        this.context = context;
        preload();
    }

    private void preload() {
        loadSprite(PIP_IDLE,       R.drawable.pip_idle,       48, 64);
        loadSprite(PIP_RUN,        R.drawable.pip_run,        48, 64);
        loadSprite(PIP_JUMP,       R.drawable.pip_jump,       48, 64);
        loadSprite(TILE_FLOOR,     R.drawable.tile_floor,     48, 48);
        loadSprite(TILE_WALL,      R.drawable.tile_wall,      48, 48);
        loadSprite(TILE_PLATFORM,  R.drawable.tile_platform,  96, 24);
        loadSprite(TILE_HAZARD,    R.drawable.tile_hazard,    48, 48);
        loadSprite(TILE_EXIT,      R.drawable.tile_exit,      48, 64);
        loadSprite(ENEMY_WOBBLE,   R.drawable.enemy_wobble,   48, 48);
        loadSprite(ENEMY_STITCHY,  R.drawable.enemy_stitchy,  48, 56);
        loadSprite(ENEMY_GLITCH,   R.drawable.enemy_glitch,   48, 56);
        loadSprite(ENEMY_DIRECTOR, R.drawable.enemy_director, 64, 64);
        loadSprite(ITEM_SHARD,     R.drawable.item_shard,     32, 40);
        loadSprite(ITEM_COIN,      R.drawable.item_coin,      28, 28);
        loadSprite(UI_STAR_ON,     R.drawable.ui_star_filled, 32, 32);
        loadSprite(UI_STAR_OFF,    R.drawable.ui_star_empty,  32, 32);
        loadSprite(UI_HEART,       R.drawable.ui_heart,       28, 28);
    }

    private void loadSprite(int id, int resId, int w, int h) {
        try {
            Drawable d = ContextCompat.getDrawable(context, resId);
            if (d == null) return;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, w, h);
            d.draw(c);
            cache.put(id, bmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Bitmap get(int id) {
        return cache.get(id);
    }

    public void recycle() {
        for (Bitmap b : cache.values()) {
            if (b != null && !b.isRecycled()) b.recycle();
        }
        cache.clear();
    }
}