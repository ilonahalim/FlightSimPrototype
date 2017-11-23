uniform mat4 u_MVPMatrix; // The model view projection matrix.

attribute vec2 a_Position;      // Per-vertex position information we will pass in.
attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information we will pass in.
attribute vec2 a_Offset;

varying vec2 v_TexCoordinate;   // To be passed into the fragment shader.

void main()
{
    vec4 position = vec4(a_Position, 0, 1); // set x,y coordinates to a_Position and z coordinate to 0.

    v_TexCoordinate = a_TexCoordinate + a_Offset; // Pass the texture coordinate.

    gl_Position = u_MVPMatrix * position; // set vertex position.
}