package com.android.wcamera.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/23 16:49
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, View.OnClickListener {

    protected static final int[] VIDEO_320 = {320, 240};
    protected static final int[] VIDEO_480 = {640, 480};
    protected static final int[] VIDEO_720 = {1280, 720};
    protected static final int[] VIDEO_1080 = {1920, 1080};
    private int screenOritation = Configuration.ORIENTATION_PORTRAIT;
    private boolean mOpenBackCamera = true;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceTexture mSurfaceTexture;
    private boolean mRunInBackground = false;
    boolean isAttachedWindow = false;
    private Camera mCamera;
    private Camera.Parameters mParam;
    private byte[] previewBuffer;
    private int mCameraId;
    protected int previewformat = ImageFormat.NV21;
    Context context;

    public CameraSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        cameraState = CameraState.START;
        if (cameraStateListener != null) {
            cameraStateListener.onCameraStateChange(cameraState);
        }
        openCamera();
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            screenOritation = Configuration.ORIENTATION_LANDSCAPE;
        }
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceTexture = new SurfaceTexture(10);
        setOnClickListener(this);
        post(new Runnable() {
            @Override
            public void run() {
                if(!isAttachedWindow){
                    mRunInBackground = true;
                    startPreview();
                }
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedWindow = true;
    }

    private void openCamera() {
        if (mOpenBackCamera) {
            mCameraId = findCamera(false);
        } else {
            mCameraId = findCamera(true);
        }
        if (mCameraId == -1) {
            mCameraId = 0;
        }
        try {
            mCamera = Camera.open(mCameraId);
        } catch (Exception ee) {
            mCamera = null;
            cameraState = CameraState.ERROR;
            if (cameraStateListener != null) {
                cameraStateListener.onCameraStateChange(cameraState);
            }
        }
        if (mCamera == null) {
            Toast.makeText(context, "打开摄像头失败", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private int findCamera(boolean front) {
        int cameraCount;
        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                int facing = front ? 1 : 0;
                if (cameraInfo.facing == facing) {
                    return camIdx;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean setDefaultCamera(boolean backCamera) {
        if (mOpenBackCamera == backCamera) return false;

        mOpenBackCamera = backCamera;
        if (mCamera != null) {
            closeCamera();
            openCamera();
            startPreview();
        }
        return true;
    }


    public void closeCamera() {

        stopPreview();
        releaseCamera();
    }

    private void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception ee) {
        }
    }

    private boolean isSupportCameraLight() {
        boolean mIsSupportCameraLight = false;
        try {
            if (mCamera != null) {
                Camera.Parameters parameter = mCamera.getParameters();
                Object a = parameter.getSupportedFlashModes();
                if (a == null) {
                    mIsSupportCameraLight = false;
                } else {
                    mIsSupportCameraLight = true;
                }
            }
        } catch (Exception e) {
            mIsSupportCameraLight = false;
            e.printStackTrace();
        }
        return mIsSupportCameraLight;
    }


    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public synchronized void onPreviewFrame(byte[] data, Camera camera) {
            Log.d("TAG", "data " + data.length);
            if (data == null) {
                releaseCamera();
                return;
            }
            //you can code media here
            if (cameraState != CameraState.PREVIEW) {
                cameraState = CameraState.PREVIEW;
                if (cameraStateListener != null) {
                    cameraStateListener.onCameraStateChange(cameraState);
                }
            }
            mCamera.addCallbackBuffer(previewBuffer);
        }
    };

    //设置Camera各项参数
    private void startPreview() {
        if (mCamera == null) return;
        try {
            mParam = mCamera.getParameters();
            mParam.setPreviewFormat(previewformat);
            mParam.setRotation(0);
            Camera.Size previewSize = CamParaUtil.getSize(mParam.getSupportedPreviewSizes(), 1000,
                    mCamera.new Size(VIDEO_720[0], VIDEO_720[1]));
            mParam.setPreviewSize(previewSize.width, previewSize.height);
            int yuv_buffersize = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(previewformat) / 8;
            previewBuffer = new byte[yuv_buffersize];
            Camera.Size pictureSize = CamParaUtil.getSize(mParam.getSupportedPictureSizes(), 1500,
                    mCamera.new Size(VIDEO_1080[0], VIDEO_1080[1]));
            mParam.setPictureSize(pictureSize.width, pictureSize.height);
            if (CamParaUtil.isSupportedFormats(mParam.getSupportedPictureFormats(), ImageFormat.JPEG)) {
                mParam.setPictureFormat(ImageFormat.JPEG);
                mParam.setJpegQuality(100);
            }

            if (screenOritation != Configuration.ORIENTATION_LANDSCAPE) {
                mParam.set("orientation", "portrait");
                mCamera.setDisplayOrientation(90);
            } else {
                mParam.set("orientation", "landscape");
                mCamera.setDisplayOrientation(0);
            }
            if (mRunInBackground) {
                Log.d("TAG", "setPreviewTexture ");
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.addCallbackBuffer(previewBuffer);
                mCamera.setPreviewCallbackWithBuffer(previewCallback);//设置摄像头预览帧回调
            } else {
                Log.d("TAG", "setPreviewDisplay ");
//                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.setPreviewCallback(previewCallback);//设置摄像头预览帧回调
            }
            mCamera.setParameters(mParam);
            mCamera.startPreview();
            if (cameraState != CameraState.START) {
                cameraState = CameraState.START;
                if (cameraStateListener != null) {
                    cameraStateListener.onCameraStateChange(cameraState);
                }
            }
        } catch (Exception e) {
            releaseCamera();
            return;
        }
        try {
            String mode = mCamera.getParameters().getFocusMode();
            if (("auto".equals(mode)) || ("macro".equals(mode))) {
                mCamera.autoFocus(null);
            }
        } catch (Exception e) {
        }
    }

    private void stopPreview() {
        if (mCamera == null) return;
        try {
            if (mRunInBackground) {
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.stopPreview();
            } else {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            }
            if (cameraState != CameraState.STOP) {
                cameraState = CameraState.STOP;
                if (cameraStateListener != null) {
                    cameraStateListener.onCameraStateChange(cameraState);
                }
            }
        } catch (Exception ee) {
        }
    }

    @Override
    public void onClick(View v) {
        if (mCamera != null) {
            mCamera.autoFocus(null);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        stopPreview();
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
        if (mRunInBackground)
            startPreview();
    }

    protected CameraState cameraState;
    private CameraStateListener cameraStateListener;

    public enum CameraState {
        START, PREVIEW, STOP, ERROR
    }

    public void setOnCameraStateListener(CameraStateListener listener) {
        this.cameraStateListener = listener;
    }

    public interface CameraStateListener {
        void onCameraStateChange(CameraState paramCameraState);
    }

    /**
     * ___________________________________前/后台运行______________________________________
     **/
    public void setRunBack(boolean b) {
        if (mCamera == null) return;
        if (b == mRunInBackground) return;
        if(!b && !isAttachedWindow){
            Toast.makeText(context, "Vew未依附在Window,无法显示", Toast.LENGTH_SHORT).show();
            return;
        }
        mRunInBackground = b;
        if (b)
            setVisibility(View.GONE);
        else
            setVisibility(View.VISIBLE);
    }

}

class CamParaUtil {

    public static Camera.Size getSize(List<Camera.Size> list, int th, Camera.Size defaultSize) {
        if(null == list || list.isEmpty()) return defaultSize;
        Collections.sort(list, new Comparator<Camera.Size>(){
            public int compare(Camera.Size lhs, Camera.Size rhs) {//作升序排序
                if (lhs.width == rhs.width) {
                    return 0;
                } else if (lhs.width > rhs.width) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        int i = 0;
        for (Camera.Size s : list) {
            if ((s.width > th) ) {//&& equalRate(s, rate)
                break;
            }
            i++;
        }
        if (i == list.size()) {
            return list.get(i-1);
        } else {
            return list.get(i);
        }
    }


//    public static Size getBestSize(List<Camera.Size> list, float rate) {
//        float previewDisparity = 100;
//        int index = 0;
//        for (int i = 0; i < list.size(); i++) {
//            Size cur = list.get(i);
//            float prop = (float) cur.width / (float) cur.height;
//            if (Math.abs(rate - prop) < previewDisparity) {
//                previewDisparity = Math.abs(rate - prop);
//                index = i;
//            }
//        }
//        return list.get(index);
//    }
//
//
//    private static boolean equalRate(Size s, float rate) {
//        float r = (float) (s.width) / (float) (s.height);
//        if (Math.abs(r - rate) <= 0.2) {
//            return true;
//        } else {
//            return false;
//        }
//    }

    public static boolean isSupportedFocusMode(List<String> focusList, String focusMode) {
        for (int i = 0; i < focusList.size(); i++) {
            if (focusMode.equals(focusList.get(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSupportedFormats(List<Integer> supportedFormats, int jpeg) {
        for (int i = 0; i < supportedFormats.size(); i++) {
            if (jpeg == supportedFormats.get(i)) {
                return true;
            }
        }
        return false;
    }
}
