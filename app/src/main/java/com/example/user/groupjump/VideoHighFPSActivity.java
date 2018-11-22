package com.example.user.groupjump;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.bytedeco.javacv.FrameGrabber;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class VideoHighFPSActivity extends AppCompatActivity {

    // Activity which starts when you press "CAMERA MODE" button on the MainActivity Screen

    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public static Bitmap action;
    public static BluetoothConnectionService mBluetoothConnectionService;
    public static boolean isTimeReceived = false;

    public static Context context;
    private static String mode;

    private static File myDir;

    private WriteVideoReceiver writeVideoReceiver;
    private static Intent writeVideoIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_high_fps);

        Intent intent = getIntent();
        mode = intent.getExtras().getString("mode");
        Log.e("mode: ", mode);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CaptureHighSpeedVideoMode.newInstance())
                    .commit();
        }
        VideoHighFPSActivity.context = getApplicationContext();
        registerWriteVideoReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(writeVideoReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static void videoWritingToast(String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static String getMode(){
        return mode;
    }


    public static void startWriteVideoService(File dir){
        myDir = dir;
        writeVideoIntent = new Intent();
        writeVideoIntent.setClass(context,WriteVideoIntentService.class);
        context.startService(writeVideoIntent);
    }

    public static File getMyDir(){
        return myDir;
    }

    private void registerWriteVideoReceiver(){
        writeVideoReceiver = new WriteVideoReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WriteVideoIntentService.WRITEVIDEO_INFO);

        registerReceiver(writeVideoReceiver,intentFilter);
    }

    public class WriteVideoReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("completion");
            videoWritingToast(message);
        }
    }
}