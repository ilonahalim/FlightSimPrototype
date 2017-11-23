package com.example.ilona.flightsimprototype.loaders;

import com.example.ilona.flightsimprototype.utility.App;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

/**
 * Class: ObjLoader
 * Author: Ilona
 *
 * Reads an obj file and converts it to vertices, normals, texture coordinates and indices arrays.
 * Used in ObjTextureModel class to get the values of the vertices, normals, texture coordinates and indices arrays.
 */

public class ObjLoader {
    private Vector<float[]> vertices;
    private Vector<float[]> normals;
    private Vector<float[]> textures;
    private Vector<int[]> indices;
    private Vector<String[]> faces;

    public float[] verticesArray;
    public float[] normalsArray;
    public float[] textureArray;
    public short[] indicesArray;

    /**
     * Constructor, initialize vectors.
     */
    public ObjLoader(){
        vertices = new Vector<>();
        normals = new Vector<>();
        textures = new Vector<>();
        indices = new Vector<>();
        faces = new Vector<>();
    }

    /**
     * Read Obj text file line by line and store values based on the first character of the line.
     *
     * @param resId the res ID of the Obj file.
     */
    public void readObjFile(int resId) {
        InputStream inputStream = App.context().getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null){
                String[] currentLine = line.split(" ");
                if (line.startsWith("v ")) {        //indicates that the line stores vertex information.
                    int length = currentLine.length;
                    float[] vertex = {Float.parseFloat(currentLine[length-3]),
                            Float.parseFloat(currentLine[length-2]), Float.parseFloat(currentLine[length-1])};
                    vertices.add(vertex);
                } else if (line.startsWith("vt ")) {        //indicates that the line stores texture coordinate information.
                    float[] texture = {Float.parseFloat(currentLine[1]),
                                    Float.parseFloat(currentLine[2])};
                    textures.add(texture);
                } else if (line.startsWith("vn ")) {    //indicates that the line stores normals information.
                    float[] normal = {Float.parseFloat(currentLine[1]),
                            Float.parseFloat(currentLine[2]), Float.parseFloat(currentLine[3])};
                    normals.add(normal);
                } else if (line.startsWith("f ")) {       //indicates that the line stores face information.
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
        }

        for(int i=0;i<indices.size();i++){
            indicesArray[i] = (short) (indices.get(i)[0]);
        }
    }

    /**
     * Process vertex based on face information.
     */
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

    /**
     * Gets vertices information read from the Obj file.
     */
    public Vector<float[]> getVertices(){
        return vertices;
    }
}
