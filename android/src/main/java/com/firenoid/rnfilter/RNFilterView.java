package com.firenoid.rnfilter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.firenoid.rnfilter.Filters;

public class RNFilterView extends GLSurfaceView implements GLSurfaceView.Renderer {

    public static final int COMMAND_CAPTURE_CURRENT_VIEW = 3;


    private Activity mActivity;
    private Context mContext;


    private int mShaderProgramBase;
    private int mShaderProgramFinalPass;
    private int mShaderProgramContrastSaturationBrightness;
    public boolean isInitialized;
    public boolean EnableContrastSaturationBrightness;
    public boolean isThumbnail;


    public boolean BOOL_LoadTexture = false;
    public int mImageWidth = 0;
    public int mImageHeigth = 0;

    public boolean SaveImage = false;
    private boolean shallRenderImage = false;
    public String SavePath = "";

    public void Render()
    {
        shallRenderImage = true;
    }

    private FloatBuffer VB;
    private ShortBuffer IB;
    private FloatBuffer TC;

    public int[] hToFilterTexture = new int[3];

    private EffectContext mEffectContext;
    private Effect mEffect;


    private boolean preFilter = false;
    public float mSaturationValue = 1f;
    public float mContrastValue = 1f;
    public float mBrightnessValue = 1f;
    private volatile boolean saveFrame;

    public Bitmap toLoad = null;
    public Bitmap originalBitmap = null;
    public Bitmap currentBitmap = null;
    public Bitmap thumbnailBitmap = null;
    public int currentFilterId;
    public boolean IsUsedBitmap()
    {
        return !default_b;
    }
    private boolean startup = true;
    private boolean default_b = true;
    private String source;
    private int hPos, hTex;
    private int cmp_W, cmp_H, cmp_X, cmp_Y;
    private int tx;
    String ERROR = "";


    boolean first = true;
    boolean didRender = false;
    boolean firstRender = true;


    private int[] fb;
    private int[] depthRb;


    private String generalVS =
            "attribute vec4 vPosition;" +
                    "attribute vec2 texCoords;" +
                    "uniform sampler2D filteredPhoto;" +
                    "varying vec2 UV;" +
                    "void main() {" +
                    "  gl_Position = vPosition;" +
                    "  UV = texCoords;" +
                    "}";
    private String generalreverseVS =
            "attribute vec4 vPosition;" +
                    "attribute vec2 texCoords;" +
                    "uniform sampler2D filteredPhoto;" +
                    "varying vec2 UV;" +
                    "void main() {" +
                    "  gl_Position = vPosition;" +
                    "  vec2 tc = vec2(1, 1) - texCoords;" +
                    "  tc.x = texCoords.x;" +
                    "  UV = tc;" +
                    "}";

    float[] vertices = new float[]
            {
                    -1f,  1f, 0.0f,
                    -1f, -1f, 0.0f,
                    1f, -1f, 0.0f,
                    1f,  1f, 0.0f,
                    -1f,  1f, 0.0f,
                    1f, -1f, 0.0f
            };
    short[] indices = new short[]
            {
                    0, 1, 2, 0, 2, 3
            };
    float[] texCoords =
            {
                    0.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 0.0f,
                    1.0f, 1.0f
            };

    public RNFilterView(Context context, Activity activity) {
        super(context);
        mActivity = activity;
        mContext = context;
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }



    public void setSource(String source) {
        if(this.source == null){
            this.source = source;
        }
    }

    public void setBrightness(double value) {
        mBrightnessValue = (float) value;
        shallRenderImage = true;
        this.requestRender();

    }
    public void setContrast(double value) {
        shallRenderImage = true;
        mContrastValue = (float) value;
        this.requestRender();
    }
    public void setSaturation(double value) {
        shallRenderImage = true;
        mSaturationValue = (float) value;
        this.requestRender();
    }
    public void setFilter(double value) {
        preFilter = true;
        currentFilterId = (int)value;

        this.requestRender();
    }
    public void setThumbnail(boolean value) {
        isThumbnail = value;
    }

    public void generaThumbnail() {

//        String url = generateBitmap(this.thumbnailBitmap);
        new fileFromBitmap(this.thumbnailBitmap, getId()  , mContext).execute();
    }

