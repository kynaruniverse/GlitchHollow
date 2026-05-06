/*
 * Vertex Shader — sprite.vert
 *
 * GL concept: A vertex shader runs once per CORNER of every quad (sprite).
 * A sprite has 4 corners, so this runs 4 times per sprite.
 *
 * Its job: take a corner's position in world space and convert it to
 * clip space (the -1 to +1 coordinate system GL uses internally).
 *
 * It also passes the UV coordinate (texture sample position) through
 * to the fragment shader unchanged — the 'varying' keyword does this.
 *
 * 'attribute' = data that changes per vertex (comes from SpriteBatch's buffer)
 * 'uniform'   = data that stays the same for the entire draw call (the matrix)
 * 'varying'   = data passed from vertex shader to fragment shader
 */

// Position of this corner in world space (x, y)
attribute vec2 a_position;

// UV coordinate for this corner (0,0 = top-left of texture, 1,1 = bottom-right)
attribute vec2 a_texCoord;

// Optional per-vertex tint colour (RGBA) — used for flash effects, fade-outs
attribute vec4 a_color;

// The combined projection matrix — transforms world coords to clip space
// Set once per draw call by SpriteBatch
uniform mat4 u_projTrans;

// Pass UV and colour through to the fragment shader
varying vec2 v_texCoord;
varying vec4 v_color;

void main() {
    v_texCoord = a_texCoord;
    v_color    = a_color;

    // gl_Position is the required output — the final clip-space position
    // We're in 2D so z=0, w=1
    gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
}