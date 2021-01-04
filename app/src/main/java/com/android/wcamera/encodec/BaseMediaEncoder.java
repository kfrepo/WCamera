package com.android.wcamera.encodec;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;

import com.android.wcamera.egl.EglHelper;
import com.android.wcamera.egl.WEGLSurfaceView;
import com.android.wcamera.util.LogUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLContext;

/**
 * Description :
 *
 * @author wangmh
 * @date 2020/12/24 11:54
 */
public abstract class BaseMediaEncoder {

    private static final String TAG = "BaseMediaEncoder";

    private Surface surface;
    private EGLContext eglContext;

    private int width;
    private int height;

    private MediaCodec videoEncodec;
    private MediaFormat videoFormat;
    private MediaCodec.BufferInfo videoBufferInfo;

    //音频
    private MediaCodec audioEncodec;
    private MediaFormat audioFormat;
    private MediaCodec.BufferInfo audioBufferinfo;
    private long audioPts = 0;
    private int sampleRate;

    private MediaMuxer mediaMuxer;
    private boolean encodecStart;
    private boolean audioExit;
    private boolean videoExit;

    private EGLMediaThread eglMediaThread;
    private VideoEncodecThread videoEncodecThread;
    private AudioEncodecThread audioEncodecThread;

    private WEGLSurfaceView.GLRender glRender;

    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;
    private int mRenderMode = RENDERMODE_CONTINUOUSLY;

    private OnMediaInfoListener onMediaInfoListener;

    public BaseMediaEncoder(Context context) {}

    public void setRender(WEGLSurfaceView.GLRender glRender){
        this.glRender = glRender;
    }

    public void setmRenderMode(int mRenderMode) {
        if(glRender == null) {
            throw  new RuntimeException("must set render before");
        }
        this.mRenderMode = mRenderMode;
    }

    public void setOnMediaInfoListener(OnMediaInfoListener onMediaInfoListener) {
        this.onMediaInfoListener = onMediaInfoListener;
    }

    /**
     * 初始化音频 视频编码器
     * @param eglContext
     * @param savePath
     * @param width
     * @param height
     */
    public void initEncodec(EGLContext eglContext, String savePath, int width, int height, int sampleRate, int channelCount) {
        this.width = width;
        this.height = height;
        this.eglContext = eglContext;
        initMediaEncodec(savePath, width, height, sampleRate, channelCount);
    }

