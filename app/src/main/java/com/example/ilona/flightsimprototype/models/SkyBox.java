package com.example.ilona.flightsimprototype.models;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_LESS;

/**
 * Class: Skybox
 * Author: Ilona
 * <p> The purpose of this class is to create a skybox
 * that can be reused for various states. It uses a cube
 * with cubemap textureHandle to create an illusion of a huge sky.
 * It uses the skybox vertex & fragment shader.</>
 */

public class SkyBox {
    private static final float SIZE = 1000f; //size of the skybox

    private FloatBuffer skyVertices;

    private int skyProgram;

    private int skyPositionParam;
    private int skyModelViewProjectionParam;
    private int skyTextureParam;
    private int textureHandle;

    //the vertices of the skybox's cube.
    private static final float[] VERTICES = {
            -SIZE,  SIZE, -SIZE,
            -SIZE, -SIZE, -SIZE,
            SIZE, -SIZE, -SIZE,
            SIZE, -SIZE, -SIZE,
            SIZE,  SIZE, -SIZE,
            -SIZE,  SIZE, -SIZE,

            -SIZE, -SIZE,  SIZE,
            -SIZE, -SIZE, -SIZE,
            -SIZE,  SIZE, -SIZE,
            -SIZE,  SIZE, -SIZE,
            -SIZE,  SIZE,  SIZE,
            -SIZE, -SIZE,  SIZE,

            SIZE, -SIZE, -SIZE,
            SIZE, -SIZE,  SIZE,
            SIZE,  SIZE,  SIZE,
            SIZE,  SIZE,  SIZE,
            SIZE,  SIZE, -SIZE,
            SIZE, -SIZE, -SIZE,

            -SIZE, -SIZE,  SIZE,
            -SIZE,  SIZE,  SIZE,
            SIZE,  SIZE,  SIZE,
            SIZE,  SIZE,  SIZE,
            SIZE, -SIZE,  SIZE,
            -SIZE, -SIZE,  SIZE,

            -SIZE,  SIZE, -SIZE,
            SIZE,  SIZE, -SIZE,
            SIZE,  SIZE,  SIZE,
            SIZE,  SIZE,  SIZE,
            -SIZE,  SIZE,  SIZE,
            -SIZE,  SIZE, -SIZE,

            -SIZE, -SIZE, -SIZE,
            -SIZE, -SIZE,  SIZE,
            SIZE, -SIZE, -SIZE,
            SIZE, -SIZE, -SIZE,
            -SIZE, -SIZE,  SIZE,
            SIZE, -SIZE,  SIZE
    };


    /**
    * Set up the skybox program, shaders, get shader parameter locations and texture.
    */
    public void setUpOpenGl(int vertexShader, int fragmentShader, int textureId){
        textureHandle = textureId;

        ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bbFloorVertices.order(ByteOrder.nativeOrder());
        skyVertices = bbFloorVertices.asFloatBuffer();
        skyVertices.put(VERTICES);
        skyVertices.position(0);

        skyProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(skyProgram, vertexShader);
        GLES20.glAttachShader(skyProgram, fragmentShader);
        GLES20.glLinkProgram(skyProgram);
        GLES20.glUseProgram(skyProgram);

        skyModelViewProjectionParam = GLES20.glGetUniformLocation(skyProgram, "u_MVP");
        skyTextureParam = GLES20.glGetUniformLocation(skyProgram, "u_SkyBoxTexture");

        skyPositionParam = GLES20.glGetAttribLocation(skyProgram, "a_Position");
    }

    /**
     * Draws the skybox.
     */
    public void draw(float[] modelViewProjection){
        GLES20.glUseProgram(skyProgram);
        // Set the active textureHandle unit to textureHandle unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);

        // Bind the textureHandle to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureHandle);

        // Tell the textureHandle uniform sampler to use this textureHandle in the shader by binding to textureHandle unit 1.
        GLES20.glUniform1i(skyTextureParam, 1);


        // Set uniforms
        GLES20.glUniformMatrix4fv(skyModelViewProjectionParam, 1, false, modelViewProjection, 0);
        GLES20.glVertexAttribPointer(skyPositionParam, 3, GLES20.GL_FLOAT, false, 0, skyVertices);
        GLES20.glEnableVertexAttribArray(skyPositionParam);


        GLES20.glDepthFunc(GL_LEQUAL);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);

        GLES20.glDepthFunc(GL_LESS);

        GLES20.glDisableVertexAttribArray(skyPositionParam);
    }
}
