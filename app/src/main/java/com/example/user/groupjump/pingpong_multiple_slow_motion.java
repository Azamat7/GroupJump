package com.example.user.groupjump;

import android.text.TextUtils;
import android.util.Log;

import org.bytedeco.javacpp.presets.opencv_core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by sanzhbakh on 11/09/2018.
 */

public class pingpong_multiple_slow_motion {

    private static single_camera_tools sct = new single_camera_tools();

    public pingpong_multiple_slow_motion() {

    }

    public static void process_video(File sourceFolderPath){
        String input_video = "encoded.mp4";
        File videoFile = new File(sourceFolderPath, input_video);

        int[] PEAK_OPTIONS = new int[] {25,50}; // (MPH, MPD)
        int[] SLOW_OPTIONS = new int[] {40,40,50,20}; // (dtGradPre, dtGradPost, dtSlowPre, dtSlowPost) Milliseconds
        String motionType = "swing";
        int REAL_PEAK_OFFSET = 0;

        List<Float> t_peaks = plot_multiple_phone_data(sourceFolderPath,"Vertical",PEAK_OPTIONS);

        String resultFolder = "/sm_results";
        File sm_results_folder = sct.create_folder(sourceFolderPath,resultFolder);
        //File outputFolderPath = sct.write_slow_video(sourceFolderPath,sm_results_folder,t_peaks,SLOW_OPTIONS,motionType,REAL_PEAK_OFFSET);


    }

    private static List<Float> plot_multiple_phone_data(File sourceFolderPath, String acc_data_type, int[] peak_options){
        float mph = peak_options[0];
        float mpd = peak_options[1];

        List<jumpStats> jumpStats = sct.readJumpStats(sourceFolderPath);
        List<Long> phone_offsets = new ArrayList<Long>();
        List<String> phone_names = new ArrayList<String>();
        List<String> acc_data_names = new ArrayList<String>();
        List<List<List<Float>>> msd_pairs = new ArrayList<List<List<Float>>>();
        List<Float> t_peaks = new ArrayList<Float>();

        for (int i=0; i<jumpStats.size(); i++){
            long offset = jumpStats.get(i).getdataOffset();
            phone_offsets.add(offset);
            String phone_tag = jumpStats.get(i).getdeviceID();
            phone_names.add(phone_tag);
            String acc_data_filename = sct.getAccDataFileName(sourceFolderPath,phone_tag,acc_data_type);
            acc_data_names.add(acc_data_filename);
            List<List<Float>> accTimeData = sct.readAccelerationData(sourceFolderPath,acc_data_filename);
            List<Float> acc_data = accTimeData.get(0);
            List<Float> time_data = accTimeData.get(0);
            msd_pairs.add(accTimeData);

            List<List<Float>> accTimePeaks = sct.getAccelerationPeaks(acc_data,time_data,mph,mpd);
            List<Float> accPeaks = accTimePeaks.get(0);
            List<Float> timePeaks = accTimePeaks.get(1);

            List<Float> adjusted_time_peaks = new ArrayList<Float>();
            int peaksLen = accPeaks.size();
            for (int j=0;j<peaksLen;j++){
                adjusted_time_peaks.add(timePeaks.get(j)+offset);
            }

            t_peaks.addAll(adjusted_time_peaks);

            Log.e("phone_tag: ", phone_tag);
            Log.e("offset", String.valueOf(offset));
            StringBuilder builder = new StringBuilder();
            int lengthTemp = adjusted_time_peaks.size();
            for (int j=0;j<lengthTemp;j++){
                builder.append(adjusted_time_peaks.get(j));
                builder.append(" ");
            }
            Log.e("adjusted_time_peaks: ",builder.toString());

            builder = new StringBuilder();
            lengthTemp = timePeaks.size();
            for (int j=0;j<lengthTemp;j++){
                builder.append(timePeaks.get(j));
                builder.append(" ");
            }
            Log.e("time_peaks: ",builder.toString());


//            List<Float> adjusted_time_data = new ArrayList<Float>();
//            int timeLen = time_data.size();
//            for (int j=0;j<timeLen;j++){
//                adjusted_time_data.add(time_data.get(j)+offset);
//            }
        }

        Collections.sort(t_peaks);
        StringBuilder builder = new StringBuilder();
        builder.append("original: ");
        int lengthTemp = t_peaks.size();
        for (int i=0;i<lengthTemp;i++){
            builder.append(t_peaks.get(i));
            builder.append(" ");
        }
        Log.e("t_peaks: ",builder.toString());
        return t_peaks;
    }

























}