    private void initMediaEncodec(String savePath, int width, int height, int sampleRate, int channelCount) {
        try {
            mediaMuxer = new MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initVideoEncodec(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            initAudioEncodec(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化视频编码器
     * @param mimeType
     * @param width
     * @param height
     */
    private void initVideoEncodec(String mimeType, int width, int height){

        try {
            LogUtil.e(TAG, "start init video mediaCodec! " + mimeType + " width:" + width + " height:" + height);
            videoBufferInfo = new MediaCodec.BufferInfo();
            videoFormat = MediaFormat.createVideoFormat(mimeType, width, height);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            videoFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            videoFormat.setInteger("level", MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            videoEncodec = MediaCodec.createEncoderByType(mimeType);
            videoEncodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            //创建一个目的surface来存放输入数据
            surface = videoEncodec.createInputSurface();
            LogUtil.e(TAG, "init video mediaCodec success!");
        } catch (Exception e){
            LogUtil.e(TAG, "initVideoEncodec exception:" +e.toString());
            videoEncodec = null;
            videoFormat = null;
            videoBufferInfo = null;
        }
    }

    /**
     * 初始化音频编码器
     * @param mimeType
     * @param sampleRate
     * @param channelCount
     */
    private void initAudioEncodec(String mimeType, int sampleRate, int channelCount) {
        try {
            LogUtil.e(TAG, "start init audio mediaCodec! " + mimeType + " " + sampleRate + " " + channelCount);
            this.sampleRate = sampleRate;
            audioBufferinfo = new MediaCodec.BufferInfo();
            audioFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //作用于inputBuffer的大小
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096);

            audioEncodec = MediaCodec.createEncoderByType(mimeType);
            audioEncodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            LogUtil.e(TAG, "init audio mediaCodec success!");
        } catch (IOException e) {
            e.printStackTrace();
            audioBufferinfo = null;
            audioFormat = null;
            audioEncodec = null;
        }
    }

    public void startRecord(){
        if (surface != null && eglContext != null){
            LogUtil.d(TAG, "startRecord!");
            audioPts = 0;
            audioExit = false;
            videoExit = false;
            encodecStart = false;

            eglMediaThread = new EGLMediaThread(new WeakReference<BaseMediaEncoder>(this));
            videoEncodecThread = new VideoEncodecThread(new WeakReference<BaseMediaEncoder>(this));
            audioEncodecThread = new AudioEncodecThread(new WeakReference<BaseMediaEncoder>(this));

            eglMediaThread.isCreate = true;
            eglMediaThread.isChange = true;
            eglMediaThread.start();
            videoEncodecThread.start();
            audioEncodecThread.start();
        }
    }

    public void stopRecord() {
        if(eglMediaThread != null && videoEncodecThread != null && audioEncodecThread != null) {
            LogUtil.d(TAG, "stopRecord!");
            videoEncodecThread.exit();
            audioEncodecThread.exit();
            eglMediaThread.onDestory();

            videoEncodecThread = null;
            audioEncodecThread = null;
            eglMediaThread = null;
        }
    }



    /**
     * EGL
     */
    static class EGLMediaThread extends Thread {
        private WeakReference<BaseMediaEncoder> encoderWeakReference;
        private EglHelper eglHelper;
        private Object object;

        private boolean isExit = false;
        private boolean isCreate = false;
        private boolean isChange = false;
        private boolean isStart = false;

        public EGLMediaThread(WeakReference<BaseMediaEncoder> encoder) {
            encoderWeakReference = encoder;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            isStart = false;
            object = new Object();
            eglHelper = new EglHelper();
            eglHelper.initEgl(encoderWeakReference.get().surface, encoderWeakReference.get().eglContext);

            while (true) {
                if (isExit){
                    release();
                    break;
                }

                if (isStart) {
                    if(encoderWeakReference.get().mRenderMode == RENDERMODE_WHEN_DIRTY) {
                        synchronized (object) {
                            try {
                                object.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if(encoderWeakReference.get().mRenderMode == RENDERMODE_CONTINUOUSLY) {
                        try {
                            Thread.sleep(1000 / 60);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw  new RuntimeException("mRenderMode is wrong value");
                    }
                }

                onCreate();
                onChange(encoderWeakReference.get().width, encoderWeakReference.get().height);
                onDraw();
                isStart = true;
            }
        }

        private void onCreate() {
            if(isCreate && encoderWeakReference.get().glRender != null) {
                isCreate = false;
                encoderWeakReference.get().glRender.onSurfaceCreated();
            }
        }

        private void onChange(int width, int height) {
            if(isChange && encoderWeakReference.get().glRender != null) {
                isChange = false;
                encoderWeakReference.get().glRender.onSurfaceChanged(width, height);
            }
        }

        private void onDraw() {
            if (encoderWeakReference.get().glRender != null && eglHelper != null) {
                encoderWeakReference.get().glRender.onDrawFrame();
                if (!isStart) {
                    encoderWeakReference.get().glRender.onDrawFrame();
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
                encoderWeakReference = null;
            }
        }
    }

    /**
     * 视频编码线程
     */
    static class VideoEncodecThread extends Thread {

        private static final String TAG = "VideoEncodecThread";
        private WeakReference<BaseMediaEncoder> encoderWeakReference;

        private boolean isExit;

        private MediaCodec videoEncodec;

        private MediaCodec.BufferInfo videoBufferInfo;
        private MediaMuxer mediaMuxer;

        private int videoTrackIndex = -1;
        private long pts;

        public VideoEncodecThread(WeakReference<BaseMediaEncoder> encoder){
            encoderWeakReference = encoder;
            videoEncodec = encoder.get().videoEncodec;

            videoBufferInfo = encoder.get().videoBufferInfo;
            mediaMuxer = encoder.get().mediaMuxer;
            videoTrackIndex = -1;
        }

        @Override
        public void run() {
            super.run();
            pts = 0;
            videoTrackIndex = -1;
            isExit = false;
            // 启动MediaCodec ，等待传入数据
            videoEncodec.start();
            LogUtil.d(TAG, "video MediaCodec start！");

            while (true){
                if (isExit){
                    videoEncodec.stop();
                    videoEncodec.release();
                    videoEncodec = null;

                    encoderWeakReference.get().videoExit = true;
                    if (encoderWeakReference.get().audioExit){
                        mediaMuxer.stop();
                        mediaMuxer.release();
                        mediaMuxer = null;
                    }

                    LogUtil.d(TAG, "退出视频编码线程！");
                    break;
                }

                // 读取MediaCodec编码后的数据
                int outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferInfo, 0);
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    videoTrackIndex = mediaMuxer.addTrack(videoEncodec.getOutputFormat());
                    LogUtil.d(TAG, "videoTrackIndex is:" + videoTrackIndex);
                    if(encoderWeakReference.get().audioEncodecThread.audioTrackIndex != -1) {
                        LogUtil.d(TAG, "audioTrackIndex ！= -1!");
                        mediaMuxer.start();
                        encoderWeakReference.get().encodecStart = true;
                    }

                } else {
                    while (outputBufferIndex >= 0){

                        if (encoderWeakReference.get().encodecStart){
                            ByteBuffer outputBuffer = videoEncodec.getOutputBuffers()[outputBufferIndex];
                            outputBuffer.position(videoBufferInfo.offset);
                            outputBuffer.limit(videoBufferInfo.offset + videoBufferInfo.size);

                            if(pts == 0) {
                                pts = videoBufferInfo.presentationTimeUs;
                                LogUtil.d(TAG, "video set pts " + pts);
                            }
                            videoBufferInfo.presentationTimeUs = videoBufferInfo.presentationTimeUs - pts;
                            LogUtil.d(TAG, "video buffer set pts " + videoBufferInfo.presentationTimeUs);
                            mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, videoBufferInfo);
                            if(encoderWeakReference.get().onMediaInfoListener != null) {
                                encoderWeakReference.get().onMediaInfoListener.onMediaTime(videoBufferInfo.presentationTimeUs);
                            }
                        }

                        videoEncodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferInfo, 0);
                    }
                }
            }

        }

        public void exit() {
            isExit = true;
        }
    }

    public void putPCMData(byte[] buffer, int size) {
        if(audioEncodecThread != null && !audioEncodecThread.isExit && buffer != null && size > 0) {
            //LogUtil.d(TAG, "putPCMData readSize is : " + size + " " + buffer.length);
            int inputBufferindex = audioEncodec.dequeueInputBuffer(0);
            if(inputBufferindex >= 0) {
                ByteBuffer byteBuffer = audioEncodec.getInputBuffers()[inputBufferindex];
                byteBuffer.clear();
                byteBuffer.put(buffer);
                long pts = getAudioPts(size, sampleRate);
                LogUtil.d(TAG, "putPCMData pts is : " + pts);
                audioEncodec.queueInputBuffer(inputBufferindex, 0, size, pts, 0);
            }
        }
    }

    static class AudioEncodecThread extends Thread {

        private WeakReference<BaseMediaEncoder> encoder;
        private boolean isExit;

        private MediaCodec audioEncodec;
        private MediaCodec.BufferInfo bufferInfo;
        private MediaMuxer mediaMuxer;

        private int audioTrackIndex = -1;
        long pts;

        public AudioEncodecThread(WeakReference<BaseMediaEncoder> encoder) {
            this.encoder = encoder;
            audioEncodec = encoder.get().audioEncodec;
            bufferInfo = encoder.get().audioBufferinfo;
            mediaMuxer = encoder.get().mediaMuxer;
            audioTrackIndex = -1;
        }

        @Override
        public void run() {
            super.run();
            pts = 0;
            isExit = false;
            audioEncodec.start();
            while(true) {
                if(isExit) {
                    audioEncodec.stop();
                    audioEncodec.release();
                    audioEncodec = null;
                    encoder.get().audioExit = true;
                    if(encoder.get().videoExit) {
                        mediaMuxer.stop();
                        mediaMuxer.release();
                        mediaMuxer = null;
                    }
                    LogUtil.d(TAG, "退出音频编码线程!");
                    break;
                }

                int outputBufferIndex = audioEncodec.dequeueOutputBuffer(bufferInfo, 0);
                if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if(mediaMuxer != null) {
                        audioTrackIndex = mediaMuxer.addTrack(audioEncodec.getOutputFormat());
                        LogUtil.d(TAG, "audioTrackIndex is:" + audioTrackIndex);
                        if(encoder.get().videoEncodecThread.videoTrackIndex != -1) {
                            LogUtil.d(TAG, "videoTrackIndex != -1!");
                            mediaMuxer.start();
                            encoder.get().encodecStart = true;
                        }
                    }
                } else {
                    while(outputBufferIndex >= 0) {
                        if(encoder.get().encodecStart) {

                            ByteBuffer outputBuffer = audioEncodec.getOutputBuffers()[outputBufferIndex];
                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                            if(pts == 0) {
                                pts = bufferInfo.presentationTimeUs;
                                LogUtil.d(TAG, "audio set pts " + pts);
                            }

                            bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts;
                            LogUtil.d(TAG, "audio buffer set pts " + bufferInfo.presentationTimeUs);
                            mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo);
                        }
                        audioEncodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = audioEncodec.dequeueOutputBuffer(bufferInfo, 0);
                    }
                }
            }

        }
        public void exit() {
            isExit = true;
        }
    }

    private long getAudioPts(int size, int sampleRate) {
        audioPts += (long)(1.0 * size / (sampleRate * 2 * 2) * 1000000.0);
        return audioPts;
    }


    public interface OnMediaInfoListener {
        void onMediaTime(long times);
    }
}
