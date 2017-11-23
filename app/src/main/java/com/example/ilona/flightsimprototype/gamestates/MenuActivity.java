package com.example.ilona.flightsimprototype.gamestates;

import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.example.ilona.flightsimprototype.models.Quad;
import com.example.ilona.flightsimprototype.utility.App;
import com.example.ilona.flightsimprototype.models.Camera;
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

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Class: MenuActivity
 * Author: Ilona
 * <p> The purpose of this class is as a controller and model of the menu function.
 * It displays a Skybox, the 3D layered game title, subtitle, menu options, controller guide and instructions.
 * It ensures that a controller is always connected to the device before the player can interact with the game.</>
 */

public class MenuActivity extends GvrActivity implements GvrView.StereoRenderer{

    private static final String TAG = "MenuActivity";
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000.0f;

    private Camera camera;

    //menu layered quads
    private Quad menuTitle;
    private Quad menuSubtitle;
    private Quad menuInstruction0;
    private Quad menuInstruction1;
    private Quad menuOption1;
    private Quad menuOption2;
    private Quad menuOption3;
    private Quad menuSelection;
    private Quad menuGuide;

    private int selection; //stores user menu selection

    private SkyBox menuSkybox;
    private float[] modelSkybox;

    private float[] view;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] modelPosition;

    private ShaderLoader shaderLoader;

    private static final String BACKGROUND_SOUND_FILE = "menu_sound.3gp";
    private GvrAudioEngine gvrAudioEngine;
    private volatile int sourceId = GvrAudioEngine.INVALID_ID;

    private InputManager inputManager;
    private InputDevice inputDevice;

    float[] tempMatrix;

    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeGvrView();

        tempMatrix = new float[16];

        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        modelPosition = new float[] {0.0f, 0.0f, 0.0f};

        camera = new Camera();

        menuSkybox = new SkyBox();
        modelSkybox = new float[16];

        shaderLoader = new ShaderLoader();

        menuTitle = new Quad();
        menuSubtitle = new Quad();
        menuInstruction0 = new Quad();
        menuInstruction1 = new Quad();
        menuOption1 = new Quad();
        menuOption2 = new Quad();
        menuOption3 = new Quad();
        menuSelection = new Quad();
        menuGuide = new Quad();

        selection = 2;

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
     * Overriden method from GvrActivity class.
     * Pauses audio.
     */
    @Override
    public void onPause() {
        gvrAudioEngine.pause();
        super.onPause(); //pause audio.
    }

    /**
     * Overriden method from GvrActivity class.
     * Resumes audio.
     */
    @Override
    public void onResume() {
        super.onResume();
        gvrAudioEngine.resume(); //resume audio play.
    }

    /**
     * Gets the active controller before drawing frames.
     * Implemented method from StereoRenderer class.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        for(int i = 0; i<inputManager.getInputDeviceIds().length; i++){
            InputDevice tempDevice = inputManager.getInputDevice(inputManager.getInputDeviceIds()[i]);
            if(tempDevice.getControllerNumber() != 0){
                inputDevice = tempDevice;
            }
            Log.d(TAG, "onNewFrame: Input Device: " + inputManager.getInputDevice(inputManager.getInputDeviceIds()[i]).getControllerNumber());
        }

        // Regular update call to GVR audio engine.
        gvrAudioEngine.update();
    }

    /**
     * Implemented method from StereoRenderer class. Draws frame for one eye.
     * Applies transformations then draw the skybox and quads.
     */
    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera.getPositionMatrix(), 0);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        Matrix.multiplyMM(modelView, 0, view, 0, modelSkybox, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        menuSkybox.draw(modelViewProjection);

        menuGuide.draw(view, perspective);
        menuTitle.draw(view, perspective);
        menuSubtitle.draw(view, perspective);

        if(inputDevice == null) {
            menuInstruction0.draw(view, perspective);
        }
        else {
            menuInstruction1.draw(view, perspective);
            menuOption1.draw(view, perspective);
            menuOption2.draw(view, perspective);
            menuOption3.draw(view, perspective);
            moveSelection();
            menuSelection.draw(view, perspective);
            Matrix.setIdentityM(tempMatrix, 0);
            menuSelection.setTransformationMatrix(tempMatrix);
        }

    }

    /**
     * Implemented method from StereoRenderer class.
     */
    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    /**
     * Implemented method from StereoRenderer class.
     */
    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    /**
     * Implemented method from StereoRenderer class.
     * Load shaders and textures. Set up the quads and skybox when surface is created.
     */
    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        gvrAudioEngine.preloadSoundFile(BACKGROUND_SOUND_FILE);
                        sourceId = gvrAudioEngine.createSoundObject(BACKGROUND_SOUND_FILE);
                        gvrAudioEngine.setSoundObjectPosition(
                                sourceId, modelPosition[0], modelPosition[1], modelPosition[2]);
                        gvrAudioEngine.playSound(sourceId, true);
                    }
                })
                .start();

        inputManager = (InputManager) App.context().getSystemService(Context.INPUT_SERVICE);

        int skyVertexShader = shaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.skybox_vertex);
        int skyFragmentShader = shaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.skybox_fragment);

        int quadVertexShader = shaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.quad_vertex);
        int quadFragmentShader = shaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.quad_fragment);

        int[] skyResources = new int[6];

        skyResources[0] = R.drawable.drakeq_rt;
        skyResources[1] = R.drawable.drakeq_lf;
        skyResources[2] = R.drawable.drakeq_up;
        skyResources[3] = R.drawable.drakeq_dn;
        skyResources[4] = R.drawable.drakeq_bk;
        skyResources[5] = R.drawable.drakeq_ft;

        menuSkybox.setUpOpenGl(skyVertexShader, skyFragmentShader, TextureLoader.loadCubeTexture(App.context(), skyResources));
        Matrix.setIdentityM(modelSkybox, 0);
        Matrix.translateM(modelSkybox, 0, modelPosition[0], modelPosition[1], modelPosition[2]);

        menuGuide.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.menu_controller_guide), quadVertexShader, quadFragmentShader);
        menuOption1.setTextureCoor(new float[]{0, 0, 0, 1, 0.95f, 0, 0.9f, 1});
        menuGuide.setBuffers();
        tempMatrix = menuGuide.getTransformationMatrix();
        //Matrix.rotateM(tempMatrix, 0, 180, 0, 1, 0);
        //Matrix.translateM(tempMatrix, 0, 0, 0, 7f);
        //Matrix.rotateM(tempMatrix, 0, 180, 0, 1, 0);
        Matrix.translateM(tempMatrix, 0, -2.5f, 0, 5.25f);
        Matrix.rotateM(tempMatrix, 0, 90, 0, 1, 0);
        menuGuide.setTransformationMatrix(tempMatrix);

        menuTitle.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.menu_title), quadVertexShader, quadFragmentShader);
        menuTitle.setBuffers();
        tempMatrix = menuTitle.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0, 0, 3.5f);
        menuTitle.setTransformationMatrix(tempMatrix);

        menuSubtitle.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.menu_subtitle), quadVertexShader, quadFragmentShader);
        menuSubtitle.setBuffers();
        tempMatrix = menuSubtitle.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0, 0, 3.25f);
        //Matrix.scaleM(tempMatrix, 0, 2, 2, 0);
        menuSubtitle.setTransformationMatrix(tempMatrix);

        menuInstruction0.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.menu_instruction_0), quadVertexShader, quadFragmentShader);
        menuInstruction0.setBuffers();
        tempMatrix = menuInstruction0.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0, 0, 3.25f);
        //Matrix.scaleM(tempMatrix, 0, 2, 2, 0);
        menuInstruction0.setTransformationMatrix(tempMatrix);

        menuInstruction1.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.menu_instruction_1), quadVertexShader, quadFragmentShader);
        menuInstruction1.setBuffers();
        tempMatrix = menuInstruction1.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0, 0.15f, 3.2f);
        Matrix.scaleM(tempMatrix, 0, 0.75f, 0.75f, 0);
        menuInstruction1.setTransformationMatrix(tempMatrix);

        menuOption1.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.criminal_impact_ft), quadVertexShader, quadFragmentShader);
        menuOption1.setTextureCoor(new float[]{0, 0, 0, 1, 0.95f, 0, 0.9f, 1});
        menuOption1.setBuffers();
        tempMatrix = menuOption1.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, -0.6f, -0.25f, 3.5f);
        Matrix.scaleM(tempMatrix, 0, 0.25f, 0.25f, 0);
        menuOption1.setTransformationMatrix(tempMatrix);

        menuOption2.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.day_back), quadVertexShader, quadFragmentShader);
        menuOption2.setTextureCoor(new float[]{0, 0, 0, 1, 0.95f, 0, 0.9f, 1});
        menuOption2.setBuffers();
        tempMatrix = menuOption2.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0, -0.25f, 3.5f);
        Matrix.scaleM(tempMatrix, 0, 0.25f, 0.25f, 0);
        menuOption2.setTransformationMatrix(tempMatrix);

        menuOption3.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.menu_credits), quadVertexShader, quadFragmentShader);
        menuOption3.setTextureCoor(new float[]{0, 0, 0, 1, 0.95f, 0, 0.9f, 1});
        menuOption3.setBuffers();
        tempMatrix = menuOption3.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0.6f, -0.25f, 3.5f);
        Matrix.scaleM(tempMatrix, 0, 0.25f, 0.25f, 0);
        menuOption3.setTransformationMatrix(tempMatrix);

        menuSelection.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.menu_selection), quadVertexShader, quadFragmentShader);
        menuSelection.setTextureCoor(new float[]{0, 0, 0, 1, 0.95f, 0, 0.9f, 1});
        menuSelection.setBuffers();
        tempMatrix = menuSelection.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0, -0.25f, 3.5f);
        Matrix.scaleM(tempMatrix, 0, 0.26f, 0.26f, 0);
        menuSelection.setTransformationMatrix(tempMatrix);

    }

    /**
     * Implemented method from StereoRenderer class.
     */
    @Override
    public void onRendererShutdown() {

    }

    /**
     * Override method from Activity class to handle controller input.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() != KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y) { //Labelled button A in MOKE controller
                Log.d(TAG, "dispatchKeyEvent: BUTTON A");
                if(selection != 3){
                    Intent intent = new Intent(this, PlayActivity.class);
                    intent.putExtra("TERRAIN_SELECTION", Integer.toString(selection));
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent(this, CreditsActivity.class);
                    startActivity(intent);
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_X) { //Labelled button C in MOKE controller
                Log.d(TAG, "dispatchKeyEvent: BUTTON C");
                selection--;
                if (selection < 1) {
                    selection = 3; //because there are 3 options.
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B) { //Labelled button D in MOKE controller
                Log.d(TAG, "dispatchKeyEvent: BUTTON D");
                selection++;
                if (selection > 3) {
                    selection = 1;
                }
            }
        }
        return true; //hides controller's in built events from android so app doesn't close on B button
    }

    /**
     * Moves the selection cursor based on current selection.
     */
    private void moveSelection(){
        tempMatrix = menuSelection.getTransformationMatrix();
        if(selection == 1){
            Matrix.translateM(tempMatrix, 0, -0.6f, -0.25f, 3.5f);
        }
        else if(selection == 2){
            Matrix.translateM(tempMatrix, 0, 0, -0.25f, 3.5f);
        }
        else if(selection == 3){
            Matrix.translateM(tempMatrix, 0, 0.6f, -0.25f, 3.5f);
        }
        Matrix.scaleM(tempMatrix, 0, 0.26f, 0.26f, 0);
        menuSelection.setTransformationMatrix(tempMatrix);
    }

}