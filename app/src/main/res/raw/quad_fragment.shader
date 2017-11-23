precision mediump float; // Define default precision

uniform sampler2D u_Texture;    // The input texture.

varying vec2 v_TexCoordinate;   // Texture coordinate per fragment.

void main()
{
    gl_FragColor = texture2D(u_Texture, v_TexCoordinate).rgba; // Set fragment color to texture color.
}