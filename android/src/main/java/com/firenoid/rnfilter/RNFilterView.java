package com.firenoid.rnfilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.provider.MediaStore;
import android.util.Log;

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
            "#version 300 es\n" +
            "layout(location = 0) in  vec4 vPosition;\n" +
            "layout(location = 1) in  vec2 texCoords;\n" +
            "out vec2 UV;\n" +
            "void main() {\n" +
            "  gl_Position = vPosition;\n" +
            "  UV = texCoords;\n" +
            "}";

    private String generalreverseVS =
            "#version 300 es\n" +
            "layout(location = 0) in  vec4 vPosition;\n" +
            "layout(location = 1) in  vec2 texCoords;\n" +
            "out vec2 UV;\n" +
            "void main() {\n" +
            "  gl_Position = vPosition;\n" +
            "  UV = vec2(texCoords.x, 1.0 - texCoords.y);\n" +
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
        setEGLContextClientVersion(3);
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
            GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
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
        GLES30.glViewport(0, 0, width, height);
    }

    public void onDrawFrame(GL10 unused) {
        if (BOOL_LoadTexture) {
            if (this.toLoad != null) {
                this.LoadTexture(this.toLoad);
                refreshSize();
            }
            this.toLoad = null;
            System.gc();
            BOOL_LoadTexture = false;
        } else {
            if (!SaveImage && !shallRenderImage) {
                this.SetDefault(cmp_X, cmp_Y, cmp_W, cmp_H);
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
                GLES30.glUseProgram(mShaderProgramFinalPass);
                setVSParams();
                setShaderParamPhoto(mShaderProgramFinalPass, this.mCurrentTexture);
                drawquad();
                return;
            } else {
                shallRenderImage = false;
            }
        }

        mIndex = 0;
        mCurrentTexture = hToFilterTexture[mIndex];
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        if (!previewOriginal) {
            if (SFilter || BFilter || CFilter) {
                mIndex++;
                this.Set(hToFilterTexture[mIndex]);
                GLES30.glUseProgram(mShaderProgramContrastSaturationBrightness);
                setVSParams();
                setShaderParamPhoto(mShaderProgramContrastSaturationBrightness, mCurrentTexture);
                int sat = GLES30.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Saturation");
                int br = GLES30.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Brightness");
                int ctr = GLES30.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Contrast");
                GLES30.glUniform1f(sat, mSaturationValue);
                GLES30.glUniform1f(br, mBrightnessValue);
                GLES30.glUniform1f(ctr, mContrastValue);
                drawquad();
                mCurrentTexture = hToFilterTexture[mIndex];
            }

            if (BlurFilter) {
                mIndex++;
                this.Set(hToFilterTexture[mIndex]);
                GLES30.glUseProgram(hShaderProgramGaussianBlur);
                setVSParams();
                setShaderParamPhoto(hShaderProgramGaussianBlur, mCurrentTexture);
                SetBlurEffectParameters(1.0f / (float) mImageWidth, 0);
                drawquad();
                mCurrentTexture = hToFilterTexture[mIndex];
            }

            if (VignetteFilter) {
                mIndex++;
                this.Set(hToFilterTexture[mIndex]);
                GLES30.glUseProgram(hShaderProgramToneMapping);
                setVSParams();
                setShaderParamPhoto(hShaderProgramToneMapping, mCurrentTexture);
                int vign = GLES30.glGetUniformLocation(hShaderProgramToneMapping, "vign");
                GLES30.glUniform1f(vign, mVignetteValue);
                drawquad();
                mCurrentTexture = hToFilterTexture[mIndex];
            }
        }

        this.SetDefault(cmp_X, cmp_Y, cmp_W, cmp_H);
        GLES30.glUseProgram(mShaderProgramFinalPass);
        setVSParams();
        setShaderParamPhoto(mShaderProgramFinalPass, mCurrentTexture);
        drawquad();

        shallRenderImage = false;

        if (SaveImage) {
            SaveImage = false;
            saveBitmap(takeScreenshot(unused));
        }
    }

    void SetBlurEffectParameters(float dx, float dy) {
        int sox = GLES30.glGetUniformLocation(hShaderProgramGaussianBlur, "SampleOffsetsX");
        int soy = GLES30.glGetUniformLocation(hShaderProgramGaussianBlur, "SampleOffsetsY");
        int wei = GLES30.glGetUniformLocation(hShaderProgramGaussianBlur, "SampleWeights");

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

        GLES30.glUniform1fv(sox, sampleCount, sampleOffsetsX, 0);
        GLES30.glUniform1fv(soy, sampleCount, sampleOffsetsY, 0);
        GLES30.glUniform1fv(wei, sampleCount, sampleWeights, 0);
    }

    float ComputeGaussian(float n, float theta) {
        return (float) ((1.0 / Math.sqrt(2 * Math.PI * theta)) *
                Math.exp(-(n * n) / (2 * theta * theta)));
    }

    public boolean Set(int texId) {
        GLES30.glViewport(0, 0, mImageWidth, mImageHeigth);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fb[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, texId, 0);
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, depthRb[0]);
        int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete: " + status);
        }
        GLES30.glClearColor(.0f, .0f, .0f, 1.0f);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
        return true;
    }

    private void generateframebuffer(int textId) {
        fb = new int[1];
        depthRb = new int[1];

        GLES30.glGenFramebuffers(1, fb, 0);
        GLES30.glGenRenderbuffers(1, depthRb, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textId);

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR);

        int[] buf = new int[mImageWidth * mImageHeigth];
        IntBuffer texBuffer = ByteBuffer.allocateDirect(buf.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();

        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, mImageWidth, mImageHeigth, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, texBuffer);

        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, depthRb[0]);
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, mImageWidth, mImageHeigth);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fb[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textId, 0);
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, depthRb[0]);
    }

    public static void SetDefault(int x, int y, int w, int h) {
        GLES30.glViewport(x, y, w, h);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glClearColor(.0f, .0f, .0f, 1.0f);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
    }

    private int createprogram(String fssource) {
        return createprogram(generalVS, fssource);
    }

    private int createprogram(String vssource, String fssource) {
        int program = GLES30.glCreateProgram();
        int vs = compileshader(GLES30.GL_VERTEX_SHADER, vssource);
        int fs = compileshader(GLES30.GL_FRAGMENT_SHADER, fssource);
        GLES30.glAttachShader(program, vs);
        GLES30.glAttachShader(program, fs);
        GLES30.glLinkProgram(program);

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e("RNFilterView", "Program link FAILED:\n" + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            throw new RuntimeException("Program link failed.");
        }

        return program;
    }

    private int compileshader(int type, String shaderCode) {
        final int shader = GLES30.glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("glCreateShader failed for type " + type);
        }

        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        final int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);

        final String log = GLES30.glGetShaderInfoLog(shader);
        if (log != null && log.trim().length() > 0) {
            Log.w("RNFilterView", "Shader compile log (" + type + "):\n" + log);
        }

        if (compileStatus[0] == 0) {
            Log.e("RNFilterView", "Shader compile FAILED:\n" + log + "\nSource (start):\n" + shaderCode.substring(0, Math.min(500, shaderCode.length())));
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed.");
        }

        return shader;
    }

    private void loadShaders() {
        String baseshader_FS =
                "#version 300 es\n" +
                "precision mediump float;\n" +
                "in  vec2 UV;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "  FragColor = vec4(UV.x, UV.y, 0.0, 1.0);\n" +
                "}";
        mShaderProgramBase = createprogram(baseshader_FS);

        //Contrast, brightness, saturation
        String cbs_FS =
                "#version 300 es\n" +
                "precision mediump float;\n" +
                "uniform sampler2D filteredPhoto;\n" +
                "uniform float Contrast;\n" +
                "uniform float Brightness;\n" +
                "uniform float Saturation;\n" +
                "in  vec2 UV;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "  vec4 c = texture(filteredPhoto, UV);\n" +
                "  /* brightness */\n" +
                "  c.rgb *= Brightness;\n" +
                "  /* contrast   */\n" +
                "  c.rgb = ((c.rgb - 0.5) * Contrast) + 0.5;\n" +
                "  /* saturation */\n" +
                "  float grey = dot(c.rgb, vec3(0.299, 0.587, 0.114));\n" +
                "  c.rgb = grey + (c.rgb - grey) * Saturation;\n" +
                "  FragColor = c;\n" +
                "}";
        mShaderProgramContrastSaturationBrightness = createprogram(generalreverseVS, cbs_FS);

        String gaussianBlur_FS =
                "#version 300 es\n" +
                "precision mediump float;\n" +
                "uniform float SampleOffsetsX[15];\n" +
                "uniform float SampleOffsetsY[15];\n" +
                "uniform float SampleWeights[15];\n" +
                "uniform sampler2D filteredPhoto;\n" +
                "in  vec2 UV;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "  vec4 c = vec4(0.0);\n" +
                "  for (int i = 0; i < 15; i++) {\n" +
                "    c += texture(filteredPhoto, UV + vec2(SampleOffsetsX[i], SampleOffsetsY[i])) * SampleWeights[i];\n" +
                "  }\n" +
                "  FragColor = c;\n" +
                "}";
        hShaderProgramGaussianBlur = createprogram(generalreverseVS, gaussianBlur_FS);

        //ToneMapping+
        String tonemapping_FS =
                "#version 300 es\n" +
                "precision mediump float;\n" +
                "uniform sampler2D filteredPhoto;\n" +
                "uniform float vign;\n" +
                "in  vec2 UV;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "  vec2  vtc      = UV - 0.5;\n" +
                "  float vignette = pow(1.0 - dot(vtc, vtc) * vign, 2.0);\n" +
                "  vec4  color    = texture(filteredPhoto, UV);\n" +
                "  FragColor = vec4(color.rgb * vignette, 1.0);\n" +
                "}";

        hShaderProgramToneMapping = createprogram(generalreverseVS, tonemapping_FS);

        String finalPass_FS =
                "#version 300 es\n" +
                "precision mediump float;\n" +
                "uniform sampler2D filteredPhoto;\n" +
                "in  vec2 UV;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "  FragColor = texture(filteredPhoto, UV);\n" +
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
        } else {
            cmp_H = scrH;
            cmp_Y = 0;
            cmp_W = (int) ((float) scrH / (float) aspect);
            cmp_X = (int) (((float) scrW - (float) cmp_W) / 2f);
        }
    }

    private void setShaderParamPhoto(int program, int texID) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texID);
        int loc = GLES30.glGetUniformLocation(program, "filteredPhoto");
        GLES30.glUniform1i(loc, 0);
    }

    private void drawquad() {
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6);
        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
    }

    private void setVSParams() {
        setVSParamspos();
        setVSParamstc();
    }

    private void setVSParamspos() {
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 12, VB);
    }

    private void setVSParamstc() {
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 8, TC);
    }

    public void LoadBitmap(Bitmap bmp) {
        if (!startup && default_b) {
            default_b = false;
        }
        startup = false;
        if ((bmp.getHeight() * bmp.getWidth()) >= 7900001) { // If the picture is bigger than 8Mpx, resize to 8Mpx
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
        if (hToFilterTexture != null) {
            GLES30.glDeleteTextures(hToFilterTexture.length, hToFilterTexture, 0);
        }

        this.mImageHeigth = bmp.getHeight();
        this.mImageWidth = bmp.getWidth();
        this.hToFilterTexture = loadTexture(bmp);
        generateframebuffer(hToFilterTexture[1]);
        generateframebuffer(hToFilterTexture[2]);
        generateframebuffer(hToFilterTexture[3]);
    }

    private int[] loadTexture(Bitmap bitmap) {
        final int[] textureHandle = new int[4];
        GLES30.glGenTextures(4, textureHandle, 0);
        if (textureHandle[0] == 0) {
            throw (new RuntimeException("error generating t"));
        }

        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
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
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
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
