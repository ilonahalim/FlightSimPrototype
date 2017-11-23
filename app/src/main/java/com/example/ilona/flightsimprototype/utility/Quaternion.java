package com.example.ilona.flightsimprototype.utility;

import android.opengl.Matrix;

import com.google.vr.sdk.base.sensors.internal.Vector3d;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

/**
 * Class: Quaternion
 * Author: Ilona
 * <p> The purpose of this class is to store transformation in the form of quaternion.
 * It also can return a conversion of quaternion into matrix, multiplication result of two quaternion
 * and multiplication result of quaternion and vector.
 * This class will be used mainly to control model movement.</>
 */

public class Quaternion {

    private float x;
    private float y;
    private float z;
    private float w;

    public Quaternion(float x1, float y1, float z1, float w1)
    {
        x = x1;
        y = y1;
        z = z1;
        w = w1;
    }

    /**
     * Returns quaternion as a transformation matrix.
     *
     */
    public float[] getMatrix()
    {
        return new float[]{
        w*w + x*x - y*y - z*z,  2*x*y + 2*w*z, 	        2*x*z - 2*w*y,        	0,
                2*x*y - 2*w*z, 	      w*w - x*x + y*y - z*z, 	2*y*z + 2*w*x, 	        0,
                2*x*z + 2*w*y, 	      2*y*z - 2*w*x, 	        w*w - x*x - y*y + z*z, 	0,
                0, 	                  0,                        0,                      w*w + x*x + y*y + z*z
        };
    }

    /**
     * Returns quaternion as a transformation matrix with translation of xTrans, yTrans and zTrans.
     *
     */
    public float[] getTranslatedMatrix(float xTrans, float yTrans, float zTrans)
    {
        float[] translationMatrix   =   {
                1,        0, 	        0,        	0,
                0, 	      1, 	        0, 	        0,
                0, 	      0, 	        1, 	        0,
                xTrans, 	      yTrans,            zTrans,          1
        };
        Matrix.multiplyMM(translationMatrix, 0, translationMatrix, 0, this.getMatrix(), 0);
        return translationMatrix;
    }

    /**
     * Set the quaternion to rotate at an angle around vector
     */
    public void formAxis(Vector3d vector, double angle)
    {
        double sinAngle = sin(angle * .5);
        x = (float)(vector.x * sinAngle);  //sets up the quaternion compnonests
        y = (float)(vector.y * sinAngle);
        z = (float)(vector.z * sinAngle);
        w = (float)cos(angle);
        this.normalise();
    }


    /**
     * Gets the conjugate of the quaternion
     */
    private Quaternion getConjugate()
    {
        return new Quaternion(-x, -y, -z, w);
    }

    /**
     * normalizes the quaternion.
     */
    public void normalise()
    {
        double mag2 = w * w + x * x + y * y + z * z;
        if (  mag2!=0.f && (mag2 - 1.0f) > .000001f) {
            float mag = (float)sqrt(mag2);
            w /= mag;
            x /= mag;
            y /= mag;
            z /= mag;
        }
    }

    /**
     * multiply 2 quaternions together.
     * @return a quaternion that results from two combined quaternions.
     */
    public Quaternion multiplyQuatWith(Quaternion quaternion)
    {
        return new Quaternion(w * quaternion.x + x * quaternion.w + y * quaternion.z - z * quaternion.y,
                w * quaternion.y + y * quaternion.w + z * quaternion.x - x * quaternion.z,
                w * quaternion.z + z * quaternion.w + x * quaternion.y - y * quaternion.x,
                w * quaternion.w - x * quaternion.x - y * quaternion.y - z * quaternion.z);
    }

    /**
     * Applies quaternion to a vector and returns the transformed vector.
     */
     public Vector3d multiplyQuatWith(Vector3d vector)
    {
        Quaternion vectorQuat = new Quaternion((float)vector.x, (float)vector.y, (float)vector.z, 0);
        Quaternion resultQuat;
        resultQuat = vectorQuat.multiplyQuatWith(this.getConjugate()); //Multiply by the quaternion's conjugate
        resultQuat = this.multiplyQuatWith(resultQuat); //Multiply by the quaternion
        return new Vector3d(resultQuat.x, resultQuat.y, resultQuat.z);
    }
}
