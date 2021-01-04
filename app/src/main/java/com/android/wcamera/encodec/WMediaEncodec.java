package com.android.wcamera.encodec;

import android.content.Context;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/24 11:54
 */
public class WMediaEncodec extends BaseMediaEncoder{

    private EncodecRender encodecRender;

    public WMediaEncodec(Context context, int textureId) {
        super(context);
        encodecRender = new EncodecRender(context, textureId);
        setRender(encodecRender);
        setmRenderMode(BaseMediaEncoder.RENDERMODE_CONTINUOUSLY);
    }
}
