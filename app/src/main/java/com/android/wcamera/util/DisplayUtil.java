package com.android.wcamera.util;

import android.content.Context;
import android.util.DisplayMetrics;

public class DisplayUtil {

    public static int getScreenWidth(Context context) {
        DisplayMetrics metric = context.getResources().getDisplayMetrics();
        LogUtil.d("DisplayUtil", "widthPixels " + metric.widthPixels);
        return metric.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics metric = context.getResources().getDisplayMetrics();
        LogUtil.d("DisplayUtil", "heightPixels " + metric.heightPixels
                + " ydpi " + metric.ydpi
                + " densityDpi " + metric.densityDpi);
        return metric.heightPixels;
    }

}
