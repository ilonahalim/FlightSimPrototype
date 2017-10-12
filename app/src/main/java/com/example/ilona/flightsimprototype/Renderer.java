package com.example.ilona.flightsimprototype;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Vibrator;
import android.os.Bundle;
import android.util.Log;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.google.vr.sdk.base.sensors.internal.Vector3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

import static android.R.attr.start;
import static android.opengl.GLES20.glGetError;

public class Renderer extends GvrActivity implements GvrView.StereoRenderer{
    protected float[] modelCube;
    protected float[] modelPosition;

    private static final String TAG = "Renderer";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CAMERA_Z = 0.501f;
    private Vector3d cameraCoor;
    private int[] prevQuad;
    private int[] currentQuad;
    private float[] forwardVec;
    private float[] transposeMatrix;
    private static final float TIME_DELTA = 0.3f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;
    private static final float MIN_ALTITUDE = 0.0f;

    private float altitude;

    private static final int COORDS_PER_VERTEX = 3;

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {0.0f, 2.0f, 0.0f, 1.0f};

    // Convenience vector for extracting the position from a matrix via multiplication.
    private static final float[] POS_MATRIX_MULTIPLY_VEC = {0, 0, 0, 1.0f};

    private static final float MIN_MODEL_DISTANCE = 3.0f;
    private static final float MAX_MODEL_DISTANCE = 7.0f;

    private static final String OBJECT_SOUND_FILE = "cube_sound.wav";
    private static final String SUCCESS_SOUND_FILE = "success.wav";

    private final float[] lightPosInEyeSpace = new float[4];

    short[] indices;

    private FloatBuffer cubeVertices;
    private FloatBuffer cubeColors;
    private FloatBuffer cubeFoundColors;
    private FloatBuffer cubeNormals;

    private int cubeProgram;

    private int cubePositionParam;
    private int cubeNormalParam;
    private int cubeColorParam;
    private int cubeModelParam;
    private int cubeModelViewParam;
    private int cubeModelViewProjectionParam;
    private int cubeLightPosParam;

