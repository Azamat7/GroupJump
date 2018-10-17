package com.example.user.groupjump;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class slow_motion_code {
    private static Map<String, int[]> SLOW_OPTIONS_DIC = new HashMap<String, int[]>();
    private static Map<String, int[]> PEAK_OPTIONS_DIC = new HashMap<String, int[]>();
    private static Map<String, String> MOTION_TYPE_DIC = new HashMap<String, String>();
    private static Map<String, Integer> REAL_PEAK_OFFSET_DIC = new HashMap<String, Integer>();
    private static single_camera_tools sct = new single_camera_tools();


    public slow_motion_code() {
        int[] slow_tennis = new int[] {50,75,30,60}; // dtGradPre, dtGradPost, dtSlowPre, dtSlowPost
        int[] slow_golf = new int[] {50,75,30,60};
        int[] slow_baseball = new int[] {50,75,30,60};
        int[] slow_football = new int[] {50,75,30,60};
        SLOW_OPTIONS_DIC.put("tennis", slow_tennis);
        SLOW_OPTIONS_DIC.put("golf", slow_golf);
        SLOW_OPTIONS_DIC.put("baseball", slow_baseball);
        SLOW_OPTIONS_DIC.put("football", slow_football);

        int[] peak_tennis = new int[] {25,50}; // (MPH, MPD)
        int[] peak_golf = new int[] {25,50};
        int[] peak_baseball = new int[] {20,40};
        int[] peak_football = new int[] {76,50};
        PEAK_OPTIONS_DIC.put("tennis", peak_tennis);
        PEAK_OPTIONS_DIC.put("golf", peak_golf);
        PEAK_OPTIONS_DIC.put("baseball", peak_baseball);
        PEAK_OPTIONS_DIC.put("football", peak_football);

        MOTION_TYPE_DIC.put("tennis", "swing");
        MOTION_TYPE_DIC.put("golf", "swing");
        MOTION_TYPE_DIC.put("baseball", "swing");
        MOTION_TYPE_DIC.put("football", "swing");

        REAL_PEAK_OFFSET_DIC.put("tennis", 0);
        REAL_PEAK_OFFSET_DIC.put("golf", 0);
        REAL_PEAK_OFFSET_DIC.put("baseball", 0);
        REAL_PEAK_OFFSET_DIC.put("football", 130);
    }

    public static void process_video(File sourceFolderPath, String action_name, boolean process, boolean plotShow, boolean toAbs, boolean toFlip){
        int[] slow_options = SLOW_OPTIONS_DIC.get(action_name);
        int[] peakOptions = PEAK_OPTIONS_DIC.get(action_name);
        String motion_type = MOTION_TYPE_DIC.get(action_name);
        int real_peak_offset = REAL_PEAK_OFFSET_DIC.get(action_name);

        float mph = (float) peakOptions[0];
        float mpd = (float) peakOptions[1];

        // 1. Read jump stats to get VideoEnd/start UTC and Data start UTC to compute Offset.
        List<jumpStats> jumpStats = sct.readJumpStats(sourceFolderPath);

        // 2. Read acceleration data from txt file.
        String accDataFileName = sct.getAccDataFileName(sourceFolderPath,"Vertical");
        List<List<Float>> accAndTime = sct.readAccelerationData(sourceFolderPath,accDataFileName);
        List<Float> accData = accAndTime.get(0);
        List<Float> timeData = accAndTime.get(1);

        long offset = jumpStats.get(0).getdataOffset();
        Log.i("offset:", String.valueOf(offset));

        // Notes:
        // Use absolute acc values in case if you think it can increase the accuracy of the algorithm
        // For example, sometimes the negative acceleration values are much bigger than the positive one
        // in this case, using absolute values will provide with higher peak values, which in turn lead to higher threshold
        // values. Generally, higher threshold i.e. clear difference between true-positive peaks and false-positive peaks
        // will increase the robustness of the algorithm

        if (toAbs) {
            List<Float> newAccData = new ArrayList<Float>();
            int accDataLength = accData.size();
            for (int i=0;i<accDataLength;i++){
                newAccData.add(Math.abs(accData.get(i)));
            }
            accData = newAccData;
        }

        if (toFlip){
            List<Float> newAccData = new ArrayList<Float>();
            int accDataLength = accData.size();
            for (int i=0;i<accDataLength;i++){
                newAccData.add(-1*accData.get(i));
            }
            accData = newAccData;
        }


        // 3. Get acceleration peaks
        List<List<Float>> accTimePeaks = sct.getAccelerationPeaks(accData,timeData,mph,mpd);
        List<Float> accPeaks = accTimePeaks.get(0);
        List<Float> timePeaks = accTimePeaks.get(1);

        // 4. shift time peaks and time data by the offset in order to synchronize video timeline and data timeline
        List<Float> adjusted_time_peaks = new ArrayList<Float>();
        int peaksLen = accPeaks.size();
        for (int i=0;i<peaksLen;i++){
            adjusted_time_peaks.add(timePeaks.get(i)+offset);
        }

        List<Float> adjusted_time_data = new ArrayList<Float>();
        int timeLen = timeData.size();
        for (int i=0;i<timeLen;i++){
            adjusted_time_data.add(timeData.get(i)+offset);
        }



        //debugging
        StringBuilder builder = new StringBuilder();
        builder.append("original: ");
        int lengthTemp = accPeaks.size();
        for (int i=0;i<lengthTemp;i++){
            builder.append(accPeaks.get(i));
            builder.append(" ");
        }
        for (int i=0;i<lengthTemp;i++){
            builder.append(timePeaks.get(i));
            builder.append(" ");
        }
        Log.e("accPeaks, timePeaks ", builder.toString());

        builder = new StringBuilder();
        builder.append("original: ");
        for (int i=0;i<lengthTemp;i++){
            builder.append(adjusted_time_peaks.get(i));
            builder.append(" ");
        }
        Log.e("acctimePeaks adjusted: ", builder.toString());

        if (process){
            // 5. create ./sm_results/ folder where output slow-mo videos will be located.
            String resultFolder = "/sm_results";
            File sm_results_folder = sct.create_folder(sourceFolderPath,resultFolder);

            // 6. Create slow motion video
            File outputFolderPath = sct.write_slow_video(sourceFolderPath,sm_results_folder,adjusted_time_peaks,slow_options,motion_type,real_peak_offset);

            // 7. save slow_options and peak_options that were used to create the following slow motion video.

        }


    }

}