package com.example.user.groupjump;

import android.app.Activity;
import android.hardware.camera2.TotalCaptureResult;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class single_camera_tools {
    private static int videoDuration;

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
        videoDuration = Integer.valueOf(row[4].replaceAll("\\s",""));
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
        int baseFPS = 22;
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

        File videoFile = new File(sourceFolderPath, input_video);

        final FrameGrabber videoGrabber = new FFmpegFrameGrabber(videoFile.getAbsoluteFile());
        try {
            videoGrabber.setFormat("mp4");//mp4 for example
            videoGrabber.start();
        } catch (Exception e)
        {
            Log.e("javacv", "Failed to start grabber" + e);
        }

        double sourceFPS = ((FFmpegFrameGrabber) videoGrabber).getVideoFrameRate();
        Log.e("Frame Rate: ", String.valueOf(sourceFPS));


        //int nTotalFrameSource = i;
        Double x = videoDuration*sourceFPS/1000;
        final int nTotalFrameSource = x.intValue();
        Log.e("Frame Number: ", Integer.toString(nTotalFrameSource));

        //if (motinoType.equals("swing")){
        List<List<Integer>> framesIndices = get_sm_frames_swing(tPeaksAdjustedOffset,slowOptions,nTotalFrameSource,sourceFPS,baseFPS);
        final List<Integer> baseFramesIndices = framesIndices.get(0);
        final List<Integer> slowFramesIndices = framesIndices.get(1);
        final List<Integer> gradStartFramesIndices = framesIndices.get(2);
        final List<Integer> gradEndFramesIndices = framesIndices.get(3);
        //}
        Log.e("Frames: ",Integer.toString(baseFramesIndices.size()+slowFramesIndices.size()+gradStartFramesIndices.size()+gradEndFramesIndices.size()));

        for (int z=0;z<gradStartFramesIndices.size();z++){
            Log.e("GradStartFrameIndices",Integer.toString(gradStartFramesIndices.get(z)));
        }

        final File resultsFolder = resultsFolderPath;





        class writeVideo extends AsyncTask<Void,String,Void>{
            @Override
            protected Void doInBackground(Void... voids) {
                File file = new File(resultsFolder, "test.mp4");
                Frame vFrame = null;
                int i = 0;
                try {
                    FrameRecorder recorder = new FFmpegFrameRecorder(file,videoGrabber.getImageWidth(),videoGrabber.getImageHeight());
                    recorder.setFrameRate(24);
                    recorder.setVideoBitrate(100000000);
                    recorder.start();
                    int ratio = 10;
                    do {
                        try {
                            vFrame = videoGrabber.grabFrame();
                            if(vFrame != null){
                                if (baseFramesIndices.contains(i) || slowFramesIndices.contains(i) || gradStartFramesIndices.contains(i) || gradEndFramesIndices.contains(i)){
                                    try{
                                        int currentRatio = Math.round(i*100/nTotalFrameSource);
                                        if (currentRatio>ratio) {
                                            String message = "Completion: ";
                                            message += Integer.toString(ratio);
                                            message += " %";
                                            publishProgress(message);
                                            ratio+=10;
                                        }
                                        Log.e("Frame written:",Integer.toString(i));
                                        recorder.record(vFrame);
                                    }catch (Exception e){
                                        Log.e("Not written: ", e.toString());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e("javacv", "video grabFrame failed: "+ e);
                        }
                        i+=1;
                    }while(vFrame!=null);
                    recorder.stop();
                    try {
                        videoGrabber.stop();
                    }catch (Exception e) {
                        Log.e("javacv", "failed to stop video grabber", e);
                    }
                }catch(Exception e){
                    Log.e("encoder: ",e.toString());
                } finally {
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
                VideoHighFPSActivity.videoWritingToast(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                VideoHighFPSActivity.videoWritingToast("Video writing finished");
            }
        }

        writeVideo task = new writeVideo();
        task.execute();



        return resultsFolderPath;
    }

    private static List<List<Integer>> get_sm_frames_swing(List<Float> tPeaksList,int[] slowOptions,int nTotalFrameSources, double sourceFPS, int baseFPS ){
        int desiredSlowFactor = 10;
        List<List<Integer>> frames = new ArrayList<List<Integer>>();

        //slowOptions is a tuple containing dtGradPre, dtGradPost, dtSlowPre, dtSlowPost values in milliseconds
        int dtGradPre = slowOptions[0];
        int dtGradPost = slowOptions[1];
        int dtSlowPre = slowOptions[2];
        int dtSlowPost = slowOptions[3];

        int maxSlowFactor = (int) sourceFPS/baseFPS;
        int tVideoEnd = (int) (nTotalFrameSources/sourceFPS)*1000;

        if (desiredSlowFactor>maxSlowFactor){
            desiredSlowFactor = maxSlowFactor;
        }

        int nMotions = tPeaksList.size();
        List<Integer> edges = new ArrayList<Integer>();
        edges.add(0);

        // iterate over peaks to get edges.
        if (dtSlowPre >= 0){
            for (int p=0;p<nMotions;p++){
                int peakIndex = Math.round(tPeaksList.get(p));

                int edgeSlowStart = peakIndex - dtSlowPre;
                int edgeSlowEnd = peakIndex + dtSlowPost;
                int edgeGradStart = edgeSlowStart - dtGradPre;
                int edgeGradEnd = edgeSlowEnd + dtGradPost;

                edges.add(edgeGradStart);
                edges.add(edgeSlowStart);
                edges.add(peakIndex);
                edges.add(edgeSlowEnd);
                edges.add(edgeGradEnd);
            }
        }

        edges.add(tVideoEnd);

        // edgePairs contains all edges including baseEdges, gradEdges, and slowEdges.
        List<List<Integer>> edgePairs = new ArrayList<List<Integer>>();
        for (int p=0;p<(edges.size()-1);p++){
            List<Integer> edge = new ArrayList<Integer>();
            edge.add(edges.get(p));
            edge.add(edges.get(p+1));
            edgePairs.add(edge);
        }

        List<List<List<Integer>>> edgePairsSpecified = getSpecificEdges(edgePairs,5);
        List<List<Integer>> baseEdges = edgePairsSpecified.get(0);
        List<List<Integer>> gradStartEdges = edgePairsSpecified.get(1);
        List<List<Integer>> slowEdgesPre = edgePairsSpecified.get(2);
        List<List<Integer>> slowEdgesPost = edgePairsSpecified.get(3);
        List<List<Integer>> gradEndEdges = edgePairsSpecified.get(4);

        // here we collect all frame indices, by running slowTimeInterval for base frames and slow frames

        List<Integer> baseFramesList = new ArrayList<Integer>();
        for (int i=0;i<baseEdges.size();i++){
            List<Integer> temp = slowTimeInterval(baseEdges.get(i).get(0),baseEdges.get(i).get(1),baseFPS,sourceFPS,1);
            baseFramesList.addAll(temp);
        }

        List<Integer> gradStartFramesList = new ArrayList<Integer>();
        for (int i=0;i<gradStartEdges.size();i++){
            List<Integer> temp = getGradientFrameIndices(gradStartEdges.get(i).get(0),gradStartEdges.get(i).get(1),sourceFPS);
            gradStartFramesList.addAll(temp);
        }

        List<Integer> slowPreFramesList = new ArrayList<Integer>();
        for (int i=0;i<slowEdgesPre.size();i++){
            List<Integer> temp = slowTimeInterval(slowEdgesPre.get(i).get(0),slowEdgesPre.get(i).get(1),baseFPS,sourceFPS,desiredSlowFactor);
            slowPreFramesList.addAll(temp);
        }

        List<Integer> slowPostFramesList = new ArrayList<Integer>();
        for (int i=0;i<slowEdgesPost.size();i++){
            List<Integer> temp = slowTimeInterval(slowEdgesPost.get(i).get(0),slowEdgesPost.get(i).get(1),baseFPS,sourceFPS,desiredSlowFactor);
            slowPostFramesList.addAll(temp);
        }

        List<Integer> gradEndFramesList = new ArrayList<Integer>();
        for (int i=0;i<gradEndEdges.size();i++){
            List<Integer> temp = getGradientFrameIndices(gradEndEdges.get(i).get(0),gradEndEdges.get(i).get(1),sourceFPS);
            gradEndFramesList.addAll(temp);
        }

        List<Integer> slowFramesList = new ArrayList<Integer>();
        slowFramesList.addAll(slowPreFramesList);
        slowFramesList.addAll(slowPostFramesList);

        frames.add(baseFramesList);
        frames.add(slowFramesList);
        frames.add(gradStartFramesList);
        frames.add(gradEndFramesList);

        return frames;
    }

    private static List<List<List<Integer>>> getSpecificEdges(List<List<Integer>> edgePairs, int nPhases){
        // create empty output list
        List<List<List<Integer>>> outputList = new ArrayList<List<List<Integer>>>();
        for (int i=0;i<nPhases;i++){
            List<List<Integer>> temp = new ArrayList<List<Integer>>();
            for (int j=i;j<edgePairs.size();j+=nPhases){
                temp.add(edgePairs.get(j));
            }
            outputList.add(temp);
        }
        return outputList;
    }

    private static List<Integer> slowTimeInterval(int tStart, int tEnd, double baseFPS, double sourceFps, int slowFactor){
        Double a = (tStart*sourceFps/1000);
        int frameSlowStart = a.intValue();
        Double b = (tEnd*sourceFps/1000);
        int frameSlowEnd = b.intValue();

        float nSourceFrames = frameSlowEnd - frameSlowStart;
        float nBaseFrames = (float)(nSourceFrames * (baseFPS/sourceFps));

        float nSlowFrames = nBaseFrames * slowFactor;

        // step: required for range step in picking frames for slow motion from source frames.
        List<Integer> indices = new ArrayList<Integer>();
        if (nSlowFrames!=0){
            float step = nSourceFrames/nSlowFrames;
            for (int j=frameSlowStart;j<frameSlowEnd;j+=step){
                indices.add(j);
            }
        }
        return indices;
    }

    private static List<Integer> getGradientFrameIndices(int tGradStart, int tGradEnd, double sourceFPS){
        int[] slowRatesList = new int[] {2,3,4,5,7};
        boolean isRising = true;
        int baseFPS = 24;

        int nSlowRates = slowRatesList.length;
        // +1 is to have same amount of intervals as nSlowRates
        List<Integer> gradTransitionPoints = new ArrayList<Integer>();
        float step = (tGradEnd-tGradStart)/(nSlowRates+1);
        for (int i=0; i<(nSlowRates+1); i++){
            int index = Math.round(tGradStart + i*(step));
            gradTransitionPoints.add(index);
        }

        List<List<Integer>> edgePairs = new ArrayList<List<Integer>>();
        for (int p=0;p<(gradTransitionPoints.size()-1);p++){
            List<Integer> edge = new ArrayList<Integer>();
            edge.add(gradTransitionPoints.get(p));
            edge.add(gradTransitionPoints.get(p+1));
            edgePairs.add(edge);
        }

        List<Integer> gradIndices = new ArrayList<Integer>();

        Log.e("sdf","YAY");
        for (int k=0; k<edgePairs.size();k++){
            Log.e("edgePairs.get(k).get(0)",Integer.toString(edgePairs.get(k).get(0)));
            Log.e("edgePairs.get(k).get(1)",Integer.toString(edgePairs.get(k).get(1)));
            List<Integer> temp = slowTimeInterval(edgePairs.get(k).get(0),edgePairs.get(k).get(1),baseFPS,sourceFPS,slowRatesList[k]);
            Log.e("temp.size()",Integer.toString(temp.size()));
            gradIndices.addAll(temp);
        }

        return gradIndices;
    }

}
