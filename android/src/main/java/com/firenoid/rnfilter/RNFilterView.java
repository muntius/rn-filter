package com.firenoid.rnfilter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firenoid.rnfilter.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RNFilterView extends GLSurfaceView implements GLSurfaceView.Renderer {

    public static final int COMMAND_CAPTURE_CURRENT_VIEW = 3;


    private Activity mActivity;
    private Context mContext;


    private int hShaderProgramBase;
    private int hShaderProgramFinalPass;
    private int hShaderProgramContrastSaturationBrightness;
    public boolean PARAMS_EnableContrastSaturationBrightness;


    public boolean BOOL_LoadTexture = false;
    public RenderTarget2D target1, target2, saveTarget, blur1, blur2;

    public int ImageWidth = 0;
    public int ImageHeigth = 0;

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

    public int[] hToFilterTexture;


    private boolean mInitialized = false;
    public float mSaturationValue = 1f;
    public float mContrastValue = 1f;
    public float mBrightnessValue = 1f;
    private volatile boolean saveFrame;

    public Bitmap toLoad = null;
    public Bitmap currentBitmap = null;
    public Bitmap testBitmap = null;
    public boolean IsUsedBitmap()
    {
        return !default_b;
    }
    private boolean startup = true;
    private boolean default_b = true;
    private String source;


    public RNFilterView(Context context, Activity activity) {
        super(context);
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "source");
        mActivity = activity;
        mContext = context;

        //setBackgroundColor(Color.YELLOW);
//        setZOrderMediaOverlay(true);

//        this.filterView = new RNFilterView(context,this);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

//        mCurrentEffect = null;
    }



    public void setSource(String source) {
        this.source = source;
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "source");
        Log.d("React:", source);
        PARAMS_EnableContrastSaturationBrightness= true;
//        this.update();
    }
    public void setBrightness(double value) {
        this.source = source;
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "setBrightness");
        Log.d("React:",  String.valueOf(value));
        Log.d("React:", "setBrightness");

//        mCurrentEffect = "brightness";
        mBrightnessValue = (float) value;
        Log.d("React:", "mBrightnessValue");
        Log.d("React:", String.valueOf(mBrightnessValue));
        shallRenderImage = true;
        this.requestRender();

//        this.update();
    }
    public void setContrast(double value) {
        this.source = source;
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "setContrast");
        Log.d("React:",  String.valueOf(value));
//        mCurrentEffect = "contrast";
        shallRenderImage = true;
        mContrastValue = (float) value;
        this.requestRender();
    }
    public void setSaturation(double value) {
        this.source = source;
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "setSaturation");
        Log.d("React:", String.valueOf(value));
//        mCurrentEffect = "saturation";
        shallRenderImage = true;
        mSaturationValue = (float) value;
        this.requestRender();
    }




    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
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
//        testBitmap = generateTestBitmap();
        int id = R.drawable.duckling;
        // Load input bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),  id);
