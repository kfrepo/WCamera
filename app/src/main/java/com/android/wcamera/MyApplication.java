package com.android.wcamera;

import android.app.Application;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/22 16:15
 */
public class MyApplication extends Application {

    private static final String TAG = "Application";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

}
