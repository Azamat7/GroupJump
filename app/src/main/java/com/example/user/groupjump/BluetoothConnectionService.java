package com.example.user.groupjump;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BTConnectionService";

    private static final String appName = "MYAPP";

    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    private File pingLatencyFile;

    private static ArrayList<String> pingArray;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
        File path = mContext.getExternalFilesDir(null);
        pingLatencyFile = new File(path, "pingLatencyFile.txt");
        pingArray = new ArrayList<>();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);

                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            try {
                Log.d(TAG, "run: RFCOM server socket start...");

                socket = mmServerSocket.accept();

                Log.d(TAG, "run: RFCOM server socket accepted conncetion.");
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (socket != null) {
                connected(socket, mmDevice);
            }

            Log.i(TAG, "End AcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");

            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG,  "Run mConnectThread ");

            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " + MY_UUID_INSECURE);
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmSocket = tmp;

            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();

                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE);
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");

        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];

            int bytes;

            int i = 0;

            while(true) {
                try {
                    if (i == 0) {
                        VideoHighFPSActivity.isTimeReceived = false;
                        bytes = mmInStream.read(buffer);
                        String incomingMessage = new String(buffer, 0, bytes);
                        Log.d(TAG, "InputStream " + incomingMessage);
                        DataActivity.dataStartTimeInMillis = System.currentTimeMillis();
                        i++;
                    } else {
                        VideoHighFPSActivity.isTimeReceived = true;
                        bytes = mmInStream.read(buffer);
                        String incomingMessage = new String(buffer, 0, bytes);
                        Log.d(TAG, "InputStream " + incomingMessage);
                        DataActivity.timeToSend = Long.valueOf(incomingMessage);
                        DataActivity.timeToSend += DataActivity.dataStartTimeInMillis;
                        i = 0;
                    }
//                    if (i < 1000) {
//                        bytes = mmInStream.read(buffer);
//                        String incomingMessage = new String(buffer, 0, bytes);
//                        Long currentTime = System.currentTimeMillis();
//                        Log.d(TAG, "Time: " + (currentTime - DataActivity.dataStartTimeInMillis));
//
//                        pingArray.add(String.valueOf((currentTime - DataActivity.dataStartTimeInMillis)));
//
//                        DataActivity.dataStartTimeInMillis = System.currentTimeMillis();
//                        String message = "dfgsdafg";
//                        MainActivity.mBluetoothConnectionService.write(message.getBytes(Charset.defaultCharset()));
//                        i++;
//                    }
//
//                    if (i == 1000) {
//                        for (String str : pingArray) {
//                            BufferedWriter out = new BufferedWriter(new FileWriter(pingLatencyFile, true), 1024);
//                            String entry = str + "\n";
//                            out.write(entry);
//                            out.close();
//                        }
//                        i++;
//                        Toast.makeText(MainActivity.context, "File is ready!", Toast.LENGTH_LONG).show();
//                        Log.d(TAG, "File is ready!");
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: Starting.");

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {
        ConnectedThread r;

        Log.d(TAG, "write: Write called.");
        mConnectedThread.write(out);
    }
}