    private float[] camera;
    private float[] view;
    private float[] headView;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] modelFloor;
    protected float[] modelSkybox;
    private Terrain myTerrain;
    private Terrain[] myTerrainArray;

    private SkyBox mySkybox;
    private EndlessTerrain[] myEndlessTerrain;
    private EndlessTerrain endlessTerrain;
    /*
    private EndlessTerrain endlessTerrain2;
    private EndlessTerrain endlessTerrain3;
    private EndlessTerrain endlessTerrain4;
    private EndlessTerrain endlessTerrain5;
    private EndlessTerrain endlessTerrain6;
    private EndlessTerrain endlessTerrain7;
    private EndlessTerrain endlessTerrain8;
    private EndlessTerrain endlessTerrain9;
    */

    private float totalTime = 0f;
    private int totalFrames = 0;

    private float[] tempPosition;
    private float[] headRotation;

    private float objectDistance = MAX_MODEL_DISTANCE / 2.0f;
    private float floorDepth = 20f;

    private Vibrator vibrator;

    private GvrAudioEngine gvrAudioEngine;
    private volatile int sourceId = GvrAudioEngine.INVALID_ID;
    private volatile int successSourceId = GvrAudioEngine.INVALID_ID;


    private ShaderLoader myShaderLoader;


    public static int loadTexture(Context context, int resourceId)
    {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
            //Bitmap bitmap = BitmapFactory.decodeResource(App.context().getResources(), R.drawable.grass);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_MIRRORED_REPEAT);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            checkGLError("Texture");
            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public static int loadCubeTexture(Context context, int[] resourceIdArray)
    {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureHandle[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_R, GLES20.GL_CLAMP_TO_EDGE);


            for(int i = 0; i<6;i++) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;   // No pre-scaling

                // Read in the resource
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceIdArray[i], options);

                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, bitmap, 0);
                checkGLError("Texture");
                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bitmap.recycle();
            }
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
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

    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myShaderLoader = new ShaderLoader();
        initializeGvrView();
        modelCube = new float[16];
        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        altitude = 0.0f;

        modelFloor = new float[20];
        //myTerrain = new Terrain(1);
        /*
        myTerrainArray = new Terrain[9];
        for(int i = 0; i< myTerrainArray.length; i++){
            myTerrainArray[i] = new Terrain(1);
        }
*/
        endlessTerrain = new EndlessTerrain();
        /*
        endlessTerrain2 = new EndlessTerrain();
        endlessTerrain3 = new EndlessTerrain();
        endlessTerrain4 = new EndlessTerrain();
        endlessTerrain5 = new EndlessTerrain();
        endlessTerrain6 = new EndlessTerrain();
        endlessTerrain7 = new EndlessTerrain();
        endlessTerrain8 = new EndlessTerrain();
        endlessTerrain9 = new EndlessTerrain();
        */

        myEndlessTerrain = new EndlessTerrain[9];
        for(int i = 0; i< myEndlessTerrain.length; i++){
            myEndlessTerrain[i] = new EndlessTerrain();
        }

        modelSkybox = new float[16];
        mySkybox = new SkyBox();

        tempPosition = new float[4];
        // Model first appears directly in front of user.
        modelPosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
        headRotation = new float[4];
        headView = new float[16];
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cameraCoor = new Vector3d(0f, 0f, 0f);
        prevQuad = new int[2];
        currentQuad = new int[2];
        forwardVec = new float[3];
        transposeMatrix = new float[16];
        //Matrix.setLookAtM(camera, 0, (float) cameraCoor.x + 512, (float) cameraCoor.y, (float) cameraCoor.z - 512, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        Matrix.setLookAtM(camera, 0, 0, 0, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        // Initialize 3D audio engine.
        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
    }

    public void initializeGvrView() {
        setContentView(R.layout.activity_main);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);

        // Enable Cardboard-trigger feedback with Daydream headsets. This is a simple way of supporting
        // Daydream controller input for basic interactions using the existing Cardboard trigger API.
        gvrView.enableCardboardTriggerEmulation();

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }

        setGvrView(gvrView);
    }

    @Override
    public void onPause() {
        gvrAudioEngine.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        gvrAudioEngine.resume();
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    /**
     * Creates the buffers we use to store information about the 3D world.
     *
     * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
     * Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COORDS.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        cubeVertices = bbVertices.asFloatBuffer();
        cubeVertices.put(WorldLayoutData.CUBE_COORDS);
        cubeVertices.position(0);

        ByteBuffer bbColors = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COLORS.length * 4);
        bbColors.order(ByteOrder.nativeOrder());
        cubeColors = bbColors.asFloatBuffer();
        cubeColors.put(WorldLayoutData.CUBE_COLORS);
        cubeColors.position(0);

        ByteBuffer bbFoundColors =
                ByteBuffer.allocateDirect(WorldLayoutData.CUBE_FOUND_COLORS.length * 4);
        bbFoundColors.order(ByteOrder.nativeOrder());
        cubeFoundColors = bbFoundColors.asFloatBuffer();
        cubeFoundColors.put(WorldLayoutData.CUBE_FOUND_COLORS);
        cubeFoundColors.position(0);

        ByteBuffer bbNormals = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_NORMALS.length * 4);
        bbNormals.order(ByteOrder.nativeOrder());
        cubeNormals = bbNormals.asFloatBuffer();
        cubeNormals.put(WorldLayoutData.CUBE_NORMALS);
        cubeNormals.position(0);


        int vertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int gridShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
        int passthroughShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        int endlessTerrainVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.endless_terrain_vertex);
        int terrainVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.terrain_vertex);
        int terrainFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.terrain_fragment);

        int skyVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.skybox_vertex);
        int skyFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.skybox_fragment);

        cubeProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(cubeProgram, vertexShader);
        GLES20.glAttachShader(cubeProgram, passthroughShader);
        GLES20.glLinkProgram(cubeProgram);
        GLES20.glUseProgram(cubeProgram);

        checkGLError("Cube program");

        cubePositionParam = GLES20.glGetAttribLocation(cubeProgram, "a_Position");
        cubeNormalParam = GLES20.glGetAttribLocation(cubeProgram, "a_Normal");
        cubeColorParam = GLES20.glGetAttribLocation(cubeProgram, "a_Color");

        cubeModelParam = GLES20.glGetUniformLocation(cubeProgram, "u_Model");
        cubeModelViewParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVMatrix");
        cubeModelViewProjectionParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVP");
        cubeLightPosParam = GLES20.glGetUniformLocation(cubeProgram, "u_LightPos");

        checkGLError("Cube program params");

        Matrix.setIdentityM(modelFloor, 0);
        Matrix.translateM(modelFloor, 0, 0, -floorDepth, 0); // Floor appears below user.

        int[] skyResources = new int[6];
        skyResources[0] = R.drawable.right;
        skyResources[1] = R.drawable.left;
        skyResources[2] = R.drawable.top;
        skyResources[3] = R.drawable.bottom;
        skyResources[4] = R.drawable.back;
        skyResources[5] = R.drawable.front;
        mySkybox.setUpOpenGl(skyVertexShader, skyFragmentShader, loadCubeTexture(App.context(), skyResources));
        Matrix.setIdentityM(modelSkybox, 0);
        Matrix.translateM(modelSkybox, 0, modelPosition[0], modelPosition[1], modelPosition[2]);

        //myTerrain.setTexture(loadTexture(this, R.drawable.grass));
        //myTerrain.generateFlatTerrain();
        //myTerrain.linkFloorProgram(terrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));
        /*
        for(int i = 0; i< myTerrainArray.length; i++){
            myTerrainArray[i].generateFlatTerrain();
            myTerrainArray[i].linkFloorProgram(terrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));
        }
*/
        endlessTerrain.generateFlatTerrain();
        endlessTerrain.setQuadrantIndex(0, 0);
        endlessTerrain.linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));
