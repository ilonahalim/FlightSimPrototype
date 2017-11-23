precision highp float; // Set default precision.

uniform samplerCube u_SkyBoxTexture; // The skybox cubemap texture.

varying vec3 v_TexCoordinates; // The texture coordinates.

void main()
{

    gl_FragColor = textureCube(u_SkyBoxTexture,v_TexCoordinates); // set fragment color to texture color.

}