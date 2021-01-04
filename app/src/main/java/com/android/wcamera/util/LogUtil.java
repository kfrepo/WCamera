package com.android.wcamera.util;

import android.util.Log;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/15 14:53
 */
public class LogUtil {

    public static void d(String tag, String msg){
        Log.d(tag, msg);
    }

    public static void e(String tag, String msg){
        Log.e(tag, msg);
    }
}
