uniform mat4 u_Model; // The model's transformation matrix.
uniform mat4 u_MVMatrix; //The model view matrix.
uniform mat4 u_MVPMatrix; // The model view projection matrix.
uniform vec3 u_LightPos; // The light direction.
uniform vec2 u_Quadrant; // The terrain's quadrant location information.

attribute vec4 a_Position;      // Per-vertex position information to be passed.
attribute vec3 a_Normal;        // Per-vertex normal information to be passed.
attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information to be passed.

varying vec3 v_Position;        // To be passed into the fragment shader.
varying vec3 v_Normal;          // To be passed into the fragment shader.
varying vec2 v_TexCoordinate;   // To be passed into the fragment shader.

void main()
{
    // Calculate position based on position matrix and quadrant information.
    vec4 tempPos = a_Position;
    tempPos.x = tempPos.x + u_Quadrant.x * 60.8294;
    tempPos.z = tempPos.z + u_Quadrant.y * 60.8294;
    v_Position = vec3(tempPos);

    v_TexCoordinate = a_TexCoordinate; // Pass through the texture coordinate.

    v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0)); // Transform the normal's orientation into eye space.

    gl_Position = u_MVPMatrix * tempPos; // Set vertex position.
}