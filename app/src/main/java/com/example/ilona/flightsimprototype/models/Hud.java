package com.example.ilona.flightsimprototype.models;

import android.opengl.GLES11;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Class: Hud
 * Author: Ilona
 * <p> A class to generate a plane HUD for the gameplay.</>
 */

public class Hud {
    private static final String TAG = "Hud";

    private int program;

    private int positionParam;
    private int textureCoordinatesParam;
    private int textureParam;
    private int transformationParam;
    private int offsetParam;

    private float[] positionArray;
    private float[] textureCoordinateArray;
    private int textureHandle;

    private FloatBuffer positions;
    private FloatBuffer textureCoordinates;

    private float[] transformationMatrix;

    /**
     * Constructor method.
     */
    public Hud(){
        transformationMatrix = new float[16];
        Matrix.setIdentityM(transformationMatrix, 0);
        positionArray  = new float[8];
        textureCoordinateArray = new float[8];
    }

    /**
     * Set the position and texture coordinates.
     *
     */
    public void setPositionTexture(){
        float[] texCoordinate = new float[]{0, 0, 0, 1, 1, 0, 1, 1};
        float[] posCoordinates = new float[]{-1, 1, -1, -1, 1, 1, 1, -1};
        for(int i = 0; i < 8; i++) {
            textureCoordinateArray[i] = texCoordinate[i];
            positionArray[i] = posCoordinates[i];
            Log.d(TAG, "setPositionTexture " + i + " = " + textureCoordinateArray[i]);
        }
    }

    /**
     * Setup openGl program, attach shader and texture.
     *
     * @param texture the texture to be drawn
     * @param vertexShader the vertex shader to be attached to Hud program
     * @param fragmentShader the fragment shader to be attached to Hud program
     */
    public void setUpOpenGl(int texture, int vertexShader, int fragmentShader){
        textureHandle = texture;
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glUseProgram(program);

        //checkGLError("Floor program");
        textureParam = GLES20.glGetUniformLocation(program, "u_Texture");
        transformationParam = GLES20.glGetUniformLocation(program, "u_Transformation");

        positionParam = GLES20.glGetAttribLocation(program, "a_Position");
        textureCoordinatesParam = GLES20.glGetAttribLocation(program, "a_TexCoordinate");
        offsetParam = GLES20.glGetAttribLocation(program, "a_Offset");
    }

    /**
     * Attach position and texture coordinate arrays to buffers.
     */
    public void setBuffers(){
        ByteBuffer bbTextureCoords = ByteBuffer.allocateDirect(textureCoordinateArray.length * 4);
        bbTextureCoords.order(ByteOrder.nativeOrder());
        textureCoordinates = bbTextureCoords.asFloatBuffer();
        textureCoordinates.put(textureCoordinateArray);
        textureCoordinates.position(0);

        ByteBuffer bbPositions = ByteBuffer.allocateDirect(positionArray.length * 4);
        bbPositions.order(ByteOrder.nativeOrder());
        positions = bbPositions.asFloatBuffer();
        positions.put(positionArray);
        positions.position(0);
    }

    /**
     * Set transformations of the Hud.
     */
    public void transform(float xScale, float yScale, float rotationAngle){
        Matrix.setIdentityM(transformationMatrix, 0);
        Matrix.rotateM(transformationMatrix, 0, rotationAngle, 0, 0, 1);
        Matrix.scaleM(transformationMatrix, 0, xScale, yScale, 0);
    }

    /**
     * Draws the HUD.
     *
     * @param xScrollOffset x offset value for scrolling textures
     * @param yScrollOffset y offset value for scrolling textures
     */
    public void draw(float xScrollOffset, float yScrollOffset){
        GLES20.glUseProgram(program);
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(textureParam, 0);
        GLES20.glUniformMatrix4fv(transformationParam, 1, false, transformationMatrix, 0);

        GLES20.glVertexAttribPointer(positionParam, 2, GLES20.GL_FLOAT, false, 0, positions);
        GLES20.glVertexAttribPointer(textureCoordinatesParam, 2, GLES20.GL_FLOAT, false, 0, textureCoordinates);
        GLES20.glVertexAttrib2f(offsetParam, xScrollOffset, yScrollOffset);

        GLES20.glEnableVertexAttribArray(positionParam);
        GLES20.glEnableVertexAttribArray(textureCoordinatesParam);

        //Enable blend to draw texture transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES11.glBlendFunc(GLES11.GL_SRC_ALPHA, GLES11.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisableVertexAttribArray(positionParam);
        GLES20.glDisableVertexAttribArray(textureCoordinatesParam);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

}
