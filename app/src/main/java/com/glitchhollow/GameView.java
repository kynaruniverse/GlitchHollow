package com.glitchhollow;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread  gameThread;
    private GameEngine  engine;
    private Renderer    renderer;
    private Camera      camera;
    private SpriteSheet sprites;
    private InputHandler input;

    private boolean paused = false;
    private int screenW, screenH;

    public GameView(Context context, int world, int level) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        sprites  = new SpriteSheet(context);
        engine   = new GameEngine(context, world, level);

        // Camera sized after surface created; pre-init with estimate
        camera   = new Camera(1080, 1920);
        renderer = new Renderer(sprites, camera);
        input    = new InputHandler(engine.player);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenW = getWidth();
        screenH = getHeight();

        camera   = new Camera(screenW, screenH);
        camera.setLevelSize(engine.tileMap.cols, engine.tileMap.rows);
        renderer = new Renderer(sprites, camera);

        gameThread = new GameThread(holder, this);
        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int f, int w, int h2) {
        screenW = w; screenH = h2;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        gameThread.setRunning(false);
        boolean retry = true;
        while (retry) {
            try { gameThread.join(); retry = false; }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        sprites.recycle();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        handleStateTouch(event);
        input.onTouch(event, screenW, screenH);
        return true;
    }

    private void handleStateTouch(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return;

        switch (engine.gameState) {
            case Constants.STATE_PAUSED:
                engine.gameState = Constants.STATE_PLAYING;
                paused = false;
                break;
            case Constants.STATE_DEAD:
                engine.respawnAfterDeath();
                break;
            case Constants.STATE_GAMEOVER:
                engine.restart();
                break;
            case Constants.STATE_WIN:
                // Return to calling activity
                if (getContext() instanceof GameActivity) {
                    ((GameActivity) getContext()).finish();
                }
                break;
        }
    }

    public void update() {
        if (paused || engine.gameState == Constants.STATE_PAUSED) return;
        engine.update(16); // ~60fps fixed step
        camera.update(
            engine.player.x + Constants.PLAYER_WIDTH  / 2f,
            engine.player.y + Constants.PLAYER_HEIGHT / 2f
        );
    }

    public void draw(Canvas canvas) {
        if (canvas == null) return;

        renderer.drawBackground(canvas, engine.world, screenW, screenH);
        renderer.drawTileMap(canvas, engine.tileMap, engine.world);
        renderer.drawItems(canvas, engine);
        renderer.drawEnemies(canvas, engine.enemies);
        renderer.drawPlayer(canvas, engine.player);
        renderer.drawHUD(canvas, engine, screenW);
        renderer.drawGlitchEffect(canvas, screenW, screenH, engine.glitchFrames);

        switch (engine.gameState) {
            case Constants.STATE_PAUSED:
                renderer.drawPauseOverlay(canvas, screenW, screenH);
                break;
            case Constants.STATE_DEAD:
            case Constants.STATE_GAMEOVER:
                renderer.drawDeathOverlay(canvas, screenW, screenH);
                break;
            case Constants.STATE_WIN:
                renderer.drawWinOverlay(canvas, screenW, screenH,
                    engine.starsEarned, engine.coinGotThisRun);
                break;
        }
    }

    public void resume()       { paused = false; }
    public void pause()        { paused = true; }
    public void togglePause()  { paused = !paused;
        engine.gameState = paused
            ? Constants.STATE_PAUSED : Constants.STATE_PLAYING; }
    public boolean isPaused()  { return paused; }
}