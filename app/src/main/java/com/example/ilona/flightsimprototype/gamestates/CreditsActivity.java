package com.example.ilona.flightsimprototype.gamestates;

import android.content.Intent;
import android.hardware.input.InputManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.example.ilona.flightsimprototype.R;
import com.example.ilona.flightsimprototype.loaders.ShaderLoader;
import com.example.ilona.flightsimprototype.loaders.TextureLoader;
import com.example.ilona.flightsimprototype.models.Camera;
import com.example.ilona.flightsimprototype.models.GUI;
import com.example.ilona.flightsimprototype.models.Quad;
import com.example.ilona.flightsimprototype.models.SkyBox;
import com.example.ilona.flightsimprototype.utility.App;
import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

public class CreditsActivity extends GvrActivity implements GvrView.StereoRenderer{

    private static final String TAG = "CreditsActivity";
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 2000.0f;

    private Camera camera;
    private Quad menuTitle;
    private GUI myGui;

    private float[] view;
    private float[] modelViewProjection;
    private float[] modelView;

    private ShaderLoader shaderLoader;

    private float counter;

    private GvrAudioEngine gvrAudioEngine;
    private volatile int sourceId = GvrAudioEngine.INVALID_ID;

    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeGvrView();

        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];

        camera = new Camera();
        myGui = new GUI();
        menuTitle = new Quad();

        counter = -2;

        shaderLoader = new ShaderLoader();

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
    public void onNewFrame(HeadTransform headTransform) {

    }

    @Override
    public void onDrawEye(Eye eye) {
        float[] tempMatrix = new float[16];
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera.getPositionMatrix(), 0);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        myGui.draw(0, 0);
        Matrix.setIdentityM(tempMatrix, 0);
        Matrix.translateM(tempMatrix, 0, 0, counter, 3.5f);
        Matrix.scaleM(tempMatrix, 0, 2, 2, 0);
        menuTitle.setTransformationMatrix(tempMatrix);
        menuTitle.draw(view, perspective);
        counter+= 0.001f;
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        int guiVertexShader = shaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.gui_vertex);
        int guiFragmentShader = shaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.gui_fragment);

        int quadVertexShader = shaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.quad_vertex);
        int quadFragmentShader = shaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.quad_fragment);

        myGui = new GUI();
        myGui.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.credits_text), guiVertexShader, guiFragmentShader);
        myGui.setPositionTexture();
        //myGui.transform(0.75f, 0.75f, 0,0,0);
        myGui.setBuffers();

        //float[] tempMatrix = new float[16];
        menuTitle.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.credits_text_long), quadVertexShader, quadFragmentShader);
        menuTitle.setBuffers();
        /*
        tempMatrix = menuTitle.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0, 0, 3.5f);
        //Matrix.scaleM(tempMatrix, 0, 2, 2, 0);
        menuTitle.setTransformationMatrix(tempMatrix);
        */
    }

    @Override
    public void onRendererShutdown() {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() != KeyEvent.ACTION_DOWN) {
            Intent intent = new Intent(this, MenuActivity.class);
            startActivity(intent);
        }
        return true; //hides controller's in built events from android so app doesn't close on B button
    }
}