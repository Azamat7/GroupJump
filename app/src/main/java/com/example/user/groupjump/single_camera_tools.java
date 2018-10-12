package com.example.user.groupjump;


import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
}
