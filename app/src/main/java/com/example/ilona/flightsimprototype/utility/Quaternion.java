package com.example.ilona.flightsimprototype.utility;

import android.opengl.Matrix;

import com.google.vr.sdk.base.sensors.internal.Vector3d;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;


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

    //returns the transformation matrix form of the quaternion
    public float[] getMatrix()
    {
        float[] temp1   =   {
        w*w + x*x - y*y - z*z,  2*x*y + 2*w*z, 	        2*x*z - 2*w*y,        	0,
                2*x*y - 2*w*z, 	      w*w - x*x + y*y - z*z, 	2*y*z + 2*w*x, 	        0,
                2*x*z + 2*w*y, 	      2*y*z - 2*w*x, 	        w*w - x*x - y*y + z*z, 	0,
                0, 	                  0,                        0,                      w*w + x*x + y*y + z*z
        };
        return temp1;
    }

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


    //gets the transformation angle
    public double getAngle()
    {
        return acos(w) * 2.0;
    }

    //sets the quaternion to rotation about Vector &v1 with angle
    public void formAxis(Vector3d vector, double angle)
    {
        double sinAngle = sin(angle * .5);
        x = (float)(vector.x * sinAngle);  //sets up the quaternion compnonests
        y = (float)(vector.y * sinAngle);
        z = (float)(vector.z * sinAngle);
        w = (float)cos(angle);
/*
        //everthing beyond this is to make suer that the magnitude
        //of the quaternion is exactly 1, it can be off from using doubles
        ///These lines of code ensure that it is exactly 1
        double q = x *x + y*y + z*z + w*w;
        float xtra = (float)sqrt(1.000000000/q);
        x *= xtra;
        y *= xtra;
        z *= xtra;
        w  *= xtra;
        q = x *x + y*y + z*z + w*w;
        xtra = (float)sqrt((double)1.000000000/q);
        x *= xtra;
        y *= xtra;
        z *= xtra;
        w  *= xtra;
        */
        this.normalise();
    }


    //Gets the conjugate of the quaternion
    public Quaternion getConjugate()
    {
        Quaternion temp = new Quaternion(-x, -y, -z, w);
        return temp;
    }


    ///normalizes the quaternion so that the magnitude is 1
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


    //multiply 2 quaternions together
    //Returns a quaternion that results from two combined quaternions
    public Quaternion multiplyQuatWith(Quaternion rq)
    {
        Quaternion temp = new Quaternion(w * rq.x + x * rq.w + y * rq.z - z * rq.y,
                w * rq.y + y * rq.w + z * rq.x - x * rq.z,
                w * rq.z + z * rq.w + x * rq.y - y * rq.x,
                w * rq.w - x * rq.x - y * rq.y - z * rq.z);
        return temp;
    }

    //Applies quaternion to a vector and returns the transformed vector.
     public Vector3d multiplyQuatWith(Vector3d vector)
    {
        Quaternion vectorQuat = new Quaternion((float)vector.x, (float)vector.y, (float)vector.z, 0);
        Quaternion resultQuat;
        resultQuat = vectorQuat.multiplyQuatWith(this.getConjugate()); //Multiply by the quaternion's conjugate
        resultQuat = this.multiplyQuatWith(resultQuat); //Multiply by the quaternion
        Vector3d resultVec = new Vector3d(resultQuat.x, resultQuat.y, resultQuat.z);
        return resultVec;
    }
}
