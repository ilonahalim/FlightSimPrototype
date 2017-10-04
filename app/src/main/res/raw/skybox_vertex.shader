attribute vec4 a_Position;

varying vec3 v_TexCoordinates;

uniform mat4 u_MVP;

void main()
{

//4. Generate the UV coordinates

v_TexCoordinates=normalize(a_Position.xyz);

//5. transform every position vertex by the model-view-projection matrix

gl_Position=u_MVP * a_Position;

gl_Position=gl_Position.xyww;

}