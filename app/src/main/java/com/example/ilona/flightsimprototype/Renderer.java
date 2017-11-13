package com.example.ilona.flightsimprototype;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Vibrator;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.FieldOfView;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.google.vr.sdk.base.sensors.internal.Vector3d;

import javax.microedition.khronos.egl.EGLConfig;

import static android.opengl.GLES20.glGetError;

public class Renderer extends GvrActivity implements GvrView.StereoRenderer{
    //private InputManager myInputManager;
    private InputDevice myInputDevice;
    private InputManagerCompat myInputManagerCompat;

    protected float[] modelPosition;

    private static final String TAG = "Renderer";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 2000.0f;

    private static final float CAMERA_Z = 5.501f;
    private Vector3d cameraCoor;
    private int[] prevQuad;
    private int[] currentQuad;
    private float[] forwardVec;
    private float[] rotationVec;
    private float[] transposeMatrix;
    private static final float TIME_DELTA = 0.3f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;
    private static final float MIN_ALTITUDE = 0.0f;

    private float altitude;

    private static final int COORDS_PER_VERTEX = 3;

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {50.0f, -10.0f, 50.0f, 0.0f};

    // Convenience vector for extracting the position from a matrix via multiplication.
    private static final float[] POS_MATRIX_MULTIPLY_VEC = {0, 0, 0, 1.0f};

    private static final float MIN_MODEL_DISTANCE = 3.0f;
    private static final float MAX_MODEL_DISTANCE = 7.0f;

    private static final String OBJECT_SOUND_FILE = "cube_sound.wav";
    private static final String SUCCESS_SOUND_FILE = "success.wav";

    private final float[] lightPosInEyeSpace = new float[4];

    short[] indices;