/*
        endlessTerrain2.generateFlatTerrain();
        endlessTerrain2.setQuadrantIndex(0, -1);
        endlessTerrain2.linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));

        endlessTerrain3.generateFlatTerrain();
        endlessTerrain3.setQuadrantIndex(0, 1);
        endlessTerrain3.linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));

        endlessTerrain4.generateFlatTerrain();
        endlessTerrain4.setQuadrantIndex(1, -1);
        endlessTerrain4.linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));

        endlessTerrain5.generateFlatTerrain();
        endlessTerrain5.setQuadrantIndex(1, 0);
        endlessTerrain5.linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));

        endlessTerrain6.generateFlatTerrain();
        endlessTerrain6.setQuadrantIndex(1, 1);
        endlessTerrain6.linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));

        endlessTerrain7.generateFlatTerrain();
        endlessTerrain7.setQuadrantIndex(-1, -1);
        endlessTerrain7.linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));

        endlessTerrain8.generateFlatTerrain();
        endlessTerrain8.setQuadrantIndex(-1, 0);
        endlessTerrain8.linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));

        endlessTerrain9.generateFlatTerrain();
        endlessTerrain9.setQuadrantIndex(-1, 1);
        endlessTerrain9.linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, loadTexture(this, R.drawable.grass));
*/

        int tempTexture = loadTexture(this, R.drawable.grass);
        for(int i = 0; i< myEndlessTerrain.length; i++){
            myEndlessTerrain[i].generateFlatTerrain();
            myEndlessTerrain[i].linkFloorProgram(endlessTerrainVertexShader, terrainFragmentShader, tempTexture);
        }
        int n = 0;
        for (int xTemp = -1; xTemp <= 1; xTemp++){
            for (int zTemp = -1; zTemp <= 1; zTemp++){
                myEndlessTerrain[n].setQuadrantIndex(xTemp, zTemp);
                n++;
                Log.d(TAG, "onSurfaceCreated: index:[" + xTemp + ", " + zTemp + "]");
            }
        }

        // Avoid any delays during start-up due to decoding of sound files.
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        // Start spatial audio playback of OBJECT_SOUND_FILE at the model position. The
                        // returned sourceId handle is stored and allows for repositioning the sound object
                        // whenever the cube position changes.
                        gvrAudioEngine.preloadSoundFile(OBJECT_SOUND_FILE);
                        sourceId = gvrAudioEngine.createSoundObject(OBJECT_SOUND_FILE);
                        gvrAudioEngine.setSoundObjectPosition(
                                sourceId, modelPosition[0], modelPosition[1], modelPosition[2]);
                        gvrAudioEngine.playSound(sourceId, true /* looped playback */);
                        // Preload an unspatialized sound to be played on a successful trigger on the cube.
                        gvrAudioEngine.preloadSoundFile(SUCCESS_SOUND_FILE);
                    }
                })
                .start();

        updateModelPosition();

        checkGLError("onSurfaceCreated");
        Log.d(TAG, "Quad Change: current quad:" + currentQuad[0] +", " + currentQuad[1]);
    }

    /**
     * Updates the cube model position.
     */
    protected void updateModelPosition() {
        Matrix.setIdentityM(modelCube, 0);
        Matrix.translateM(modelCube, 0, modelPosition[0], modelPosition[1], modelPosition[2]);

        // Update the sound location to match it with the new cube position.
        if (sourceId != GvrAudioEngine.INVALID_ID) {
            gvrAudioEngine.setSoundObjectPosition(
                    sourceId, modelPosition[0], modelPosition[1], modelPosition[2]);
        }
        checkGLError("updateCubePosition");
    }

    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    private String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        //setCubeRotation();

        // Build the camera matrix and apply it to the ModelView.
        //Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        //Matrix.setLookAtM(camera, 0, (float) cameraCoor.x, (float) cameraCoor.y, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        //Matrix.translateM(camera, 0, 0, -50, 0);
        headTransform.getHeadView(headView, 0);

        // Update the 3d audio engine with the most recent head rotation.
        headTransform.getQuaternion(headRotation, 0);
        gvrAudioEngine.setHeadRotation(
                headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
        //Log.d("ALTITUDE", "altitude = " + altitude + "   forward vec=  " +forwardVec[1]);
        //Log.d(TAG, "On new frame: current pos:" + cameraCoor.x +", " + cameraCoor.z);
        //Log.d(TAG, "On new frame: current quad:" + currentQuad[0] +", " + currentQuad[1]);
        headTransform.getForwardVector(forwardVec, 0);
        //if(isCrashLanding(altitude + forwardVec[1])){
        //    resetPosition();
        //}
        if(!isCrashLanding(altitude + forwardVec[1])){
        //else{
            Matrix.translateM(camera, 0, -forwardVec[0], -forwardVec[1], -forwardVec[2]);
            Matrix.translateM(modelSkybox, 0, forwardVec[0], forwardVec[1], forwardVec[2]);
            Matrix.translateM(modelCube, 0, forwardVec[0], forwardVec[1], forwardVec[2]);
            altitude += forwardVec[1];
            cameraCoor.x += forwardVec[0];
            cameraCoor.y += forwardVec[1];
            cameraCoor.z += forwardVec[2];
            Log.d(TAG, "On new frame: current pos:" + cameraCoor.x +", " + cameraCoor.z);
            Log.d(TAG, "On new frame: current quad:" + currentQuad[0] +", " + currentQuad[1]);
            if(isQuadChange()){
                //handleQuadChange();
                //endlessTerrain.setQuadrantIndex(currentQuad[0], currentQuad[1]);
            }
        }

        //Log.d("NEW FRAME", "Z = " + cameraCoor.z + "   forward Z=  " +forwardVec[2]);
        // Regular update call to GVR audio engine.
        gvrAudioEngine.update();

        checkGLError("onReadyToDraw");
        checkSpeed();
    }

    protected void setCubeRotation() {
        Matrix.rotateM(modelCube, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);
    }

    /**
     * Draws a frame for an eye.
     *
     * @param eye The eye to render. Includes all required transformations.
     */
    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGLError("colorParam");

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        // Set the position of the light
        Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(modelView, 0, view, 0, modelCube, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawCube();
        //mySkybox.drawSkybox(modelViewProjection);

        Matrix.multiplyMM(modelView, 0, view, 0, modelSkybox, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        mySkybox.drawSkybox(modelViewProjection);

        // Set modelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, modelFloor, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        //myTerrain.generateFlatTerrain();
        //myTerrain.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, cameraCoor);

        endlessTerrain.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        //endlessTerrain2.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        //endlessTerrain3.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        //endlessTerrain4.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        //endlessTerrain5.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        //endlessTerrain6.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        //endlessTerrain7.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        //endlessTerrain8.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        //endlessTerrain9.drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);

        /*
        for(int i = 0; i< myEndlessTerrain.length; i++){
            myEndlessTerrain[i].drawFloor(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        }
*/


    }

    @Override
    public void onFinishFrame(Viewport viewport) {}

    /**
     * Draw the cube.
     *
     * <p>We've set all of our transformation matrices. Now we simply pass them into the shader.
     */
    public void drawCube() {

        GLES20.glUseProgram(cubeProgram);

        GLES20.glUniform3fv(cubeLightPosParam, 1, lightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(cubeModelParam, 1, false, modelCube, 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(cubeModelViewParam, 1, false, modelView, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(
                cubePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, cubeVertices);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(cubeModelViewProjectionParam, 1, false, modelViewProjection, 0);

        // Set the normal positions of the cube, again for shading
        GLES20.glVertexAttribPointer(cubeNormalParam, 3, GLES20.GL_FLOAT, false, 0, cubeNormals);
        GLES20.glVertexAttribPointer(cubeColorParam, 4, GLES20.GL_FLOAT, false, 0,
                isLookingAtObject() ? cubeFoundColors : cubeColors);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(cubePositionParam);
        GLES20.glEnableVertexAttribArray(cubeNormalParam);
        GLES20.glEnableVertexAttribArray(cubeColorParam);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(cubePositionParam);
        GLES20.glDisableVertexAttribArray(cubeNormalParam);
        GLES20.glDisableVertexAttribArray(cubeColorParam);

        checkGLError("Drawing cube");

    }


    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");

        if (isLookingAtObject()) {
            successSourceId = gvrAudioEngine.createStereoSound(SUCCESS_SOUND_FILE);
            gvrAudioEngine.playSound(successSourceId, false /* looping disabled */);
            hideObject();
        }

        // Always give user feedback.
        vibrator.vibrate(50);
    }

    /**
     * Find a new random position for the object.
     *
     * <p>We'll rotate it around the Y-axis so it's out of sight, and then up or down by a little bit.
     */
    protected void hideObject() {
        float[] rotationMatrix = new float[16];
        float[] posVec = new float[4];

        // First rotate in XZ plane, between 90 and 270 deg away, and scale so that we vary
        // the object's distance from the user.
        float angleXZ = (float) Math.random() * 180 + 90;
        Matrix.setRotateM(rotationMatrix, 0, angleXZ, 0f, 1f, 0f);
        float oldObjectDistance = objectDistance;
        objectDistance =
                (float) Math.random() * (MAX_MODEL_DISTANCE - MIN_MODEL_DISTANCE) + MIN_MODEL_DISTANCE;
        float objectScalingFactor = objectDistance / oldObjectDistance;
        Matrix.scaleM(rotationMatrix, 0, objectScalingFactor, objectScalingFactor, objectScalingFactor);
        Matrix.multiplyMV(posVec, 0, rotationMatrix, 0, modelCube, 12);

        float angleY = (float) Math.random() * 80 - 40; // Angle in Y plane, between -40 and 40.
        angleY = (float) Math.toRadians(angleY);
        float newY = (float) Math.tan(angleY) * objectDistance;

        modelPosition[0] = posVec[0];
        modelPosition[1] = newY;
        modelPosition[2] = posVec[2];

        updateModelPosition();
    }

    /**
     * Check if user is looking at object by calculating where the object is in eye-space.
     *
     * @return true if the user is looking at the object.
     */
    private boolean isLookingAtObject() {
        // Convert object space to camera space. Use the headView from onNewFrame.

        Matrix.multiplyMM(modelView, 0, headView, 0, modelCube, 0);
        Matrix.multiplyMV(tempPosition, 0, modelView, 0, POS_MATRIX_MULTIPLY_VEC, 0);

        float pitch = (float) Math.atan2(tempPosition[1], -tempPosition[2]);
        float yaw = (float) Math.atan2(tempPosition[0], -tempPosition[2]);

        return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
    }
    private boolean isCrashLanding(float tempAlt) {
        return tempAlt < MIN_ALTITUDE;
    }

    private void resetPosition(){
        Matrix.setLookAtM(camera, 0, 0, 0, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        Matrix.setIdentityM(modelCube, 0);
        Matrix.translateM(modelCube, 0, modelPosition[0], modelPosition[1], modelPosition[2]);
        Matrix.setIdentityM(modelSkybox, 0);
        Matrix.translateM(modelSkybox, 0, modelPosition[0], modelPosition[1], modelPosition[2]);
        altitude = 0;
    }

    private boolean isQuadChange(){
        int i = 0;
        int j = 0;
        /*
        if (cameraCoor.x >= 0){
            while (i*512-256 < cameraCoor.x){
                i++;
            }
        }
        else {
            while ((i*512-256)*-1 > cameraCoor.x){
                i--;
            }
        }
        if (cameraCoor.z >= 0){
            while (i*512-256 < cameraCoor.z){
                j++;
            }
        }
        else {
            while ((i*512-256)*-1 > cameraCoor.z){
                j--;
            }
        }
        */
        currentQuad[0] = (int) Math.round(cameraCoor.x/512);
        currentQuad[1] = (int) Math.round(cameraCoor.z/512);
        if (currentQuad[0] != prevQuad[0] || currentQuad[1] != prevQuad[1]){
            return true;
        }
        else
            return false;
    }

    private void handleQuadChange(){
        if(prevQuad[0] < currentQuad[0]){//move to +x axis
            for(int i = 0; i<9;i++){
                int[] temp = myEndlessTerrain[i].getQuadrantIndex();
                    myEndlessTerrain[i].setQuadrantIndex(temp[0]+1, temp[1]);
            }
        }
        else if (prevQuad[0] > currentQuad[0]) {//move to -x axis
            for(int i = 0; i<9;i++){
                int[] temp = myEndlessTerrain[i].getQuadrantIndex();
                myEndlessTerrain[i].setQuadrantIndex(temp[0]-1, temp[1]);
            }
        }
        if(prevQuad[1] < currentQuad[1]){ //move to +z axis
            for(int i = 0; i<9;i++){
                int[] temp = myEndlessTerrain[i].getQuadrantIndex();
                myEndlessTerrain[i].setQuadrantIndex(temp[0], temp[1]+1);
            }
        }
        else if (prevQuad[1] > currentQuad[1]) { //move to -z axis
            for(int i = 0; i<9;i++){
                int[] temp = myEndlessTerrain[i].getQuadrantIndex();
                myEndlessTerrain[i].setQuadrantIndex(temp[0], temp[1]-1);
            }
        }
        prevQuad = currentQuad;
    }

    private void checkSpeed(){
        long start = System.nanoTime();

        long stop = System.nanoTime();

        totalTime += stop - start;
        totalFrames++;

        Log.d("TEST", (totalTime / totalFrames) / 1000000 + " ms");
    }
}