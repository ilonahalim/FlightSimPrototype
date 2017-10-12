package com.example.ilona.flightsimprototype;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class Cockpit {

    protected int textureId;
    protected int width, height;
    int resourceId;
    boolean mipmaps;
    private FloatBuffer tempTextureBuffer = null;

    public Cockpit(int resourceId, boolean mipmaps) {
        this.resourceId = resourceId;
        this.textureId = -1;
        this.mipmaps = mipmaps;
    }

    public Cockpit(int resourceId) {
        this(resourceId, false);
    }

    private static final int newTextureID() {
        int[] temp = new int[1];
        GLES10.glGenTextures(1, temp, 0);
        return temp[0];
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public final void load(Context context) {

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resourceId, opts);
        width = bmp.getWidth();
        height = bmp.getHeight();
        textureId = newTextureID();
        GLES10.glBindTexture(GL10.GL_TEXTURE_2D, textureId); GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0); GLES10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        if(mipmaps) {
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            GLES10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR_MIPMAP_LINEAR);
        } else GLES10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);

        bmp.recycle();

        if(tempTextureBuffer == null)
            buildTextureMapping();

    }

    private void buildTextureMapping() {

        final float texture[] = {
                0, 0,
                1, 0,
                0, 1,
                1, 1,
        };

        final ByteBuffer ibb = ByteBuffer.allocateDirect(texture.length * 4);
        ibb.order(ByteOrder.nativeOrder());
        tempTextureBuffer = ibb.asFloatBuffer();
        tempTextureBuffer.put(texture);
        tempTextureBuffer.position(0);

    }

    public final void destroy() {
        GLES10.glDeleteTextures(1, new int[] {textureId}, 0);
        textureId = -1;
    }

    public final boolean isLoaded() {
        return textureId >= 0;
    }

    public final void prepare(int wrap) {
        GLES20.glEnable(GL10.GL_TEXTURE_2D);
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, wrap);
        GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, wrap);
        GLES10.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        GLES10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, tempTextureBuffer);
    }

    public final void draw(float x, float y, float w, float h, float rot) {
        GLES10.glPushMatrix();
        GLES10.glTranslatef(x, y, 0);
        GLES10.glRotatef(rot, 0, 0, 1);
        GLES10.glScalef(w, h, 0);
        GLES10.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
        GLES10.glPopMatrix();
    }
}