package com.example.ilona.flightsimprototype;

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

//constructor
    public Quaternion(float x1, float y1, float z1, float w1)
    {
        x = x1;
        y = y1;
        z = z1;
        w = w1;
    }
//returns the transformation matrix form of the quaternion
//this matrix can be direclty pushed on the opengl matrix stack
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
        float[] temp   =   {
                1,        0, 	        0,        	0,
                0, 	      1, 	        0, 	        0,
                0, 	      0, 	        1, 	        0,
                xTrans, 	      yTrans,            zTrans,          1
        };
        Matrix.multiplyMM(temp, 0, temp, 0, this.getMatrix(), 0);
        return temp;
    }
    //gets axis of rotation from quatertion
    public Vector3d getAxis()
    {
        float scale = (float)sqrt((x * x + y * y + z * z));
        float axisx = (float) x / scale;
        float axisy = (float) y / scale;
        float axisz = (float) z / scale;
        Vector3d toreturn = new Vector3d(axisx, axisy, axisz);
        return toreturn;
    }

    //gets the angle of the quaternion trasformation
    public double getAngle()
    {
        double toreturn = acos(w) * 2.0;
        return toreturn;
    }
    //sets the quaternion to rotation about Vector &v1 with angle
    public void fromAxis(Vector3d v1, double angle)
    {
        double sinAngle;
        Vector3d vn = new Vector3d(v1.x,v1.y,v1.z);//gets a vector
        sinAngle = sin(angle * .5);	//takes sing of have the angle of rotation
        x = (float)(vn.x * sinAngle);  //sets up the quaternion compnonests
        y = (float)(vn.y * sinAngle);
        z = (float)(vn.z * sinAngle);
        w = (float)cos(angle);

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
    }


    //Gets the conjugate of the quaternion
    public Quaternion getConjugate()
    {
        Quaternion temp = new Quaternion(-x, -y, -z, w);
        return temp;
    }


    ///normalizes the quaternion so that the mag is 1
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
    /*
//copy constructor for the quaternion
    Quaternion::Quaternion(const Quaternion& p)
    {
        x = p.x;
        y = p.y;
        z = p.z;
        w = p.w;
    }*/
    //overloaded * operator to multiply 2 quaternions together
//effectivly combining the transformations of both quaterions'
//into a single quaternion
    public Quaternion multiplyQuatWith(Quaternion rq)
    {
        // the constructor takes its arguments as (x, y, z, w)
        Quaternion temp = new Quaternion(w * rq.x + x * rq.w + y * rq.z - z * rq.y,
                w * rq.y + y * rq.w + z * rq.x - x * rq.z,
                w * rq.z + z * rq.w + x * rq.y - y * rq.x,
                w * rq.w - x * rq.x - y * rq.y - z * rq.z);
        return temp;
    }
    //overloads teh * operator so that a quaternion
//can be applied to a vector.  returns the transformed vector
     public Vector3d multiplyQuatWith(Vector3d vec)
    {
        //To apply to a vector,
        //The vector needs to be multiplied by the quaternion
        //and that quaternions conjugate
        Vector3d vn = new Vector3d(vec.x, vec.y, vec.z);
        Quaternion vecQuat = new Quaternion((float)vn.x, (float)vn.y, (float)vn.z, 0);
        Quaternion resQuat;
        resQuat = vecQuat.multiplyQuatWith(this.getConjugate());
        resQuat = this.multiplyQuatWith(resQuat);
        Vector3d toreturn = new Vector3d(resQuat.x, resQuat.y, resQuat.z);
        return toreturn;
    }
}
