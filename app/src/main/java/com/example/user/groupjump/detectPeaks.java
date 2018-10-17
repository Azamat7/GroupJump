package com.example.user.groupjump;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

        List<Integer> ind = new ArrayList<Integer>();
        int i = 0;
        while (i<ireLength){
            int peakIndex = ire.get(i);
            float max= peaks.get(i);
            i++;
            while (i<ireLength){
                int index = ire.get(i);
                if ((index-peakIndex)<=50){
                    max = Math.max(max,peaks.get(i));
                    i++;
                }else{
                    break;
                }
            }
            ind.add(peak_index.get(max));
        }

        return ind;
    }
}
