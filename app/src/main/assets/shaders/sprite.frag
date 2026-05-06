/*
 * Fragment Shader — sprite.frag
 *
 * GL concept: A fragment shader runs once per PIXEL that a triangle covers.
 * Since each sprite is 2 triangles, this runs width×height times per sprite.
 *
 * Its job: output a final colour (RGBA) for each pixel.
 *
 * Here we sample the texture at the UV coordinate passed from the vertex
 * shader, then multiply by the vertex colour (for tinting/fading).
 *
 * 'precision mediump float' — medium precision floats. Fine for 2D sprites,
 * slightly faster than highp on mobile GPUs.
 *
 * 'uniform sampler2D' — the texture unit. We bind our PNG texture to unit 0
 * and tell the shader to read from it.
 */

precision mediump float;

uniform sampler2D u_texture;

varying vec2 v_texCoord;
varying vec4 v_color;

void main() {
    // Sample the texture at the interpolated UV coordinate
    vec4 texColor = texture2D(u_texture, v_texCoord);

    // Multiply by vertex colour — white (1,1,1,1) means no tint
    gl_FragColor = texColor * v_color;

    // Discard fully transparent pixels — prevents overdraw artifacts
    if (gl_FragColor.a < 0.01) discard;
}