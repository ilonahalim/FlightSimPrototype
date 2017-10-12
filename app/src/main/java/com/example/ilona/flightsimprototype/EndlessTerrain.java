package com.example.ilona.flightsimprototype;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;

import com.google.vr.sdk.base.sensors.internal.Vector3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glGetError;


public class EndlessTerrain{
    private static final String TAG = "TERRAIN";
    private int VERTEX_COUNT = 256;
    private int SIZE = 512;
    private int MAX_HEIGHT = 80;
    private int MAX_PIXEL_COLOR = 256 * 256 * 256;

    private FloatBuffer floorVertices;
    private FloatBuffer floorColors;
    private FloatBuffer floorNormals;
    private FloatBuffer floorTextureCoords;
    private ShortBuffer floorIndices;

    public int count;
    public float[] vertices;
    public float[] normals;
    public float[] colors;
    public float[] textureCoords;
    public float[] quadrant;
    public int[] quadrantIndex;
    public short[] indices;

    private int floorProgram;

    private int floorPositionParam;
    private int floorNormalParam;
    private int floorColorParam;
    private int floorTextureCoordsParam;
    private int floorModelParam;
    private int floorModelViewParam;
    private int floorModelViewProjectionParam;
    private int floorLightPosParam;
    private int floorQuadrantParam;
    private int floorTextureParam;
    private int mTextureDataHandle;


    private float[] modelFloor;

    private float floorDepth = 20f;
    private int COORDS_PER_VERTEX = 3;

    private Bitmap bitmap;
    private HeightGenerator hGen;
    private int vertexShader;
    private int fragmentShader;

