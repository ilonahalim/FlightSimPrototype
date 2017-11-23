package com.example.ilona.flightsimprototype.models;

import com.example.ilona.flightsimprototype.utility.Quaternion;
import com.google.vr.sdk.base.sensors.internal.Vector3d;

/**
 * Class: PlaneModel
 * Author: Ilona
 * The purpose of this class is to extend the ObjTexturedModel that generates a 3D terrain from an Obj
 * file interpreted by the ObjLoader to store and modify transformations of the plane.
 * It uses the textured_model vertex & fragment shader.
 * It uses quaternions to store and modify transformations.
 */

public class PlaneModel extends ObjTexturedModel {
    private static final double ROTATION_RATE = 0.04;

    private float[] planeRotationAngle;

    private Vector3d normal = new Vector3d(0,1,0);
    private Vector3d forwardVector = new Vector3d(0,0,1);
    private Vector3d barrel = new Vector3d(-1,0,0);
    private Quaternion transform;
    private Quaternion axisX;
    private Quaternion axisY;
    private Quaternion axisZ;

    public double speed = .2;
    private Vector3d move;

    private float[] planePos;

    /**
     * Constructor
     */
    public PlaneModel(){
        super();
        planePos = new float[] {0.0f, 25.0f, 0.0f};

        transform = new Quaternion(0,0,0,0);
        //transform.formAxis(forwardVector,0);

        axisX = new Quaternion(0,0,0,0);
        //axisX.formAxis(forwardVector,0);

        axisY = new Quaternion(0,0,0,0);

        axisZ = new Quaternion(0,0,0,0);

        planeRotationAngle = new float[3];

        move = new Vector3d(0, 0, 1);
    }

    /**
     * Set up initial transformation quaternions based on player’s initial head position.
     */
    public void initializeTransformation(Vector3d newForwardVector){
        forwardVector.set(newForwardVector.x, newForwardVector.y, newForwardVector.z);
        transform.formAxis(forwardVector,0);
        barrel.set(0,1,0);
        axisY.formAxis(barrel, Math.toRadians(180));	//set rotation axis, angle
        axisY.normalise();					//normalize the quaternions
        transform.normalise();
        transform =  transform.multiplyQuatWith(axisY);
    }

    /**
     * Rotates the plane to the negative x axis.
     */
    public void rotateXNegative(){
        barrel.set(1,0,0);
        axisY.formAxis(barrel, -ROTATION_RATE);	//set rotation axis, angle
        axisY.normalise();					//normalize the quaternions
        transform.normalise();
        transform =  transform.multiplyQuatWith(axisY);

        planeRotationAngle[0] -= ROTATION_RATE;
    }

    /**
     * Rotates the plane to the positive x axis.
     */
    public void rotateXPositive(){
        barrel.set(1,0,0);
        axisY.formAxis(barrel, ROTATION_RATE);	//set rotation axis, angle
        axisY.normalise();					//normalize the quaternions
        transform.normalise();
        transform =  transform.multiplyQuatWith(axisY);

        planeRotationAngle[0] += ROTATION_RATE;
    }

    /**
     * Rotates the plane to the negative y axis.
     */
    public void rotateYNegative(){
        barrel.set(0,1,0);
        axisZ.formAxis(barrel, -ROTATION_RATE);	//set rotation axis, angle
        axisZ.normalise();					//normalize the quaternions
        transform.normalise();
        transform =  transform.multiplyQuatWith(axisZ);
        planeRotationAngle[1] -= ROTATION_RATE;
        if (planeRotationAngle[1] > 360)
            planeRotationAngle[1] -= 360;
    }

    /**
     * Rotates the plane to the positive y axis.
     */
    public void rotateYPositive(){
        barrel.set(0,1,0);
        axisZ.formAxis(barrel, ROTATION_RATE);	//set rotation axis, angle
        axisZ.normalise();					//normalize the quaternions
        transform.normalise();
        transform =  transform.multiplyQuatWith(axisZ);
        planeRotationAngle[1] += ROTATION_RATE;
        if (planeRotationAngle[1] < 0)
            planeRotationAngle[1] += 360;
    }

    /**
     * Rotates the plane to the negative z axis.
     */
    public void rotateZNegative(){
        barrel = new Vector3d(1, 0, 0);
        axisX.formAxis(forwardVector, -ROTATION_RATE);
        barrel = axisX.multiplyQuatWith(barrel);
        axisX.normalise();
        transform.normalise();
        transform = transform.multiplyQuatWith(axisX);

        planeRotationAngle[2] -= ROTATION_RATE;
    }

    /**
     * Rotates the plane to the positive z axis.
     */
    public void rotateZPositive(){
        barrel = new Vector3d(-1, 0, 0);  //load baase horizaontal vector
        axisX.formAxis(forwardVector, ROTATION_RATE);     //set quaternion about forwardVector vector, set angle
        barrel = axisX.multiplyQuatWith(barrel);   //apply quaternion to the barrel vector
        axisX.normalise();                    //normalize the quaternions
        transform.normalise();
        transform = transform.multiplyQuatWith(axisX);

        planeRotationAngle[2] += ROTATION_RATE;
    }

    /**
     * Increase the movement speed.
     */
    public void speedUp(){
        speed += .002;
    }

    /**
     * Decrease the movement speed.
     */
    public void speedDown(){
        speed -= .002;
    }

    /**
     * Get the transformation quaternion.
     */
    public Quaternion getTransform(){
        return transform;
    }

    /**
     * Get the plane’s rotation angle.
     */
    public float[] getPlaneRotationAngle(){
        return planeRotationAngle;
    }

    /**
     * Gets the plane’s normal.
     */
    public Vector3d getNormal(){
        return normal;
    }

    /**
     * Updates plane’s position based on speed and direction.
     */
    public void updatePlanePos(){
        move.set(0, 0, 1);
        move = transform.multiplyQuatWith(move);
        move.normalize(); //normalize the vector

        planePos[0] = (float) (planePos[0] + move.x * speed);
        planePos[1] = (float) (planePos[1] + move.y * speed);
        planePos[2] = (float) (planePos[2] + move.z * speed);
    }

    /**
     * Get the plane’s translation since last updateplanepos is called.
     */
    public Vector3d getMove(){
        return move;
    }

    /**
     * Get current plane position.
     */
    public float[] getPlanePos(){
        return planePos;
    }
}
