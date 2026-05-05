package com.glitchhollow;

public class Rect2D {

    public float x, y, width, height;

    public Rect2D(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean intersects(Rect2D other) {
        return x < other.x + other.width
            && x + width > other.x
            && y < other.y + other.height
            && y + height > other.y;
    }

    public float right()  { return x + width; }
    public float bottom() { return y + height; }
    public float centerX(){ return x + width  / 2f; }
    public float centerY(){ return y + height / 2f; }

    public void set(float x, float y, float w, float h) {
        this.x = x; this.y = y;
        this.width = w; this.height = h;
    }
}