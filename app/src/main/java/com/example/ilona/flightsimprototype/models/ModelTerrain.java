package com.example.ilona.flightsimprototype.models;

import android.opengl.GLES20;
import android.util.Log;

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

public class ModelTerrain {
    String TAG = "ModelTerrain";
    private int terrainProgram;

    private float[] terrainModel;

    private float[] verticesArray;
    private float[] normalsArray;
    private float[] textureArray;
    private short[] indicesArray;

    private float[] vertices2Array;
    private float[] normals2Array;
    private float[] texture2Array;
    private short[] indices2Array;

    private FloatBuffer terrainVertices;
    private FloatBuffer terrainNormals;
    private FloatBuffer terrainTextureCoords;
    private ShortBuffer terrainIndices;

    private FloatBuffer terrainVerticesLod1;
    private FloatBuffer terrainNormalsLod1;
    private FloatBuffer terrainTextureCoordsLod1;
    private ShortBuffer terrainIndicesLod1;

    private int terrainModelParam;
    private int terrainModelViewParam;
    private int terrainModelViewProjectionParam;
    private int terrainLightPosParam;
    private int terrainTextureParam;
    private int terrainVerticesParam;
    private int terrainNormalParam;
    private int terrainTextureCoordsParam;
    private int terrainQuadrantParam;

    public float[] quadrant;
    public int[] quadrantIndex;

    ObjLoader myObjLoader;

    private int textureID;

    public ModelTerrain(){
        terrainModel = new float[20];
        quadrant = new float[2];
        quadrantIndex = new int[2];
    }

    public void setResources(ObjLoader objLoader, int textureResID){
        textureID = TextureLoader.loadTexture(App.context(), textureResID);
        myObjLoader = objLoader;
        verticesArray = myObjLoader.verticesArray;
        normalsArray = myObjLoader.normalsArray;
        textureArray = myObjLoader.textureArray;
        indicesArray = myObjLoader.indicesArray;

        float min = objLoader.min;
        float max = objLoader.max;
        Log.d(TAG, "setResources: terrain size = " + (max-min) + " max = " + max + " min = " + min);
        Log.d(TAG, "setResources: vertex count = " + objLoader.vertexCount + " grid size = " + objLoader.gridSize);

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(verticesArray.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        terrainVertices = bbVertices.asFloatBuffer();
        terrainVertices.put(verticesArray);
        terrainVertices.position(0);

        ByteBuffer bbNormals = ByteBuffer.allocateDirect(normalsArray.length * 4);
        bbNormals.order(ByteOrder.nativeOrder());
        terrainNormals = bbNormals.asFloatBuffer();
        terrainNormals.put(normalsArray);
        terrainNormals.position(0);

        ByteBuffer bbTextureCoords = ByteBuffer.allocateDirect(textureArray.length * 4);
        bbTextureCoords.order(ByteOrder.nativeOrder());
        terrainTextureCoords = bbTextureCoords.asFloatBuffer();
        terrainTextureCoords.put(textureArray);
        terrainTextureCoords.position(0);

        ByteBuffer bbIndices = ByteBuffer.allocateDirect(indicesArray.length * 2);
        bbIndices.order(ByteOrder.nativeOrder());
        terrainIndices = bbIndices.asShortBuffer();
        terrainIndices.put(indicesArray);
        terrainIndices.position(0);
    }

    public void setQuadrantIndex(int xChunk, int zChunk){
        quadrantIndex[0] = xChunk;
        quadrantIndex[1] = zChunk;
    }

    public int[] getQuadrantIndex(){
        return quadrantIndex;
    }

    public void setUpOpenGl(int vertexShader, int fragmentShader){
        terrainProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(terrainProgram, vertexShader);
        GLES20.glAttachShader(terrainProgram, fragmentShader);
        GLES20.glLinkProgram(terrainProgram);
        GLES20.glUseProgram(terrainProgram);

        terrainModelParam = GLES20.glGetUniformLocation(terrainProgram, "u_Model");
        terrainModelViewParam = GLES20.glGetUniformLocation(terrainProgram, "u_MVMatrix");
        terrainModelViewProjectionParam = GLES20.glGetUniformLocation(terrainProgram, "u_MVPMatrix");
        terrainTextureParam = GLES20.glGetUniformLocation(terrainProgram, "u_Texture");
        terrainLightPosParam = GLES20.glGetUniformLocation(terrainProgram, "u_LightPos");
        terrainQuadrantParam = GLES20.glGetUniformLocation(terrainProgram, "u_Quadrant");

        terrainVerticesParam = GLES20.glGetAttribLocation(terrainProgram, "a_Position");
        terrainNormalParam = GLES20.glGetAttribLocation(terrainProgram, "a_Normal");
        terrainTextureCoordsParam = GLES20.glGetAttribLocation(terrainProgram, "a_TexCoordinate");
    }

    public void draw(float[] lightPosInEyeSpace, float[] modelView, float[] modelViewProjection, int[] camQuadrant){
        quadrant[0] = camQuadrant[0] + quadrantIndex[0];
        quadrant[1] = camQuadrant[1] + quadrantIndex[1];

        GLES20.glDisable(GL_CULL_FACE);
        GLES20.glUseProgram(terrainProgram);
        //checkGLError("using floor program");

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(terrainTextureParam, 0);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform2fv(terrainQuadrantParam, 1, quadrant, 0);
        GLES20.glUniform3fv(terrainLightPosParam, 1, lightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(terrainModelParam, 1, false, terrainModel, 0);
        GLES20.glUniformMatrix4fv(terrainModelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(terrainModelViewProjectionParam, 1, false, modelViewProjection, 0);
        //checkGLError("uniforms");

        GLES20.glVertexAttribPointer(terrainVerticesParam, 3, GLES20.GL_FLOAT, false, 0, terrainVertices);
        //checkGLError("vertex attrib pointer vertices");
        GLES20.glVertexAttribPointer(terrainNormalParam, 3, GLES20.GL_FLOAT, false, 0, terrainNormals);
        //checkGLError("vertex attrib normals");
        GLES20.glVertexAttribPointer(terrainTextureCoordsParam, 2, GLES20.GL_FLOAT, false, 0, terrainTextureCoords);
        //checkGLError("vertex attrib texture coords");

        GLES20.glEnableVertexAttribArray(terrainVerticesParam);
        GLES20.glEnableVertexAttribArray(terrainNormalParam);
        GLES20.glEnableVertexAttribArray(terrainTextureCoordsParam);
        //checkGLError("enable vertex attribs");

        glDisable(GL_CULL_FACE);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesArray.length, GLES20.GL_UNSIGNED_SHORT, terrainIndices);
        //checkGLError("drawing floor");

        GLES20.glDisableVertexAttribArray(terrainVerticesParam);
        GLES20.glDisableVertexAttribArray(terrainNormalParam);
        GLES20.glDisableVertexAttribArray(terrainTextureCoordsParam);
    }


}
