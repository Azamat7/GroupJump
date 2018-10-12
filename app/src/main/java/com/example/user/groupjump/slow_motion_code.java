package com.example.user.groupjump;

import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class slow_motion_code {
    private static Map<String, int[]> SLOW_OPTIONS_DIC = new HashMap<String, int[]>();
    private static Map<String, int[]> PEAK_OPTIONS_DIC = new HashMap<String, int[]>();
    private static Map<String, String> MOTION_TYPE_DIC = new HashMap<String, String>();
    private static Map<String, Integer> REAL_PEAK_OFFSET_DIC = new HashMap<String, Integer>();


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

        int mph = peakOptions[0];
        int mpd = peakOptions[1];

        // 1. Read jump stats to get VideoEnd/start UTC and Data start UTC to compute Offset.
        List<jumpStats> jumpStats = single_camera_tools.readJumpStats(sourceFolderPath);

        // 2. Read acceleration data from txt file.

        long offset = jumpStats.get(0).getdataOffset();
        Log.i("offset:", String.valueOf(offset));
    }

}