package com.example.user.groupjump;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class VideoActivity extends AppCompatActivity {

    private Button recordButton;
    private Button stopButton;
    private CameraView videoView;
    public static long frameTimeInMillis;
    public static long videoStartTimeInMillis;
    public static long videoStopTimeInMillis;
    public static long differenceWithServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        recordButton = (Button) findViewById(R.id.recordButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        videoView = (CameraView) findViewById(R.id.videoView);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecordButton();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopButton();
            }
        });

        videoView.setCameraListener(new CameraListener() {
            @Override
            public void onVideoTaken(File video) {
                super.onVideoTaken(video);

//                int height, width;
//                String filePath = video.getPath();
//
//                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
//                mediaMetadataRetriever.setDataSource(filePath);
//
//                FFmpegMediaMetadataRetriever mediaRetriever = new FFmpegMediaMetadataRetriever();
//                mediaRetriever.setDataSource(filePath);
//
//                long duration = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
//
//                Bitmap sample = mediaRetriever.getFrameAtTime(0);
//
//                height = sample.getHeight();
//                width = sample.getWidth();

                final File new_video = video;

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int height, width;
                        String filePath = new_video.getPath();

                        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                        mediaMetadataRetriever.setDataSource(filePath);

                        FFmpegMediaMetadataRetriever mediaRetriever = new FFmpegMediaMetadataRetriever();
                        mediaRetriever.setDataSource(filePath);

                        long duration = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

                        Bitmap sample = mediaRetriever.getFrameAtTime(0);

                        height = sample.getHeight();
                        width = sample.getWidth();

                        long diff = frameTimeInMillis - (videoStopTimeInMillis - duration);

                        int i = -100000;
                        int j = 0;
                        double a = 0;
                        Bitmap frameToSave = null;
                        while (i <= 100000) {
                            Bitmap frame = mediaRetriever.getFrameAtTime((diff - 100) * 1000 + i, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);

                            Matrix matrix = new Matrix();

                            matrix.postRotate(90);

                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(frame,width,height,true);

                            Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

//                            saveImage(rotatedBitmap);

                            if (j == 0) {
                                a = amountOfBluriness(rotatedBitmap);
                                frameToSave = rotatedBitmap;
                                j++;
                            } else {
                                if (a < amountOfBluriness(rotatedBitmap)) {
                                    a = amountOfBluriness(rotatedBitmap);
                                    frameToSave = rotatedBitmap;
                                }
                            }
                            i += 50000;
                        }

                        saveImage(frameToSave);

                        Toast.makeText(getApplicationContext(), "Image is saved!", Toast.LENGTH_SHORT).show();
                    }
                }, 7000);

//                long diff = frameTimeInMillis - (videoStopTimeInMillis - duration);
//
//                Bitmap frame = mediaRetriever.getFrameAtTime(diff * 1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
//
//                Matrix matrix = new Matrix();
//
//                matrix.postRotate(90);
//
//                Bitmap scaledBitmap = Bitmap.createScaledBitmap(frame,width,height,true);
//
//                Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
//
//                saveImage(rotatedBitmap);
//
//                Toast.makeText(getApplicationContext(), "All images are saved!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onRecordButton() {
        new TimeVideoStart(this).execute();
        Toast.makeText(getApplicationContext(), "Record started!", Toast.LENGTH_SHORT).show();
        videoView.startRecordingVideo();
//        new TimeServer(this).execute();
    }

    private void onStopButton() {
        new TimeVideoStop(this).execute();
//        videoStopTimeInMillis = System.currentTimeMillis() + differenceWithServer;
        Toast.makeText(getApplicationContext(), "Stopped!", Toast.LENGTH_SHORT).show();
        new TimeServer(this).execute();
        videoView.stopRecordingVideo();
//        Toast.makeText(getApplicationContext(), "Stopped!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        videoView.stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoView.start();
    }

    private String saveImage(Bitmap finalBitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".png";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fname;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if(hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private double amountOfBluriness(Bitmap bmp) {
        Mat destination = new Mat();
        Mat matGray = new Mat();

        Mat image = new Mat();
        Utils.bitmapToMat(bmp, image);
        Imgproc.cvtColor(image, matGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(matGray, destination, 3);
        MatOfDouble median = new MatOfDouble();
        MatOfDouble std= new MatOfDouble();
        Core.meanStdDev(destination, median , std);

        return Math.pow(std.get(0,0)[0],2);
    }
}