    private void initEffect() {
        EffectFactory effectFactory = mEffectContext.getFactory();
        if (mEffect != null) {
            mEffect.release();
        }
        /**
         * Initialize the correct effect based on the selected menu/action item
         */
        switch (currentFilterId) {

            case 0:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
                break;
            case 1:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
                mEffect.setParameter("scale", 0.7f);
                break;

            case 2:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_BLACKWHITE);
                mEffect.setParameter("black", .1f);
                mEffect.setParameter("white", .7f);
                break;

            case 3:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_BRIGHTNESS);
                mEffect.setParameter("brightness", 2.0f);
                break;

            case 4:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_CONTRAST);
                mEffect.setParameter("contrast", 1.4f);
                break;

            case 5:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS);
                break;

            case 6:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
                break;

            case 7:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                mEffect.setParameter("first_color", Color.YELLOW);
                mEffect.setParameter("second_color", Color.DKGRAY);
                break;

            case 8:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FILLLIGHT);
                mEffect.setParameter("strength", .8f);
                break;

            case 9:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FISHEYE);
                mEffect.setParameter("scale", .5f);
                break;
            case 10:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FLIP);
                mEffect.setParameter("horizontal", true);
                break;

            case 11:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_GRAIN);
                mEffect.setParameter("strength", 1.0f);
                break;

            case 12:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
                break;

            case 13:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH);
                break;

            case 14:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
                break;

            case 15:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE);
                break;

            case 16:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_ROTATE);
                mEffect.setParameter("angle", 180);
                break;

            case 17:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SATURATE);
                mEffect.setParameter("scale", .5f);
                break;

            case 18:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
                break;

            case 19:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SHARPEN);
                break;

            case 20:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE);
                mEffect.setParameter("scale", .9f);
                break;

            case 21:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_TINT);
                mEffect.setParameter("tint", Color.MAGENTA);
                break;

            case 22:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
                mEffect.setParameter("scale", .5f);
                break;

            default:
                break;

        }
    }

    private void applyEffect() {

        mEffect.apply(hToFilterTexture[0], mImageWidth, mImageHeigth, hToFilterTexture[1]);
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        if(!isInitialized){
            isInitialized = true;
            mEffectContext = EffectContext.createWithCurrentGlContext();
            try {
                this.originalBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver() , Uri.parse(source));
            } catch (IOException e) {
                e.printStackTrace();
            }

            EnableContrastSaturationBrightness = true;
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            ByteBuffer bVB = ByteBuffer.allocateDirect(vertices.length * 4); bVB.order(ByteOrder.nativeOrder());
            ByteBuffer bIB = ByteBuffer.allocateDirect(indices.length  * 2); bIB.order(ByteOrder.nativeOrder());
            ByteBuffer bTC = ByteBuffer.allocateDirect(texCoords.length * 4); bTC.order(ByteOrder.nativeOrder());

            VB = bVB.asFloatBuffer();
            IB = bIB.asShortBuffer();
            TC = bTC.asFloatBuffer();
            VB.put(vertices); VB.position(0);
            IB.put(indices); IB.position(0);
            TC.put(texCoords); TC.position(0);
            loadShaders();
                LoadBitmap(this.originalBitmap);
                if(!isThumbnail){
                    generaThumbnail();
                }
            }

    }


    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
//        if (mTexRenderer != null) {
//            mTexRenderer.updateViewSize(width, height);
//        } else {
            GLES20.glViewport(0, 0, width, height);
