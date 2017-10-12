uniform mat4 u_Model;
uniform mat4 u_MVMatrix;
uniform mat4 u_MVPMatrix;
uniform vec3 u_LightPos;
uniform vec2 u_Quadrant;

attribute vec4 a_Position;      // Per-vertex position information we will pass in.
attribute vec4 a_Color;         // Per-vertex color information we will pass in.
attribute vec3 a_Normal;        // Per-vertex normal information we will pass in.
//attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information we will pass in.
//attribute vec2 a_Quadrant;

varying vec3 v_Position;        // This will be passed into the fragment shader.
varying vec4 v_Color;           // This will be passed into the fragment shader.
varying vec3 v_Normal;          // This will be passed into the fragment shader.
//varying vec2 v_TexCoordinate;   // This will be passed into the fragment shader.
//varying vec2 v_Quadrant;

// The entry point for our vertex shader.
void main()
{
    // Transform the vertex into eye space.
    vec3 tempPos = vec3(a_Position.x + u_Quadrant.x * 512.0, a_Position.y, a_Position.z + u_Quadrant.x * 512.0);
    v_Position = vec3(a_Position);

    // Pass through the color.
    v_Color = a_Color;

    // Pass through the texture coordinate.
    //vec2 temp = vec2(a_TexCoordinate.x+u_Quadrant.x, a_TexCoordinate.y+u_Quadrant.y);
    //v_TexCoordinate = temp;
    //v_TexCoordinate = a_TexCoordinate;

    // Transform the normal's orientation into eye space.
    v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

    // gl_Position is a special variable used to store the final position.
    // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
    gl_Position = u_MVPMatrix * a_Position;
}