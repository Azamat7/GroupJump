package com.example.user.groupjump;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;


public class TimeClient extends AsyncTask<String, Void, Boolean> {

    private Context context;
    private Boolean isConnected = false;

    public TimeClient(Context context) {
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(String[] params) {

            try {
                    Socket socket = new Socket();
                    SocketAddress address = new InetSocketAddress(DataActivity.ipAddress, 8800);
                    socket.connect(address, 500);
                    isConnected = socket.isConnected();

                    OutputStream os = socket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    BufferedWriter bw = new BufferedWriter(osw);

                    bw.write(String.valueOf(DataActivity.timeToSend));
                    bw.flush();

                    socket.close();
        } catch (IOException e) {
                e.printStackTrace();
        }

        return isConnected;
}

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(context, "The frame is sent!", Toast.LENGTH_SHORT).show();
        } else {
            new TimeClient(context).execute();
        }
    }
}


