package com.glitchhollow;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000L / TARGET_FPS;

    private SurfaceHolder surfaceHolder;
    private GameView gameView;
    private boolean running = false;

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double accumulator = 0.0;
        final double NS_PER_TICK = 1000000000.0 / 60.0; 

        while (running) {
            long now = System.nanoTime();
            double passed = (now - lastTime) / 1000000000.0;
            lastTime = now;
            accumulator += passed;

            // Update physics/logic at fixed 60Hz interval
            while (accumulator >= (1.0 / 60.0)) {
                gameView.update(); 
                accumulator -= (1.0 / 60.0);
            }

            // Draw as fast as possible
            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    gameView.draw(canvas);
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {}
                }
            }
        }
    }
}