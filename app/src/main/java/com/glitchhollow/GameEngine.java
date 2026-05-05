package com.glitchhollow;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class GameEngine {

    // Core refs
    public Context context;
    public Player player;
    public TileMap tileMap;
    public Camera camera;
    public List<Enemy> enemies;

    // Level meta
    public int world, level;
    public String levelName;

    // Items
    public float[] shardX, shardY;
    public boolean[] shardCollected;
    public float[] coinX, coinY;
    public boolean[] coinCollected;
    public boolean coinGotThisRun = false;

    // State
    public int gameState = Constants.STATE_PLAYING;
    public long elapsedMs = 0;
    public int tick = 0;
    public int glitchFrames = 0;

    // Stars
    public boolean[] starsEarned = new boolean[3];

    // Spawn points
    private float spawnX, spawnY;

    private LevelLoader loader;
    private final Rect2D collisionCache = new Rect2D(0, 0, 0, 0);

    public GameEngine(Context context, int world, int level) {
        this.context = context;
        this.world   = world;
        this.level   = level;
        loader = new LevelLoader(context);
        loadLevel(world, level);
    }

    private void loadLevel(int w, int l) {
        LevelData data = loader.load(w, l);
        levelName = data.name;

        // Build tile map
        tileMap = new TileMap(data.tiles, data.rows, data.cols, w);

        // Player
        spawnX = data.playerStartX * Constants.TILE_SIZE;
        spawnY = data.playerStartY * Constants.TILE_SIZE;
        player = new Player(spawnX, spawnY);

        // Items
        int sc = data.shards.size();
        shardX = new float[sc]; shardY = new float[sc];
        shardCollected = new boolean[sc];
        for (int i = 0; i < sc; i++) {
            shardX[i] = data.shards.get(i)[0] * Constants.TILE_SIZE
                        + (Constants.TILE_SIZE - 32) / 2f;
            shardY[i] = data.shards.get(i)[1] * Constants.TILE_SIZE
                        + (Constants.TILE_SIZE - 40) / 2f;
        }

        int cc = data.coins.size();
        coinX = new float[cc]; coinY = new float[cc];
        coinCollected = new boolean[cc];
        for (int i = 0; i < cc; i++) {
            coinX[i] = data.coins.get(i)[0] * Constants.TILE_SIZE
                       + (Constants.TILE_SIZE - 28) / 2f;
            coinY[i] = data.coins.get(i)[1] * Constants.TILE_SIZE
                       + (Constants.TILE_SIZE - 28) / 2f;
        }

        // Enemies
        enemies = new ArrayList<>();
        for (LevelData.EnemyData ed : data.enemies) {
            enemies.add(new Enemy(ed.type,
                ed.startCol * Constants.TILE_SIZE,
                ed.startRow * Constants.TILE_SIZE,
                ed.patrolCols * Constants.TILE_SIZE));
        }

        // Reset state
        gameState    = Constants.STATE_PLAYING;
        elapsedMs    = 0;
        tick         = 0;
        glitchFrames = 0;
        starsEarned  = new boolean[3];
        coinGotThisRun = false;
    }

    public void update(long deltaMs) {
        if (gameState != Constants.STATE_PLAYING) return;

        elapsedMs += deltaMs;
        tick++;

        if (glitchFrames > 0) glitchFrames--;

        // Update player physics + collision
        player.update(tileMap);

        // Update enemies
        for (Enemy e : enemies) {
            e.update(tileMap);
        }

        // Check shard collection
        checkShardPickup();

        // Check coin collection
        checkCoinPickup();

        // Check enemy collision
        checkEnemyCollision();

        // Check exit
        checkExit();

        // Check death by pit
        if (player.y > tileMap.pixelHeight() + 100) {
            killPlayer();
        }
    }

    private void checkShardPickup() {
        Rect2D pRect = player.getRect();
        for (int i = 0; i < shardX.length; i++) {
            if (shardCollected[i]) continue;
            collisionCache.set(shardX[i], shardY[i], 32, 40);
            if (pRect.intersects(collisionCache)) {

                shardCollected[i] = true;
                glitchFrames = Constants.GLITCH_DURATION;
            }
        }
    }

    private void checkCoinPickup() {
        Rect2D pRect = player.getRect();
        for (int i = 0; i < coinX.length; i++) {
            if (coinCollected[i]) continue;
            collisionCache.set(shardX[i], shardY[i], 28, 28);
            if (pRect.intersects(collisionCache)) {
                coinCollected[i] = true;
                coinGotThisRun   = true;
            }
        }
    }

    private void checkEnemyCollision() {
        if (player.invincibleFrames > 0) return;
        Rect2D pRect = player.getRect();

        for (Enemy e : enemies) {
            if (e.dead) continue;
            collisionCache.set(e.x, e.y, e.width, e.height);
            if (!pRect.intersects(collisionCache)) continue;

            // Stomp detection: player falling + player bottom near enemy top
            boolean stomp = player.velY > 2f
                && (player.y + Constants.PLAYER_HEIGHT)
                   < (e.y + e.height * 0.4f);

            if (stomp) {
                e.dead = true;
                player.velY = Constants.JUMP_FORCE * 0.6f;
                camera.shake(6f, 8);
                glitchFrames = Constants.GLITCH_DURATION;
            } else {
                killPlayer();
            }
        }
    }

    private void checkExit() {
        // All shards collected?
        for (boolean b : shardCollected) if (!b) return;

        Rect2D pRect = player.getRect();
        int col = (int)(player.x / Constants.TILE_SIZE);
        int row = (int)(player.y / Constants.TILE_SIZE);

        if (tileMap.isExit(col, row) || tileMap.isExit(col + 1, row)) {
            winLevel();
        }
    }

    private void killPlayer() {
        player.lives--;
        camera.shake(10f, 14);
        glitchFrames = Constants.GLITCH_DURATION * 2;

        if (player.lives <= 0) {
            gameState = Constants.STATE_GAMEOVER;
        } else {
            // Respawn
            player.respawn(spawnX, spawnY);
            gameState = Constants.STATE_DEAD;
        }
    }

    private void winLevel() {
        gameState = Constants.STATE_WIN;

        // Star 1 — always for finishing
        starsEarned[0] = true;

        // Star 2 — finish within time bonus
        starsEarned[1] = elapsedMs < Constants.STAR_TIME_BONUS * 1000L;

        // Star 3 — got the hidden coin
        starsEarned[2] = coinGotThisRun;
    }

    public void respawnAfterDeath() {
        player.respawn(spawnX, spawnY);
        gameState = Constants.STATE_PLAYING;
    }

    public void restart() {
        loadLevel(world, level);
    }

    public boolean allShardsCollected() {
        for (boolean b : shardCollected) if (!b) return false;
        return true;
    }
}