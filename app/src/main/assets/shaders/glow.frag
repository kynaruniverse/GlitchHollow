/*
 * Glow fragment shader — additive blending pass.
 *
 * GL concept — additive blending:
 * Standard blending: dest = src * srcAlpha + dest * (1 - srcAlpha)
 * Additive blending: dest = src * srcAlpha + dest
 *
 * Additive means colours only ADD to what's already drawn.
 * Black = invisible. White = full brightness. This is how
 * neon glow, fire, and bloom effects work in real games.
 *
 * We draw the same sprite twice:
 *   Pass 1 (sprite.frag, normal blend) — the actual sprite
 *   Pass 2 (glow.frag,  additive blend) — a larger, softer version
 *                                          that adds light around it
 *
 * The soft edge comes from the UV coordinates: as we sample near
 * the sprite's edges, the alpha falls off smoothly using a
 * radial gradient calculation.
 */

precision mediump float;

uniform sampler2D u_texture;
uniform float     u_intensity;   // 0.0–1.0, set per-sprite type
uniform float     u_time;        // for animated pulse

varying vec2 v_texCoord;
varying vec4 v_color;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoord);

    // Skip invisible pixels entirely
    if (texColor.a < 0.05) discard;

    // UV distance from centre (0.5, 0.5) — used for radial falloff
    vec2  centred = v_texCoord - vec2(0.5);
    float dist    = length(centred) * 2.0;          // 0 at centre, 1 at corner
    float falloff = 1.0 - smoothstep(0.0, 1.0, dist);

    // Animated pulse — subtle brightness oscillation
    float pulse = 0.85 + 0.15 * sin(u_time * 3.14159 * 2.0);

    // Glow colour — use the sprite's own colour tinted by vertex colour
    vec3 glowColor = texColor.rgb * v_color.rgb * pulse;

    // Final output — additive: black areas contribute nothing
    float alpha = texColor.a * falloff * u_intensity * v_color.a;
    gl_FragColor = vec4(glowColor, alpha);
}