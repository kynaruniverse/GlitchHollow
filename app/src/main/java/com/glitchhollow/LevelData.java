package com.glitchhollow;

import java.util.List;

public class LevelData {

    public int world;
    public int level;
    public String name;
    public int rows;
    public int cols;
    public int[][] tiles;

    public int playerStartX;
    public int playerStartY;

    public int exitX;
    public int exitY;

    public List<int[]> shards;   // each int[] = {tileCol, tileRow}
    public List<int[]> coins;    // each int[] = {tileCol, tileRow}
    public List<EnemyData> enemies;

    public static class EnemyData {
        public int type;
        public int startCol;
        public int startRow;
        public int patrolCols; // how many tiles it patrols
    }
}