package com.example.ilona.flightsimprototype;

import android.util.Log;

import com.google.vr.sdk.base.sensors.internal.Vector3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Vector;

/**
 * Created by Ilona on 10-Nov-17.
 */

public class ObjLoader {
    private String TAG = "ObjLoader";

    public Vector<float[]> vertices;
    public Vector<float[]> normals;
    public Vector<float[]> textures;
    public Vector<int[]> indices;
    public Vector<String[]> faces;
    public Vector<int[]> textureMap;

    public float[] verticesArray;
    public float[] normalsArray;
    public float[] textureArray;
    public short[] indicesArray;

    public float min = 100;
    public float max = -2;

    public ObjLoader(){
        vertices = new Vector<>();
        normals = new Vector<>();
        textures = new Vector<>();
        indices = new Vector<>();
        faces = new Vector<>();
        textureMap = new Vector<>();
    }
    public String readObjFile2(int resId) {
        InputStream inputStream = App.context().getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null){
                String[] currentLine = line.split(" ");
                if (line.startsWith("v ")) {
                    //Log.d(TAG, "readObjFile: current line v length " + currentLine.length);
                    int length = currentLine.length;
                    float[] vertex = {Float.parseFloat(currentLine[length-3]),
                            Float.parseFloat(currentLine[length-2]), Float.parseFloat(currentLine[length-1])};
                    vertices.add(vertex);
                } else if (line.startsWith("vt ")) {
                    float[] texture = {Float.parseFloat(currentLine[1]),
                                    Float.parseFloat(currentLine[2])};
                    textures.add(texture);
                } else if (line.startsWith("vn ")) {
                    float[] normal = {Float.parseFloat(currentLine[1]),
                            Float.parseFloat(currentLine[2]), Float.parseFloat(currentLine[3])};
                    normals.add(normal);
                } else if (line.startsWith("f ")) {
                    for (int i = 1; i<currentLine.length; i++) {
                        String[] vertex1 = currentLine[i].split("/");
                        faces.add(vertex1);
                    }
                }

            }
            textureArray = new float[vertices.size() * 2];
            normalsArray = new float[vertices.size() * 3];
            for (String[] vertex:faces
                 ) {
                processVertex(vertex,indices,textures,normals,textureArray,normalsArray);
            }
            reader.close();

            //return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        verticesArray = new float[vertices.size()*3];
        indicesArray = new short[indices.size()];

        int vertexPointer = 0;
        for(float[] vertex:vertices){
            verticesArray[vertexPointer++] = vertex[0];
            verticesArray[vertexPointer++] = vertex[1];
            verticesArray[vertexPointer++] = vertex[2];
            if(vertex[0] < min)
                min = vertex[0];
            if(vertex[0]>max)
                max = vertex[0];
        }

        for(int i=0;i<indices.size();i++){
            indicesArray[i] = (short) (indices.get(i)[0]);
        }

        return null;
    }

    private static void processVertex(String[] vertexData, Vector<int[]> indices,
                                      Vector<float[]> textures, Vector<float[]> normals, float[] textureArray,
                                      float[] normalsArray) {
        int[] currentVertexPointer = {Integer.parseInt(vertexData[0]) - 1};
        indices.add(currentVertexPointer);
        float[] currentTex = textures.get(Integer.parseInt(vertexData[1])-1);
        textureArray[currentVertexPointer[0]*2] = currentTex[0];
        textureArray[currentVertexPointer[0]*2+1] = 1 - currentTex[1];
        float[] currentNorm = normals.get(Integer.parseInt(vertexData[2])-1);
        normalsArray[currentVertexPointer[0]*3] = currentNorm[0];
        normalsArray[currentVertexPointer[0]*3+1] = currentNorm[1];
        normalsArray[currentVertexPointer[0]*3+2] = currentNorm[2];
    }
}
