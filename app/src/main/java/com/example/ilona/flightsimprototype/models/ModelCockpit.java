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
 * Created by Ilona on 11-Nov-17.
 */

public class ModelCockpit {
    private int cockpitProgram;

    private float[] cockpitModel;

    private float[] verticesArray;
    private float[] normalsArray;
    private float[] textureArray;
    private short[] indicesArray;

    private FloatBuffer cockpitVertices;
    private FloatBuffer cockpitNormals;
    private FloatBuffer cockpitTextureCoords;
    private ShortBuffer cockpitIndices;

    private int cockpitModelParam;
    private int cockpitModelViewParam;
    private int cockpitModelViewProjectionParam;
    private int cockpitLightPosParam;
    private int cockpitTextureParam;
    private int cockpitVerticesParam;
    private int cockpitNormalParam;
    private int cockpitTextureCoordsParam;

    ObjLoader myObjLoader;

    private int textureID;

    public ModelCockpit(){
        cockpitModel = new float[20];

    }

    public void setResources(ObjLoader objLoader, int textureResID){
        textureID = TextureLoader.loadTexture(App.context(), textureResID);
        myObjLoader = objLoader;
        verticesArray = myObjLoader.verticesArray;
        normalsArray = myObjLoader.normalsArray;
        textureArray = myObjLoader.textureArray;
        indicesArray = myObjLoader.indicesArray;

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(verticesArray.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        cockpitVertices = bbVertices.asFloatBuffer();
        cockpitVertices.put(verticesArray);
        cockpitVertices.position(0);

        ByteBuffer bbNormals = ByteBuffer.allocateDirect(normalsArray.length * 4);
        bbNormals.order(ByteOrder.nativeOrder());
        cockpitNormals = bbNormals.asFloatBuffer();
        cockpitNormals.put(normalsArray);
        cockpitNormals.position(0);

        ByteBuffer bbTextureCoords = ByteBuffer.allocateDirect(textureArray.length * 4);
        bbTextureCoords.order(ByteOrder.nativeOrder());
        cockpitTextureCoords = bbTextureCoords.asFloatBuffer();
        cockpitTextureCoords.put(textureArray);
        cockpitTextureCoords.position(0);

        ByteBuffer bbIndices = ByteBuffer.allocateDirect(indicesArray.length * 2);
        bbIndices.order(ByteOrder.nativeOrder());
        cockpitIndices = bbIndices.asShortBuffer();
        cockpitIndices.put(indicesArray);
        cockpitIndices.position(0);
    }

    public void setUpOpenGl(int vertexShader, int fragmentShader){
        cockpitProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(cockpitProgram, vertexShader);
        GLES20.glAttachShader(cockpitProgram, fragmentShader);
        GLES20.glLinkProgram(cockpitProgram);
        GLES20.glUseProgram(cockpitProgram);

        cockpitModelParam = GLES20.glGetUniformLocation(cockpitProgram, "u_Model");
        cockpitModelViewParam = GLES20.glGetUniformLocation(cockpitProgram, "u_MVMatrix");
        cockpitModelViewProjectionParam = GLES20.glGetUniformLocation(cockpitProgram, "u_MVPMatrix");
        cockpitTextureParam = GLES20.glGetUniformLocation(cockpitProgram, "u_Texture");
        cockpitLightPosParam = GLES20.glGetUniformLocation(cockpitProgram, "u_LightPos");

        cockpitVerticesParam = GLES20.glGetAttribLocation(cockpitProgram, "a_Position");
        cockpitNormalParam = GLES20.glGetAttribLocation(cockpitProgram, "a_Normal");
        cockpitTextureCoordsParam = GLES20.glGetAttribLocation(cockpitProgram, "a_TexCoordinate");
    }

    public void draw(float[] lightPosInEyeSpace, float[] modelView, float[] modelViewProjection){
        GLES20.glDisable(GL_CULL_FACE);
        GLES20.glUseProgram(cockpitProgram);
        //checkGLError("using floor program");

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(cockpitTextureParam, 0);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(cockpitLightPosParam, 1, lightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(cockpitModelParam, 1, false, cockpitModel, 0);
        GLES20.glUniformMatrix4fv(cockpitModelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(cockpitModelViewProjectionParam, 1, false, modelViewProjection, 0);
        //checkGLError("uniforms");

        GLES20.glVertexAttribPointer(cockpitVerticesParam, 3, GLES20.GL_FLOAT, false, 0, cockpitVertices);
        //checkGLError("vertex attrib pointer vertices");
        GLES20.glVertexAttribPointer(cockpitNormalParam, 3, GLES20.GL_FLOAT, false, 0, cockpitNormals);
        //checkGLError("vertex attrib normals");
        GLES20.glVertexAttribPointer(cockpitTextureCoordsParam, 2, GLES20.GL_FLOAT, false, 0, cockpitTextureCoords);
        //checkGLError("vertex attrib texture coords");

        GLES20.glEnableVertexAttribArray(cockpitVerticesParam);
        GLES20.glEnableVertexAttribArray(cockpitNormalParam);
        GLES20.glEnableVertexAttribArray(cockpitTextureCoordsParam);
        //checkGLError("enable vertex attribs");

        glDisable(GL_CULL_FACE);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesArray.length, GLES20.GL_UNSIGNED_SHORT, cockpitIndices);
        //checkGLError("drawing floor");

        GLES20.glDisableVertexAttribArray(cockpitVerticesParam);
        GLES20.glDisableVertexAttribArray(cockpitNormalParam);
        GLES20.glDisableVertexAttribArray(cockpitTextureCoordsParam);
    }


}
