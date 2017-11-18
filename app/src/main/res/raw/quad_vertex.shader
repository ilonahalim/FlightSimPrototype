uniform mat4 u_MVPMatrix;

attribute vec2 a_Position;      // Per-vertex position information we will pass in.
attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information we will pass in.
attribute vec2 a_Offset;

varying vec2 v_TexCoordinate;   // This will be passed into the fragment shader.

// The entry point for our vertex shader.
void main()
{
    // set x,y coordinates to a_Position and z coordinate to 0.
    vec4 position = vec4(a_Position, 0, 1);

    // Pass through the texture coordinate.
    v_TexCoordinate = a_TexCoordinate + a_Offset;

    // set gl_Position to vertex position
    gl_Position = u_MVPMatrix * position;
}