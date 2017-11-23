package com.example.ilona.flightsimprototype.models;

import android.opengl.GLES11;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Class: Quad
 * Author: Ilona
 * <p> The purpose of this class is to create a textured quad
 * to be used for titles, instructions, credits and menu options.
 * It uses the quad vertex & fragment shader. .</>
 */

public class Quad {
    private int program;

    private int positionParam;
    private int textureCoordsParam;
    private int textureParam;
    private int modelViewProjectionMatrixParam;
    private int offsetParam;

    private float[] positionArray;
    private float[] textureCoordsArray;
    private float[] offsetArray;
    private int textureHandle;

    private FloatBuffer positions;
    private FloatBuffer textureCoords;

    private float[] transformationMatrix;
    private float[] modelView;
    private float[] modelViewProjection;

    public Quad(){
        transformationMatrix = new float[16];
        Matrix.setIdentityM(transformationMatrix, 0);
        positionArray = new float[]{-1, 1, -1, -1, 1, 1, 1, -1};
        textureCoordsArray = new float[]{0, 0, 0, 1, 1, 0, 1, 1};
        modelViewProjection = new float[16];
        modelView = new float[16];
        offsetArray = new float[2];
    }

    /**
     * Set the texture coordinates.
     */
    public void setTextureCoor(float[] textureCoordinates){
        textureCoordsArray = textureCoordinates;
    }

    /**
     * Set transformation matrix to newTransformationMatrix.
     */
    public void setTransformationMatrix(float[] newTransformationMatrix){
        transformationMatrix = newTransformationMatrix;
    }

    /**
     * Gets the transformationMatrix.
     *
     * @return the transformationMatrix as a float array.
     */
    public float[] getTransformationMatrix(){
        return transformationMatrix;
    }

    /**
     * Attach position and texture coordinate arrays to buffers.
     */
    public void setBuffers(){
        ByteBuffer bbTextureCoords = ByteBuffer.allocateDirect(textureCoordsArray.length * 4);
        bbTextureCoords.order(ByteOrder.nativeOrder());
        textureCoords = bbTextureCoords.asFloatBuffer();
        textureCoords.put(textureCoordsArray);
        textureCoords.position(0);

        ByteBuffer bbPositions = ByteBuffer.allocateDirect(positionArray.length * 4);
        bbPositions.order(ByteOrder.nativeOrder());
        positions = bbPositions.asFloatBuffer();
        positions.put(positionArray);
        positions.position(0);
    }

    /**
     * Setup openGl program, attach shader and texture.
     *
     * @param texture the texture to be drawn
     * @param vertexShader the vertex shader to be attached to program
     * @param fragmentShader the fragment shader to be attached to program
     */
    public void setUpOpenGl(int texture, int vertexShader, int fragmentShader){
        textureHandle = texture;
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glUseProgram(program);

        textureParam = GLES20.glGetUniformLocation(program, "u_Texture");
        modelViewProjectionMatrixParam = GLES20.glGetUniformLocation(program, "u_MVPMatrix");

        positionParam = GLES20.glGetAttribLocation(program, "a_Position");
        textureCoordsParam = GLES20.glGetAttribLocation(program, "a_TexCoordinate");
        offsetParam = GLES20.glGetAttribLocation(program, "a_Offset");
    }

    /**
     * Use view and perspective to calculate model view projection matrix to be passed as a uniform.
     * Draws the quad.
     *
     * @param view the view matrix
     * @param perspective the eye perspective
     */
    public void draw(float[] view, float[] perspective){
        Matrix.multiplyMM(modelView, 0, view, 0, transformationMatrix, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);

        GLES20.glUseProgram(program);
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(textureParam, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionMatrixParam, 1, false, modelViewProjection, 0);

        GLES20.glVertexAttribPointer(positionParam, 2, GLES20.GL_FLOAT, false, 0, positions);
        GLES20.glVertexAttribPointer(textureCoordsParam, 2, GLES20.GL_FLOAT, false, 0, textureCoords);
        GLES20.glVertexAttrib2f(offsetParam, offsetArray[0], offsetArray[1]);

        GLES20.glEnableVertexAttribArray(positionParam);
        GLES20.glEnableVertexAttribArray(textureCoordsParam);

        //Enable blend to draw texture transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES11.glBlendFunc(GLES11.GL_SRC_ALPHA, GLES11.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisableVertexAttribArray(positionParam);
        GLES20.glDisableVertexAttribArray(textureCoordsParam);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
