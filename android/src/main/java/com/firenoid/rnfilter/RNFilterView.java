package com.firenoid.rnfilter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firenoid.rnfilter.R;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
    private EffectContext mEffectContext;


    private String source;
    private String type;

    private float cornerRadius;

    private int[] mTextures = new int[2];
    private Effect mEffect;
    private TextureRenderer mTexRenderer = new TextureRenderer();
    private int mImageWidth;
    private int mImageHeight;

    private boolean mInitialized = false;
    String mCurrentEffect = null;
    double mSaturationValue = 1;
    double mContrastValue = 1;
    double mBrightnessValue = 1;
    private volatile boolean saveFrame;


    public RNFilterView(Context context, Activity activity) {
        super(context);
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "source");
        Log.d("React:", String.valueOf( source ));
        mActivity = activity;
        mContext = context;

        //setBackgroundColor(Color.YELLOW);
//        setZOrderMediaOverlay(true);

//        this.filterView = new RNFilterView(context,this);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mCurrentEffect = null;
    }


    private void loadTextures() {
        // Generate textures
        GLES20.glGenTextures(2, mTextures, 0);
        int id = R.drawable.duckling;
        // Load input bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),  id);
        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();
        mTexRenderer.updateTextureSize(mImageWidth, mImageHeight);

        // Upload to texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // Set texture parameters
        GLToolbox.initTexParams();
    }


    private void renderResult() {
        if (mCurrentEffect != null) {
            // if no effect is chosen, just render the original bitmap
            mTexRenderer.renderTexture(mTextures[1]);
        } else {
//            saveFrame=true;
            // render the result of applyEffect()
            mTexRenderer.renderTexture(mTextures[0]);
        }
    }




//    private void initializeBuffers() {
//        ByteBuffer buff = ByteBuffer.allocateDirect(vertices.length * 4);
//        buff.order(ByteOrder.nativeOrder());
//        verticesBuffer = buff.asFloatBuffer();
//        verticesBuffer.put(vertices);
//        verticesBuffer.position(0);
//
//        buff = ByteBuffer.allocateDirect(textureVertices.length * 4);
//        buff.order(ByteOrder.nativeOrder());
//        textureBuffer = buff.asFloatBuffer();
//        textureBuffer.put(textureVertices);
//    }


    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mInitialized && !source.isEmpty()) {
            // Only need to do this once
            mEffectContext = EffectContext.createWithCurrentGlContext();
            mTexRenderer.init();
            loadTextures();
            mInitialized = true;
        }
        if (mCurrentEffect != null) {
            // if an effect is chosen initialize it and apply it to the texture
            initEffect();
            applyEffect();
        }
        renderResult();
        if (saveFrame) {
            saveBitmap(takeScreenshot(gl));
        }
    }

    private void initEffect() {
        EffectFactory effectFactory = mEffectContext.getFactory();
        if (mEffect != null) {
            mEffect.release();
        }
        /**
         * Initialize the correct effect based on the selected menu/action item
         */
        switch (mCurrentEffect) {
            case "brightness":
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_BRIGHTNESS);
                mEffect.setParameter("brightness", (float)mBrightnessValue);
                break;

            case  "contrast":
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_CONTRAST);
                mEffect.setParameter("contrast", (float) mContrastValue);
                break;

            case "saturation":
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SATURATE);
                mEffect.setParameter("scale", (float) mSaturationValue);
                break;
            default:
                break;

        }
    }

    private void applyEffect() {
        mEffect.apply(mTextures[0], mImageWidth, mImageHeight, mTextures[1]);
    }

    @SuppressLint("WrongThread")
    private void saveBitmap(Bitmap bitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File file = new File(myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Log.i("TAG", "Image SAVED=========="+file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Bitmap takeScreenshot(GL10 mGL) {
        final int mWidth = this.getWidth();
        final int mHeight = this.getHeight();
        IntBuffer ib = IntBuffer.allocate(mWidth * mHeight);
        IntBuffer ibt = IntBuffer.allocate(mWidth * mHeight);
        mGL.glReadPixels(0, 0, mWidth, mHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

        // Convert upside down mirror-reversed image to right-side up normal
        // image.
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                ibt.put((mHeight - i - 1) * mWidth + j, ib.get(i * mWidth + j));
            }
        }

        Bitmap mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(ibt);
        return mBitmap;
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("Filter", "onSurfaceCreated");

        Log.d("Filter", String.valueOf(gl));
            }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d("Filter", "onSurfaceChanged");
        Log.d("Filter",  String.valueOf(gl));
        Log.d("Filter", String.valueOf(width));
        Log.d("Filter", String.valueOf(height));

        if (mTexRenderer != null) {
            mTexRenderer.updateViewSize(width, height);
        }
    }

//    @Override
//    public void onDrawFrame(GL10 gl) {
//        Log.d("Filter", "onDrawFrame");
//        Log.d("Filter", String.valueOf(gl));
//            }


    // Getter & setter

//    public void setProgress(float progress) {
//        this.progress = progress;
////        this.update();
//    }

    public void setSource(String source) {
        this.source = source;
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "source");
        Log.d("React:", source);
//        this.update();
    }

    public void setBrightness(double value) {
        this.source = source;
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "setBrightness");
        Log.d("React:",  String.valueOf(value));
        Log.d("React:", "setBrightness");
        Log.d("React:", "setBrightness");
        Log.d("React:", "setBrightness");
        Log.d("React:", "setBrightness");
        mCurrentEffect = "brightness";
        mBrightnessValue = value;
        this.requestRender();

//        this.update();
    }
    public void setContrast(double value) {
        this.source = source;
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "setContrast");
        Log.d("React:",  String.valueOf(value));
        mCurrentEffect = "contrast";
        mContrastValue = value;
        this.requestRender();
    }

    public void setSaturation(double value) {
        this.source = source;
        Log.d("React:", "RNFilterMainView(Contructtor)");
        Log.d("React:", "setSaturation");
        Log.d("React:", String.valueOf(value));
        mCurrentEffect = "saturation";
        mSaturationValue = value;
        this.requestRender();
    }

}