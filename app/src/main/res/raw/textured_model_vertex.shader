uniform mat4 u_Model; // The model transformation matrix.
uniform mat4 u_MVMatrix; // The model view matrix.
uniform mat4 u_MVPMatrix; // The model view projection matrix.
uniform vec3 u_LightPos; // The light direction.

attribute vec4 a_Position;      // Per-vertex position information to be passed.
attribute vec3 a_Normal;        // Per-vertex normal information we to be passed.
attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information to be passed.

varying vec3 v_Position;        // To be passed into the fragment shader.
varying vec3 v_Normal;          // To be passed into the fragment shader.
varying vec2 v_TexCoordinate;   // To be passed into the fragment shader.

void main()
{
    v_Position = vec3(a_Position); // Transform the vertex into eye space.

    v_TexCoordinate = a_TexCoordinate; // Pass through the texture coordinate.

    v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0)); // Transform the normal's orientation into eye space.

    gl_Position = u_MVPMatrix * a_Position; // Set vertex position.
}