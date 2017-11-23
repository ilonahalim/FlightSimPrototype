package com.example.ilona.flightsimprototype.gamestates;

import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Vibrator;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.example.ilona.flightsimprototype.models.Camera;
import com.example.ilona.flightsimprototype.models.PlaneModel;
import com.example.ilona.flightsimprototype.models.TerrainModel;
import com.example.ilona.flightsimprototype.utility.App;
import com.example.ilona.flightsimprototype.models.Hud;
import com.example.ilona.flightsimprototype.loaders.ObjLoader;
import com.example.ilona.flightsimprototype.R;
import com.example.ilona.flightsimprototype.loaders.ShaderLoader;
import com.example.ilona.flightsimprototype.models.SkyBox;
import com.example.ilona.flightsimprototype.loaders.TextureLoader;
import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.google.vr.sdk.base.sensors.internal.Vector3d;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Class: PlayActivity
 * Author: Ilona
 * <p> The purpose of this class is as a controller and model of the game play function.
 * It displays a Skybox, the terrain, the plane cockpit and HUD.
 * It retrieves user’s selection from the menu screen and then loads the appropriate textures and Obj files.
 * The game logic is also done in this class.</>
 */

public class PlayActivity extends GvrActivity implements GvrView.StereoRenderer{
    private static final String TAG = "PlayActivity";
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 2000.0f;

    private final static float[] LIGHT_DIR_IN_WORLD_SPACE = new float[] {0.0f, 2.0f, 0.0f, 0.0f}; // Directional light, shining down the Y axis
    private final static float QUADRANT_SIZE = 60.8294f; // The quadrant (chunk) size

    private static final String BACKGROUND_SOUND_FILE = "plane_sound.3gp";
    private static final String CRASH_SOUND_FILE = "crashlanding_sound.3gp";

    private int[] currentQuad;

    private InputManager myInputManager;
    private InputDevice myInputDevice;

