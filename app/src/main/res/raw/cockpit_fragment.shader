precision highp float;

uniform samplerCube u_SkyBoxTexture;

varying vec3 v_TexCoordinates;

void main()
{

gl_FragColor = textureCube(u_SkyBoxTexture,v_TexCoordinates).rgba;

}