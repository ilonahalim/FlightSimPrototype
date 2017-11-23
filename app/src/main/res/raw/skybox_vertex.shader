attribute vec4 a_Position; // The vertex coordinate information.

varying vec3 v_TexCoordinates; // The texture coordinate.

uniform mat4 u_MVP; // The model view matrix.

void main()
{
    v_TexCoordinates = normalize(a_Position.xyz); // Generate the UV coordinates

    gl_Position = (u_MVP * a_Position).xyww; // Set vertex position.
}