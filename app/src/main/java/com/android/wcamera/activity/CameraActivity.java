package com.android.wcamera.activity;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.android.wcamera.R;
import com.android.wcamera.camera.CameraView;
import com.android.wcamera.util.LogUtil;
import com.android.wcamera.util.PermissionUtils;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/21 10:55
 */
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private CameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.preview_cameraview);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        LogUtil.d(TAG, "rotation " + rotation);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setContentView(R.layout.activity_camera);
        if (cameraView != null){
            cameraView.onDestory();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LogUtil.d(TAG, "onConfigurationChanged " + newConfig.orientation);
        cameraView.previewAngle(this);
    }

}
