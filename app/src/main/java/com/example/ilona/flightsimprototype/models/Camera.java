package com.example.ilona.flightsimprototype.models;

import android.opengl.Matrix;

/**
 * Created by Ilona on 16-Nov-17.
 */

public class Camera {
    private float[] positionMatrix;
    private float camObjectDistance;

    public Camera(){
        positionMatrix = new float[16];
        Matrix.setLookAtM(positionMatrix, 0, 0, 0, 5f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    public void setPositionMatrix(float[] newEyePos, float[] targetObjectPos, float[] newNormalPos){
        Matrix.setLookAtM(positionMatrix, 0, newEyePos[0], newEyePos[1], newEyePos[2],
                targetObjectPos[0], targetObjectPos[1], targetObjectPos[2],
                newNormalPos[0], newNormalPos[1], newNormalPos[2]);
    }

    public float[] getPositionMatrix(){
        return positionMatrix;
    }
}
