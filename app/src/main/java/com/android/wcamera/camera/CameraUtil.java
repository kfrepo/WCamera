package com.android.wcamera.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.android.wcamera.util.DisplayUtil;
import com.android.wcamera.util.LogUtil;

import java.util.List;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/23 10:56
 */
public class CameraUtil {

    private static final String TAG = "CameraUtil";

    private Camera camera;
    private int width, height;
    public int mOrientation;
    private static final int[] P1080 = {1920, 1080};

    private SurfaceTexture surfaceTexture;

    public CameraUtil(Context context){
        this.width = DisplayUtil.getScreenWidth(context);
        this.height = DisplayUtil.getScreenHeight(context);
    }

    public void initCamera(SurfaceTexture surfaceTexture, int cameraId){
        this.surfaceTexture = surfaceTexture;
        setCameraParm(cameraId);
    }

    public void setCameraParm(int cameraId){
        try {
            camera = Camera.open(cameraId);
//            camera.setDisplayOrientation (90);
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            mOrientation = info.orientation;
            LogUtil.e(TAG, "Camera open " + cameraId + " mOrientation:" + mOrientation);

            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);

            Camera.Size previewSize = getFitSize(parameters.getSupportedPreviewSizes());
            parameters.setPreviewSize(1920, 1080);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//            Camera.Size picSize = getFitSize(parameters.getSupportedPictureSizes());
//            parameters.setPictureSize(picSize.width, picSize.height);

//            parameters.setPreviewSize(parameters.getSupportedPreviewSizes().get(0).width,
//                    parameters.getSupportedPreviewSizes().get(0).height);
            camera.setPreviewTexture(surfaceTexture);
            //设置摄像头预览帧回调
//            camera.setPreviewCallback(previewCallback);

            camera.setParameters(parameters);
            camera.startPreview();
            if(onStartPreviewListener != null) {
                onStartPreviewListener.onStartPreviewListener(parameters.getPreviewSize());
            }
            LogUtil.e(TAG, "屏幕物理尺寸 " + width + " " + height + " Camera startPreview w:" +
                    parameters.getPreviewSize().width + " h:" + parameters.getPreviewSize().height);
        }catch (Exception e){
            LogUtil.e(TAG, "setCameraParm error:" + e.toString());
        }
    }

    public void stopPreview(){
        if(camera != null) {
            LogUtil.d(TAG, "stopPreview!");
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void changeCamera(int cameraId){
        if(camera != null) {
            stopPreview();
        }
        setCameraParm(cameraId);
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public synchronized void onPreviewFrame(byte[] data, Camera camera) {
//            LogUtil.d(TAG, "previewCallback " + data.length);
        }
    };

    /**
     * 与屏幕物理尺寸比较 从相机的Size中选取和Surface等比的宽高
     * @param sizes
     * @return
     */
    private Camera.Size getFitSize(List<Camera.Size> sizes) {
        if(width < height) {
            int t = height;
            height = width;
            width = t;
        }
        for(Camera.Size size : sizes) {
            LogUtil.d(TAG, "支持 " + size.width + " " + size.height);
        }
        for(Camera.Size size : sizes) {
            if(1.0f * size.width / size.height == 1.0f * width / height) {
                LogUtil.d(TAG, "getFitSize success " + size.width + " " + size.height);
                return size;
            }
        }
        LogUtil.d(TAG, "final getFitSize set max " + sizes.get(0).width + " " + sizes.get(0).height);
        return sizes.get(0);
    }


    private OnStartPreviewListener onStartPreviewListener;
    public void setOnSurfaceCreateListener(OnStartPreviewListener onStartPreviewListener) {
        this.onStartPreviewListener = onStartPreviewListener;
    }

    public interface OnStartPreviewListener {
        void onStartPreviewListener(Camera.Size previewSize);
    }

}
