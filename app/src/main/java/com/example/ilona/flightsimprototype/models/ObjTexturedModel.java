package com.example.ilona.flightsimprototype.models;

import android.opengl.GLES20;

import com.example.ilona.flightsimprototype.utility.App;
import com.example.ilona.flightsimprototype.loaders.ObjLoader;
import com.example.ilona.flightsimprototype.loaders.TextureLoader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.glDisable;

/**
 * Class: ObjTexturedModel
 * Author: Ilona
 * <p> A class to generate a 3D model from an Obj file interpreted by the ObjLoader.
 * Uses the textured model vertex & fragment shader.</>
 */

public class ObjTexturedModel {
    int modelProgram;

    float[] model;

    short[] indicesArray;

    FloatBuffer verticesBuffer;
    FloatBuffer normalsBuffer;
    FloatBuffer textureCoordinateBuffer;
    ShortBuffer indicesBuffer;

    int modelParam;
    int modelViewParam;
    int modelViewProjectionParam;
    int lightPosParam;
    int textureParam;
    int verticesParam;
    int normalParam;
    int textureCoordsParam;

    int textureHandle;

    ObjTexturedModel(){
        model = new float[20];
    }

    /**
     * Set the vertices, normal, texture coordinates and indices based on the objLoader and set up the model texture.
     */
    public void setResources(ObjLoader objLoader, int textureResID){
        textureHandle = TextureLoader.loadTexture(App.context(), textureResID);
        float[] verticesArray = objLoader.verticesArray;
        float[] normalsArray = objLoader.normalsArray;
        float[] textureArray = objLoader.textureArray;
        indicesArray = objLoader.indicesArray;

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(verticesArray.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        verticesBuffer = bbVertices.asFloatBuffer();
        verticesBuffer.put(verticesArray);
        verticesBuffer.position(0);

        ByteBuffer bbNormals = ByteBuffer.allocateDirect(normalsArray.length * 4);
        bbNormals.order(ByteOrder.nativeOrder());
        normalsBuffer = bbNormals.asFloatBuffer();
        normalsBuffer.put(normalsArray);
        normalsBuffer.position(0);

        ByteBuffer bbTextureCoords = ByteBuffer.allocateDirect(textureArray.length * 4);
        bbTextureCoords.order(ByteOrder.nativeOrder());
        textureCoordinateBuffer = bbTextureCoords.asFloatBuffer();
        textureCoordinateBuffer.put(textureArray);
        textureCoordinateBuffer.position(0);

        ByteBuffer bbIndices = ByteBuffer.allocateDirect(indicesArray.length * 2);
        bbIndices.order(ByteOrder.nativeOrder());
        indicesBuffer = bbIndices.asShortBuffer();
        indicesBuffer.put(indicesArray);
        indicesBuffer.position(0);
    }

    /**
     * Setup openGl program, attach shader and set parameter variables.
     */
    public void setUpOpenGl(int vertexShader, int fragmentShader){
        modelProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(modelProgram, vertexShader);
        GLES20.glAttachShader(modelProgram, fragmentShader);
        GLES20.glLinkProgram(modelProgram);
        GLES20.glUseProgram(modelProgram);

        modelParam = GLES20.glGetUniformLocation(modelProgram, "u_Model");
        modelViewParam = GLES20.glGetUniformLocation(modelProgram, "u_MVMatrix");
        modelViewProjectionParam = GLES20.glGetUniformLocation(modelProgram, "u_MVPMatrix");
        textureParam = GLES20.glGetUniformLocation(modelProgram, "u_Texture");
        lightPosParam = GLES20.glGetUniformLocation(modelProgram, "u_LightPos");

        verticesParam = GLES20.glGetAttribLocation(modelProgram, "a_Position");
        normalParam = GLES20.glGetAttribLocation(modelProgram, "a_Normal");
        textureCoordsParam = GLES20.glGetAttribLocation(modelProgram, "a_TexCoordinate");
    }

    /**
     * Pass lightDirection, modelView, modelViewProjection and previously set parameters as shader uniforms.
     * Pass vertices, normal, texture coordinates and indices to shader as attributes.
     * Draws the model.
     */
    public void draw(float[] lightDirection, float[] modelView, float[] modelViewProjection){
        GLES20.glDisable(GL_CULL_FACE);
        GLES20.glUseProgram(modelProgram);

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        GLES20.glUniform1i(textureParam, 0);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(lightPosParam, 1, lightDirection, 0);
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
