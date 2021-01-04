package com.android.wcamera.egl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.wcamera.util.LogUtil;

import java.lang.ref.WeakReference;

import javax.microedition.khronos.egl.EGLContext;

/**
 * Description :自定义GLSurfaceView
 *
 * @author wangmh
 * @date 2020/12/22 17:11
 */
public abstract class WEGLSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "WEGLSurfaceView";

    private Surface surface;

    private EGLContext eglContext;
    private EGLThread eglThread;
    private GLRender glRender;

    /**
     * 控制手动刷新还是自动刷新 RENDERMODE_WHEN_DIRTY时只有在创建和调用requestRender()时才会刷新
     */
    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;

    private int mRenderMode = RENDERMODE_CONTINUOUSLY;

    public WEGLSurfaceView(Context context) {
        this(context, null);
    }

    public WEGLSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WEGLSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }

    public void setRender(GLRender glRender) {
        this.glRender = glRender;
    }

    public void setRenderMode(int renderMode) {

        if(glRender == null) {
            throw  new RuntimeException("must set render before");
        }
        mRenderMode = renderMode;
    }

    public void setSurfaceAndEglContext(Surface surface, EGLContext eglContext) {
        this.surface = surface;
        this.eglContext = eglContext;
    }

    public EGLContext getEglContext() {
        if(eglThread != null) {
            return eglThread.getEglContext();
        }
        return null;
    }

    public void requestRender() {
        if(eglThread != null) {
            eglThread.requestRender();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(surface == null) {
            surface = holder.getSurface();
        }
        eglThread = new EGLThread(new WeakReference<WEGLSurfaceView>(this));
        eglThread.isCreate = true;
        eglThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        eglThread.width = width;
        eglThread.height = height;
        eglThread.isChange = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        eglThread.onDestory();
        eglThread = null;
        surface = null;
        eglContext = null;
    }

    public interface GLRender {
        void onSurfaceCreated();
        void onSurfaceChanged(int width, int height);
        void onDrawFrame();
    }

    private static class EGLThread extends Thread{

        private WeakReference<WEGLSurfaceView> mEGLSurfaceViewWeakReference;
        private EglHelper eglHelper = null;
        private Object object = null;

        private boolean isExit = false;
        private boolean isCreate = false;
        private boolean isChange = false;
        private boolean isStart = false;

        private int width;
        private int height;

        public EGLThread(WeakReference<WEGLSurfaceView> reference) {
            LogUtil.d(TAG, "EGLThread obj create!");
            this.mEGLSurfaceViewWeakReference = reference;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            isStart = false;
            object = new Object();
            eglHelper = new EglHelper();
            eglHelper.initEgl(mEGLSurfaceViewWeakReference.get().surface, mEGLSurfaceViewWeakReference.get().eglContext);

            while (true) {
                if(isExit) {
                    //释放资源
                    release();
                    break;
                }

                if(isStart) {
                    if(mEGLSurfaceViewWeakReference.get().mRenderMode == RENDERMODE_WHEN_DIRTY) {
                        synchronized (object) {
                            try {
                                object.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if(mEGLSurfaceViewWeakReference.get().mRenderMode == RENDERMODE_CONTINUOUSLY) {
                        try {
                            Thread.sleep(1000 / 60);//每秒60帧
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw  new RuntimeException("mRenderMode is wrong value");
                    }
                }

                onCreate();
                onChange(width, height);
                onDraw();

                isStart = true;
            }
        }

        private void onCreate() {
            if(isCreate && mEGLSurfaceViewWeakReference.get().glRender != null) {
                isCreate = false;
                mEGLSurfaceViewWeakReference.get().glRender.onSurfaceCreated();
            }
        }

        private void onChange(int width, int height) {
            if(isChange && mEGLSurfaceViewWeakReference.get().glRender != null) {
                isChange = false;
                mEGLSurfaceViewWeakReference.get().glRender.onSurfaceChanged(width, height);
            }
        }

        private void onDraw() {
            if(mEGLSurfaceViewWeakReference.get().glRender != null && eglHelper != null) {
                mEGLSurfaceViewWeakReference.get().glRender.onDrawFrame();
                if(!isStart) {
                    mEGLSurfaceViewWeakReference.get().glRender.onDrawFrame();
                }
                eglHelper.swapBuffers();
            }
        }

        private void requestRender() {
            if(object != null) {
                synchronized (object) {
                    object.notifyAll();
                }
            }
        }

        public void onDestory() {
            isExit = true;
            requestRender();
        }

        public void release() {
            if(eglHelper != null) {
                eglHelper.destroyEgl();
                eglHelper = null;
                object = null;
                mEGLSurfaceViewWeakReference = null;
            }
        }

        public EGLContext getEglContext() {
            if(eglHelper != null) {
                return eglHelper.getEglCotext();
            }
            return null;
        }

    }

}