    //transformation matrices
    protected float[] modelPosition;
    private float[] view;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] terrainTransformationMatrix;
    protected float[] skyboxTransformationMatrix;
    protected float[] planeTransformationMatrix;
    private float[] headRotation;


    private Camera camera;
    private SkyBox skybox;
    private PlaneModel planeModel;
    private TerrainModel[] terrainArray;
    private Hud myHud;
    ObjLoader terrainObjLoader;


    boolean isStart;
    Vector3d forwardVector = new Vector3d(0,0,1);

    private Vibrator vibrator;

    private GvrAudioEngine gvrAudioEngine;
    private volatile int sourceId = GvrAudioEngine.INVALID_ID;
    private volatile int crashSourceId = GvrAudioEngine.INVALID_ID;

    private ShaderLoader myShaderLoader;

    /**
     * Sets the view to GvrView and initialize member variables.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeGvrView();

        isStart = true;

        myInputManager = (InputManager) App.context().getSystemService(Context.INPUT_SERVICE);
        myShaderLoader = new ShaderLoader();

        camera = new Camera();
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];

        terrainTransformationMatrix = new float[20];

        terrainArray = new TerrainModel[9];
        for(int i = 0; i< terrainArray.length; i++){
            terrainArray[i] = new TerrainModel();
        }

        skyboxTransformationMatrix = new float[16];
        skybox = new SkyBox();

        planeTransformationMatrix = new float[16];
        planeModel = new PlaneModel();
        modelPosition = new float[] {0.0f, 0.0f, 0.0f};
        headRotation = new float[4];
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        currentQuad = new int[2];

        // Initialize 3D audio engine.
        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
    }

    /**
     * Sets view to GvrView.
     */
    public void initializeGvrView() {
        setContentView(R.layout.activity_main);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(false);

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

    /**
     * Override method from GvrActivity.
     * Pauses audio.
     */
    @Override
    public void onPause() {
        gvrAudioEngine.pause();
        super.onPause();
    }

    /**
     * Override method from GvrActivity.
     * Resumes audio.
     */
    @Override
    public void onResume() {
        super.onResume();
        gvrAudioEngine.resume();
    }

    /**
     * Implemented method from StereoRenderer.
     */
    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    /**
     * Implemented method from StereoRenderer.
     */
    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    /**
     * Implemented method from StereoRenderer.
     * Load shaders, textures and Obj files. Set up the terrains, plane, skybox and plane HUD.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.

        int[] skyResources = new int[6];
        int terrainTextureID;
        if(getTerrainSelection().equals("1")){
            skyResources[0] = R.drawable.criminal_impact_lf;
            skyResources[1] = R.drawable.criminal_impact_rt;
            skyResources[2] = R.drawable.criminal_impact_up;
            skyResources[3] = R.drawable.criminal_impact_dn;
            skyResources[4] = R.drawable.criminal_impact_ft;
            skyResources[5] = R.drawable.criminal_impact_bk;

            terrainTextureID = R.drawable.play_night_terrain;
        }
        else {
            skyResources[0] = R.drawable.day_right;
            skyResources[1] = R.drawable.day_left;
            skyResources[2] = R.drawable.day_top;
            skyResources[3] = R.drawable.day_bottom;
            skyResources[4] = R.drawable.day_back;
            skyResources[5] = R.drawable.day_front;

            terrainTextureID = R.drawable.play_day_terrain;
        }

        int endlessTerrainVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.terrain_vertex);
        int endlessTerrainFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.terrain_fragment);

        int skyVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.skybox_vertex);
        int skyFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.skybox_fragment);

        int textureVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.textured_model_vertex);
        int textureFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.textured_model_fragment);

        int guiVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.gui_vertex);
        int guiFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.gui_fragment);

        Matrix.setIdentityM(terrainTransformationMatrix, 0);
        Matrix.translateM(terrainTransformationMatrix, 0, 0, 0, 0);

        skybox.setUpOpenGl(skyVertexShader, skyFragmentShader, TextureLoader.loadCubeTexture(App.context(), skyResources));
        Matrix.setIdentityM(skyboxTransformationMatrix, 0);
        Matrix.translateM(skyboxTransformationMatrix, 0, modelPosition[0], modelPosition[1], modelPosition[2]);

        ObjLoader objLoader = new ObjLoader();
        objLoader.readObjFile(R.raw.play_cockpit);
        planeModel.setResources(objLoader, R.drawable.play_cockpit_textureatlas);
        planeModel.setUpOpenGl(textureVertexShader, textureFragmentShader);
        Matrix.setIdentityM(planeTransformationMatrix, 0);

        terrainObjLoader = new ObjLoader();
        terrainObjLoader.readObjFile(R.raw.play_terrain);

        for (TerrainModel terrain:terrainArray) {
            terrain.setResources(terrainObjLoader, terrainTextureID);
            terrain.setUpOpenGl(endlessTerrainVertexShader, endlessTerrainFragmentShader);
        }

        int n = 0;
        int col = 3;
        for (int xTemp = 0; xTemp < col; xTemp++){
            for (int zTemp = 0; zTemp < col; zTemp++){
                terrainArray[n].setIndex(xTemp - ((col-1)/2), zTemp - ((col-1)/2));
                n++;
                Log.d(TAG, "onSurfaceCreated: index:[" + xTemp + ", " + zTemp + "]");
            }
        }

        myHud = new Hud();
        myHud.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.play_direction), guiVertexShader, guiFragmentShader);
        myHud.setPositionTexture();
        myHud.transform(0.75f, 0.75f, 0);
        myHud.setBuffers();

        // Avoid any delays during start-up due to decoding of sound files.
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        // Start spatial audio playback of BACKGROUND_SOUND_FILE at the model position. The
                        // returned sourceId handle is stored and allows for repositioning the sound object
                        // whenever the cube position changes.
                        gvrAudioEngine.preloadSoundFile(BACKGROUND_SOUND_FILE);
                        sourceId = gvrAudioEngine.createSoundObject(BACKGROUND_SOUND_FILE);
                        gvrAudioEngine.setSoundObjectPosition(
                                sourceId, modelPosition[0], modelPosition[1], modelPosition[2]);
                        gvrAudioEngine.playSound(sourceId, true /* looped playback */);
                        // Preload an unspatialized sound to be played on a successful trigger on the cube.
                        gvrAudioEngine.preloadSoundFile(CRASH_SOUND_FILE);
                    }
                })
                .start();

    }

    /**
     * Implemented method from StereoRenderer.
     * Gets the active controller before drawing frames. Updates transformation matrices.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
         // Update the 3d audio engine with the most recent head rotation.
        headTransform.getQuaternion(headRotation, 0);

        gvrAudioEngine.setHeadRotation(headRotation[0], headRotation[1], headRotation[2], headRotation[3]);

        Log.d(TAG, "On new frame: current quad:" + currentQuad[0] + ", " + currentQuad[1]);
        float[] forwardVec = new float[3];
        headTransform.getForwardVector(forwardVec, 0);
        forwardVector.set((double) forwardVec[0], (double) forwardVec[1], (double) forwardVec[2]);
        if (isStart) {
            planeModel.initializeTransformation(forwardVector);
            isStart = false;
        }


        updateModelPositions();
        updateCamera();
        if(!isControllerConnected()){
            Intent intent = new Intent(this, MenuActivity.class);
            startActivity(intent);
        }

            // Regular update call to GVR audio engine.
        gvrAudioEngine.update();

    }


    /**
     * Implemented method from StereoRenderer.
     * Draws frame for one eye. Applies transformations then draw all models.
     */
    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera.getPositionMatrix(), 0);

        // Set the position of the light
        //Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_DIR_IN_WORLD_SPACE, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        Matrix.multiplyMM(modelView, 0, view, 0, skyboxTransformationMatrix, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        skybox.draw(modelViewProjection);

        // Set modelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, terrainTransformationMatrix, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);

        for(TerrainModel terrain:terrainArray){
            //terrainArray[i].draw(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
            terrain.draw(LIGHT_DIR_IN_WORLD_SPACE, modelView, modelViewProjection, currentQuad);
        }

        Matrix.multiplyMM(modelView, 0, view, 0, planeTransformationMatrix, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        planeModel.draw(LIGHT_DIR_IN_WORLD_SPACE, view, modelViewProjection);

        myHud.draw(planeModel.getPlaneRotationAngle()[1], 0);
    }

    /**
     * Implemented method from StereoRenderer.
     */
    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    /**
     * Handles joystick input.
     */
    private void processJoystickInput(MotionEvent event, int historyPos) {
        // Get joystick position.
        if (null == myInputDevice) {
            myInputDevice = event.getDevice();
        }
        float x = getCenteredAxis(event, myInputDevice, MotionEvent.AXIS_X, historyPos);
        if (x == 0) {
            x = getCenteredAxis(event, myInputDevice, MotionEvent.AXIS_HAT_X, historyPos);
        }
        if (x == 0) {
            x = getCenteredAxis(event, myInputDevice, MotionEvent.AXIS_Z, historyPos);
        }

        float z = getCenteredAxis(event, myInputDevice, MotionEvent.AXIS_Y, historyPos);
        if (z == 0) {
            z = getCenteredAxis(event, myInputDevice, MotionEvent.AXIS_HAT_Y, historyPos);
        }
        if (z == 0) {
            z = getCenteredAxis(event, myInputDevice, MotionEvent.AXIS_RZ, historyPos);
        }

        Log.d(TAG, "processJoystickInput: x = " + x +", z = " + z);
        if (x < 0){
            planeModel.rotateYPositive();
        }
        else if (x > 0){
            planeModel.rotateYNegative();
        }



    }

    /**
     * Override method from Activity.
     * Handles controller button input.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y){ //Labelled button A in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON A");
            planeModel.rotateXNegative();
            Log.d(TAG, "processJoystickInput: rotate up");
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A){ //Labelled button B in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON B");
            planeModel.rotateXPositive();
            Log.d(TAG, "processJoystickInput: rotate down");
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_X){ //Labelled button C in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON C");
            planeModel.rotateZPositive();
            Log.d(TAG, "processJoystickInput: rotate z negative");
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B){ //Labelled button D in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON D");
            planeModel.rotateZNegative();
            Log.d(TAG, "processJoystickInput: rotate z positive");
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A){
            planeModel.speedUp();
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B){
            planeModel.speedDown();
        }
        else {
            Log.d(TAG, "dispatchKeyEvent: keycode = " + event.getKeyCode());
        }

        return true; //hides controller's in built events from android so app doesn't close on B button
    }

    /**
     * Override method from Activity.
     * Handles controller joystick input.
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {

            // Process all historical movement samples in the batch
            final int historySize = event.getHistorySize();

            // Process the movements starting from the
            // earliest historical position in the batch
            for (int i = 0; i < historySize; i++) {
                // Process the event at historical position i
                processJoystickInput(event, i);
            }

            // Process the current movement sample in the batch (position -1)
            processJoystickInput(event, -1);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    /**
     * Gets the joystick input value and check if joystick is in flat condition,
     * as flat condition might not be exactly at 0, 0 depending on how it’s manufactured.
     */
    private static float getCenteredAxis(MotionEvent event, InputDevice device,
                                         int axis, int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        final float value = historyPos < 0 ? event.getAxisValue(axis)
                : event.getHistoricalAxisValue(axis, historyPos);
        if (range != null) {
            final float flat = range.getFlat();


            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            // A joystick at rest does not always report an absolute position of
            // (0,0).
            if (Math.abs(value) > flat) {
                Log.d(TAG, "getCenteredAxis: value = "+value);
                return value;
            }
        }
        Log.d(TAG, "getCenteredAxis: value = "+value);
        return value;
    }

    /**
     * Updates camera position based on planeModel's position
     */
    private void updateCamera()
    {
        Vector3d move = new Vector3d(0,0,1);  //gets the base direction of the plane
        move = planeModel.getTransform().multiplyQuatWith(move);
        move.normalize();				//normalize the vector

        Vector3d dir = new Vector3d(-1 * move.x, -1 * move.y, -1 * move.z);	//Get the reverse direction vector

        Vector3d normal = planeModel.getNormal();	//get the normal vector of the airplane
        normal = planeModel.getTransform().multiplyQuatWith(normal);
        normal.normalize();

        float[] planePos = planeModel.getPlanePos();

        camera.setPositionMatrix(new float[]{(float) (planePos[0] + dir.x) , (float) (planePos[1] + dir.y),  (float) (planePos[2] + dir.z)},
                new float[]{(float) (planePos[0]+ move.x), (float) (planePos[1] + move.y), (float) (planePos[2] + move.z)},
                new float[]{(float) (normal.x),(float) (normal.y),(float) (normal.z)});

        Log.d(TAG, "updateCamera: ("+planePos[0]+", "+planePos[1]+", "+planePos[2]+")");

    }

    /**
     * Updates every model's position based on planeModel's transformations.
     */
    private void updateModelPositions()
    {
        planeModel.updatePlanePos();
        if(!isColliding(planeModel.getPlanePos()[0], planeModel.getPlanePos()[1], planeModel.getPlanePos()[2])){
            Matrix.translateM(skyboxTransformationMatrix, 0, (float) (planeModel.getMove().x*planeModel.speed),
                    (float) (planeModel.getMove().y*planeModel.speed),  (float) (planeModel.getMove().z*planeModel.speed));
            gvrAudioEngine.setSoundObjectPosition(
                    sourceId, planeModel.getPlanePos()[0], planeModel.getPlanePos()[1], planeModel.getPlanePos()[2]);
            currentQuad[0] = Math.round(planeModel.getPlanePos()[0]/QUADRANT_SIZE);
            currentQuad[1] = Math.round(planeModel.getPlanePos()[2]/QUADRANT_SIZE);
            Matrix.setIdentityM(planeTransformationMatrix, 0);
            Matrix.multiplyMM(planeTransformationMatrix, 0, planeModel.getTransform().getTranslatedMatrix(planeModel.getPlanePos()[0],
                    planeModel.getPlanePos()[1], planeModel.getPlanePos()[2]), 0, planeTransformationMatrix, 0);
        }
        else{
            crashSourceId = gvrAudioEngine.createStereoSound(CRASH_SOUND_FILE);
            gvrAudioEngine.playSound(crashSourceId, false /* looping disabled */);
            vibrator.vibrate(50);
            Intent intent = new Intent(this, GameOverActivity.class);
            startActivity(intent);
        }

    }

    /**
     * Checks if a point is colliding with terrain.
     *
     * @param posX x coordinate of the point.
     * @param posY y coordinate of the point.
     * @param posZ z coordinate of the point.
     */
    private boolean isColliding(float posX, float posY, float posZ){
        float distX, distY, distZ;
        float dist;
        for (float[] terrainVertex: terrainObjLoader.getVertices()
             ) {
            distX = terrainVertex[0]+ (currentQuad[0]*QUADRANT_SIZE) -posX;
            distY = terrainVertex[1]-posY;
            distZ = terrainVertex[2]+ (currentQuad[1]*QUADRANT_SIZE)-posZ;
            dist = (float )Math.sqrt(distX*distX + distY*distY + distZ*distZ);
            if(dist < 1){
                Log.d(TAG, "isColliding: distance = " + dist);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an external controller is connected.
     */
    private boolean isControllerConnected(){
        for(int i = 0; i<myInputManager.getInputDeviceIds().length; i++){
            InputDevice tempDevice = myInputManager.getInputDevice(myInputManager.getInputDeviceIds()[i]);
            if(tempDevice.getControllerNumber() != 0){
                myInputDevice = tempDevice;
            }
        }

        return myInputDevice != null;
    }

    /**
     * Gets terrain selection.
     */
    private String getTerrainSelection(){
        String selection;
        Bundle extras = getIntent().getExtras();
        selection = extras.getString("TERRAIN_SELECTION");
        return selection;
    }
}