package com.example.user.groupjump;

import android.graphics.Bitmap;
import android.util.Log;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.jcodec.api.SequenceEncoder;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

public class single_camera_tools {
    public single_camera_tools(){
    }

    public static List<jumpStats> readJumpStats(File sourceFolderPath){
        File f = new File(sourceFolderPath, "jumpStats" + ".txt");

        List<jumpStats> mJumpStats = new ArrayList<jumpStats>();

        try {
            FileInputStream fis = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            int i =0;
            while ((strLine = br.readLine()) != null) {
                if (i==0){
                    i++;
                    continue;
                }
                i++;
                mJumpStats.add(splitRow(strLine));
            }
            br.close();
            in.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mJumpStats;
    }

    private static jumpStats splitRow(String strLine){
        String[] row = strLine.split(",");
        String deviceID = row[0].replaceAll("\\s","");
        int targetTime = Integer.valueOf(row[1].replaceAll("\\s",""));
        int tJumpStart = Integer.valueOf(row[2].replaceAll("\\s",""));
        int tJumpEnd = Integer.valueOf(row[3].replaceAll("\\s",""));
        int videoDuration = Integer.valueOf(row[4].replaceAll("\\s",""));
        long videoEndUTC = Long.parseLong(row[5].replaceAll("\\s",""));
        long dataStartUTC = Long.parseLong(row[6].replaceAll("\\s",""));
        int dataDuration = Integer.valueOf(row[7].replaceAll("\\s",""));

        long videoStartUTC = videoEndUTC - videoDuration;
        long dataOffset = dataStartUTC - videoStartUTC;
        jumpStats mJumpStats = new jumpStats(deviceID,targetTime,tJumpStart,tJumpEnd,dataOffset);
        return mJumpStats;
    }

    public static String getAccDataFileName(File sourceFolderPath,String tag){
        String fileName = "";
        for (File f : sourceFolderPath.listFiles()) {
            if (f.isFile()) {
                String name = f.getName();
                if (name.contains(tag)){
                    fileName = name;
                }
            }
        }
        return fileName;
    }

    public static List<List<Float>> readAccelerationData(File sourceFolderPath,String accDataFileName){
        File f = new File(sourceFolderPath, accDataFileName);

        List<String> stringDataList = new ArrayList<String>();
        List<Float> accDataList = new ArrayList<Float>();
        List<Float> timeDataList = new ArrayList<Float>();
        List<List<Float>> result = new ArrayList<List<Float>>();

        try {
            FileInputStream fis = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            while ((strLine = br.readLine()) != null) {
                stringDataList.add(strLine);
            }
            br.close();
            in.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int length = stringDataList.size();
        for (int i=0;i<length;i++){
            String line = stringDataList.get(i);
            String[] x = line.split(" ");
            Float acc = Float.valueOf(x[0]);
            Float t = Float.valueOf(x[1]);
            accDataList.add(acc);
            timeDataList.add(t);
        }

        result.add(accDataList);
        result.add(timeDataList);

        return result;
    }

    public static List<List<Float>> getAccelerationPeaks(List<Float> accDataList,List<Float> timeDataList, Float mph, Float mpd){
        List<List<Float>> result = new ArrayList<List<Float>>();
        List<Integer> peakIndexes = detectPeaks.detect_peaks(accDataList,mph,mpd);
        List<Float> peaks = new ArrayList<Float>();
        List<Float> tPeaks = new ArrayList<Float>();

        int lengthPeakIndeces = peakIndexes.size();
        for (int i=0;i<lengthPeakIndeces;i++){
            int index = peakIndexes.get(i);
            peaks.add(accDataList.get(index));
            tPeaks.add(timeDataList.get(index));
        }

        result.add(peaks);
        result.add(tPeaks);
        return result;
    }

    public static File create_folder(File sourceFolderPath, String name){
        File dir = new File(sourceFolderPath + name);
        if(dir.exists() && dir.isDirectory()) {
            return dir;
        }else{
            new File(sourceFolderPath + name).mkdir();
            return dir;
        }
    }

    public static File write_slow_video(File sourceFolderPath, File resultsFolderPath, List<Float> tPeaksList, int[] slowOptions, String motinoType, int real_peak_offset){
        int baseFPS = 24;
        String input_video = "encoded.mp4";

        // get slow options
        int dtGradPre = slowOptions[0];
        int dtGradPost = slowOptions[1];
        int dtSlowPre = slowOptions[2];
        int dtSlowPost = slowOptions[3];

        List<Float> tPeaksAdjustedOffset = new ArrayList<Float>();
        int tPeaksLen = tPeaksList.size();
        for (int i=0;i<tPeaksLen;i++){
            Float time = tPeaksList.get(i);
            time += real_peak_offset;
            tPeaksAdjustedOffset.add(time);
        }

        // outputFolderPath includes results of single run for slow motion
        // it assures that every new slow motion video trial is saved in separate folders
        // outputFolderPath = create_datetime_folder(resultsFolderPath);

        //sourceVideo = VideoCapture(sourceFolderPath+input_video);

        //VideoCapture vc = new VideoCapture();
        //Mat frame = new Mat();

        File videoFile = new File(sourceFolderPath, input_video);

        FrameGrabber videoGrabber = new FFmpegFrameGrabber(videoFile.getAbsoluteFile());
        try {
            videoGrabber.setFormat("mp4");//mp4 for example
            videoGrabber.start();
        } catch (Exception e)
        {
            Log.e("javacv", "Failed to start grabber" + e);
        }
        Frame vFrame = null;
        int i = 0;

        do {
            try {
                vFrame = videoGrabber.grabFrame();

                if(vFrame != null){
                    Log.e("frame: ", Integer.toString(i));

                    if (i%50==0) {
                        AndroidFrameConverter convertToBitmap = new AndroidFrameConverter();
                        Bitmap bitmap = convertToBitmap.convert(vFrame);

                        String path = resultsFolderPath.getAbsolutePath();
                        OutputStream fOut = null;
                        File file = new File(path, "Frame_" + Integer.toString(i) + ".jpg"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
                        fOut = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                        fOut.flush(); // Not really required
                        fOut.close(); // do not forget to close the stream
                    }

                }
                //do your magic here
            } catch (Exception e) {
                Log.e("javacv", "video grabFrame failed: "+ e);
            }
            i+=1;
        }while(vFrame!=null);

        try {
            videoGrabber.stop();
        }catch (Exception e) {
            Log.e("javacv", "failed to stop video grabber", e);
        }

        Log.e("Write: ","finished");











//        Log.e("Write: ",videoFile.getAbsolutePath());
////
////        if(vc.open(videoFile.getAbsolutePath())) {
////            Log.e("Write: ","opened");
////
////            Size size = new Size(vc.get(Videoio.CAP_PROP_FRAME_WIDTH), vc.get(Videoio.CAP_PROP_FRAME_HEIGHT));
////            double fps = vc.get(Videoio.CAP_PROP_FPS);
////            VideoWriter vw = new VideoWriter(resultsFolderPath.getAbsolutePath()+"slowmotion_output.mp4", VideoWriter.fourcc('X', 'V', 'I', 'D'), fps, size, true);
////
////            for (int i=0;i<100;i++) {
////                Log.e("Write: ",Integer.toString(i));
////                if (vc.read(frame)) {
////                    vw.write(frame);
////                }
////            }
////            frame.release();
////            vc.release();
////            vw.release();
////        } else {
////            Log.e("Write: ","else");
////            System.out.println("Failure");
////        }
////        Log.e("Write: ","finished");

        return resultsFolderPath;
    }
}
