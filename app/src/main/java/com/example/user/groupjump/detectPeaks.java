package com.example.user.groupjump;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class detectPeaks {

    public detectPeaks(){

    }

    public static List<Integer> detect_peaks(List<Float> x, Float mph, Float mpd){
        // find indices of all peaks
        List<Float> dx = new ArrayList<Float>();
        List<Boolean> first = new ArrayList<Boolean>();
        List<Boolean> second = new ArrayList<Boolean>();
        int lengthX = x.size();
        second.add(false);
        for (int i=0; i<(lengthX-1);i++){
            Float val = x.get(i+1)-x.get(i);
            dx.add(val);
            if (val<=0){
                first.add(true);
                second.add(false);
            }else{
                first.add(false);
                second.add(true);
            }
        }
        first.add(true);

        List<Integer> ire = new ArrayList<Integer>();

        int ireLength = first.size();
        for (int i=0;i<ireLength;i++){
            if (first.get(i) && second.get(i)){
                // first and last values of x cannot be peaks
                if (i==0 || i==(x.size()-1)){
                    continue;
                }else if (x.get(i)<mph){ // remove peaks < minimum peak height
                    continue;
                }
                ire.add(i);
            }
        }

        // remove peaks in range of mpd
        Map<Float, Integer> peak_index = new HashMap<Float, Integer>();

        List<Float> peaks = new ArrayList<Float>();
        ireLength = ire.size();
        for (int i=0;i<ireLength;i++){
            int index = ire.get(i);
            Float peak = x.get(index);
            peaks.add(peak);
            peak_index.put(peak,index);
        }

        Arrays.sort(peaks.toArray(), Collections.reverseOrder());

        List<Float> newPeaks = new ArrayList<Float>();
        while (newPeaks!=peaks){
            int peaksLength = peaks.size();
            for (int i=0;i<peaksLength;i++) {
                Float peak = peaks.get(i);
                List<Integer> eliminate = new ArrayList<Integer>();
                for (int j=0;j<peaksLength;j++){
                    Float temp = peaks.get(j);
                    if (((peak_index.get(temp)-peak_index.get(peak))*(peak_index.get(temp)-peak_index.get(peak)))<(mpd*mpd)){
                        eliminate.add(j);
                    }
                }
                if (eliminate.size()>0){
                    newPeaks = new ArrayList<Float>();
                    for (int j=0;j<peaksLength;j++){
                        int eliminateLength = eliminate.size();
                        boolean check = true;
                        for (int k=0;k<eliminateLength;k++){
                            if (j==eliminate.get(k)){
                                check = false;
                            }
                        }
                        if (check){
                            newPeaks.add(peaks.get(j));
                        }
                    }
                    peaks = newPeaks;
                    break;
                }
            }
        }

        List<Integer> ind = new ArrayList<Integer>();
        int peaksLength = peaks.size();
        for (int i=0;i<peaksLength;i++){
            ind.add(peak_index.get(peaks.get(i)));
        }
        Arrays.sort(ind.toArray());
        return ind;
    }
}
