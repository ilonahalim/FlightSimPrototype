precision mediump float; // Set default precision.

uniform vec3 u_LightPos;        // The position of the light in eye space.
uniform sampler2D u_Texture;    // The input texture.

varying vec3 v_Position;        // Interpolated position for this fragment.

varying vec3 v_Normal;          // Interpolated normal for this fragment.
varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.

void main()
{
    vec3 lightVector = normalize(u_LightPos - v_Position); // Get a lighting direction vector from the light to the vertex.

    float diffuse = max(dot(v_Normal, lightVector), 0.0); // Calculate the dot product of the light vector and vertex normal.

    diffuse = diffuse + 0.3; // Add ambient lighting

    gl_FragColor = (diffuse * texture2D(u_Texture, v_TexCoordinate).rgba); // Set fragment color.
}