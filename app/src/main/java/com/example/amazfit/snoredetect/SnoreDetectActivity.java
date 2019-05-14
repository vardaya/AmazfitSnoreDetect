package com.example.amazfit.snoredetect;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SnoreDetectActivity extends AppCompatActivity {
    static boolean canVibrate = true;

    private static final int RECORDER_SAMPLERATE = 8000;

    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;

    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private LineGraphSeries<DataPoint> mSeries;
    private double graphLastXValue = 0d;
    public double data = 0d;
    public double decibel = 0d;
    public TextView textView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonHandlers();
        enableButtons(false);
        textView = (TextView) findViewById(R.id.textView);

        GraphView graph = (GraphView) findViewById(R.id.graph);
        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(100);


    }

    private void setButtonHandlers() {
        findViewById(R.id.start).setOnClickListener(btnClick);
        findViewById(R.id.stop).setOnClickListener(btnClick);
    }

    private void enableButtons(boolean isRecording) {
        findViewById(R.id.start).setEnabled(!isRecording);
        findViewById(R.id.stop).setEnabled(isRecording);
    }

    int BufferElements2Rec = 1024;
    int BytesPerElement = 2;

    private void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        mTimer = new Runnable() {
            @Override
            public void run() {
                graphLastXValue += 1d;
                if(decibel>=-30.0){
                    textView.setText("SNORING");
                    final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if ( canVibrate) {
                        vibrator.vibrate(700);
                        canVibrate = false;
                    }
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            canVibrate = true;

                        }
                    }, 3500);
                }
                else textView.setText("NORMAL");
                mSeries.appendData(new DataPoint(graphLastXValue, decibel), true, 100);
                mHandler.postDelayed(this, 35);


            }
        };
        mHandler.postDelayed(mTimer, 1000);


        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            public void run() {

                writeAudioDataToFile();

            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    //Conversion of short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void writeAudioDataToFile() {

        short sData[] = new short[BufferElements2Rec];
        String filePath = "/sdcard/8k16bitMono.pcm";

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            recorder.read(sData, 0, BufferElements2Rec);

            for(int x=0; x<BufferElements2Rec; x++) {
                Log.d("AudioData ", String.valueOf(sData[x]));

                data = sData[x];

                if (data>0)decibel = 20.0*Math.log10(data/65535.0);
                else if (data<0){
                    data = 0d-data;
                    decibel = 20.0*Math.log10(data/65535.0);
                }
            }

            try {
                // writes the data to file from buffer stores the voice buffer
                byte bData[] = short2byte(sData);

                os.write(bData, 0, BufferElements2Rec * BytesPerElement);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;


            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;

            mHandler.removeCallbacks(mTimer);
        }
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.start: {
                    enableButtons(true);
                    startRecording();
                    break;
                }
                case R.id.stop: {
                    enableButtons(false);
                    stopRecording();
                    break;
                }
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
