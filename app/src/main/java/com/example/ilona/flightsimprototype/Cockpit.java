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

public class Cockpit {
    private static final String TAG = "COCKPIT";
    private FloatBuffer cockpitVertices;

    private int cockpitProgram;

    private int cockpitPositionParam;
    private int cockpitModelViewProjectionParam;
    private int cockpitTextureParam;
    private int skyLightPosParam;
    private int textureDataHandle;

    private static final float SIZE = 7f;

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

    public Cockpit(){
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
        cockpitVertices = bbFloorVertices.asFloatBuffer();
        cockpitVertices.put(VERTICES);
        cockpitVertices.position(0);

        cockpitProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(cockpitProgram, vertexShader);
        GLES20.glAttachShader(cockpitProgram, fragmentShader);
        GLES20.glLinkProgram(cockpitProgram);
        GLES20.glUseProgram(cockpitProgram);

        //checkGLError("Floor program");
        cockpitModelViewProjectionParam = GLES20.glGetUniformLocation(cockpitProgram, "u_MVP");
        cockpitTextureParam = GLES20.glGetUniformLocation(cockpitProgram, "u_SkyBoxTexture");

        cockpitPositionParam = GLES20.glGetAttribLocation(cockpitProgram, "a_Position");
        checkGLError("error linking program");
    }

    public void drawCockpit(float[] modelViewProjection){
        GLES20.glUseProgram(cockpitProgram);

        checkGLError("error using program");
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        checkGLError("error activate texture");
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureDataHandle);
        checkGLError("error binding texture");
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 1.
        GLES20.glUniform1i(cockpitTextureParam, 1);
        checkGLError("error setting texture uniform");
        //checkGLError("sky texture param");
        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniformMatrix4fv(cockpitModelViewProjectionParam, 1, false, modelViewProjection, 0);
        checkGLError("error setting uniform");

        GLES20.glVertexAttribPointer(cockpitPositionParam, 3, GLES20.GL_FLOAT, false, 0, cockpitVertices);
        checkGLError("error setting attribute");
        GLES20.glEnableVertexAttribArray(cockpitPositionParam);
        checkGLError("error enabling attribute");

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        checkGLError("error drawing skybox");

        GLES20.glDisableVertexAttribArray(cockpitPositionParam);
        checkGLError("error disabling attribute");
    }
}
