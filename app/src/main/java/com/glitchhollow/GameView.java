package com.glitchhollow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread gameThread;
    private Paint paint;
    private boolean paused = false;

    private int world;
    private int level;

    public GameView(Context context, int world, int level) {
        super(context);
        this.world = world;
        this.level = level;

        getHolder().addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gameThread = new GameThread(holder, this);
        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // handle resize if needed
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        gameThread.setRunning(false);
        while (retry) {
            try {
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void update() {
        if (paused) return;
        // Game logic goes here — Phase 4
    }

    public void draw(Canvas canvas) {
        if (canvas == null) return;

        // Placeholder background
        canvas.drawColor(Color.parseColor("#0D0D1A"));

        // Placeholder text
        paint.setColor(Color.parseColor("#00F5FF"));
        paint.setTextSize(48f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GLITCH HOLLOW", getWidth() / 2f, getHeight() / 2f - 40, paint);

        paint.setColor(Color.parseColor("#7B2FBE"));
        paint.setTextSize(28f);
        canvas.drawText("World " + world + "  Level " + level,
            getWidth() / 2f, getHeight() / 2f + 20, paint);

        paint.setColor(Color.parseColor("#FF3CAC"));
        paint.setTextSize(20f);
        canvas.drawText("Engine loading... Phase 4", getWidth() / 2f,
            getHeight() / 2f + 70, paint);
    }

    public void resume() {
        paused = false;
    }

    public void pause() {
        paused = true;
    }

    public void togglePause() {
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }
}