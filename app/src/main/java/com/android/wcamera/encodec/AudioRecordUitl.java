package com.android.wcamera.encodec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.android.wcamera.util.LogUtil;

/**
 * @author workw
 */
public class AudioRecordUitl {

    private static final String TAG = "AudioRecordUitl";

    private AudioRecord audioRecord;
    private int bufferSizeInBytes;
    private boolean start = false;
    private int readSize = 0;

    private OnRecordListener onRecordListener;

    public AudioRecordUitl(int sampleRate, int channelCount, int audioFormat) {
        //采集数据需要的缓冲区的大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(
                sampleRate,
                channelCount,
                audioFormat);
        LogUtil.d(TAG, "sampleRate " + sampleRate +
                " ChannelCount:" + channelCount +
                " audioFormat" + audioFormat);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelCount,
                audioFormat,
                bufferSizeInBytes);
    }

    public void setOnRecordListener(OnRecordListener onRecordListener) {
        this.onRecordListener = onRecordListener;
    }

    public void startRecord() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                start = true;
                audioRecord.startRecording();
                byte[] audiodata = new byte[bufferSizeInBytes];
                while (start) {
                    readSize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
                    if(onRecordListener != null) {
                        onRecordListener.recordByte(audiodata, readSize);
                    }
                }
                if(audioRecord != null) {
                    LogUtil.d(TAG, "audioRecord.stop()!");
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                }
            }
        }.start();
    }

    public void stopRecord() {
        start = false;
    }

    public interface OnRecordListener{
        void recordByte(byte[] audioData, int readSize);
    }

    public boolean isStart() {
        return start;
    }

}
