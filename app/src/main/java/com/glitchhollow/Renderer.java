package com.glitchhollow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

public class Renderer {

    private Paint paint;
    private Paint textPaint;
    private Paint hudPaint;
    private Paint glowPaint;
    private Matrix matrix;
    private SpriteSheet sprites;
    private Camera camera;

    // World background colours per world
    private static final int[] BG_COLORS = {
        Color.parseColor("#0D0A1A"),  // World 1 — Toon Lot
        Color.parseColor("#1A000D"),  // World 2 — Plushy Purgatory
        Color.parseColor("#000D1A"),  // World 3 — Signal Waste
        Color.parseColor("#0A0A00"),  // World 4 — Finale Screen
    };

    private static final int[] ACCENT_COLORS = {
        Color.parseColor("#FF3CAC"),
        Color.parseColor("#FF6B9D"),
        Color.parseColor("#00F5FF"),
        Color.parseColor("#FFE600"),
    };

    public Renderer(SpriteSheet sprites, Camera camera) {
        this.sprites = sprites;
        this.camera  = camera;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.MONOSPACE);

        hudPaint = new Paint();
        hudPaint.setAntiAlias(true);

        glowPaint = new Paint();
        glowPaint.setAntiAlias(true);

        matrix = new Matrix();
    }

    // ─── Background ──────────────────────────────────────────────

    public void drawBackground(Canvas canvas, int world, int screenW, int screenH) {
        int bgColor = BG_COLORS[Math.min(world - 1, BG_COLORS.length - 1)];
        canvas.drawColor(bgColor);

        // Subtle scanline overlay
        paint.setColor(0x08FFFFFF);
        paint.setStrokeWidth(1f);
        for (int y = Constants.HUD_HEIGHT; y < screenH; y += 4) {
            canvas.drawLine(0, y, screenW, y, paint);
        }
    }

    // ─── Tile map ────────────────────────────────────────────────

    public void drawTileMap(Canvas canvas, TileMap map, int world) {
        int accentColor = ACCENT_COLORS[Math.min(world - 1, ACCENT_COLORS.length - 1)];

        int startCol = Math.max(0, (int)(camera.x / Constants.TILE_SIZE) - 1);
        int endCol   = Math.min(map.cols, startCol + (int)(canvas.getWidth()  / Constants.TILE_SIZE) + 3);
        int startRow = Math.max(0, (int)(camera.y / Constants.TILE_SIZE) - 1);
        int endRow   = Math.min(map.rows, startRow + (int)(canvas.getHeight() / Constants.TILE_SIZE) + 3);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                int tile = map.getTile(c, r);
                if (tile == Constants.TILE_AIR) continue;

                float sx = camera.toScreenX(c * Constants.TILE_SIZE);
                float sy = camera.toScreenY(r * Constants.TILE_SIZE);

                Bitmap bmp = getTileBitmap(tile);
                if (bmp != null) {
                    canvas.drawBitmap(bmp, sx, sy, paint);
                } else {
                    // Fallback colour rect
                    paint.setColor(getFallbackColor(tile, accentColor));
                    canvas.drawRect(sx, sy,
                        sx + Constants.TILE_SIZE,
                        sy + Constants.TILE_SIZE, paint);
                }
            }
        }
    }

    private Bitmap getTileBitmap(int tile) {
        switch (tile) {
            case Constants.TILE_FLOOR:    return sprites.get(SpriteSheet.TILE_FLOOR);
            case Constants.TILE_WALL:     return sprites.get(SpriteSheet.TILE_WALL);
            case Constants.TILE_PLATFORM: return sprites.get(SpriteSheet.TILE_PLATFORM);
            case Constants.TILE_HAZARD:   return sprites.get(SpriteSheet.TILE_HAZARD);
            case Constants.TILE_EXIT:     return sprites.get(SpriteSheet.TILE_EXIT);
            default: return null;
        }
    }

    private int getFallbackColor(int tile, int accent) {
        switch (tile) {
            case Constants.TILE_FLOOR:    return Color.parseColor("#1E0035");
            case Constants.TILE_WALL:     return Color.parseColor("#0D0D1A");
            case Constants.TILE_PLATFORM: return Color.parseColor("#7B2FBE");
            case Constants.TILE_HAZARD:   return Color.parseColor("#FF2D55");
            case Constants.TILE_EXIT:     return Color.parseColor("#00F5FF");
            default: return Color.DKGRAY;
        }
    }

    // ─── Player ──────────────────────────────────────────────────

    public void drawPlayer(Canvas canvas, Player player) {
        float sx = camera.toScreenX(player.x);
        float sy = camera.toScreenY(player.y);

        Bitmap bmp;
        if (!player.onGround) {
            bmp = sprites.get(SpriteSheet.PIP_JUMP);
        } else if (Math.abs(player.velX) > 0.5f) {
            bmp = sprites.get(SpriteSheet.PIP_RUN);
        } else {
            bmp = sprites.get(SpriteSheet.PIP_IDLE);
        }

        if (bmp == null) {
            // Fallback rectangle
            paint.setColor(Color.parseColor("#FFE600"));
            canvas.drawRect(sx, sy,
                sx + Constants.PLAYER_WIDTH,
                sy + Constants.PLAYER_HEIGHT, paint);
            return;
        }

        matrix.reset();
        if (player.facingLeft) {
            // Flip horizontally
            matrix.setScale(-1, 1);
            matrix.postTranslate(sx + bmp.getWidth(), sy);
        } else {
            matrix.setTranslate(sx, sy);
        }
        canvas.drawBitmap(bmp, matrix, paint);

        // Invincibility flicker
        if (player.invincibleFrames > 0 && (player.invincibleFrames / 4) % 2 == 0) {
            paint.setColor(0x88FFFFFF);
            canvas.drawRect(sx, sy,
                sx + Constants.PLAYER_WIDTH,
                sy + Constants.PLAYER_HEIGHT, paint);
        }
    }

    // ─── Items ───────────────────────────────────────────────────

    public void drawItems(Canvas canvas, GameEngine engine) {
        // Shards
        for (int i = 0; i < engine.shardCollected.length; i++) {
            if (engine.shardCollected[i]) continue;
            float wx = engine.shardX[i];
            float wy = engine.shardY[i];
            if (!camera.isVisible(wx, wy, 32, 40)) continue;

            float sx = camera.toScreenX(wx);
            float sy = camera.toScreenY(wy);

            // Bob animation
            float bob = (float) Math.sin(engine.tick * 0.08f + i) * 4f;
            sy += bob;

            Bitmap bmp = sprites.get(SpriteSheet.ITEM_SHARD);
            if (bmp != null) {
                canvas.drawBitmap(bmp, sx, sy, paint);
            } else {
                paint.setColor(Color.parseColor("#00F5FF"));
                canvas.drawRect(sx, sy, sx + 32, sy + 40, paint);
            }

            // Glow
            glowPaint.setColor(0x2200F5FF);
            canvas.drawCircle(sx + 16, sy + 20, 24, glowPaint);
        }

        // Coins
        for (int i = 0; i < engine.coinCollected.length; i++) {
            if (engine.coinCollected[i]) continue;
            float wx = engine.coinX[i];
            float wy = engine.coinY[i];
            if (!camera.isVisible(wx, wy, 28, 28)) continue;

            float sx = camera.toScreenX(wx);
            float sy = camera.toScreenY(wy);
            float bob = (float) Math.sin(engine.tick * 0.1f + i * 1.5f) * 3f;
            sy += bob;

            Bitmap bmp = sprites.get(SpriteSheet.ITEM_COIN);
            if (bmp != null) {
                canvas.drawBitmap(bmp, sx, sy, paint);
            } else {
                paint.setColor(Color.parseColor("#FFE600"));
                canvas.drawCircle(sx + 14, sy + 14, 14, paint);
            }
        }
    }

    // ─── Enemies ─────────────────────────────────────────────────

    public void drawEnemies(Canvas canvas, java.util.List<Enemy> enemies) {
        for (Enemy e : enemies) {
            if (!camera.isVisible(e.x, e.y, e.width, e.height)) continue;

            float sx = camera.toScreenX(e.x);
            float sy = camera.toScreenY(e.y);

            Bitmap bmp = getEnemyBitmap(e.type);
            if (bmp != null) {
                matrix.reset();
                if (e.movingLeft) {
                    matrix.setScale(-1, 1);
                    matrix.postTranslate(sx + bmp.getWidth(), sy);
                } else {
                    matrix.setTranslate(sx, sy);
                }
                canvas.drawBitmap(bmp, matrix, paint);
            } else {
                paint.setColor(Color.parseColor("#FF3CAC"));
                canvas.drawRect(sx, sy, sx + e.width, sy + e.height, paint);
            }
        }
    }

    private Bitmap getEnemyBitmap(int type) {
        switch (type) {
            case Constants.ENEMY_WOBBLE:   return sprites.get(SpriteSheet.ENEMY_WOBBLE);
            case Constants.ENEMY_STITCHY:  return sprites.get(SpriteSheet.ENEMY_STITCHY);
            case Constants.ENEMY_GLITCH:   return sprites.get(SpriteSheet.ENEMY_GLITCH);
            case Constants.ENEMY_DIRECTOR: return sprites.get(SpriteSheet.ENEMY_DIRECTOR);
            default: return null;
        }
    }

    // ─── HUD ─────────────────────────────────────────────────────

    public void drawHUD(Canvas canvas, GameEngine engine, int screenW) {
        // HUD background
        hudPaint.setColor(Color.parseColor("#CC0D0D1A"));
        canvas.drawRect(0, 0, screenW, Constants.HUD_HEIGHT, hudPaint);

        // Bottom border glow
        hudPaint.setColor(Color.parseColor("#7B2FBE"));
        hudPaint.setStrokeWidth(2f);
        canvas.drawLine(0, Constants.HUD_HEIGHT, screenW, Constants.HUD_HEIGHT, hudPaint);

        // Hearts
        Bitmap heart = sprites.get(SpriteSheet.UI_HEART);
        for (int i = 0; i < engine.player.lives; i++) {
            float hx = 12 + i * 36f;
            float hy = (Constants.HUD_HEIGHT - 28) / 2f;
            if (heart != null) {
                canvas.drawBitmap(heart, hx, hy, paint);
            } else {
                paint.setColor(Color.parseColor("#FF2D55"));
                canvas.drawCircle(hx + 14, hy + 14, 12, paint);
            }
        }

        // Shard counter
        int shardsGot = countCollected(engine.shardCollected);
        int shardsTotal = engine.shardCollected.length;
        textPaint.setColor(Color.parseColor("#00F5FF"));
        textPaint.setTextSize(22f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("◆ " + shardsGot + "/" + shardsTotal,
            screenW / 2f, Constants.HUD_HEIGHT / 2f + 8, textPaint);

        // Timer
        int elapsed = (int)(engine.elapsedMs / 1000);
        int mins = elapsed / 60;
        int secs = elapsed % 60;
        textPaint.setColor(Color.parseColor("#FFE600"));
        textPaint.setTextSize(18f);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format("%02d:%02d", mins, secs),
            screenW - 12, Constants.HUD_HEIGHT / 2f + 6, textPaint);
    }

    private int countCollected(boolean[] arr) {
        int n = 0;
        for (boolean b : arr) if (b) n++;
        return n;
    }

    // ─── Overlay screens ─────────────────────────────────────────

    public void drawPauseOverlay(Canvas canvas, int screenW, int screenH) {
        paint.setColor(0xCC000000);
        canvas.drawRect(0, 0, screenW, screenH, paint);

        textPaint.setColor(Color.parseColor("#00F5FF"));
        textPaint.setTextSize(52f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PAUSED", screenW / 2f, screenH / 2f - 40, textPaint);

        textPaint.setColor(Color.parseColor("#7B2FBE"));
        textPaint.setTextSize(26f);
        canvas.drawText("tap to resume", screenW / 2f, screenH / 2f + 20, textPaint);
    }

    public void drawDeathOverlay(Canvas canvas, int screenW, int screenH) {
        paint.setColor(0xBB1A0000);
        canvas.drawRect(0, 0, screenW, screenH, paint);

        textPaint.setColor(Color.parseColor("#FF2D55"));
        textPaint.setTextSize(52f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME OVER", screenW / 2f, screenH / 2f - 40, textPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        canvas.drawText("tap to retry", screenW / 2f, screenH / 2f + 20, textPaint);
    }

    public void drawWinOverlay(Canvas canvas, int screenW, int screenH,
                                boolean[] stars, boolean coinGot) {
        paint.setColor(0xBB001A00);
        canvas.drawRect(0, 0, screenW, screenH, paint);

        textPaint.setColor(Color.parseColor("#39FF14"));
        textPaint.setTextSize(48f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LEVEL CLEAR!", screenW / 2f, screenH / 2f - 80, textPaint);

        // Stars
        Bitmap starOn  = sprites.get(SpriteSheet.UI_STAR_ON);
        Bitmap starOff = sprites.get(SpriteSheet.UI_STAR_OFF);
        float starY = screenH / 2f - 20;
        float starSpacing = 80f;
        for (int i = 0; i < 3; i++) {
            float starX = screenW / 2f - starSpacing + i * starSpacing - 16;
            Bitmap s = (i < stars.length && stars[i]) ? starOn : starOff;
            if (s != null) canvas.drawBitmap(s, starX, starY, paint);
        }

        // Coin indicator
        if (coinGot) {
            textPaint.setColor(Color.parseColor("#FFE600"));
            textPaint.setTextSize(20f);
            canvas.drawText("◈ Glitch Coin collected!", screenW / 2f,
                screenH / 2f + 60, textPaint);
        }

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f);
        canvas.drawText("tap to continue", screenW / 2f, screenH / 2f + 100, textPaint);
    }

    // ─── Glitch effect ───────────────────────────────────────────

    public void drawGlitchEffect(Canvas canvas, int screenW, int screenH, int frame) {
        if (frame <= 0) return;
        // Horizontal offset strips
        paint.setColor(0x22FF3CAC);
        int strips = 6;
        for (int i = 0; i < strips; i++) {
            float y = (screenH / (float) strips) * i;
            float offset = (frame % 2 == 0) ? 8f : -8f;
            canvas.drawRect(offset, y, screenW + offset,
                y + screenH / (float) strips / 2f, paint);
        }
    }
}