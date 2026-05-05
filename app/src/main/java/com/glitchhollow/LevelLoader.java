package com.glitchhollow;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LevelLoader {

    private Context context;

    public LevelLoader(Context context) {
        this.context = context;
    }

    public LevelData load(int world, int level) {
        String filename = "levels/w" + world + "_l" + level + ".json";
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            return parse(json);
        } catch (Exception e) {
            e.printStackTrace();
            return buildFallbackLevel(world, level);
        }
    }

    private LevelData parse(String json) throws Exception {
        LevelData data = new LevelData();
        JSONObject obj = new JSONObject(json);

        data.world = obj.getInt("world");
        data.level = obj.getInt("level");
        data.name  = obj.optString("name", "???");
        data.rows  = obj.getInt("rows");
        data.cols  = obj.getInt("cols");

        // Parse tile grid
        JSONArray tileArray = obj.getJSONArray("tiles");
        data.tiles = new int[data.rows][data.cols];
        for (int r = 0; r < data.rows; r++) {
            JSONArray row = tileArray.getJSONArray(r);
            for (int c = 0; c < data.cols; c++) {
                data.tiles[r][c] = row.getInt(c);
            }
        }

        // Player start
        JSONObject ps = obj.getJSONObject("player_start");
        data.playerStartX = ps.getInt("x");
        data.playerStartY = ps.getInt("y");

        // Exit
        JSONObject ex = obj.getJSONObject("exit");
        data.exitX = ex.getInt("x");
        data.exitY = ex.getInt("y");

        // Shards
        data.shards = new ArrayList<>();
        JSONArray shards = obj.optJSONArray("shards");
        if (shards != null) {
            for (int i = 0; i < shards.length(); i++) {
                JSONObject s = shards.getJSONObject(i);
                data.shards.add(new int[]{s.getInt("x"), s.getInt("y")});
            }
        }

        // Coins
        data.coins = new ArrayList<>();
        JSONArray coins = obj.optJSONArray("coins");
        if (coins != null) {
            for (int i = 0; i < coins.length(); i++) {
                JSONObject c = coins.getJSONObject(i);
                data.coins.add(new int[]{c.getInt("x"), c.getInt("y")});
            }
        }

        // Enemies
        data.enemies = new ArrayList<>();
        JSONArray enemies = obj.optJSONArray("enemies");
        if (enemies != null) {
            for (int i = 0; i < enemies.length(); i++) {
                JSONObject e = enemies.getJSONObject(i);
                LevelData.EnemyData ed = new LevelData.EnemyData();
                ed.type       = e.getInt("type");
                ed.startCol   = e.getInt("x");
                ed.startRow   = e.getInt("y");
                ed.patrolCols = e.optInt("patrol", 4);
                data.enemies.add(ed);
            }
        }

        return data;
    }

    // Fallback: generates a simple playable level if JSON missing
    private LevelData buildFallbackLevel(int world, int level) {
        LevelData d = new LevelData();
        d.world = world;
        d.level = level;
        d.name  = "World " + world + " - " + level;
        d.rows  = 12;
        d.cols  = 24;
        d.tiles = new int[d.rows][d.cols];

        // Fill floor and walls
        for (int c = 0; c < d.cols; c++) {
            d.tiles[0][c]          = Constants.TILE_WALL;
            d.tiles[d.rows - 1][c] = Constants.TILE_FLOOR;
        }
        for (int r = 0; r < d.rows; r++) {
            d.tiles[r][0]          = Constants.TILE_WALL;
            d.tiles[r][d.cols - 1] = Constants.TILE_WALL;
        }

        // Platforms
        for (int c = 4; c <= 7; c++)  d.tiles[8][c] = Constants.TILE_PLATFORM;
        for (int c = 10; c <= 14; c++) d.tiles[6][c] = Constants.TILE_PLATFORM;
        for (int c = 16; c <= 20; c++) d.tiles[8][c] = Constants.TILE_PLATFORM;

        // Hazards
        d.tiles[d.rows - 1][12] = Constants.TILE_HAZARD;
        d.tiles[d.rows - 1][13] = Constants.TILE_HAZARD;

        // Exit
        d.tiles[d.rows - 2][d.cols - 3] = Constants.TILE_EXIT;
        d.exitX = d.cols - 3;
        d.exitY = d.rows - 2;

        d.playerStartX = 2;
        d.playerStartY = d.rows - 2;

        // Shards
        d.shards = new ArrayList<>();
        d.shards.add(new int[]{5,  7});
        d.shards.add(new int[]{12, 5});
        d.shards.add(new int[]{18, 7});

        d.coins   = new ArrayList<>();
        d.enemies = new ArrayList<>();

        return d;
    }
}