package com.example.ilona.flightsimprototype.loaders;

import android.opengl.GLES20;
import android.util.Log;

import com.example.ilona.flightsimprototype.utility.App;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.google.vr.ndk.base.Version.TAG;

/**
 * Class: ShaderLoader
 * Author: Ilona
 * <p> A class to read shader text files, convert it to String,
 * then save it as an openGl shader.</>
 */

public class ShaderLoader {

    /**
     * Reads and convert a text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The contents of the text file, or null in case of error.
     */
    private String readTextFile(int resId) {
        InputStream inputStream = App.context().getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Converts a shader text file into an OpenGL ES shader.
     *
     * @param type The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
    public int loadGLShader(int type, int resId) {
        String code = readTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }




}