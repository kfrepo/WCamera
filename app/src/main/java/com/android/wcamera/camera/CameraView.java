package com.android.wcamera.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.WindowManager;

import com.android.wcamera.egl.WEGLSurfaceView;
import com.android.wcamera.util.LogUtil;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/22 17:44
 */
public class CameraView extends WEGLSurfaceView {

    private static final String TAG = "CameraView";

    private CameraUtil cameraUtil;
    private CameraRender cameraRender;

    private int textureId = -1;

    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        cameraRender = new CameraRender(context);
        cameraUtil = new CameraUtil(context);
        setRender(cameraRender);
        cameraRender.resetMatrix();
//        previewAngle(context);
        previewRotate(cameraId);

        cameraRender.setOnSurfaceCreateListener(new CameraRender.OnSurfaceCreateListener() {
            @Override
            public void onSurfaceCreate(SurfaceTexture surfaceTexture, int tid) {
                LogUtil.d(TAG, "cameraRender onSurfaceCreate!");
                cameraUtil.initCamera(surfaceTexture, cameraId);
                textureId = tid;
            }
        });
    }

    public void onDestory() {
        if(cameraUtil != null) {
            LogUtil.d(TAG, "stopPreview!");
            cameraUtil.stopPreview();
        }
    }

    public int getTextureId() {
        return textureId;
    }

    /**
     * 根据屏幕旋转的方向 旋转opengl 绘制
     * @param context
     */
    public void previewAngle(Context context) {
        int angle = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        LogUtil.e(TAG, "previewAngle angle=" + angle);
        cameraRender.resetMatrix();
        switch (angle) {
            case Surface.ROTATION_0:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                    cameraRender.setAngle(90, 0, 0, 1);
                    cameraRender.setAngle(180, 1, 0, 0);
                } else {
                    cameraRender.setAngle(90f, 0f, 0f, 1f);
                }

                break;
            case Surface.ROTATION_90:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180, 0, 0, 1);
                    cameraRender.setAngle(180, 0, 1, 0);
                } else {
                    cameraRender.setAngle(90f, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_180:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
                {
                    cameraRender.setAngle(90f, 0.0f, 0f, 1f);
                    cameraRender.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    cameraRender.setAngle(-90, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_270:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    cameraRender.setAngle(0f, 0f, 0f, 1f);
                }
                break;
            default:

        }
    }


    /**
     * 根据camera orientation旋转
     * @param cameraId
     */
    public void previewRotate(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int mOrientation = info.orientation;
        LogUtil.e(TAG, cameraId + " previewRotate orientation=" + mOrientation);
        cameraRender.resetMatrix();
        switch (mOrientation) {
            case 0:

                break;
            case 90:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(-90, 0, 0, 1);
                    cameraRender.setAngle(180, 0, 1, 0);
                } else {
                    cameraRender.setAngle(90f, 0f, 0f, 1f);
                }
                break;
            case 180:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    cameraRender.setAngle(-90, 0f, 0f, 1f);
                }
                break;
            case 270:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180f, 0.0f, 1f, 0f);
                } else {
                    cameraRender.setAngle(0f, 0f, 0f, 1f);
                }
                break;
            default:

        }
    }


}
