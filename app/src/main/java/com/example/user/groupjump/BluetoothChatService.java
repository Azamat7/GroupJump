/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.user.groupjump;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothChatService {
    // Debugging

    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothChatMulti";

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    private boolean isServer = false;

    private ArrayList<String> mDeviceAddresses;
    private ArrayList<ConnectedThread> mConnThreads;
    private ArrayList<BluetoothSocket> mSockets;
    /**
     * A bluetooth piconet can support up to 7 connections. This array holds 7 unique UUIDs.
     * When attempting to make a connection, the UUID on the client must match one that the server
     * is listening for. When accepting incoming connections server listens for all 7 UUIDs.
     * When trying to form an outgoing connection, the client tries each UUID one at a time.
     */
    private ArrayList<UUID> mUuids;

    // number of clients connected to server
    private int nClients;

    //
    private int isAllTimeReceived = 0;


    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private File pingLatencyFile;
    private static ArrayList<String> pingArray;
    Context mContext;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler, int clients) {
        mContext = context;
        nClients = clients;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        mDeviceAddresses = new ArrayList<String>();
        mConnThreads = new ArrayList<ConnectedThread>();
        mSockets = new ArrayList<BluetoothSocket>();
        mUuids = new ArrayList<UUID>();
        // 7 randomly-generated UUIDs. These must match on both server and client.


        File path = mContext.getExternalFilesDir(null);
        pingLatencyFile = new File(path, "pingLatencyFile.txt");
        pingArray = new ArrayList<>();

        mUuids.add(UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc"));

        // the one we need is above
        mUuids.add(UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"));
        mUuids.add(UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"));
        mUuids.add(UUID.fromString("a81d6504-4536-49ee-a475-7d96d09439e4"));
        mUuids.add(UUID.fromString("aa91eab1-d8ad-448e-abdb-95ebba4a9b55"));
        mUuids.add(UUID.fromString("4d34da73-d0a4-4f40-ac38-917e0a9dee97"));
        mUuids.add(UUID.fromString("5e14d4df-9c8a-4db7-81e4-c937564c86e0"));
    }

    /**
     * Set the current state of the chat connection
     *
     *
     *
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public ArrayList<ConnectedThread> getConnectedThreads() {
        return mConnThreads;
    }

    public int getClients() {
        return nClients;
    }

    public void setIsServer() {
        isServer = true;
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    public int getIsAllTimeReceived() {
        return isAllTimeReceived;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d("BTChatService", "start() is called");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
            Log.d(TAG, "Accept Thread is created");
        }
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {

        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Create a new thread and attempt to connect to each UUID one-by-one.
        for (int i = 0; i < 1; i++) {
        	try {
                mConnectThread = new ConnectThread(device, mUuids.get(i));
                mConnectThread.start();
                setState(STATE_CONNECTING);
        	} catch (Exception e) {
        	}
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        //Commented out all the cancellations of existing threads, since we want multiple connections.
        /*
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
         */

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, device);
        mConnectedThread.start();
        // Add each connected thread to an array
        mConnThreads.add(mConnectedThread);

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
    	// When writing, try to write out to all connected threads
    	for (int i = 0; i < mConnThreads.size(); i++) {
    		try {
                // Create temporary object
                ConnectedThread r;
                // Synchronize a copy of the ConnectedThread
                synchronized (this) {
                    if (mState != STATE_CONNECTED) return;
                    r = mConnThreads.get(i);
                }
                // Perform the write unsynchronized
                r.write(out);
    		} catch (Exception e) {
    		}
    	}
    }

    public void writeToOneThread(byte[] out, ConnectedThread thread) {
        try {
            // Create temporary object
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) {
                if (mState != STATE_CONNECTED) return;
                r = thread;
            }
            // Perform the write unsynchronized
            r.write(out);
        } catch (Exception e) {
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);
        // Commented out, because when trying to connect to all 7 UUIDs, failures will occur
        // for each that was tried and unsuccessful, resulting in multiple failure toasts.
        /*
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        */
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
    	BluetoothServerSocket serverSocket = null;

        public AcceptThread() {
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;
            try {
                Log.e("Accept Thread", "in try");
            	// create server socket
                serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME, mUuids.get(0));

                // Listen twice for Client Socket i.e. call accept() twice
            	for (int i = 0; i < nClients; i++) {

                    socket = serverSocket.accept();
                    Log.d(TAG, "socket accepted, iteration = "+i);
                    if (socket != null) {
                    	String address = socket.getRemoteDevice().getAddress();
	                    mSockets.add(socket);
	                    mDeviceAddresses.add(address);

	                    connected(socket, socket.getRemoteDevice());
                    }
            	}
            } catch (IOException e) {
                Log.e(TAG, "accept() failed", e);
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private UUID tempUuid;


        public ConnectThread(BluetoothDevice device, UUID uuidToTry) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            tempUuid = uuidToTry;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuidToTry);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
            	if (tempUuid.toString().contentEquals(mUuids.get(6).toString())) {
                    connectionFailed();
            	}
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothChatService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        private final BluetoothDevice connectedDevice;
        private long dataStartTimeInMillis;
        private long timeToSend;

        private String incomingData;

        private boolean isMSDReceived = false;
        private boolean isPingReceived = false;

        private ArrayList<String> generalAccDataAlongX;
        private ArrayList<String> generalAccDataAlongY;
        private ArrayList<String> generalAccDataAlongZ;

        private ArrayList<String> accDataArrayList;
        private ArrayList<String> horizontalAccDataArrayList;
        private ArrayList<String> timeDataArrayList;
        private long timeJumpStart;
        private long timeJumpEnd;

        public long getDataStartTime() {
            return dataStartTimeInMillis;
        }

        public String getConnectedDeviceAddress() {
            return connectedDevice.getAddress();
        }

        public long getTargetTime() {
            return timeToSend;
        }

        public long getTimeJumpStart() {
            return timeJumpStart;
        }

        public long getTimeJumpEnd() {
            return timeJumpEnd;
        }

        public ArrayList<String> getAccData(){
            return accDataArrayList;
        }

        public ArrayList<String> getHorAccData() {return horizontalAccDataArrayList;}

        public ArrayList<String> getGeneralAccDataAlongX() {return generalAccDataAlongX;}

        public ArrayList<String> getGeneralAccDataAlongY() {return generalAccDataAlongY;}

        public ArrayList<String> getGeneralAccDataAlongZ() {return generalAccDataAlongZ;}

        public ArrayList<String> getTimeData(){
            return timeDataArrayList;
        }

        public boolean getIsTimeReceived() {
            return isMSDReceived;
        }

        public String getConnectedDeviceName() {
            return connectedDevice.getName();
        }

        public ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            connectedDevice = device;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024 * 1024 * 4];
            byte[] imgBuffer = new byte[1024 * 1024 * 4];
            int pos = 0;
            int bytes;
            incomingData = "";
            String imageData = "";

            int i = 0;
            long startSending = 0;
            int imgSize = 0;
            int sizeOfByteArray = 0;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    if (isServer) {
                        if(!isPingReceived) {

                            // receive ping for data recording start
                            isMSDReceived = false;
                            isPingReceived = true;
                            // Read from the InputStream
                            bytes = mmInStream.read(buffer);
                            String incomingMessage = new String(buffer, 0, bytes);

                            // Send the obtained bytes to the UI Activity
                            Log.e("Incoming message", "Ping has been received! " + incomingMessage);
//                            Toast.makeText(mContext, "First Ping Received!", Toast.LENGTH_SHORT).show();

                            dataStartTimeInMillis = System.currentTimeMillis();

                        }
                        else {
                            // receive string MSD data
                            if (!isMSDReceived) {
                                Log.e("Loop", "in the loop");
                                bytes = mmInStream.read(buffer);
                                incomingData = incomingData + new String(buffer, 0, bytes);
                                if(incomingData.charAt(incomingData.length() - 1) == '#') {
                                    isAllTimeReceived++;

                                    isMSDReceived = true;
                                    Log.e("Incoming data length :", "" + incomingData.length());
                                    convertData();
                                }
                            }
                        }
                    } else {

                        Log.e("Loop", "in the loop");

                        bytes = mmInStream.read(buffer);
                        imageData = imageData + new String(buffer, 0, bytes);

                        if (imageData.charAt(imageData.length() - 1) == '#') {
                            imageData = imageData.substring(0, imageData.length() - 1);
                            Log.e("Resulting String: ", imageData);
                            Bitmap toSave = StringToBitMap(imageData);
                            saveImage(toSave);
                            Log.e("Saved ", "Image!");
                        }
//                        Toast.makeText(mContext, "Image is saved!", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }

        }

        private void convertData() {

            String[] elements = incomingData.split(":");

            String strTargetTime = elements[0];
            String strAccData = elements[1];
            String strHorAccData = elements[2];
            String strGeneralAccDataX = elements[3];
            String strGeneralAccDataY = elements[4];
            String strGeneralAccDataZ = elements[5];
            String strTimeData = elements[6];
            String strJumpStart = elements[7];
            String strJumpEnd = elements[8];
            strJumpEnd = strJumpEnd.substring(0, strJumpEnd.length() - 1);

            timeToSend = Long.valueOf(strTargetTime);
            timeToSend += dataStartTimeInMillis;

            accDataArrayList = new ArrayList<String>(Arrays.asList(strAccData.split(",")));
            horizontalAccDataArrayList = new ArrayList<String>(Arrays.asList(strHorAccData.split(",")));
            generalAccDataAlongX = new ArrayList<String>(Arrays.asList(strGeneralAccDataX.split(",")));
            generalAccDataAlongY = new ArrayList<String>(Arrays.asList(strGeneralAccDataY.split(",")));
            generalAccDataAlongZ = new ArrayList<String>(Arrays.asList(strGeneralAccDataZ.split(",")));
            timeDataArrayList = new ArrayList<String>(Arrays.asList(strTimeData.split(",")));

            timeJumpStart = Long.valueOf(strJumpStart);
            timeJumpStart += dataStartTimeInMillis;

            timeJumpEnd = Long.valueOf(strJumpEnd);
            timeJumpEnd += dataStartTimeInMillis;

            Log.e("saveData", "target Time: " + strTargetTime);
            Log.e("saveData: ", "accData size: " + accDataArrayList.size());
            Log.e("saveData: ", "timeData size: " + timeDataArrayList.size());
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {

                mmOutStream.write(buffer);

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }




        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public void removeThreads(){
        mConnThreads.clear();
    }


    public static String saveImage(Bitmap finalBitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/MSD");
        myDir.mkdirs();
        String fname = "Image" + ".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fname;
    }

    public Bitmap StringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    public void reduceIsAllTimeReceived(int nClients){
        isAllTimeReceived-=nClients;
    }
}
