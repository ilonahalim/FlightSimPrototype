package com.example.ilona.flightsimprototype.models;

import android.opengl.Matrix;

/**
 * Class: Camera
 * Author: Ilona
 * <p> A class to store and modify camera position matrix.</>
 */

public class Camera {
    private float[] positionMatrix;

    /**
     * Constructor
     */
    public Camera(){
        positionMatrix = new float[16];
        Matrix.setLookAtM(positionMatrix, 0, 0, 0, 5f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    /**
     * Sets the camera’s position matrix.
     */
    public void setPositionMatrix(float[] newEyePos, float[] targetObjectPos, float[] newNormalPos){
        Matrix.setLookAtM(positionMatrix, 0, newEyePos[0], newEyePos[1], newEyePos[2],
                targetObjectPos[0], targetObjectPos[1], targetObjectPos[2],
                newNormalPos[0], newNormalPos[1], newNormalPos[2]);
    }

    /**
     * Gets the camera’s position matrix.
     */
    public float[] getPositionMatrix(){
        return positionMatrix;
    }
}
