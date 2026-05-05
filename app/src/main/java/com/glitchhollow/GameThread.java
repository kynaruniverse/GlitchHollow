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
        long startTime;
        long elapsed;
        long sleepTime;

        while (running) {
            startTime = System.currentTimeMillis();
            Canvas canvas = null;

            try {
                canvas = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    if (canvas != null) {
                        gameView.update();
                        gameView.draw(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        // surface was destroyed
                    }
                }
            }

            elapsed = System.currentTimeMillis() - startTime;
            sleepTime = FRAME_TIME - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}