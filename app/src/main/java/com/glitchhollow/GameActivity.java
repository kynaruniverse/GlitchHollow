package com.glitchhollow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity {

    private GameView gameView;
    private int world, level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        hideSystemUI();

        world = getIntent().getIntExtra("world", 1);
        level = getIntent().getIntExtra("level", 1);

        gameView = new GameView(this, world, level);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (gameView != null) gameView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.pause();
    }

    @Override
    public void onBackPressed() {
        if (gameView != null && gameView.isPaused()) {
            finish();
        } else {
            if (gameView != null) gameView.togglePause();
        }
    }

    /**
     * Called by GameView when the player taps "continue" on the win screen.
     * Advances to the next level within this Activity (no visible transition gap).
     */
    public void advanceToNextLevel(int nextWorld, int nextLevel) {
        if (gameView != null) {
            gameView.loadLevel(nextWorld, nextLevel);
            world = nextWorld;
            level = nextLevel;
        }
    }

    /**
     * Called by GameView when there are no more levels (game complete).
     * Returns to the main menu.
     */
    public void onGameComplete() {
        finish();
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
