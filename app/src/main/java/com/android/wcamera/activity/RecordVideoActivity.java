package com.android.wcamera.activity;

import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.wcamera.R;
import com.android.wcamera.camera.CameraView;
import com.android.wcamera.encodec.AudioRecordUitl;
import com.android.wcamera.encodec.BaseMediaEncoder;
import com.android.wcamera.encodec.WMediaEncodec;
import com.android.wcamera.util.DisplayUtil;
import com.android.wcamera.util.LogUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author wangmh
 */
public class RecordVideoActivity extends AppCompatActivity {

    private static final String TAG = "RecordVideoActivity";

    private CameraView cameraView;
    private Button btnRecord;

    private WMediaEncodec wMediaEncodec;

    private int mAudioSampleRate;
    private int mAudioChannelCount;
    private int mAudioFormat;

    private int mVideoWidth;
    private int mVideoHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_recordvideo);
        getSupportActionBar().hide();

        int physicsWidth = DisplayUtil.getScreenWidth(this);
        int physicsHeight = DisplayUtil.getScreenHeight(this);
        cameraView = findViewById(R.id.record_cameraview);
        //调整预览view 宽高为16/9
//        ViewGroup.LayoutParams lp = cameraView.getLayoutParams();
//        lp.width = physicsWidth;
//        lp.height = physicsWidth * 9 / 16;
//        cameraView.setLayoutParams(lp);
//        LogUtil.d(TAG, "setLayoutParams height " + lp.height);

        btnRecord = findViewById(R.id.record_bt);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record();
            }
        });

        //录制参数
        mAudioSampleRate = 16000;
        mAudioChannelCount =  AudioFormat.CHANNEL_IN_STEREO;
        mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;

        mVideoWidth = 1920;
        mVideoHeight = 1080;
    }

    private AudioRecordUitl audioRecordUitl = null;
    public void record() {

        if (wMediaEncodec == null) {

            LogUtil.d(TAG, "textureid is " + cameraView.getTextureId());
            wMediaEncodec = new WMediaEncodec(this, cameraView.getTextureId());

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String date = simpleDateFormat.format(new Date());
            String file = getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + date + ".mp4";
            file = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + date + ".mp4";
            LogUtil.d(TAG, "start record file " + file);

            //初始化编码器
            wMediaEncodec.initEncodec(cameraView.getEglContext(),
                    file,
                    mVideoWidth,
                    mVideoHeight,
                    mAudioSampleRate,
                    2);

            //启动音频采集
            if(audioRecordUitl == null) {
                audioRecordUitl = new AudioRecordUitl(mAudioSampleRate, mAudioChannelCount, mAudioFormat);
                audioRecordUitl.setOnRecordListener(new AudioRecordUitl.OnRecordListener() {
                    @Override
                    public void recordByte(byte[] audioData, int readSize) {
                        //LogUtil.d(TAG, "recordByte readSize is : " + readSize + " " + audioData.length);
                        if (wMediaEncodec != null){
                            wMediaEncodec.putPCMData(audioData, readSize);
                        }
                    }
                });
                audioRecordUitl.startRecord();
            }

            //录制时长
            wMediaEncodec.setOnMediaInfoListener(new BaseMediaEncoder.OnMediaInfoListener() {
                @Override
                public void onMediaTime(long times) {
                    long recordTime = times/1000;
                    //LogUtil.d(TAG, "recordTime is : " + recordTime);
                }
            });

            wMediaEncodec.startRecord();
            btnRecord.setText("停止录制");

        } else {
            LogUtil.e(TAG, "停止录制！");

            wMediaEncodec.stopRecord();
            audioRecordUitl.stopRecord();
            btnRecord.setText("开始录制");
            wMediaEncodec = null;
            audioRecordUitl = null;
        }
    }

}
