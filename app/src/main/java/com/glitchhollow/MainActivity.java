package com.glitchhollow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SaveManager saveManager;

        private String[] getWorldNames() {
        return new String[] {
            getString(R.string.world_1),
            getString(R.string.world_2),
            getString(R.string.world_3),
            getString(R.string.world_4)
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        hideSystemUI();

        setContentView(R.layout.activity_main);

        saveManager = new SaveManager(this);

        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        // Refresh progress text each time we return from a level
        updateProgressDisplay();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }


    private void setupUI() {
        // PLAY — resume from last save point
        findViewById(R.id.btn_play).setOnClickListener(v -> {
            int[] resume = saveManager.getResumePoint();
            launchLevel(resume[0], resume[1]);
        });

        updateProgressDisplay();
    }

    private void updateProgressDisplay() {
        int[] resume    = saveManager.getResumePoint();
        int   world     = resume[0];
        int   level     = resume[1];
        int   stars     = saveManager.totalStars();
        int   maxStars  = saveManager.maxStars();

        // "World 1 · Level 3 — The Toon Lot"
        String[] names = getWorldNames();
        String worldName = (world - 1 < names.length)
                         ? names[world - 1] : "World " + world;
        String progressLine = "World " + world + "  ·  Level " + level
                            + "\n" + worldName;

        String starsLine = "★ " + stars + " / " + maxStars;

        TextView tvProgress = findViewById(R.id.tv_progress);
        if (tvProgress != null) tvProgress.setText(progressLine);

        TextView tvStars = findViewById(R.id.tv_stars);
        if (tvStars != null) tvStars.setText(starsLine);
    }

    private void launchLevel(int world, int level) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("world", world);
        intent.putExtra("level", level);
        startActivity(intent);
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }
}
