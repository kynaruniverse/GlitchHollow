package com.glitchhollow;

public class TileMap {

    private int[][] tiles;
    public int rows, cols;
    public int world;

    public TileMap(int[][] tiles, int rows, int cols, int world) {
        this.tiles = tiles;
        this.rows  = rows;
        this.cols  = cols;
        this.world = world;
    }

    public int getTile(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return Constants.TILE_WALL; // treat out-of-bounds as solid
        }
        return tiles[row][col];
    }

    public boolean isSolid(int col, int row) {
        int t = getTile(col, row);
        return t == Constants.TILE_FLOOR
            || t == Constants.TILE_WALL
            || t == Constants.TILE_PLATFORM;
    }

    public boolean isHazard(int col, int row) {
        return getTile(col, row) == Constants.TILE_HAZARD;
    }

    public boolean isPlatform(int col, int row) {
        return getTile(col, row) == Constants.TILE_PLATFORM;
    }

    public boolean isExit(int col, int row) {
        return getTile(col, row) == Constants.TILE_EXIT;
    }

    public int pixelWidth()  { return cols * Constants.TILE_SIZE; }
    public int pixelHeight() { return rows * Constants.TILE_SIZE; }
}