//        mImageWidth = bitmap.getWidth();
//        mImageHeight = bitmap.getHeight();
        LoadBitmap(bitmap);
    }


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
    private void loadShaders()
    {

        //BASE SHADER
        String baseshader_FS =
                "precision mediump float;" +
                        "varying vec2 UV;" +
                        "void main() {" +
                        "  gl_FragColor = vec4(UV.x, UV.y, 0, 1);" +
                        "}";
        hShaderProgramBase = createprogram(baseshader_FS);

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
        hShaderProgramContrastSaturationBrightness = createprogram(generalreverseVS, cbs_FS);
        String finalPass_FS =
                "precision mediump float;" +
                        "uniform sampler2D filteredPhoto;" +
                        "varying vec2 UV;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(filteredPhoto, UV);" +
                        "}";
        hShaderProgramFinalPass = createprogram(finalPass_FS);
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
    String ERROR = "";
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

    private int hPos, hTex;
    private int cmp_W, cmp_H, cmp_X, cmp_Y;
    private int tx;


    public void refreshSize() {
        int scrW = this.getWidth();
        int scrH = this.getHeight();
        float wRat = (float) ImageWidth / (float) scrW;
        float hRat = (float) ImageHeigth / (float) scrH;
        boolean majW = wRat > hRat;
        float aspect = (float) (majW ? (float) ImageWidth / (float) ImageHeigth : (float) ImageHeigth / (float) ImageWidth);
        if (majW) {
            cmp_W = scrW;
            cmp_X = 0;
            cmp_H = (int) ((float) scrW / aspect);
            cmp_Y = (int) (((float) scrH - (float) cmp_H) / 2f);
            //throw(new RuntimeException("IW: " + ImageWidth + "\nIH: " + ImageHeigth + "\nwRat = " + wRat + "\nhRat = " + hRat + "\nX: " + cmp_X + "\nY: " + cmp_Y + "\nW: " + cmp_W + "\nH: " + cmp_H + "\nscW: " + scrW + "\nscH: " + scrH));
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
        float wRat = (float) ImageWidth / (float) scrW;
        float hRat = (float) ImageHeigth / (float) scrH;
        boolean majW = wRat > hRat;
        float aspect = (float) (majW ? (float) ImageWidth / (float) ImageHeigth : (float) ImageHeigth / (float) ImageWidth);
        if (majW) {
            cmp_W = scrW;
            cmp_X = 0;
            cmp_H = (int) ((float) scrW / aspect);
            cmp_Y = (int) (((float) scrH - (float) cmp_H) / 2f);
            //throw(new RuntimeException("IW: " + ImageWidth + "\nIH: " + ImageHeigth + "\nwRat = " + wRat + "\nhRat = " + hRat + "\nX: " + cmp_X + "\nY: " + cmp_Y + "\nW: " + cmp_W + "\nH: " + cmp_H + "\nscW: " + scrW + "\nscH: " + scrH));
        } else {
            cmp_H = scrH;
            cmp_Y = 0;
            cmp_W = (int) ((float) scrH / (float) aspect);
            cmp_X = (int) (((float) scrW - (float) cmp_W) / 2f);
        }
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
        else {
            if (!SaveImage && !shallRenderImage) {
                RenderTarget2D.SetDefault(cmp_X, cmp_Y, cmp_W, cmp_H);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glUseProgram(hShaderProgramFinalPass);
                setVSParams(hShaderProgramFinalPass);
                setShaderParamPhoto(hShaderProgramFinalPass, GetCurTexture());
                drawquad();
                return;
            }
            else
                shallRenderImage = false;
        }
        didshit = false;
        firstshit = true;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (PARAMS_EnableContrastSaturationBrightness)
        {
            SetRenderTarget();
            GLES20.glUseProgram(hShaderProgramContrastSaturationBrightness);
            setVSParams(hShaderProgramContrastSaturationBrightness);
            setShaderParamPhoto(hShaderProgramContrastSaturationBrightness, GetCurTexture());
            int sat = GLES20.glGetUniformLocation(hShaderProgramContrastSaturationBrightness, "Saturation");
            int br = GLES20.glGetUniformLocation(hShaderProgramContrastSaturationBrightness, "Brightness");
            int ctr = GLES20.glGetUniformLocation(hShaderProgramContrastSaturationBrightness, "Contrast");
            GLES20.glUniform1f(sat, mSaturationValue);
            GLES20.glUniform1f(br, mBrightnessValue);
            GLES20.glUniform1f(ctr, mContrastValue);
            drawquad();
        }

        if (didshit)
            firstshit = false;
        first = !first;

        int tx = GetCurTexture();



        if (SaveImage) {
            SaveImage = false;


//            mActivity.toastHandler.post(mActivity.loadingRunnableShow);

            saveTarget.Set();
            GLES20.glUseProgram(hShaderProgramFinalPass);
            setVSParams(hShaderProgramFinalPass);
            setShaderParamPhoto(hShaderProgramFinalPass, tx);
            drawquad();

            saveTarget.pfsave();
            int wd = saveTarget.Width;
            int hg = saveTarget.Height;

            int screenshotSize = wd * hg;
            ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
            bb.order(ByteOrder.nativeOrder());
            GLES20.glReadPixels(0, 0, wd, hg, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
            int pixelsBuffer[] = new int[screenshotSize];
            bb.asIntBuffer().get(pixelsBuffer);
            bb = null;
            Bitmap bitmap = Bitmap.createBitmap(wd, hg, Bitmap.Config.RGB_565);
            bitmap.setPixels(pixelsBuffer, screenshotSize - wd, -wd, 0, 0, wd, hg);
            pixelsBuffer = null;

            short sBuffer[] = new short[screenshotSize];
            ShortBuffer sb = ShortBuffer.wrap(sBuffer);
            bitmap.copyPixelsToBuffer(sb);

            for (int i = 0; i < screenshotSize; ++i) {
                short v = sBuffer[i];
                sBuffer[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
            }
            sb.rewind();
            bitmap.copyPixelsFromBuffer(sb);


            File file = new File(SavePath);

            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);

            //call media scanner to show the new picture in the gallery

            MediaScannerConnection.scanFile(
                    mActivity,
                    new String[]{SavePath},
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.v("CO2 Photo Editor ",
                                    "file " + path + " was scanned seccessfully: " + uri);
                        }
                    });
            try {
                fOut.flush();
                fOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            MainActivity.toastHandler.post(MainActivity.toastRunnable);
//            MainActivity.toastHandler.post(MainActivity.loadingRunnableDismiss);
        }
        RenderTarget2D.SetDefault(cmp_X, cmp_Y, cmp_W, cmp_H);
        GLES20.glUseProgram(hShaderProgramFinalPass);
        setVSParams(hShaderProgramFinalPass);
        setShaderParamPhoto(hShaderProgramFinalPass, tx);
        drawquad();
        //didshit = false;
        //firstshit = true;
    }

    /*
        public void setPARAMS_FilmGrainSeed(float v)
        {
            Random r = new Random();
            v*= 10;
            float a = (float)(r.nextInt(10) - 1);
            v+=a;
            PARAMS_FilmGrainSeed = v;
        }
    */
    public int rtid;
    private void setShaderParamPhoto(int program, int texID)
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID);
        int loc = GLES20.glGetUniformLocation(program, "filteredPhoto");
        //if (loc == -1) throw(new RuntimeException("SHEEEET"));
        GLES20.glUniform1i(loc, 0);
    }
    boolean first = true;
    boolean didshit = false;
    boolean firstshit = true;
    private void SetRenderTarget()
    {
        if (didshit) firstshit = false;
        didshit = true;
        if (first)
        {
            target1.Set();
            first = false;
        }
        else
        {
            target2.Set();
            first = true;
        }
    }
    private int GetCurTexture()
    {
        if (firstshit) return hToFilterTexture[0];
        if (first) {rtid = 1;return target1.GetTex();}
        else { rtid = 2;return target2.GetTex();}
    }

    private void drawquad(){        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
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

    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);
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
            bmp= Bitmap.createScaledBitmap(bmp, (int)b, (int)h, true);
        }
        toLoad = bmp;
        this.BOOL_LoadTexture = true;
    }
    public void LoadTexture(Bitmap bmp)
    {
        this.ImageHeigth = bmp.getHeight();
        this.ImageWidth = bmp.getWidth();
        this.hToFilterTexture = loadTexture(bmp);
        if (this.target1 != null)
            this.target1.Release();
        if (this.target2 != null)
            this.target2.Release();
        if (this.saveTarget != null)
            this.saveTarget.Release();
        if (this.blur1 != null) this.blur1.Release();
        if (this.blur2 != null) this.blur2.Release();
        this.target1 = new RenderTarget2D(bmp.getWidth(), bmp.getHeight());
        this.target2 = new RenderTarget2D(bmp.getWidth(), bmp.getHeight());
        this.saveTarget = new RenderTarget2D(bmp.getWidth(), bmp.getHeight());
        this.blur1 = new RenderTarget2D(bmp.getWidth() /2, bmp.getHeight() / 2);
        this.blur2 = new RenderTarget2D(bmp.getWidth() / 2, bmp.getHeight() / 2);
        this.Render();
    }

    private int[] loadTexture(Bitmap bitmap)
    {
        final int[] textureHandle = new int[1];


        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] == 0)throw(new RuntimeException("SHEEEEET"));

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        this.currentBitmap = bitmap;
        //bitmap.recycle();


        return textureHandle;
    }

    public void SaveImage(String location)
    {
        this.SaveImage = true;
        this.SavePath = location;
    }


    public Bitmap generateTestBitmap()
    {
        int width = getWidth();
        int height = getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(10, 10, 0xFFFFFFFF);
        Canvas c = new Canvas(bitmap);
        Paint p = new Paint();
        p.setColor(0x0000FFFF);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++)
            {
                bitmap.setPixel(x, y, 0xFF000000);
            }
        }

        //Bitmap bemp = BitmapFactory.decodeResource(getResources(), R.drawable.abc_list_selector_disabled_holo_light);
        return bitmap;
    }

}