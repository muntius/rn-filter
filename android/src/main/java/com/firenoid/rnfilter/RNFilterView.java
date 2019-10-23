package com.firenoid.rnfilter;

import android.annotation.SuppressLint;
import android.app.Activity;

import com.facebook.react.uimanager.ThemedReactContext;

import android.graphics.Bitmap;

import android.graphics.Matrix;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

public class RNFilterView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private Activity mActivity;
    private ThemedReactContext mContext;


    private int mShaderProgramBase;
    private int mShaderProgramFinalPass;
    private int mShaderProgramContrastSaturationBrightness;
    private int hShaderProgramToneMapping;
    private int hShaderProgramGaussianBlur;

    public boolean isInitialized;
    public boolean EnableContrastSaturationBrightness;
    public boolean isThumbnail;
    public boolean previewOriginal;


    public boolean BOOL_LoadTexture = false;
    public int mImageWidth = 0;
    public int mImageHeigth = 0;

    public boolean SaveImage = false;
    private boolean shallRenderImage = false;
    public String SavePath = "";

    public void Render() {
        shallRenderImage = true;
    }

    private FloatBuffer VB;
    private ShortBuffer IB;
    private FloatBuffer TC;

    public int[] hToFilterTexture = new int[4];
    public int mCurrentTexture = 0;
    public int mIndex = 0;
    public boolean SFilter = false;
    public boolean BFilter = false;
    public boolean CFilter = false;
    public boolean BlurFilter = false;
    public boolean VignetteFilter = false;
    public float mSaturationValue = 1f;
    public float mContrastValue = 1f;
    public float mBrightnessValue = 1f;
    public float mBlurValue = 1f;
    public float mVignetteValue = 1f;
    public float mSaturationDefaultValue = 1f;
    public float mContrastDefaultValue = 1f;
    public float mBrightnessDefaultValue = 1f;
    public float mBlurDefaultValue = 0f;
    public float mVignetteDefaultValue = 0f;
    public int widthImage;
    public int heightImage;


    public Bitmap toLoad = null;
    public Bitmap originalBitmap = null;
    public Bitmap currentBitmap = null;
    public Bitmap thumbnailBitmap = null;

    public boolean IsUsedBitmap() {
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
                    -1f, 1f, 0.0f,
                    -1f, -1f, 0.0f,
                    1f, -1f, 0.0f,
                    1f, 1f, 0.0f,
                    -1f, 1f, 0.0f,
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

    public RNFilterView(ThemedReactContext context, Activity activity) {
        super(context);
        mActivity = activity;
        mContext = context;
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


    public void setSource(String source) {
        if (this.source == null) {
            this.source = source;
        }
    }

    public void setBrightness(double value) {
        shallRenderImage = true;
        BFilter = value != 1.0;
        mBrightnessValue = (float) value;
        this.requestRender();

    }

    public void setContrast(double value) {
        shallRenderImage = true;
        CFilter = value != 1.0;
        mContrastValue = (float) value;
        this.requestRender();
    }


    public void setSaturation(double value) {
        shallRenderImage = true;
        SFilter = value != 1.0;
        mSaturationValue = (float) value;
        this.requestRender();
    }

    public void setBlur(double value) {
        shallRenderImage = true;
        BlurFilter = value != 0.0;
        mBlurValue = (float) value;
        this.requestRender();
    }

    public void setVignette(double value) {
        shallRenderImage = true;
        VignetteFilter = value != 0.0;
        mVignetteValue = (float) value;
        this.requestRender();
    }

    public void setOriginal(boolean value) {
        shallRenderImage = true;
        previewOriginal = value;
        this.requestRender();
    }

    public void setReset() {
        shallRenderImage = true;
        mSaturationValue = mSaturationDefaultValue;
        mContrastValue = mContrastDefaultValue;
        mBrightnessValue = mBrightnessDefaultValue;
        mBlurValue = mBlurDefaultValue;
        mVignetteValue = mVignetteDefaultValue;
        previewOriginal = SFilter = BFilter = CFilter = BlurFilter = VignetteFilter = false;
        this.requestRender();
    }

    public void setThumbnail(boolean value) {
        isThumbnail = value;
    }

    public void generaThumbnail() {

//        String url = generateBitmap(this.thumbnailBitmap);
        new fileFromBitmap(this.thumbnailBitmap, getId(), mContext).execute();
    }


    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        if (!isInitialized) {
            isInitialized = true;
            try {
                this.originalBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), Uri.parse(source));
            } catch (IOException e) {
                e.printStackTrace();
            }

            EnableContrastSaturationBrightness = true;
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            ByteBuffer bVB = ByteBuffer.allocateDirect(vertices.length * 4);
            bVB.order(ByteOrder.nativeOrder());
            ByteBuffer bIB = ByteBuffer.allocateDirect(indices.length * 2);
            bIB.order(ByteOrder.nativeOrder());
            ByteBuffer bTC = ByteBuffer.allocateDirect(texCoords.length * 4);
            bTC.order(ByteOrder.nativeOrder());

            VB = bVB.asFloatBuffer();
            IB = bIB.asShortBuffer();
            TC = bTC.asFloatBuffer();
            VB.put(vertices);
            VB.position(0);
            IB.put(indices);
            IB.position(0);
            TC.put(texCoords);
            TC.position(0);
            loadShaders();
            LoadBitmap(this.originalBitmap);
        }

    }


    public void onSurfaceChanged(GL10 unused, int width, int height) {
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
        } else {
            if (!SaveImage && !shallRenderImage) {
                this.SetDefault(cmp_X, cmp_Y, cmp_W, cmp_H);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glUseProgram(mShaderProgramFinalPass);
                setVSParams(mShaderProgramFinalPass);
                setShaderParamPhoto(mShaderProgramFinalPass, this.mCurrentTexture);
                drawquad();
                return;
            } else
                shallRenderImage = false;
        }


        mIndex = 0;
        mCurrentTexture = hToFilterTexture[mIndex];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        /////////////////////////////  /////////////////////////////
        /////////////////////////////  /////////////////////////////
        /////////////////////////////  /////////////////////////////
        if (!previewOriginal) {
            if (SFilter || BFilter || CFilter) {
                mIndex++;
                this.Set(hToFilterTexture[mIndex]);

//
                GLES20.glUseProgram(mShaderProgramContrastSaturationBrightness);
                setVSParams(mShaderProgramContrastSaturationBrightness);
                setShaderParamPhoto(mShaderProgramContrastSaturationBrightness, mCurrentTexture);

//
                int sat = GLES20.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Saturation");
                int br = GLES20.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Brightness");
                int ctr = GLES20.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Contrast");
                GLES20.glUniform1f(sat, mSaturationValue);
                GLES20.glUniform1f(br, mBrightnessValue);
                GLES20.glUniform1f(ctr, mContrastValue);
                drawquad();

                mCurrentTexture = hToFilterTexture[mIndex];
            }

            if (BlurFilter) {
                mIndex++;
                this.Set(hToFilterTexture[mIndex]);
                GLES20.glUseProgram(hShaderProgramGaussianBlur);
                setVSParams(hShaderProgramGaussianBlur);
                setShaderParamPhoto(hShaderProgramGaussianBlur, mCurrentTexture);
                SetBlurEffectParameters(1.0f / (float) mImageWidth, 0);
                drawquad();

                mCurrentTexture = hToFilterTexture[mIndex];

            }

            if (VignetteFilter) {
                mIndex++;
                this.Set(hToFilterTexture[mIndex]);


                GLES20.glUseProgram(hShaderProgramToneMapping);
                setVSParams(hShaderProgramToneMapping);
                setShaderParamPhoto(hShaderProgramToneMapping, mCurrentTexture);
                int vign = GLES20.glGetUniformLocation(hShaderProgramToneMapping, "vign");

                GLES20.glUniform1f(vign, mVignetteValue);

                drawquad();

                mCurrentTexture = hToFilterTexture[mIndex];

            }
        }

        /////////////////////////////  /////////////////////////////
        /////////////////////////////  /////////////////////////////
        /////////////////////////////  /////////////////////////////
