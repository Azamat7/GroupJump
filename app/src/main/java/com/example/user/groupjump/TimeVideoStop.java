package com.example.user.groupjump;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;


public class TimeVideoStop extends AsyncTask<String, Void, String> {

    public static final int TIMEOUT = 10;
    public static final String TIME_SERVER = "pool.ntp.org";
    private long timeFromServer;
    private Activity activity;

    public TimeVideoStop(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected String doInBackground(String... urls) {
//        NTPUDPClient timeClient = new NTPUDPClient();
//        InetAddress inetAddress = null;
//        try {
//            inetAddress = InetAddress.getByName(TIME_SERVER);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
//        TimeInfo timeInfo = null;
//        try {
//            timeInfo = timeClient.getTime(inetAddress);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        //long returnTime = timeInfo.getReturnTime();   //local device time
//        long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time
//        timeFromServer = returnTime;
//
//        return null;

        SntpClient client = new SntpClient();

        long start = System.currentTimeMillis();
        while (!client.requestTime("1.pool.ntp.org", TIMEOUT)) {
        }
        long end = System.currentTimeMillis();
        timeFromServer = client.getNtpTime() - (end - start);
        return null;
    }

    @Override
    protected void onPostExecute(String feed) {
        VideoActivity.videoStopTimeInMillis = timeFromServer;
        Toast.makeText(activity, "Stop time received", Toast.LENGTH_SHORT).show();
        new TimeServer(activity).execute();
//        Toast.makeText(context, "Worked: " + VideoActivity.videoStopTimeInMillis, Toast.LENGTH_SHORT).show();
    }
}