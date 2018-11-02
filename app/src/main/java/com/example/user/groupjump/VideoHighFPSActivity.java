package com.example.user.groupjump;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.UUID;

public class VideoHighFPSActivity extends AppCompatActivity {

    // Activity which starts when you press "CAMERA MODE" button on the MainActivity Screen

    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public static Bitmap action;
    public static BluetoothConnectionService mBluetoothConnectionService;
    public static boolean isTimeReceived = false;

    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_high_fps);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CaptureHighSpeedVideoMode.newInstance())
                    .commit();
        }
        VideoHighFPSActivity.context = getApplicationContext();
    }

    public static void videoWritingToast(String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}