    public EndlessTerrain(){
        quadrantIndex = new int[2];
        hGen = new HeightGenerator();
        bitmap = BitmapFactory.decodeResource(App.context().getResources(), R.drawable.heightmap);
        modelFloor = new float[20];
        count = VERTEX_COUNT * VERTEX_COUNT;
        vertices = new float[count * 3];
        normals = new float[count * 3];
        colors = new float[count*4];
        textureCoords = new float[count*2];
        quadrant = new float[2];
        indices = new short[6*(VERTEX_COUNT-1)*(VERTEX_COUNT-1)];

        ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(vertices.length * 4);
        bbFloorVertices.order(ByteOrder.nativeOrder());
        floorVertices = bbFloorVertices.asFloatBuffer();
        //floorVertices.put(vertices);
        //floorVertices.position(0);

        ByteBuffer bbTextureCoords = ByteBuffer.allocateDirect(textureCoords.length * 4);
        bbTextureCoords.order(ByteOrder.nativeOrder());
        floorTextureCoords = bbTextureCoords.asFloatBuffer();
        //floorTextureCoords.put(textureCoords);
        //floorTextureCoords.position(0);

        //ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_NORMALS.length * 4);
        ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(normals.length * 4);
        bbFloorNormals.order(ByteOrder.nativeOrder());
        floorNormals = bbFloorNormals.asFloatBuffer();
        //floorNormals.put(normals);
        //floorNormals.position(0);

        //ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COLORS.length * 4);
        ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(colors.length * 4);
        bbFloorColors.order(ByteOrder.nativeOrder());
        floorColors = bbFloorColors.asFloatBuffer();
        //floorColors.put(colors);
        //floorColors.position(0);

        ByteBuffer bbFloorIndices = ByteBuffer.allocateDirect(indices.length * 2);
        bbFloorIndices.order(ByteOrder.nativeOrder());
        floorIndices = bbFloorIndices.asShortBuffer();
        //floorIndices.put(indices);
        //floorIndices.position(0);
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

    public void setQuadrantIndex(int xChunk, int zChunk){
        quadrantIndex[0] = xChunk;
        quadrantIndex[1] = zChunk;
    }

    public int[] getQuadrantIndex(){
        return quadrantIndex;
    }

    public void generateFlatTerrain(){
        int vertexPointer = 0;
        //VERTEX_COUNT = bitmap.getHeight();
        //int maxY = -20;
        //int minY = -80;
        for(int i=0;i<VERTEX_COUNT;i++){
            for(int j=0;j<VERTEX_COUNT;j++){
                vertices[vertexPointer*3] = (float)j/((float)VERTEX_COUNT - 1) * SIZE - SIZE/2;
                vertices[vertexPointer*3+1] = 0;
                vertices[vertexPointer*3+2] = (float)i/((float)VERTEX_COUNT - 1) * SIZE - SIZE/2;
                normals[vertexPointer*3] = 1;
                normals[vertexPointer*3+1] = 1;
                normals[vertexPointer*3+2] = 1;
                colors[vertexPointer*4] = 1.0f;
                colors[vertexPointer*4+1] = 0.6523f;
                colors[vertexPointer*4+2] = 0.0f;
                colors[vertexPointer*4+3] = 1.0f;
                textureCoords[vertexPointer*2] = (float)j/((float)VERTEX_COUNT - 1);
                textureCoords[vertexPointer*2+1] = (float)i/((float)VERTEX_COUNT - 1);
                vertexPointer++;
            }
        }

        int pointer = 0;
        for(int gz=0;gz<VERTEX_COUNT-1;gz++){
            for(int gx=0;gx<VERTEX_COUNT-1;gx++){
                int topLeft = (gz*VERTEX_COUNT)+gx;
                int topRight = topLeft + 1;
                int bottomLeft = ((gz+1)*VERTEX_COUNT)+gx;
                int bottomRight = bottomLeft + 1;
                indices[pointer++] = (short) topLeft;
                indices[pointer++] = (short) bottomLeft;
                indices[pointer++] = (short) topRight;
                indices[pointer++] = (short) topRight;
                indices[pointer++] = (short) bottomLeft;
                indices[pointer++] = (short) bottomRight;
            }
        }

    }

    public void linkFloorProgram(int vertexShader, int fragmentShader, int texture){
        mTextureDataHandle = texture;
        floorProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(floorProgram, vertexShader);
        GLES20.glAttachShader(floorProgram, fragmentShader);
        GLES20.glLinkProgram(floorProgram);
        GLES20.glUseProgram(floorProgram);

        //checkGLError("Floor program");

        floorModelParam = GLES20.glGetUniformLocation(floorProgram, "u_Model");
        floorModelViewParam = GLES20.glGetUniformLocation(floorProgram, "u_MVMatrix");
        floorModelViewProjectionParam = GLES20.glGetUniformLocation(floorProgram, "u_MVPMatrix");
        floorLightPosParam = GLES20.glGetUniformLocation(floorProgram, "u_LightPos");
        //floorTextureParam = GLES20.glGetUniformLocation(floorProgram, "u_Texture");
        floorQuadrantParam = GLES20.glGetUniformLocation(floorProgram, "u_Quadrant");

        floorPositionParam = GLES20.glGetAttribLocation(floorProgram, "a_Position");
        floorNormalParam = GLES20.glGetAttribLocation(floorProgram, "a_Normal");
        floorColorParam = GLES20.glGetAttribLocation(floorProgram, "a_Color");
        //floorTextureCoordsParam = GLES20.glGetAttribLocation(floorProgram, "a_TexCoordinate");
    }

    private float getHeight(int x, int z, HeightGenerator gen){
        /*
        if (x<0 || x>=bitmap.getHeight() || z<0 || z>= bitmap.getWidth()){
            return 0;
        }
        float height = bitmap.getPixel(x, z);
        height += MAX_PIXEL_COLOR/2f;
        height /= MAX_PIXEL_COLOR/2f;
        height *= MAX_HEIGHT;
        return height;
        */
        return gen.generateHeight(x, z);
    }

    private Vector3d calcNormal(int x, int z, HeightGenerator gen){
        float heightL = getHeight(x-1, z, gen);
        float heightR = getHeight(x+1, z, gen);
        float heightD = getHeight(x, z-1, gen);
        float heightU = getHeight(x, z+1, gen);
        Vector3d normal = new Vector3d(heightL-heightR, 2f, heightD-heightU);
        normal.normalize();
        return normal;
    }

    /**
     * Draw the floor.
     *
     * <p>This feeds in data for the floor into the shader. Note that this doesn't feed in data about
     * position of the light, so if we rewrite our code to draw the floor first, the lighting might
     * look strange.
     */
    public void drawFloor(float[] lightPosInEyeSpace, float[] modelView, float[] modelViewProjection, Vector3d cameraPos) {
        int i = 0;
        int j = 0;
        if (cameraPos.x >= 0){
            while (i*SIZE-SIZE/2 < cameraPos.x){
                i++;
            }
        }
        else {
            while ((i*SIZE-SIZE/2)*-1 > cameraPos.x){
                i--;
            }
        }
        if (cameraPos.z >= 0){
            while (i*SIZE-SIZE/2 < cameraPos.z){
                j++;
            }
        }
        else {
            while ((i*SIZE-SIZE/2)*-1 > cameraPos.z){
                j--;
            }
        }
        quadrant[0] = i + quadrantIndex[0];
        quadrant[1] = j + quadrantIndex[0];

        floorVertices.put(vertices);
        floorVertices.position(0);

        floorTextureCoords.put(textureCoords);
        floorTextureCoords.position(0);

        floorNormals.put(normals);
        floorNormals.position(0);

        floorColors.put(colors);
        floorColors.position(0);

        floorIndices.put(indices);
        floorIndices.position(0);

        GLES20.glUseProgram(floorProgram);
        checkGLError("using floor program");

        // Set the active texture unit to texture unit 0.
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        //GLES20.glUniform1i(floorTextureParam, 0);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(floorLightPosParam, 1, lightPosInEyeSpace, 0);
        GLES20.glUniform2fv(floorQuadrantParam, 1, quadrant, 0);
        checkGLError("uniforms");
        GLES20.glUniformMatrix4fv(floorModelParam, 1, false, modelFloor, 0);
        GLES20.glUniformMatrix4fv(floorModelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(floorModelViewProjectionParam, 1, false, modelViewProjection, 0);
        checkGLError("uniforms");

        GLES20.glVertexAttribPointer(floorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, floorVertices);
        checkGLError("vertex attrib pointer vertices");
        GLES20.glVertexAttribPointer(floorNormalParam, 3, GLES20.GL_FLOAT, false, 0, floorNormals);
        checkGLError("vertex attrib normals");
        GLES20.glVertexAttribPointer(floorColorParam, 4, GLES20.GL_FLOAT, false, 0, floorColors);
        checkGLError("vertex attrib colors");
        //GLES20.glVertexAttribPointer(floorTextureCoordsParam, 2, GLES20.GL_FLOAT, false, 0, floorTextureCoords);
        //checkGLError("vertex attrib texture coords");

        GLES20.glEnableVertexAttribArray(floorPositionParam);
        GLES20.glEnableVertexAttribArray(floorNormalParam);
        GLES20.glEnableVertexAttribArray(floorColorParam);
        GLES20.glEnableVertexAttribArray(floorTextureCoordsParam);
        //checkGLError("enable vertex attribs");

        glDisable(GL_CULL_FACE);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, floorIndices);
        checkGLError("drawing floor");

        GLES20.glDisableVertexAttribArray(floorPositionParam);
        GLES20.glDisableVertexAttribArray(floorNormalParam);
        GLES20.glDisableVertexAttribArray(floorColorParam);
        GLES20.glDisableVertexAttribArray(floorTextureCoordsParam);


    }


}
