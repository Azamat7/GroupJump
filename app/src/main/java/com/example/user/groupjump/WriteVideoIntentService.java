package com.example.user.groupjump;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

import java.io.File;
import java.util.List;

public class WriteVideoIntentService  extends IntentService{
    final static String WRITEVIDEO_INFO = "completion";

    public WriteVideoIntentService(){
        super("WriteVideo IntentService");
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        File myDir = VideoHighFPSActivity.getMyDir();
        slow_motion_code mSlowMotionCode = new slow_motion_code();
        mSlowMotionCode.process_video(myDir, "tennis", true, true, true, false);
    }

    private void sendCompletionToClient(String msg){
        Intent intent = new Intent();
        intent.setAction(WRITEVIDEO_INFO);
        intent.putExtra("completion",msg);
        sendBroadcast(intent);
    }
}
