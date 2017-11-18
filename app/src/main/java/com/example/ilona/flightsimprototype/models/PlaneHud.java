package com.example.ilona.flightsimprototype.models;

import com.example.ilona.flightsimprototype.models.GUI;

/**
 * Created by Ilona on 15-Nov-17.
 */

public class PlaneHud extends GUI {
    int dynamicTextureID;

    GUI staticHud;
    GUI headingIndicator;
    GUI altitude;
    GUI wingTilt;
    GUI pitchLadder;

    float headingOffset;
    float altitudeOffset;
    float wingRotation;
    float pitchOffset;

    public PlaneHud(){
        staticHud = new GUI();
        headingIndicator = new GUI();
        altitude = new GUI();
        wingTilt = new GUI();
        pitchLadder = new GUI();
    }

    public void setDynamicTexture(int textureId){
        dynamicTextureID = textureId;
    }

    public void setDynamicOffset(float pitchAngle, float headingAngle,  float tiltAngle, float planeAltitude){
        headingOffset = headingAngle;
        altitudeOffset = planeAltitude;
        wingRotation = tiltAngle;
        pitchOffset = pitchAngle;
    }

    @Override
    public void setUpOpenGl(int texture, int vertexShader, int fragmentShader){
        super.setUpOpenGl(dynamicTextureID, vertexShader, fragmentShader);
        headingIndicator.setUpOpenGl(dynamicTextureID, vertexShader, fragmentShader);
        altitude.setUpOpenGl(dynamicTextureID, vertexShader, fragmentShader);
        wingTilt.setUpOpenGl(dynamicTextureID, vertexShader, fragmentShader);
        pitchLadder.setUpOpenGl(dynamicTextureID, vertexShader, fragmentShader);
    }

    @Override
    public void setBuffers(){
        super.setBuffers();
        headingIndicator.setBuffers();
        altitude.setBuffers();
        wingTilt.setBuffers();
        pitchLadder.setBuffers();
    }

    @Override
    public void transform(float xScale, float yScale, float rotationAngle, float translateX, float translateY){
        super.transform(xScale, yScale, rotationAngle, translateX, translateY);
        headingIndicator.transform(0.5f, 0.5f, 0, 0, 1f);
        altitude.transform(0.5f, 0.5f, 0, 0, 0);
        wingTilt.transform(0.5f, 0.5f, rotationAngle, 0, 0);
        pitchLadder.transform(0, 0, rotationAngle, 0, 0);
    }

    @Override
    public void setPositionTexture(){
        float[] headingPos = new float[]{-1, 1, -1, -0.2f, 1, 1, 1, -0.2f};
        float[] pitchPos = new float[]{-1, 1, -1, -1, 0.6f, 1, 0.6f, -1};
        float[] altitudePos = new float[]{-1, 1, -1, -1, 0.2f, 1, 0.2f, -1};
        float[] rollPos = new float[]{-1, 1, -1, -1, 0, 1, 0, -1};

        //define texture coordinates based on texture atlas
        float[] headingTex = new float[]{0, 1, 0.1f, 1, 0, 0, 0.1f, 0};
        float[] pitchTex = new float[]{0.1f, 0, 0.1f, 1, 0.4f, 0, 0.4f, 1};
        float[] altitudeTex = new float[]{0.4f, 0, 0.4f, 1, 0.5f, 0, 0.5f, 1};
        float[] rollTex = new float[]{0.5f, 0, 0.5f, 1, 1, 0, 1, 1};

        super.setPositionTexture();
        headingIndicator.setCustomPositionTexture(headingPos, headingTex);
        pitchLadder.setCustomPositionTexture(pitchPos, pitchTex);
        altitude.setCustomPositionTexture(altitudePos, altitudeTex);
        wingTilt.setCustomPositionTexture(rollPos, rollTex);
    }

    @Override
    public void draw(float xScrollOffset, float yScrollOffset){

        this.transform(0,0,0,0,0);
        super.draw(0, 0);
        headingIndicator.draw(0, headingOffset);
        //pitchLadder.draw(0, pitchOffset);
        altitude.draw(0, altitudeOffset);
        //wingTilt.draw(0, 0);
    }
}
