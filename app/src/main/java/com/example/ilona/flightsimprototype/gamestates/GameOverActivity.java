package com.example.ilona.flightsimprototype.gamestates;

import android.content.Intent;
import android.hardware.input.InputManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.example.ilona.flightsimprototype.R;
import com.example.ilona.flightsimprototype.loaders.ShaderLoader;
import com.example.ilona.flightsimprototype.loaders.TextureLoader;
import com.example.ilona.flightsimprototype.models.Camera;
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

public class GameOverActivity extends GvrActivity implements GvrView.StereoRenderer{
    private static final String TAG = "GameOverActivity";
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 2000.0f;

    private Camera camera;
    private Quad menuTitle;
    private Quad menuSubtitle;

    private float[] view;

    private ShaderLoader shaderLoader;

    private GvrAudioEngine gvrAudioEngine;
    private volatile int sourceId = GvrAudioEngine.INVALID_ID;

    private InputManager myInputManager;
    private InputDevice myInputDevice;

    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeGvrView();

        view = new float[16];

        camera = new Camera();
        menuTitle = new Quad();
        menuSubtitle = new Quad();

        shaderLoader = new ShaderLoader();
    }

    public void initializeGvrView() {
        setContentView(R.layout.activity_main);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(false);

        gvrView.enableCardboardTriggerEmulation();

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }

        setGvrView(gvrView);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        /*
        for(int i = 0; i<myInputManager.getInputDeviceIds().length; i++){
            InputDevice tempDevice = myInputManager.getInputDevice(myInputManager.getInputDeviceIds()[i]);
            if(tempDevice.getControllerNumber() != 0){
                myInputDevice = tempDevice;
            }
            //Log.d(TAG, "onNewFrame: Input Device: " + myInputManagerCompat.getInputDevice(myInputManagerCompat.getInputDeviceIds()[i]).getControllerNumber());
        }
        */
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera.getPositionMatrix(), 0);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        menuTitle.draw(view, perspective);
        menuSubtitle.draw(view, perspective);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        int quadVertexShader = shaderLoader.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.quad_vertex);
        int quadFragmentShader = shaderLoader.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.quad_fragment);

        float[] tempMatrix = new float[16];
        menuTitle.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.gameover_title), quadVertexShader, quadFragmentShader);
        menuTitle.setBuffers();
        tempMatrix = menuTitle.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0, 0, 3.5f);
        //Matrix.scaleM(tempMatrix, 0, 2, 2, 0);
        menuTitle.setTransformationMatrix(tempMatrix);

        menuSubtitle.setUpOpenGl(TextureLoader.loadTexture(App.context(), R.drawable.gameover_subtitle), quadVertexShader, quadFragmentShader);
        menuSubtitle.setBuffers();
        tempMatrix = menuSubtitle.getTransformationMatrix();
        Matrix.translateM(tempMatrix, 0, 0, 0, 3.25f);
        //Matrix.scaleM(tempMatrix, 0, 2, 2, 0);
        menuSubtitle.setTransformationMatrix(tempMatrix);


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