//        }

    }


    public void onDrawFrame(GL10 unused) {

            if (BOOL_LoadTexture) {
                if (this.toLoad != null) {
                    this.LoadTexture(this.toLoad);
                    refreshSize();
                    //what was written here is now in the refreshSize() function just above this one :)
                }

                this.toLoad = null;
                System.gc();
                BOOL_LoadTexture = false;
            }


            didRender = false;
            firstRender = true;
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//            if(currentFilterId != 0) {
                initEffect();
                applyEffect();
//            }
//            drawquad();
            if (!isThumbnail)
            {
                GLES20.glViewport(0, 0, mImageWidth, mImageHeigth);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,  hToFilterTexture[2], 0);
                GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRb[0]);
                int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
                if (status != GLES20.GL_FRAMEBUFFER_COMPLETE)
                    throw (new RuntimeException("SHEE"));
                GLES20.glClearColor(.0f, .0f, .0f, 1.0f);
                GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glUseProgram(mShaderProgramContrastSaturationBrightness);
                setVSParams(mShaderProgramContrastSaturationBrightness);
                setShaderParamPhoto(mShaderProgramContrastSaturationBrightness, hToFilterTexture[1]);
                int sat = GLES20.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Saturation");
                int br = GLES20.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Brightness");
                int ctr = GLES20.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Contrast");
                GLES20.glUniform1f(sat, mSaturationValue);
                GLES20.glUniform1f(br, mBrightnessValue);
                GLES20.glUniform1f(ctr, mContrastValue);
                drawquad();
            }
//            int tx;
//            if (didRender)
//                firstRender = false;
//            first = !first;
//            if(currentFilterId != 0){
            int tx = !isThumbnail ?  hToFilterTexture[2] : hToFilterTexture[1];
//            } else {
//                tx =  hToFilterTexture[0];
//            }

            GLES20.glViewport(cmp_X, cmp_Y, cmp_W, cmp_H);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glClearColor(.0f, .0f, .0f, 1.0f);
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(mShaderProgramFinalPass);
            setVSParams(mShaderProgramFinalPass);
            setShaderParamPhoto(mShaderProgramFinalPass, tx);
            drawquad();
            shallRenderImage = false;


