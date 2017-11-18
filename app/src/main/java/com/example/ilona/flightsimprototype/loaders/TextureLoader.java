package com.example.ilona.flightsimprototype.loaders;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;


public class TextureLoader {
    public TextureLoader(){}

    public static int loadTexture(Context context, int resourceId)
    {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
            //Bitmap bitmap = BitmapFactory.decodeResource(App.context().getResources(), R.drawable.grass);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public static int loadCubeTexture(Context context, int[] resourceIdArray)
    {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureHandle[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_R, GLES20.GL_CLAMP_TO_EDGE);


            for(int i = 0; i<6;i++) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;   // No pre-scaling

                // Read in the resource
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceIdArray[i], options);

                // Load the bitmap into the bound texture.
                //GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, bitmap, 0);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GLES20.GL_RGBA, bitmap, 0);

                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bitmap.recycle();
            }
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }
}
