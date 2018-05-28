package com.example.user.groupjump;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class TimeServer extends AsyncTask<String, Void, String> {

    private Activity activity;
    private String output;
    public TimeServer(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected String doInBackground(String[] params) {

        try {
            ServerSocket serverSocket = new ServerSocket(8800);
            Socket socket = serverSocket.accept();

            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            output = br.readLine();

            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        VideoActivity.frameTimeInMillis = Long.parseLong(output);
        Toast.makeText(activity, "Data is received!", Toast.LENGTH_SHORT).show();
        final String filePath = CaptureHighSpeedVideoMode.videoFile.getPath();

        int height, width;
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(filePath);

        FFmpegMediaMetadataRetriever mediaRetriever = new FFmpegMediaMetadataRetriever();
        mediaRetriever.setDataSource(filePath);

        long duration = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        Bitmap sample = mediaRetriever.getFrameAtTime(0);

        height = sample.getHeight();
        width = sample.getWidth();

        long diff = VideoActivity.frameTimeInMillis - (VideoActivity.videoStopTimeInMillis - duration);

        Bitmap frame = mediaRetriever.getFrameAtTime(diff * 1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);

        Matrix matrix = new Matrix();

        matrix.postRotate(90);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(frame,width,height,true);

        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        VideoHighFPSActivity.action = rotatedBitmap;

//        String savedImage = CaptureHighSpeedVideoMode.saveImage(rotatedBitmap);

//        try {
//            BufferedWriter out = new BufferedWriter(new FileWriter(VideoData, true), 1024);
//            String entry = "Start: " + VideoActivity.videoStartTimeInMillis + " Stop: " + VideoActivity.videoStopTimeInMillis + " Difference: " + (VideoActivity.videoStopTimeInMillis - VideoActivity.videoStartTimeInMillis) + " Duration: " + duration + " Delta: " + (VideoActivity.videoStopTimeInMillis-VideoActivity.videoStartTimeInMillis-duration);
//            out.write(entry);
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//                int i = -50000;
//                int j = 0;
//                double a = 0;
//                Bitmap frameToSave = null;
//                while (i <= 50000) {
//                    Bitmap frame = mediaRetriever.getFrameAtTime(diff * 1000 + i, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
//
//                    Matrix matrix = new Matrix();
//
//                    matrix.postRotate(90);
//
//                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(frame,width,height,true);
//
//                    Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
//
//                            saveImage(rotatedBitmap);
//
//                    if (j == 0) {
//                        a = amountOfBluriness(rotatedBitmap);
//                        frameToSave = rotatedBitmap;
//                        j++;
//                    } else {
//                        if (a < amountOfBluriness(rotatedBitmap)) {
//                            a = amountOfBluriness(rotatedBitmap);
//                            frameToSave = rotatedBitmap;
//                        }
//                    }
//                    i += 10000;
//                }
//
//                saveImage(frameToSave);

        Intent intent = new Intent(activity, ImagePreviewActivity.class);
        activity.startActivity(intent);
    }
}