//        if (SaveImage) {
//            SaveImage = false;
//
//
////            mActivity.toastHandler.post(mActivity.loadingRunnableShow);
//
//            saveTarget.Set();
//            GLES20.glUseProgram(mShaderProgramFinalPass);
//            setVSParams(mShaderProgramFinalPass);
//            setShaderParamPhoto(mShaderProgramFinalPass, tx);
//            drawquad();
//
//            saveTarget.pfsave();
//            int wd = saveTarget.Width;
//            int hg = saveTarget.Height;
//
//            int screenshotSize = wd * hg;
//            ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
//            bb.order(ByteOrder.nativeOrder());
//            GLES20.glReadPixels(0, 0, wd, hg, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
//            int pixelsBuffer[] = new int[screenshotSize];
//            bb.asIntBuffer().get(pixelsBuffer);
//            bb = null;
//            Bitmap bitmap = Bitmap.createBitmap(wd, hg, Bitmap.Config.RGB_565);
//            bitmap.setPixels(pixelsBuffer, screenshotSize - wd, -wd, 0, 0, wd, hg);
//            pixelsBuffer = null;
//
//            short sBuffer[] = new short[screenshotSize];
//            ShortBuffer sb = ShortBuffer.wrap(sBuffer);
//            bitmap.copyPixelsToBuffer(sb);
//
//            for (int i = 0; i < screenshotSize; ++i) {
//                short v = sBuffer[i];
//                sBuffer[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
//            }
//            sb.rewind();
//            bitmap.copyPixelsFromBuffer(sb);
//
//
//            File file = new File(SavePath);
//
//            FileOutputStream fOut = null;
//            try {
//                fOut = new FileOutputStream(file);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
//
//            //call media scanner to show the new picture in the gallery
//
//            MediaScannerConnection.scanFile(
//                    mActivity,
//                    new String[]{SavePath},
//                    null,
//                    new MediaScannerConnection.OnScanCompletedListener() {
//                        @Override
//                        public void onScanCompleted(String path, Uri uri) {
//                            Log.v("CO2 Photo Editor ",
//                                    "file " + path + " was scanned seccessfully: " + uri);
//                        }
//                    });
//            try {
//                fOut.flush();
//                fOut.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
////            MainActivity.toastHandler.post(MainActivity.toastRunnable);
////            MainActivity.toastHandler.post(MainActivity.loadingRunnableDismiss);
//        }


    }

    private void generateframebuffer(int textId)
    {
        fb = new int[1];
        depthRb = new int[1];

        GLES20.glGenFramebuffers(1, fb, 0);
        GLES20.glGenRenderbuffers(1, depthRb, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textId);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);

        int[] buf = new int[mImageWidth * mImageHeigth];
        IntBuffer texBuffer = ByteBuffer.allocateDirect(buf.length
                * 4).order(ByteOrder.nativeOrder()).asIntBuffer();

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, mImageWidth, mImageHeigth, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, texBuffer);

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRb[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, mImageWidth, mImageHeigth);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textId, 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRb[0]);

    }


    private int createprogram(String fssource) { return createprogram(generalVS, fssource);}
    private int createprogram(String vssource, String fssource)
    {
        int toRet;
        toRet = GLES20.glCreateProgram();
        GLES20.glAttachShader(toRet, compileshader(GLES20.GL_VERTEX_SHADER, vssource));
        GLES20.glAttachShader(toRet, compileshader(GLES20.GL_FRAGMENT_SHADER, fssource));
        GLES20.glLinkProgram(toRet);
        return toRet;
    }

    private int compileshader(int type, String shaderCode)
    {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        ERROR = GLES20.glGetShaderInfoLog(shader);
        if (ERROR.length() > 0) { //Log.e("CO2 Photo editor - ", "Exception not thrown! FilterRenderer line 688\n"+ERROR);
            Log.e("badango", ERROR);
            throw (new RuntimeException(ERROR));
        }
        return shader;
    }

    private void loadShaders()
    {

        //BASE SHADER
        String baseshader_FS =
                "precision mediump float;" +
                        "varying vec2 UV;" +
                        "void main() {" +
                        "  gl_FragColor = vec4(UV.x, UV.y, 0, 1);" +
                        "}";
        mShaderProgramBase = createprogram(baseshader_FS);

        //Contrast, brightness, saturation
        String cbs_FS =
                "precision mediump float;\n" +
                        "uniform sampler2D filteredPhoto;\n" +
                        "uniform float Contrast;\n" +
                        "uniform float Brightness;\n" +
                        "uniform float Saturation;\n" +
                        "varying vec2 UV;\n" +
                        "" +
                        "void main()\n" +
                        "{" +
                        "   vec4 c = texture2D(filteredPhoto, UV);\n" +
                        "   //Brightness\n" +
                        "   c = vec4(c.r * Brightness, c.g * Brightness, c.b * Brightness, 1);\n" +
                        "   //Contrast\n" +
                        "   c.r = ((c.r-0.5) * Contrast) + 0.5;\n" +
                        "   c.g = ((c.g-0.5) * Contrast) + 0.5;\n" +
                        "   c.b = ((c.b-0.5) * Contrast) + 0.5;\n" +
                        "   //Saturation\n" +
                        "   float grey = c.r * 0.299 + c.g * 0.587 + c.b * 0.114;\n" +
                        "   c.r = grey + ((c.r - grey) * Saturation);\n" +
                        "   c.g = grey + ((c.g - grey) * Saturation);\n" +
                        "   c.b = grey + ((c.b - grey) * Saturation);\n" +
                        "\n" +
                        "   gl_FragColor = c;\n" +
                        "}";
        mShaderProgramContrastSaturationBrightness = createprogram(generalreverseVS, cbs_FS);
        String finalPass_FS =
                "precision mediump float;" +
                        "uniform sampler2D filteredPhoto;" +
                        "varying vec2 UV;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(filteredPhoto, UV);" +
                        "}";
        mShaderProgramFinalPass = createprogram(finalPass_FS);
    }

    public void refreshSize() {
        int scrW = this.getWidth();
        int scrH = this.getHeight();
        float wRat = (float) mImageWidth / (float) scrW;
        float hRat = (float) mImageHeigth / (float) scrH;
        boolean majW = wRat > hRat;
        float aspect = (float) (majW ? (float) mImageWidth / (float) mImageHeigth : (float) mImageHeigth / (float) mImageWidth);
        if (majW) {
            cmp_W = scrW;
            cmp_X = 0;
            cmp_H = (int) ((float) scrW / aspect);
            cmp_Y = (int) (((float) scrH - (float) cmp_H) / 2f);
            //throw(new RuntimeException("IW: " + mImageWidth + "\nIH: " + mImageHeigth + "\nwRat = " + wRat + "\nhRat = " + hRat + "\nX: " + cmp_X + "\nY: " + cmp_Y + "\nW: " + cmp_W + "\nH: " + cmp_H + "\nscW: " + scrW + "\nscH: " + scrH));
        } else {
            cmp_H = scrH;
            cmp_Y = 0;
            cmp_W = (int) ((float) scrH / (float) aspect);
            cmp_X = (int) (((float) scrW - (float) cmp_W) / 2f);
        }
    }
    public void refreshSize(int width_, int height_) {
        int scrW = width_;
        int scrH = height_;
        float wRat = (float) mImageWidth / (float) scrW;
        float hRat = (float) mImageHeigth / (float) scrH;
        boolean majW = wRat > hRat;
        float aspect = (float) (majW ? (float) mImageWidth / (float) mImageHeigth : (float) mImageHeigth / (float) mImageWidth);
        if (majW) {
            cmp_W = scrW;
            cmp_X = 0;
            cmp_H = (int) ((float) scrW / aspect);
            cmp_Y = (int) (((float) scrH - (float) cmp_H) / 2f);
            //throw(new RuntimeException("IW: " + mImageWidth + "\nIH: " + mImageHeigth + "\nwRat = " + wRat + "\nhRat = " + hRat + "\nX: " + cmp_X + "\nY: " + cmp_Y + "\nW: " + cmp_W + "\nH: " + cmp_H + "\nscW: " + scrW + "\nscH: " + scrH));
        } else {
            cmp_H = scrH;
            cmp_Y = 0;
            cmp_W = (int) ((float) scrH / (float) aspect);
            cmp_X = (int) (((float) scrW - (float) cmp_W) / 2f);
        }
    }


    private void setShaderParamPhoto(int program, int texID)
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID);
        int loc = GLES20.glGetUniformLocation(program, "filteredPhoto");
        //if (loc == -1) throw(new RuntimeException("SHEEEET"));
        GLES20.glUniform1i(loc, 0);
    }

    private void drawquad(){
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDisableVertexAttribArray(hPos);
        GLES20.glDisableVertexAttribArray(hTex);}
    private void setVSParams(int program){setVSParamspos(program); setVSParamstc(program);}
    private void setVSParamspos(int program) {
        hPos = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(hPos);
        GLES20.glVertexAttribPointer(hPos, 3,
                GLES20.GL_FLOAT, false,
                12, VB);
    }
    private void setVSParamstc(int program) {
        hTex = GLES20.glGetAttribLocation(program, "texCoords");
        GLES20.glEnableVertexAttribArray(hTex);
        GLES20.glVertexAttribPointer(hTex, 2,
                GLES20.GL_FLOAT, false,
                8, TC);
    }




    public void LoadBitmap(Bitmap bmp)
    {
        if (!startup && default_b) default_b = false;
        startup = false;
        if ((bmp.getHeight()*bmp.getWidth())>=7900001) { //if pic bigger then 8Mpx resize to 8Mpx
            double h = bmp.getHeight();
            double b = bmp.getWidth();
            double y = b/h;
            double x = 7900000;
            h=Math.sqrt(x/y);
            b=x/(h);
            bmp = Bitmap.createScaledBitmap(bmp, (int)b, (int)h, true);
        }
        toLoad = bmp;
        this.BOOL_LoadTexture = true;
        this.thumbnailBitmap = Bitmap.createScaledBitmap(bmp, (int)120, (int)120, true);
    }
    public void LoadTexture(Bitmap bmp)
    {
        this.mImageHeigth = bmp.getHeight();
        this.mImageWidth = bmp.getWidth();
        this.hToFilterTexture = loadTexture(bmp);
        generateframebuffer(hToFilterTexture[2]);
        this.Render();
    }
    private int[] loadTexture(Bitmap bitmap)
    {
        final int[] textureHandle = new int[3];
        GLES20.glGenTextures(3, textureHandle, 0);
        if (textureHandle[0] == 0)throw(new RuntimeException("error generating t"));
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        this.currentBitmap = bitmap;
        bitmap.recycle();
        return textureHandle;
    }

    public void SaveImage(String location)
    {
        this.SaveImage = true;
        this.SavePath = location;
    }


}