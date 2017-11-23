uniform mat4 u_Transformation; // Transformation matrix information.

attribute vec2 a_Position;      // Per-vertex position information.
attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information.
attribute vec2 a_Offset;

varying vec2 v_TexCoordinate;   // To be passed into the fragment shader.

void main()
{
    vec4 position = vec4(a_Position, 30, 1); // set x,y coordinates to a_Position and z coordinate to 30.

    v_TexCoordinate = a_TexCoordinate + a_Offset; // Pass the texture coordinate.

    gl_Position = position * u_Transformation; // set gl_Position to vertex position
}