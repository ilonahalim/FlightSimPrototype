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

import com.example.ilona.flightsimprototype.utility.App;
import com.example.ilona.flightsimprototype.models.GUI;
import com.example.ilona.flightsimprototype.utility.InputManagerCompat;
import com.example.ilona.flightsimprototype.models.ModelCockpit;
import com.example.ilona.flightsimprototype.models.ModelTerrain;
import com.example.ilona.flightsimprototype.loaders.ObjLoader;
import com.example.ilona.flightsimprototype.models.PlaneHud;
import com.example.ilona.flightsimprototype.utility.Quaternion;
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

import static android.opengl.GLES20.glGetError;

public class PlayActivity extends GvrActivity implements GvrView.StereoRenderer{
    private InputManager myInputManager;
    private InputDevice myInputDevice;
    //private InputManagerCompat myInputManagerCompat;

    protected float[] modelPosition;

    private static final String TAG = "PlayActivity";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 2000.0f;

    private static final float CAMERA_Z = 5.501f;
    private int[] prevQuad;
    private int[] currentQuad;
    private float[] forwardVec;

    // We keep the light always position just above the user.
    private float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {0.0f, 2.0f, 0.0f, 0.0f};

    // Convenience vector for extracting the position from a matrix via multiplication.
    private static final float[] POS_MATRIX_MULTIPLY_VEC = {0, 0, 0, 1.0f};

    private static final String OBJECT_SOUND_FILE = "cube_sound.wav";
    private static final String SUCCESS_SOUND_FILE = "success.wav";

    private final float[] lightPosInEyeSpace = new float[4];

