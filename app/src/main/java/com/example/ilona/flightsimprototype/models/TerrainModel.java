package com.example.ilona.flightsimprototype.models;

import android.opengl.GLES20;

import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.glDisable;

/**
 * Class: TerrainModel
 * Extends: ObjTexturedModel
 * Author: Ilona
 * <p> A class to generate a 3D terrain from an Obj file interpreted by the ObjLoader.
 * Uses the terrain vertex & fragment shader.</>
 */

public class TerrainModel extends ObjTexturedModel {
    private int terrainQuadrantParam;

    private float[] quadrant;
    private int[] index;

    public TerrainModel(){
        super();
        quadrant = new float[2];
        index = new int[2];
    }

    /**
    * Set the index of the terrain
    */
    public void setIndex(int x, int z){
        index[0] = x;
        index[1] = z;
    }


    /**
     * Setup openGl program, attach shader, set parameter variables by calling super method,
     * then set an additional ‘u_Quadrant’ shader uniform parameter.
     */
    @Override
    public void setUpOpenGl(int vertexShader, int fragmentShader){
        super.setUpOpenGl(vertexShader, fragmentShader);
        terrainQuadrantParam = GLES20.glGetUniformLocation(modelProgram, "u_Quadrant");
    }

    /**
    * Pass lightDirection, modelView, modelViewProjection, camQuadrant and previously set parameters as shader uniforms.
    * Pass vertices, normal, texture coordinates and indices to shader as attributes.
    * Draws the model.
     */
    public void draw(float[] lightPosInEyeSpace, float[] modelView, float[] modelViewProjection, int[] camQuadrant){
        quadrant[0] = camQuadrant[0] + index[0];
        quadrant[1] = camQuadrant[1] + index[1];

        GLES20.glDisable(GL_CULL_FACE);
        GLES20.glUseProgram(modelProgram);

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        GLES20.glUniform1i(textureParam, 0);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform2fv(terrainQuadrantParam, 1, quadrant, 0);
        GLES20.glUniform3fv(lightPosParam, 1, lightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(modelParam, 1, false, model, 0);
        GLES20.glUniformMatrix4fv(modelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionParam, 1, false, modelViewProjection, 0);

        GLES20.glVertexAttribPointer(verticesParam, 3, GLES20.GL_FLOAT, false, 0, verticesBuffer);
        GLES20.glVertexAttribPointer(normalParam, 3, GLES20.GL_FLOAT, false, 0, normalsBuffer);
        GLES20.glVertexAttribPointer(textureCoordsParam, 2, GLES20.GL_FLOAT, false, 0, textureCoordinateBuffer);

        GLES20.glEnableVertexAttribArray(verticesParam);
        GLES20.glEnableVertexAttribArray(normalParam);
        GLES20.glEnableVertexAttribArray(textureCoordsParam);

        glDisable(GL_CULL_FACE);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesArray.length, GLES20.GL_UNSIGNED_SHORT, indicesBuffer);

        GLES20.glDisableVertexAttribArray(verticesParam);
        GLES20.glDisableVertexAttribArray(normalParam);
        GLES20.glDisableVertexAttribArray(textureCoordsParam);
    }
}
