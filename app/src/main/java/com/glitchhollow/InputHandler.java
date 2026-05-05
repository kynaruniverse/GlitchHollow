package com.glitchhollow;

import android.view.MotionEvent;
import java.util.HashMap;
import java.util.Map;

public class InputHandler {

    private Player player;

    // Track active pointers for virtual buttons
    private Map<Integer, float[]> pointers = new HashMap<>();

    // Screen zones — set on first touch
    private int screenW, screenH;

    // Swipe detection
    private float swipeStartX, swipeStartY;
    private long  swipeStartTime;
    private boolean swipeActive = false;

    // Virtual button zones (set dynamically)
    // Left button: bottom-left quarter
    // Right button: bottom-left quarter but right of centre-left
    // Jump button: bottom-right half

    public InputHandler(Player player) {
        this.player = player;
    }

    public void onTouch(MotionEvent event, int sw, int sh) {
        screenW = sw;
        screenH = sh;

        int action      = event.getActionMasked();
        int pointerIdx  = event.getActionIndex();
        int pointerId   = event.getPointerId(pointerIdx);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                float dx = event.getX(pointerIdx);
                float dy = event.getY(pointerIdx);
                pointers.put(pointerId, new float[]{dx, dy});
                handleDown(dx, dy, sw, sh);

                // Begin swipe detection
                swipeStartX    = dx;
                swipeStartY    = dy;
                swipeStartTime = System.currentTimeMillis();
                swipeActive    = true;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                pointers.remove(pointerId);
                handleUp(event.getX(pointerIdx), event.getY(pointerIdx), sw, sh);

                // Evaluate swipe
                if (swipeActive) {
                    long elapsed = System.currentTimeMillis() - swipeStartTime;
                    if (elapsed < Constants.SWIPE_MAX_TIME) {
                        float distX = event.getX(pointerIdx) - swipeStartX;
                        float distY = event.getY(pointerIdx) - swipeStartY;
                        evaluateSwipe(distX, distY, sw, sh);
                    }
                    swipeActive = false;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                pointers.clear();
                player.wantsLeft  = false;
                player.wantsRight = false;
                swipeActive = false;
                break;
        }

        // Re-evaluate all active pointers for held buttons
        player.wantsLeft  = false;
        player.wantsRight = false;
        for (float[] pos : pointers.values()) {
            applyHeld(pos[0], pos[1], sw, sh);
        }
    }

    private void handleDown(float x, float y, int sw, int sh) {
        // Bottom 40% of screen is control zone
        if (y > sh * 0.6f) {
            if (x < sw * 0.5f) {
                // Left zone — split: left/right arrows
                if (x < sw * 0.25f) {
                    player.wantsLeft = true;
                } else {
                    player.wantsRight = true;
                }
            } else {
                // Right zone — jump
                player.wantsJump = true;
            }
        }
    }

    private void handleUp(float x, float y, int sw, int sh) {
        // Jump is a tap, so clear it on release
        if (y > sh * 0.6f && x >= sw * 0.5f) {
            player.wantsJump = false;
        }
    }

    private void applyHeld(float x, float y, int sw, int sh) {
        if (y > sh * 0.6f && x < sw * 0.5f) {
            if (x < sw * 0.25f) {
                player.wantsLeft = true;
            } else {
                player.wantsRight = true;
            }
        }
    }

    private void evaluateSwipe(float distX, float distY, int sw, int sh) {
        float absX = Math.abs(distX);
        float absY = Math.abs(distY);
        int   t    = Constants.SWIPE_THRESHOLD;

        if (absX > absY && absX > t) {
            // Horizontal swipe
            if (distX < 0) player.wantsLeft  = true;
            else            player.wantsRight = true;
        } else if (absY > t && distY < 0) {
            // Upward swipe — jump
            player.wantsJump = true;
        }
    }
}