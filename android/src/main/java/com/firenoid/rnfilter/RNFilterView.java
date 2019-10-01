package com.firenoid.rnfilter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
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
    public boolean EnableContrastSaturationBrightness;


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


    private boolean preFilter = false;
    public float mSaturationValue = 1f;
    public float mContrastValue = 1f;
    public float mBrightnessValue = 1f;
    private volatile boolean saveFrame;

    public Bitmap toLoad = null;
    public Bitmap originalBitmap = null;
    public Bitmap currentBitmap = null;
    public Bitmap thumbnailBitmap = null;
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


    public RNFilterView(Context context, Activity activity) {
        super(context);
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "source");
        mActivity = activity;
        mContext = context;
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
        EnableContrastSaturationBrightness= true;
//        this.update();
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
        setFilterManual((int)value);
        this.requestRender();
    }







    public void setFilterManual(int value) {
        Bitmap result = null;
        Bitmap newBitmap = this.originalBitmap.copy(this.originalBitmap.getConfig(), true);

        switch (value){
            case 0:
                result = Filters.vignette(newBitmap);
            break;
            case 1:
                result = Filters.saturation(newBitmap, 160);
            break;
            case 2:
                result = Filters.sepia(newBitmap);
                break;
            case 3:
                result = Filters.hue(newBitmap, 50);
                break;
            case 4:
                result = Filters.grayscale(newBitmap);
                break;
            case 5:
                result = Filters.boost(newBitmap, 1, 30);
                break;
            case 6:
                result = Filters.boost(newBitmap, 2, 30);
                break;
            case 7:
                result = Filters.boost(newBitmap,3, 30);
                break;
            case 8:
                result = Filters.brightness(newBitmap, 70);
                break;
            case 9:
                result = Filters.gaussian(newBitmap);
                break;
        }
        if(result != null){
            LoadBitmap(result);
//            newBitmap.recycle();
        }


    }


    public void generateFilters() {
        final WritableMap map = generateThumbs();
        final ReactContext reactContext = (ReactContext) mContext;
        reactContext.runOnJSQueueThread(new Runnable() {
            @Override
            public void run() {
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "thumbsReturned", map);
            }
        });


    }

    public WritableMap generateThumbs(){

        WritableNativeMap resultMap = new WritableNativeMap();
        WritableNativeArray AlbumListArray = new WritableNativeArray();
        WritableMap map;
        String url;
        Bitmap result;


        map = Arguments.createMap();
        result = Filters.vignette(this.thumbnailBitmap);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "vignette");
        map.putInt("id", 0);
        AlbumListArray.pushMap(map);


        map = Arguments.createMap();
        result = Filters.saturation(this.thumbnailBitmap, 160);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "saturation");
        map.putInt("id", 1);
        AlbumListArray.pushMap( map);


        map = Arguments.createMap();
        result = Filters.gaussian(this.thumbnailBitmap);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "gaussian");
        map.putInt("id", 9);
        AlbumListArray.pushMap(map);


        map = Arguments.createMap();
        result = Filters.sepia(this.thumbnailBitmap);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "sepia");
        map.putInt("id", 2);
        AlbumListArray.pushMap( map);

        map = Arguments.createMap();
        result = Filters.hue(this.thumbnailBitmap, 50);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "hue");
        map.putInt("id", 3);
        AlbumListArray.pushMap(map);

        map = Arguments.createMap();
        result = Filters.grayscale(this.thumbnailBitmap);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "grayscale");
        map.putInt("id", 4);
        AlbumListArray.pushMap( map);

        map = Arguments.createMap();
        result = Filters.boost(this.thumbnailBitmap, 1, 30);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "boost");
        map.putInt("id", 5);
        AlbumListArray.pushMap( map);

        map = Arguments.createMap();
        result = Filters.boost(this.thumbnailBitmap, 2, 30);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "boost");
        map.putInt("id", 6);
        AlbumListArray.pushMap( map);

        map = Arguments.createMap();
        result = Filters.boost(this.thumbnailBitmap,3, 30);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "boost");
        map.putInt("id", 7);
        AlbumListArray.pushMap( map);

        map = Arguments.createMap();
        result = Filters.brightness(this.thumbnailBitmap, 70);
        url = generateBitmap(result);
        map.putString("uri", url);
        map.putString("name", "brightness");
        map.putInt("id", 8);
        AlbumListArray.pushMap( map);

        Log.d("ARRAY", String.valueOf(AlbumListArray));
        resultMap.putArray("thumbs", AlbumListArray);
        return resultMap;
    }



    public String  generateBitmap(Bitmap source ){
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        source.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File file = new File(mContext.getCacheDir(), System.currentTimeMillis()+".jpeg");
        try {
            FileOutputStream fo = new FileOutputStream(file);
            fo.write(bytes.toByteArray());
            fo.flush();
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return "file://" + file.getAbsolutePath();
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
        // Load input bitmap
//        mImageWidth = bitmap.getWidth();
//        mImageHeight = bitmap.getHeight();
        try
        {
            this.originalBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver() , Uri.parse(source));
            LoadBitmap(this.originalBitmap);
        }
        catch (Exception e)
        {
            //handle exception
        }

    }


    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);
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
                GLES20.glUseProgram(mShaderProgramFinalPass);
                setVSParams(mShaderProgramFinalPass);
                setShaderParamPhoto(mShaderProgramFinalPass, GetCurTexture());
                drawquad();
                return;
            }
            else
                shallRenderImage = false;
        }
        didshit = false;
        firstshit = true;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (EnableContrastSaturationBrightness)
        {
            SetRenderTarget();
            GLES20.glUseProgram(mShaderProgramContrastSaturationBrightness);
            setVSParams(mShaderProgramContrastSaturationBrightness);
            setShaderParamPhoto(mShaderProgramContrastSaturationBrightness, GetCurTexture());
            int sat = GLES20.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Saturation");
            int br = GLES20.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Brightness");
            int ctr = GLES20.glGetUniformLocation(mShaderProgramContrastSaturationBrightness, "Contrast");
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
            GLES20.glUseProgram(mShaderProgramFinalPass);
            setVSParams(mShaderProgramFinalPass);
            setShaderParamPhoto(mShaderProgramFinalPass, tx);
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
        GLES20.glUseProgram(mShaderProgramFinalPass);
        setVSParams(mShaderProgramFinalPass);
        setShaderParamPhoto(mShaderProgramFinalPass, tx);
        drawquad();
        //didshit = false;
        //firstshit = true;
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
        return bitmap;
    }

}