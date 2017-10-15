package com.example.ilona.flightsimprototype;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_LESS;
import static android.opengl.GLES20.glGetError;

/**
 * Created by Ilona on 29-Sep-17.
 */

public class SkyBox {
    private static final String TAG = "SKYBOX";
    private FloatBuffer skyVertices;
    private FloatBuffer skyColors;
    private FloatBuffer skyFoundColors;
    private FloatBuffer skyNormals;

    private int skyProgram;

    private int skyPositionParam;
    private int skyNormalParam;
    private int skyColorParam;
    private int skyModelParam;
    private int skyModelViewParam;
    private int skyModelViewProjectionParam;
    private int skyTextureParam;
    private int skyLightPosParam;
    private int textureDataHandle;

    private static final float SIZE = 500f;

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

    public SkyBox(){
        //textures = new int[6];
    }

    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     */
    private static void checkGLError(String label) {
        int error;
        while ((error = glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    public void setUpOpenGl(int vertexShader, int fragmentShader, int theTexture){
        textureDataHandle = theTexture;

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

        //checkGLError("Floor program");
        skyModelViewProjectionParam = GLES20.glGetUniformLocation(skyProgram, "u_MVP");
        skyTextureParam = GLES20.glGetUniformLocation(skyProgram, "u_SkyBoxTexture");

        skyPositionParam = GLES20.glGetAttribLocation(skyProgram, "a_Position");
        checkGLError("error linking program");
    }

    public void drawSkybox(float[] modelViewProjection){
        GLES20.glUseProgram(skyProgram);
        checkGLError("error using program");
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        checkGLError("error activate texture");
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureDataHandle);
        checkGLError("error binding texture");
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 1.
        GLES20.glUniform1i(skyTextureParam, 1);
        checkGLError("error setting texture uniform");
        //checkGLError("sky texture param");
        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniformMatrix4fv(skyModelViewProjectionParam, 1, false, modelViewProjection, 0);
        checkGLError("error setting uniform");

        GLES20.glVertexAttribPointer(skyPositionParam, 3, GLES20.GL_FLOAT, false, 0, skyVertices);
        checkGLError("error setting attribute");
        GLES20.glEnableVertexAttribArray(skyPositionParam);
        checkGLError("error enabling attribute");

        GLES20.glDepthFunc(GL_LEQUAL);
        checkGLError("error depth func");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        checkGLError("error drawing skybox");

        GLES20.glDepthFunc(GL_LESS);
        checkGLError("error second depth func");

        GLES20.glDisableVertexAttribArray(skyPositionParam);
        checkGLError("error disabling attribute");
    }
}