    private float[] camera;
    private float[] view;
    private float[] headView;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] modelTerrain;
    protected float[] modelSkybox;
    protected float[] modelCockpit;

    private SkyBox mySkybox;
    private Cockpit myCockpit;
    private ModelCockpit myModelCockpit;
    private ModelCockpit objTerrain;
    private ModelTerrain[] myEndlessTerrain;
    //private EndlessTerrain endlessTerrain;
    private ModelTerrain myModelTerrain;

    private float totalTime = 0f;
    private int totalFrames = 0;

    private float[] tempPosition;
    private float[] headRotation;
    private float[] prevHeadRotation;
    private float[] headEuler;
    private float[] prevHeadEuler;

    //
    double rotationRate = 0.04;
    double angleUp = rotationRate * -1;;   //angles for the rotations
    double angleDown = rotationRate;
    double angle2up = rotationRate * -1;
    double angle2down = rotationRate;
    float[] planeRotationAngle;
    float[] planeRotationMatrix;

    boolean drawParticles = false;

    boolean isStart;
    Vector3d normal = new Vector3d(0,1,0);      //inital vectors of the plane, defines coordinage space for plane
    Vector3d forwardVector = new Vector3d(0,0,1);
    Vector3d barrel = new Vector3d(-1,0,0);
    Quaternion transform;                   //transofmration quaternion
    Quaternion planeAxis1;
    Quaternion axis1;		                //forwardVector axis quaterion
    Quaternion axis2;		                //horizaontal axis quaternion
    Quaternion axis3;                       //vertical axis quaternion

    double leftFlapAngle = 0;               //angles of the flaps
    double rightFlapAngle = 0;
    double speed = .2;

    double cameraHeight = 0.57;             //default camera height
    double cameraReferenceHeight = 0.93;    //desult reference height
    double cameraforwardVectorFactor = 10.0;

    //float[] planePos = {1.0f, 10.0f, 1.0f};  //starting position of the plane
    float[] planePos;
    float[] particlePos = {1.0f, 10.0f, 1.0f};
    Vector3d particleVel = new Vector3d(0,0,0);
    //

    private float objectDistance = MAX_MODEL_DISTANCE / 2.0f;
    private float floorDepth = 0f;

    private Vibrator vibrator;

    private GvrAudioEngine gvrAudioEngine;
    private volatile int sourceId = GvrAudioEngine.INVALID_ID;
    private volatile int successSourceId = GvrAudioEngine.INVALID_ID;


    private ShaderLoader myShaderLoader;



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

        isStart = true;
        transform = new Quaternion(0,0,0,0);
        transform.fromAxis(forwardVector,0);

        axis1 = new Quaternion(0,0,0,0);
        axis1.fromAxis(forwardVector,0);

        axis2 = new Quaternion(0,0,0,0);
        //axis2.fromAxis(forwardVector,0);

        axis3 = new Quaternion(0,0,0,0);

        planeAxis1 = new Quaternion(0,0,0,0);

        planeRotationAngle = new float[3];
        planeRotationMatrix = new float[16];
        Matrix.setIdentityM(planeRotationMatrix, 0);

        //myInputManager = (InputManager) App.context().getSystemService(Context.INPUT_SERVICE);
        myInputManagerCompat = new InputManagerCompat(App.context());
        myShaderLoader = new ShaderLoader();
        initializeGvrView();
        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        altitude = 0.0f;

        modelTerrain = new float[20];

        myModelTerrain = new ModelTerrain();

        myEndlessTerrain = new ModelTerrain[9];
        for(int i = 0; i< myEndlessTerrain.length; i++){
            myEndlessTerrain[i] = new ModelTerrain();
        }

        modelSkybox = new float[16];
        mySkybox = new SkyBox();

        modelCockpit = new float[16];

        myCockpit = new Cockpit();
        myModelCockpit = new ModelCockpit();
        objTerrain  = new ModelCockpit();

        tempPosition = new float[4];
        // Model first appears directly in front of user.
        //modelPosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
        modelPosition = new float[] {0.0f, 0.0f, 0.0f};
        //planePos = new float[] {50.0f, 50.0f, 50.0f};
        planePos = new float[] {0.0f, 0.0f, 0.0f};
        headRotation = new float[4];
        prevHeadRotation = new float[4];
        headEuler = new float[3];
        prevHeadEuler = new float[3];
        headView = new float[16];
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cameraCoor = new Vector3d(0f, 0f, 0f);
        prevQuad = new int[2];
        currentQuad = new int[2];
        forwardVec = new float[3];
        rotationVec = new float[3];
        transposeMatrix = new float[16];

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


        int vertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int gridShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
        int passthroughShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        int endlessTerrainVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.endless_terrain_vertex);
        int endlessTerrainFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.endless_terrain_fragment);
        int terrainVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.terrain_vertex);
        int terrainFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.terrain_fragment);

        int skyVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.skybox_vertex);
        int skyFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.skybox_fragment);

        int cockpitVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.cockpit_vertex);
        int cockpitFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.cockpit_fragment);

        int textureVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.textured_model_vertex);
        int textureFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.textured_model_fragment);

        Matrix.setIdentityM(modelTerrain, 0);
        Matrix.translateM(modelTerrain, 0, 0, -floorDepth, 0); // Floor appears below user.

        int[] skyResources = new int[6];
        skyResources[0] = R.drawable.right;
        skyResources[1] = R.drawable.left;
        skyResources[2] = R.drawable.top;
        skyResources[3] = R.drawable.bottom;
        skyResources[4] = R.drawable.back;
        skyResources[5] = R.drawable.front;
        mySkybox.setUpOpenGl(skyVertexShader, skyFragmentShader, TextureLoader.loadCubeTexture(App.context(), skyResources));
        Matrix.setIdentityM(modelSkybox, 0);
        Matrix.translateM(modelSkybox, 0, modelPosition[0], modelPosition[1], modelPosition[2]);

        int[] cockpitResources = new int[6];
        cockpitResources[0] = R.drawable.cockpit_right;
        cockpitResources[1] = R.drawable.cockpit_left;
        cockpitResources[2] = R.drawable.cockpit_top;
        cockpitResources[3] = R.drawable.cockpit_bottom;//bottom
        cockpitResources[4] = R.drawable.cockpit_front;
        cockpitResources[5] = R.drawable.cockpit_back;
        myCockpit.setUpOpenGl(cockpitVertexShader, cockpitFragmentShader, TextureLoader.loadCubeTexture(App.context(), cockpitResources));
        ObjLoader objLoader = new ObjLoader();
        objLoader.readObjFile2(R.raw.retexturedcockpit3);
        myModelCockpit.setResources(objLoader, R.drawable.textureatlas_2);
        myModelCockpit.setUpOpenGl(textureVertexShader, textureFragmentShader);

        ObjLoader objLoader3 = new ObjLoader();
        objLoader3.readObjFile2(R.raw.medterrain);
        objTerrain.setResources(objLoader3, R.drawable.grass);
        objTerrain.setUpOpenGl(textureVertexShader, textureFragmentShader);

        Matrix.setIdentityM(modelCockpit, 0);

        //Matrix.translateM(modelCockpit, 0, modelPosition[0], modelPosition[1], modelPosition[2]);
        //Matrix.translateM(modelCockpit, 0, planePos[0], planePos[1], planePos[2]+5);

        //ObjLoader objLoader3 = new ObjLoader();
        //objLoader3.readObjFile2(R.raw.medterrain);
        myModelTerrain.setResources(objLoader3, R.drawable.grass);
        myModelTerrain.setUpOpenGl(endlessTerrainVertexShader, endlessTerrainFragmentShader);
        myModelTerrain.setQuadrantIndex(0, 1);

        for(int i = 0; i< myEndlessTerrain.length; i++){
            myEndlessTerrain[i].setResources(objLoader3, R.drawable.grass);
            myEndlessTerrain[i].setUpOpenGl(endlessTerrainVertexShader, endlessTerrainFragmentShader);
            checkGLError("endless terrain array link program");
        }
        int n = 0;
        int col = 3;
        for (int xTemp = 0; xTemp < col; xTemp++){
            for (int zTemp = 0; zTemp < col; zTemp++){
                myEndlessTerrain[n].setQuadrantIndex(xTemp - ((col-1)/2), zTemp - ((col-1)/2));
                n++;
                Log.d(TAG, "onSurfaceCreated: index:[" + xTemp + ", " + zTemp + "]");
                checkGLError("endless terrain array set quad index");
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

        checkGLError("onSurfaceCreated");
        Log.d(TAG, "Quad Change: current quad:" + currentQuad[0] +", " + currentQuad[1]);
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        gvrAudioEngine.pause();
        //myInputDevice = myInputManager.getInputDevice(myInputManager.getInputDeviceIds()[0]);

        myInputDevice = myInputManagerCompat.getInputDevice(myInputManagerCompat.getInputDeviceIds()[0]);
        for(int i = 0; i<myInputManagerCompat.getInputDeviceIds().length; i++){
            InputDevice tempDevice = myInputManagerCompat.getInputDevice(myInputManagerCompat.getInputDeviceIds()[i]);
            if(tempDevice.getControllerNumber() != 0){
                myInputDevice = tempDevice;
            }
            //Log.d(TAG, "onNewFrame: Input Device: " + myInputManagerCompat.getInputDevice(myInputManagerCompat.getInputDeviceIds()[i]).getControllerNumber());
        }
        Log.d(TAG, "onNewFrame: Input Device: " + myInputDevice.getControllerNumber());

        //Log.d(TAG, "onNewFrame: Input devices = " +myInputManagerCompat.getInputDeviceIds().length);
        
        //setCubeRotation();

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        headTransform.getHeadView(headView, 0);

        // Update the 3d audio engine with the most recent head rotation.
        headTransform.getQuaternion(headRotation, 0);

        headTransform.getEulerAngles(headEuler, 0);
        gvrAudioEngine.setHeadRotation(
                headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
        //Log.d("ALTITUDE", "altitude = " + altitude + "   forward vec=  " +forwardVec[1]);
        //Log.d(TAG, "On new frame: current pos:" + cameraCoor.x +", " + cameraCoor.z);
        Log.d(TAG, "On new frame: current quad:" + currentQuad[0] +", " + currentQuad[1]);
        headTransform.getForwardVector(forwardVec, 0);
        forwardVector.set((double)forwardVec[0], (double)forwardVec[1], (double)forwardVec[2]);
        if(isStart) {
            transform.fromAxis(forwardVector,0);
            isStart = false;
        }
        if(myInputDevice.getControllerNumber() == 0){
            //transform.fromAxis(forwardVector,0);
            //Quaternion headQuaternion = new Quaternion(headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
            //headQuaternion.normalise();
            //transform.normalise();
            //transform.multiplyQuatWith(headQuaternion);
            //transform = transform.multiplyQuatWith(headQuaternion);
            //Log.d(TAG, "onNewFrame: rotate with head angle = " + headQuaternion.getAngle());
        }

        updatePlanePos();
        updateCamera();
        Log.d(TAG, "onNewFrame: rotate with angle = " + transform.getAngle());

        prevHeadRotation = headRotation;
        prevHeadEuler = headEuler;

        // Regular update call to GVR audio engine.
        gvrAudioEngine.update();

        checkGLError("onReadyToDraw");
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
        FieldOfView eyeFov = eye.getFov();
        float[] fovAngles = {eyeFov.getLeft(), eyeFov.getRight(), eyeFov.getTop(), eyeFov.getBottom()};
        Log.d(TAG, "onDrawEye: fovAngles = {"+fovAngles[0]+", "+fovAngles[1]+", "+fovAngles[2]+", "+fovAngles[3]+"}");

        // Set the position of the light
        Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        //Matrix.multiplyMM(modelView, 0, view, 0, modelCube, 0);
        //Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        //drawCube();
        //mySkybox.drawSkybox(modelViewProjection);

        Matrix.multiplyMM(modelView, 0, view, 0, modelSkybox, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        mySkybox.drawSkybox(modelViewProjection);


        // Set modelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, modelTerrain, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        //objTerrain.draw(lightPosInEyeSpace, modelView, modelViewProjection);
        //myModelTerrain.draw(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
        //objTerrain.draw(LIGHT_POS_IN_WORLD_SPACE, modelView, modelViewProjection);
        //myModelTerrain.draw(LIGHT_POS_IN_WORLD_SPACE, modelView, modelViewProjection, currentQuad);

        for(int i = 0; i< myEndlessTerrain.length; i++){
            myEndlessTerrain[i].draw(LIGHT_POS_IN_WORLD_SPACE, modelView, modelViewProjection, currentQuad);
        }

        Matrix.multiplyMM(modelView, 0, view, 0, modelCockpit, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        //myCockpit.drawCockpit(modelViewProjection);
        myModelCockpit.draw(lightPosInEyeSpace, modelView, modelViewProjection);

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

        int[] tempViewportData = new int[4];
        viewport.getAsArray(tempViewportData, 0);
        tempViewportData[3] += 50;
        Log.d(TAG, "onFinishFrame: " + viewport.y + ", " + tempViewportData[1]);

        viewport.setViewport(tempViewportData[2], tempViewportData[3], tempViewportData[0], tempViewportData[1]);
        viewport.setGLViewport();
    }


    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");
        // Always give user feedback.
        vibrator.vibrate(50);
    }


    private boolean isCrashLanding(float tempAlt) {
        return tempAlt < MIN_ALTITUDE;
    }

    private void resetPosition(){
        Matrix.setLookAtM(camera, 0, 0, 0, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        Matrix.setIdentityM(modelSkybox, 0);
        Matrix.translateM(modelSkybox, 0, modelPosition[0], modelPosition[1], modelPosition[2]);
        altitude = 0;
    }

    private boolean isQuadChange(){
        currentQuad[0] = (int) Math.round(planePos[2]/512);
        currentQuad[1] = (int) Math.round(planePos[0]/512);
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
        //Log.d("TEST", (totalTime / totalFrames) / 1000000 + " ms");
        Log.d("TEST", totalFrames / (totalTime / 1000000000) + " fps");
    }

    /**
     * The ship directly handles joystick input.
     *
     * @param event
     * @param historyPos
     */
    private void processJoystickInput(MotionEvent event, int historyPos) {
        // Get joystick position.
        // Many game pads with two joysticks report the position of the
        // second
        // joystick
        // using the Z and RZ axes so we also handle those.
        // In a real game, we would allow the user to configure the axes
        // manually.
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
        float tempAngle = (float) Math.toDegrees(rotationRate);
        if (x < 0){
            barrel.set(0,1,0);
            axis3.fromAxis(barrel, angle2up);	//set rotation axis, angle
            axis3.normalise();					//normalize the quaternions
            transform.normalise();
            transform =  transform.multiplyQuatWith(axis3);
        }
        else if (x > 0){
            barrel.set(0,1,0);
            axis3.fromAxis(barrel, angle2down);	//set rotation axis, angle
            axis3.normalise();					//normalize the quaternions
            transform.normalise();
            transform =  transform.multiplyQuatWith(axis3);
        }



    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y){ //Labelled button A in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON A");
            barrel.set(1,0,0);
            axis2.fromAxis(barrel, angle2up);	//set rotation axis, angle
            axis2.normalise();					//normalize the quaternions
            transform.normalise();
            transform =  transform.multiplyQuatWith(axis2);

            //planeRotationAngle[0] += tempAngle;
            Log.d(TAG, "processJoystickInput: rotate up");
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A){ //Labelled button B in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON B");
            barrel.set(1,0,0);
            axis2.fromAxis(barrel, angle2down);	//set rotation axis, angle
            axis2.normalise();					//normalize the quaternions
            transform.normalise();
            transform =  transform.multiplyQuatWith(axis2);

            //planeRotationAngle[0] -= tempAngle;
            Log.d(TAG, "processJoystickInput: rotate down");
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_X){ //Labelled button C in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON C");
            barrel = new Vector3d(-1, 0, 0);  //load baase horizaontal vector
            axis1.fromAxis(forwardVector, angleDown);     //set quaternion about forwardVector vector, set angle
            barrel = axis1.multiplyQuatWith(barrel);   //apply quaternion to the barrel vector
            axis1.normalise();                    //normalize the quaternions
            transform.normalise();
            transform = transform.multiplyQuatWith(axis1);

            //planeRotationAngle[2] += tempAngle;
            Log.d(TAG, "processJoystickInput: rotate " + planeRotationAngle[2]);
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B){ //Labelled button D in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON D");
            barrel = new Vector3d(1, 0, 0);  //load baase horizaontal vector
            axis1.fromAxis(forwardVector, angleUp);     //set quaternion about forwardVector vector, set angle
            barrel = axis1.multiplyQuatWith(barrel);   //apply quaternion to the barrel vector
            axis1.normalise();                    //normalize the quaternions
            transform.normalise();
            transform = transform.multiplyQuatWith(axis1);

            //planeRotationAngle[2] -= tempAngle;
            Log.d(TAG, "processJoystickInput: rotate " + planeRotationAngle[2]);
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A){
            speed += .2;
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B){
            speed -= .02;
        }
        else {
            Log.d(TAG, "dispatchKeyEvent: keycode = " + event.getKeyCode());
        }

        return true; //hides controller's in built events from android so app doesn't close on B button
    }

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
     * Set the game controller to be used to control the ship.
     *
     * @param dev the input device that will be controlling the ship
     */
    public void setInputDevice(InputDevice dev) {
        //mInputDevice = dev;
    }

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

    void updateCamera()
    {
        //glLoadIdentity ();     //loads identiy to reset an transformations on the martrix stack
        Vector3d move = new Vector3d(0,0,1);  //gets the base direction of the plane
        move = transform.multiplyQuatWith(move);		//applies tranformation to diretion vector
        //Vector3d move;
        //move = transform.multiplyQuatWith(forwardVector);
        move.normalize();				//normalize the vector

        Vector3d dir = new Vector3d(-1 * move.x, -1 * move.y, -1 * move.z);				//Get the reverise of the direction vector

        //dir.x *= cameraforwardVectorFactor;					//scale the vector to set forwardVector, backward camera position
        //dir.y *= cameraforwardVectorFactor;
        //dir.z *= cameraforwardVectorFactor;

        normal = new Vector3d(0,1,0);	//get the normal vector of the airplane
        normal = transform.multiplyQuatWith(normal);
        normal.normalize();

        Vector3d dist = new Vector3d(move.x - dir.x, move.y - dir.y, move.z - dir.z);
        //Vector3d dist = new Vector3d(- dir.x, - dir.y, - dir.z);
        dist.normalize();


        Matrix.setLookAtM
                (camera, 0, (float) (planePos[0] + dir.x) , (float) (planePos[1] + dir.y),  (float) (planePos[2] + dir.z) ,
                //        (camera, 0, (float) (planePos[0]) , (float) (planePos[1]),  (float) (planePos[2]) ,
                (float) (planePos[0]+ move.x), (float) (planePos[1] + move.y), (float) (planePos[2] + move.z) ,
                //planePos[0],                        (float) (planePos[1] + newy),                        (float) (planePos[2]+newz),
                (float) (normal.x),(float) (normal.y),(float) (normal.z));

        Log.d(TAG, "updateCamera: ("+planePos[0]+", "+planePos[1]+", "+planePos[2]+")");

    }

    void updatePlanePos()
    {
        Vector3d move = new Vector3d(0, 0, 1);
        move = transform.multiplyQuatWith(move);//apply transformation
        move.normalize(); //normalize the vector

        if(planePos[1]+ move.y * speed > 0) {
            planePos[0] = (float) (planePos[0] + move.x * speed);  //updeate the position by adding a factor
            planePos[1] = (float) (planePos[1] + move.y * speed);    //of the direction components
            planePos[2] = (float) (planePos[2] + move.z * speed);
            Matrix.translateM(modelSkybox, 0, (float) (move.x*speed), (float) (move.y * speed),  (float) (move.z * speed));
        }

        Matrix.setIdentityM(modelCockpit, 0);

        //Matrix.rotateM(modelCockpit, 0, 180, 0, 1, 0);
        Matrix.multiplyMM(modelCockpit, 0, transform.getTranslatedMatrix(planePos[0], planePos[1], planePos[2]), 0, modelCockpit, 0);

        currentQuad[0] = (int) Math.round(planePos[2]/60.8294);
        currentQuad[1] = (int) Math.round(planePos[0]/60.8294);
        /*
        Log.d(TAG, "updatePlanePos: -----------------------------------------------------------------------------");
        Log.d(TAG, "updatePlanePos: ("+planePos[0]+", "+planePos[1]+", "+planePos[2]+")");
        //Log.d(TAG, "updatePlanePos: model cockpit = |" + modelCockpit[0] + ", " + modelCockpit[1] + ", "  + modelCockpit[2] + ", "  + modelCockpit[3] + "| ");
        //Log.d(TAG, "updatePlanePos: model cockpit = |" + modelCockpit[4] + ", "+ modelCockpit[5] + ", " + modelCockpit[6] + ", "  + modelCockpit[7] + "| ");
        //Log.d(TAG, "updatePlanePos: model cockpit = |" + modelCockpit[8] + ", " + modelCockpit[9] + ", " + modelCockpit[10] + ", " + modelCockpit[11] + "| ");
        //Log.d(TAG, "updatePlanePos: model cockpit = |" + modelCockpit[12] + ", " + modelCockpit[13] + ", "  + modelCockpit[14] + ", "  + modelCockpit[15] + "| ");
        //Log.d(TAG, "updatePlanePos: ("+planePos[0]+", "+planePos[1]+", "+planePos[2]+")");
        Log.d(TAG, "updatePlanePos: Transform rotation axis("+temp.x+", "+temp.y+", "+temp.z+")");
        Log.d(TAG, "updatePlanePos: Transform rotation angle = " + Math.toDegrees(transform.getAngle()));
        Log.d(TAG, "updatePlanePos: Rotation angle("+planeRotationAngle[0]+", "+planeRotationAngle[1]+", "+planeRotationAngle[2]+")");
        particlePos[0] = (float)(particlePos[0] + particleVel.x) ;
        particlePos[1] = (float)(particlePos[1] + particleVel.y) ;
        particlePos[2] = (float)(particlePos[2] + particleVel.z) ;
        Log.d(TAG, "updatePlanePos: -----------------------------------------------------------------------------");
        */

    }
    public boolean isInside(float point, Vector3d camPos){
        return false;
    }
}