//
        this.SetDefault(cmp_X, cmp_Y, cmp_W, cmp_H);
        GLES20.glUseProgram(mShaderProgramFinalPass);
        setVSParams(mShaderProgramFinalPass);
        setShaderParamPhoto(mShaderProgramFinalPass, mCurrentTexture);
        drawquad();

        shallRenderImage = false;


        if (SaveImage) {
            SaveImage = false;
            saveBitmap(takeScreenshot(unused));

        }


    }


    void SetBlurEffectParameters(float dx, float dy) {
        //GLES20.glUseProgram(hShaderProgramGaussianBlur);
        int sox = GLES20.glGetUniformLocation(hShaderProgramGaussianBlur, "SampleOffsetsX");
        int soy = GLES20.glGetUniformLocation(hShaderProgramGaussianBlur, "SampleOffsetsY");
        int wei = GLES20.glGetUniformLocation(hShaderProgramGaussianBlur, "SampleWeights");

        int sampleCount = 15;
        float[] sampleOffsetsX = new float[sampleCount];
        float[] sampleOffsetsY = new float[sampleCount];
        float[] sampleWeights = new float[sampleCount];

        sampleWeights[0] = ComputeGaussian(0, mBlurValue);
        sampleOffsetsX[0] = 0;
        sampleOffsetsY[0] = 0;

        float totalWeights = sampleWeights[0];

        for (int i = 0; i < sampleCount / 2; i++) {
            float weight = ComputeGaussian(i + 1, mBlurValue);

            sampleWeights[i * 2 + 1] = weight;
            sampleWeights[i * 2 + 2] = weight;

            totalWeights += weight * 2;

            float sampleOffset = i * 2 + 1.5f;

            sampleOffsetsX[i * 2 + 1] = dx * sampleOffset;
            sampleOffsetsY[i * 2 + 1] = dy * sampleOffset;
            sampleOffsetsX[i * 2 + 2] = -dx * sampleOffset;
            sampleOffsetsY[i * 2 + 2] = -dy * sampleOffset;
        }

        for (int i = 0; i < sampleWeights.length; i++) {
            sampleWeights[i] /= totalWeights;
        }

        GLES20.glUniform1fv(sox, sampleCount, sampleOffsetsX, 0);
        GLES20.glUniform1fv(soy, sampleCount, sampleOffsetsY, 0);
        GLES20.glUniform1fv(wei, sampleCount, sampleWeights, 0);
    }

    float ComputeGaussian(float n, float theta) {
        return (float) ((1.0 / Math.sqrt(2 * Math.PI * theta)) *
                Math.exp(-(n * n) / (2 * theta * theta)));
    }


    public boolean Set(int texId) {
        GLES20.glViewport(0, 0, mImageWidth, mImageHeigth);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texId, 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRb[0]);
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE)
            throw (new RuntimeException("SHEE"));
        GLES20.glClearColor(.0f, .0f, .0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        return true;
    }

    private void generateframebuffer(int textId) {
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


    public static void SetDefault(int x, int y, int w, int h) {
        GLES20.glViewport(x, y, w, h);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glClearColor(.0f, .0f, .0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
    }

    private int createprogram(String fssource) {
        return createprogram(generalVS, fssource);
    }

    private int createprogram(String vssource, String fssource) {
        int toRet;
        toRet = GLES20.glCreateProgram();
        GLES20.glAttachShader(toRet, compileshader(GLES20.GL_VERTEX_SHADER, vssource));
        GLES20.glAttachShader(toRet, compileshader(GLES20.GL_FRAGMENT_SHADER, fssource));
        GLES20.glLinkProgram(toRet);
        return toRet;
    }

    private int compileshader(int type, String shaderCode) {
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

    private void loadShaders() {

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

        String gaussianBlur_FS =
                "precision mediump float;" +
                        "uniform float SampleOffsetsX[15];" +
                        "uniform float SampleOffsetsY[15];" +
                        "uniform float SampleWeights[15];" +
                        "uniform sampler2D filteredPhoto;" +
                        "varying vec2 UV;" +
                        "" +
                        "void main()" +
                        "{" +
                        "   int i = 0;" +
                        "   vec4 c = vec4( 0.000000);" +
                        "   for ( ; (i < 15); ( i++ )) {" +
                        "        c += (texture2D( filteredPhoto, (UV + vec2(SampleOffsetsX[ i ], SampleOffsetsY[ i ]))) * SampleWeights[ i ]);" +
                        "    }" +
                        "    gl_FragColor = c;" +
                        "}";
        hShaderProgramGaussianBlur = createprogram(generalreverseVS, gaussianBlur_FS);


        //ToneMapping+
        String tonemapping_FS =
                "precision mediump float;" +
                        "uniform sampler2D filteredPhoto;" +
                        "uniform float vign;" +
                        "varying vec2 UV;" +
                        "varying vec4 color;" +
                        "" +
                        "void main() {" +
                        "float vignette;\n" +
                        "vec2 vtc = vec2( (UV - 0.500000));" +
                        "vec4 color = texture2D( filteredPhoto, UV);\n" +
                        "vignette = pow( (1.00000 - (dot( vtc, vtc) * vign)), 2.00000);\n" +
                        "gl_FragColor = vec4(color.r * vignette, color.g * vignette, color.b * vignette, 1);" +
                        "}";

        hShaderProgramToneMapping = createprogram(generalreverseVS, tonemapping_FS);


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


    private void setShaderParamPhoto(int program, int texID) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID);
        int loc = GLES20.glGetUniformLocation(program, "filteredPhoto");
        //if (loc == -1) throw(new RuntimeException("SHEEEET"));
        GLES20.glUniform1i(loc, 0);
    }

    private void drawquad() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDisableVertexAttribArray(hPos);
        GLES20.glDisableVertexAttribArray(hTex);
    }

    private void setVSParams(int program) {
        setVSParamspos(program);
        setVSParamstc(program);
    }

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


    public void LoadBitmap(Bitmap bmp) {
        if (!startup && default_b) default_b = false;
        startup = false;
        if ((bmp.getHeight() * bmp.getWidth()) >= 7900001) { //if pic bigger then 8Mpx resize to 8Mpx
            double h = bmp.getHeight();
            double b = bmp.getWidth();
            double y = b / h;
            double x = 7900000;
            h = Math.sqrt(x / y);
            b = x / (h);
            bmp = Bitmap.createScaledBitmap(bmp, (int) b, (int) h, true);
        }
        toLoad = bmp;
        this.BOOL_LoadTexture = true;
        this.thumbnailBitmap = Bitmap.createScaledBitmap(bmp, (int) 120, (int) 120, true);
    }

    public void LoadTexture(Bitmap bmp) {
        this.mImageHeigth = bmp.getHeight();
        this.mImageWidth = bmp.getWidth();
        this.hToFilterTexture = loadTexture(bmp);
        generateframebuffer(hToFilterTexture[1]);
        generateframebuffer(hToFilterTexture[2]);
        generateframebuffer(hToFilterTexture[3]);

    }

    private int[] loadTexture(Bitmap bitmap) {
        final int[] textureHandle = new int[4];
        GLES20.glGenTextures(4, textureHandle, 0);
        if (textureHandle[0] == 0) throw (new RuntimeException("error generating t"));
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        this.currentBitmap = bitmap;
        bitmap.recycle();
        return textureHandle;
    }

    public void SaveImage(String location) {
        this.SaveImage = true;
        this.SavePath = location;
    }


    public Bitmap takeScreenshot(GL10 mGL) {
        final int mWidth = this.getWidth();
        final int mHeight = this.getHeight();
        IntBuffer ib = IntBuffer.allocate(mWidth * mHeight);
        IntBuffer ibt = IntBuffer.allocate(mWidth * mHeight);
        mGL.glReadPixels(0, 0, mWidth, mHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

        // Convert upside down mirror-reversed image to right-side up normal image.
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                ibt.put((mHeight - i - 1) * mWidth + j, ib.get(i * mWidth + j));
            }
        }
        Bitmap mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(ibt);
        mBitmap = getResizedBitmap(mBitmap, mWidth, mHeight);

        return mBitmap;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int width, int height) {
        float scaleWidth = ((float) widthImage) / width;
        float scaleHeight = ((float) heightImage) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }


    private void saveBitmap(Bitmap bitmap) {
        try {
            String now = String.valueOf(System.currentTimeMillis());
            String mPath = this.getContext().getCacheDir() + "/" + now + ".jpg";
            File imageFile = new File(mPath);
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
            WritableMap arg = Arguments.createMap();
            arg.putString("url", "file://" + imageFile.getCanonicalPath());
            mContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "dataReturned", arg);
        } catch (Exception e) {
            Log.e("TAG", e.toString(), e);
        }
    }

}