    private float[] camera;
    private float[] view;
    private float[] headView;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] modelTerrain;
    protected float[] modelSkybox;
    protected float[] modelCockpit;

    private SkyBox mySkybox;
    private ModelCockpit myModelCockpit;
    private ModelTerrain[] myEndlessTerrain;
    private GUI myGui;
    private PlaneHud planeHud;
    ObjLoader terrainObjLoader;

    private float totalTime = 0f;
    private int totalFrames = 0;

    private float[] headRotation;

    //
    double rotationRate = 0.04;
    double angleUp = rotationRate * -1;;   //angles for the rotations
    double angleDown = rotationRate;
    double angle2up = rotationRate * -1;
    double angle2down = rotationRate;
    float[] planeRotationAngle;
    float[] planeRotationMatrix;

    boolean isStart;
    Vector3d normal = new Vector3d(0,1,0);      //inital vectors of the plane, defines coordinage space for plane
    Vector3d forwardVector = new Vector3d(0,0,1);
    Vector3d barrel = new Vector3d(-1,0,0);
    Quaternion transform;                   //transofmration quaternion
    Quaternion axis1;		                //forwardVector axis quaterion
    Quaternion axis2;		                //horizaontal axis quaternion
    Quaternion axis3;                       //vertical axis quaternion

    double speed = .2;

    float[] planePos;
    //
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
        transform.formAxis(forwardVector,0);

        axis1 = new Quaternion(0,0,0,0);
        axis1.formAxis(forwardVector,0);

        axis2 = new Quaternion(0,0,0,0);

        axis3 = new Quaternion(0,0,0,0);



        planeRotationAngle = new float[3];
        planeRotationMatrix = new float[16];
        Matrix.setIdentityM(planeRotationMatrix, 0);

        myInputManager = (InputManager) App.context().getSystemService(Context.INPUT_SERVICE);
        //myInputManagerCompat = new InputManagerCompat(App.context());
        myShaderLoader = new ShaderLoader();
        initializeGvrView();
        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];

        modelTerrain = new float[20];

        myEndlessTerrain = new ModelTerrain[9];
        for(int i = 0; i< myEndlessTerrain.length; i++){
            myEndlessTerrain[i] = new ModelTerrain();
        }

        modelSkybox = new float[16];
        mySkybox = new SkyBox();

        modelCockpit = new float[16];
        myModelCockpit = new ModelCockpit();
        modelPosition = new float[] {0.0f, 0.0f, 0.0f};
        planePos = new float[] {0.0f, 25.0f, 0.0f};
        headRotation = new float[4];
        headView = new float[16];
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        prevQuad = new int[2];
        currentQuad = new int[2];
        forwardVec = new float[3];

        Matrix.setLookAtM(camera, 0, 0, 0, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        // Initialize 3D audio engine.
        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
    }

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

        int endlessTerrainVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.endless_terrain_vertex);
        int endlessTerrainFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.endless_terrain_fragment);

        int skyVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.skybox_vertex);
        int skyFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.skybox_fragment);

        int textureVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.textured_model_vertex);
        int textureFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.textured_model_fragment);

        int guiVertexShader = myShaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.gui_vertex);
        int guiFragmentShader = myShaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.gui_fragment);

        Matrix.setIdentityM(modelTerrain, 0);
        Matrix.translateM(modelTerrain, 0, 0, 0, 0);

        int[] skyResources = new int[6];
        int terrainTextureID = 0;
        if(getTerrainSelection().equals("1")){
            skyResources[0] = R.drawable.criminal_impact_bk;
            skyResources[1] = R.drawable.criminal_impact_ft;
            skyResources[2] = R.drawable.criminal_impact_up;
            skyResources[3] = R.drawable.criminal_impact_dn;
            skyResources[4] = R.drawable.criminal_impact_lf;
            skyResources[5] = R.drawable.criminal_impact_rt;

            terrainTextureID = R.drawable.glass;
        }
        else {
            skyResources[0] = R.drawable.day_right;
            skyResources[1] = R.drawable.day_left;
            skyResources[2] = R.drawable.day_top;
            skyResources[3] = R.drawable.day_bottom;
            skyResources[4] = R.drawable.day_back;
            skyResources[5] = R.drawable.day_front;

            terrainTextureID = R.drawable.grass;
        }
        mySkybox.setUpOpenGl(skyVertexShader, skyFragmentShader, TextureLoader.loadCubeTexture(App.context(), skyResources));
        Matrix.setIdentityM(modelSkybox, 0);
        Matrix.translateM(modelSkybox, 0, modelPosition[0], modelPosition[1], modelPosition[2]);

        ObjLoader objLoader = new ObjLoader();
        objLoader.readObjFile2(R.raw.retexturedcockpit3);
        myModelCockpit.setResources(objLoader, R.drawable.textureatlas_2);
        myModelCockpit.setUpOpenGl(textureVertexShader, textureFragmentShader);
        Matrix.setIdentityM(modelCockpit, 0);

        terrainObjLoader = new ObjLoader();
        terrainObjLoader.readObjFile2(R.raw.terrain_inverted);

        for(int i = 0; i< myEndlessTerrain.length; i++){
            myEndlessTerrain[i].setResources(terrainObjLoader, terrainTextureID);
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

        myGui = new GUI();
        myGui.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.hud_1), guiVertexShader, guiFragmentShader);
        myGui.setPositionTexture();
        myGui.transform(0.75f, 0.75f, 0,0,0);
        myGui.setBuffers();

        planeHud = new PlaneHud();
        planeHud.setDynamicTexture(TextureLoader.loadTexture(App.context(), R.drawable.hud_dynamic));
        planeHud.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.hud_static), guiVertexShader, guiFragmentShader);
        planeHud.setPositionTexture();
        planeHud.setBuffers();

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
        for(int i = 0; i<myInputManager.getInputDeviceIds().length; i++){
            InputDevice tempDevice = myInputManager.getInputDevice(myInputManager.getInputDeviceIds()[i]);
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

        gvrAudioEngine.setHeadRotation(
                headRotation[0], headRotation[1], headRotation[2], headRotation[3]);

        Log.d(TAG, "On new frame: current quad:" + currentQuad[0] +", " + currentQuad[1]);
        headTransform.getForwardVector(forwardVec, 0);
        forwardVector.set((double)forwardVec[0], (double)forwardVec[1], (double)forwardVec[2]);
        if(isStart) {
            transform.formAxis(forwardVector,0);
            barrel.set(0,1,0);
            axis2.formAxis(barrel, Math.toRadians(180));	//set rotation axis, angle
            axis2.normalise();					//normalize the quaternions
            transform.normalise();
            transform =  transform.multiplyQuatWith(axis2);
            isStart = false;
        }


        updatePlanePos();
        updateCamera();
        Log.d(TAG, "onNewFrame: rotate with angle = " + transform.getAngle());

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
        if(myInputDevice == null){
            Intent intent = new Intent(this, MenuActivity.class);
            startActivity(intent);
        }

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

        Matrix.multiplyMM(modelView, 0, view, 0, modelSkybox, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        mySkybox.drawSkybox(modelViewProjection);
        int[] origin = new int[]{0, 0};

        // Set modelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, modelTerrain, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        //objTerrain.draw(lightPosInEyeSpace, modelView, modelViewProjection);
        //myModelTerrain.draw(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);

        for(int i = 0; i< myEndlessTerrain.length; i++){
            //myEndlessTerrain[i].draw(lightPosInEyeSpace, modelView, modelViewProjection, currentQuad);
            myEndlessTerrain[i].draw(LIGHT_POS_IN_WORLD_SPACE, modelView, modelViewProjection, currentQuad);
        }

        Matrix.multiplyMM(modelView, 0, view, 0, modelCockpit, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        //myCockpit.drawCockpit(modelViewProjection);
        myModelCockpit.draw(lightPosInEyeSpace, modelView, modelViewProjection);

        myGui.draw(planeRotationAngle[1], 0);
        planeHud.setDynamicOffset(planeRotationAngle[0], planeRotationAngle[1], planeRotationAngle[2], planePos[1]);
        //planeHud.draw(0, 0);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
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


    /**
     * Handles joystick input.
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
            axis3.formAxis(barrel, angle2up);	//set rotation axis, angle
            axis3.normalise();					//normalize the quaternions
            transform.normalise();
            transform =  transform.multiplyQuatWith(axis3);
            planeRotationAngle[1] += angle2up;
            if (planeRotationAngle[1] > 360)
                planeRotationAngle[1] -= 360;
        }
        else if (x > 0){
            barrel.set(0,1,0);
            axis3.formAxis(barrel, angle2down);	//set rotation axis, angle
            axis3.normalise();					//normalize the quaternions
            transform.normalise();
            transform =  transform.multiplyQuatWith(axis3);
            planeRotationAngle[1] += angle2down;
            if (planeRotationAngle[1] < 0)
                planeRotationAngle[1] += 360;
        }



    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y){ //Labelled button A in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON A");
            barrel.set(1,0,0);
            axis2.formAxis(barrel, angle2up);	//set rotation axis, angle
            axis2.normalise();					//normalize the quaternions
            transform.normalise();
            transform =  transform.multiplyQuatWith(axis2);

            planeRotationAngle[0] += angle2up;
            Log.d(TAG, "processJoystickInput: rotate up");
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A){ //Labelled button B in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON B");
            barrel.set(1,0,0);
            axis2.formAxis(barrel, angle2down);	//set rotation axis, angle
            axis2.normalise();					//normalize the quaternions
            transform.normalise();
            transform =  transform.multiplyQuatWith(axis2);

            planeRotationAngle[0] += angle2down;
            Log.d(TAG, "processJoystickInput: rotate down");
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_X){ //Labelled button C in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON C");
            barrel = new Vector3d(-1, 0, 0);  //load baase horizaontal vector
            axis1.formAxis(forwardVector, angleDown);     //set quaternion about forwardVector vector, set angle
            barrel = axis1.multiplyQuatWith(barrel);   //apply quaternion to the barrel vector
            axis1.normalise();                    //normalize the quaternions
            transform.normalise();
            transform = transform.multiplyQuatWith(axis1);

            planeRotationAngle[2] += angleDown;
            Log.d(TAG, "processJoystickInput: rotate " + planeRotationAngle[2]);
        }
        else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B){ //Labelled button D in MOKE controller
            Log.d(TAG, "dispatchKeyEvent: BUTTON D");
            barrel = new Vector3d(1, 0, 0);  //load baase horizaontal vector
            axis1.formAxis(forwardVector, angleUp);     //set quaternion about forwardVector vector, set angle
            barrel = axis1.multiplyQuatWith(barrel);   //apply quaternion to the barrel vector
            axis1.normalise();                    //normalize the quaternions
            transform.normalise();
            transform = transform.multiplyQuatWith(axis1);

            planeRotationAngle[2] += angleUp;
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
        move.normalize();				//normalize the vector

        Vector3d dir = new Vector3d(-1 * move.x, -1 * move.y, -1 * move.z);				//Get the reverise of the direction vector

        //dir.x *= cameraforwardVectorFactor;					//scale the vector to set forwardVector, backward camera position
        //dir.y *= cameraforwardVectorFactor;
        //dir.z *= cameraforwardVectorFactor;

        normal = new Vector3d(0,1,0);	//get the normal vector of the airplane
        normal = transform.multiplyQuatWith(normal);
        normal.normalize();

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

        planePos[0] = (float) (planePos[0] + move.x * speed);  //updeate the position by adding a factor
        planePos[1] = (float) (planePos[1] + move.y * speed);    //of the direction components
        planePos[2] = (float) (planePos[2] + move.z * speed);

        if(!isColliding(planePos[0], planePos[1], planePos[2])){
            Matrix.translateM(modelSkybox, 0, (float) (move.x*speed), (float) (move.y * speed),  (float) (move.z * speed));
            currentQuad[0] = (int) Math.round(planePos[0]/60.8294);
            currentQuad[1] = (int) Math.round(planePos[2]/60.8294);
            Matrix.setIdentityM(modelCockpit, 0);
            Matrix.multiplyMM(modelCockpit, 0, transform.getTranslatedMatrix(planePos[0], planePos[1], planePos[2]), 0, modelCockpit, 0);
        }
        else{
            Intent intent = new Intent(this, GameOverActivity.class);
            startActivity(intent);
        }

    }
    public boolean isColliding(float posX, float posY, float posZ){
        float distX, distY, distZ;
        float dist;
        for (float[] terrainVertex: terrainObjLoader.vertices
             ) {
            distX = terrainVertex[0]+ (currentQuad[0]*60.8294f) -posX;
            distY = terrainVertex[1]-posY;
            distZ = terrainVertex[2]+ (currentQuad[1]*60.8294f)-posZ;
            dist = (float )Math.sqrt(distX*distX + distY*distY + distZ*distZ);
            if(dist < 1){
                Log.d(TAG, "isColliding: distance = " + dist);
                return true;
            }
        }
        return false;
    }

    public boolean isQuadrantChanged(){
        int[] prevQuadrant = new int[]{currentQuad[0], currentQuad[1]};
        currentQuad[0] = (int) Math.round(planePos[0]/60.8294);
        currentQuad[1] = (int) Math.round(planePos[2]/60.8294);
        if(currentQuad[0] != prevQuadrant[0] || currentQuad[1] != prevQuadrant[1]){
            return true;
        }
        return false;
    }

    private String getTerrainSelection(){
        String selection;
        Bundle extras = getIntent().getExtras();
        selection = extras.getString("TERRAIN_SELECTION");
        return selection;
    }
}