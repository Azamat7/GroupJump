package com.example.user.groupjump;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import static com.example.user.groupjump.Constants.DEVICE_NAME;
import static com.example.user.groupjump.Constants.TOAST;


/**
 * This activity will be invoked when Camera mode button on the Main activity is pressed.
 * This Activity is used to (1) establish BT server to connect multiple jump devices (clients).
 * In this activity there will be a SPINNER populated with number of devices (jumpers) to connect.
 */


public class ServerConnectionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private Spinner spinner;
    private static final String TAG = "ServConnAct";
    private int nClients;

    // Debugging
    private static final boolean D = true;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    public static BluetoothChatService mServerChatService = null;


    // paired device with which we want to create a connection
    // mBTdevice is the device to which we are bonded.
    BluetoothDevice mBTDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D) Log.e(TAG, "++ ON CREATE ++");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_connection);

        spinner = (Spinner) findViewById(R.id.clients_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.clients_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Turn on BT Device discoverability
        ensureDiscoverable();
        checkBTPermissions();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
//            // Otherwise, setup the chat session
//        }else {
//            if (mClientChatService == null) setupChat();
//        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mServerChatService != null) mServerChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        // setting up the BTChatService
        if (mServerChatService == null) {
            mServerChatService = new BluetoothChatService(this, mHandler, nClients);

        }
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                        switch (msg.arg1) {
                            case BluetoothChatService.STATE_CONNECTED:
                                Log.d("mHandler: ", "connected to "+mConnectedDeviceName);
                                Toast.makeText(getApplicationContext(), "connected to" + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                                break;
                            case BluetoothChatService.STATE_CONNECTING:
                                Log.d("mHandler: ", "state connecting");
                                Toast.makeText(getApplicationContext(), "connected to" + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                                break;
                            case BluetoothChatService.STATE_LISTEN:
                            case BluetoothChatService.STATE_NONE:
                                Log.d("mHandler: ", "state not connected");
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        byte[] writeBuf = (byte[]) msg.obj;
                        // construct a string from the buffer
                        String writeMessage = new String(writeBuf);
                        break;
                    case Constants.MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        if (readMessage.length() > 0) {
                            Toast.makeText(getApplicationContext(), "data received: " + readMessage, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.MESSAGE_TOAST:
                        //if (!msg.getData().getString(TOAST).contains("Unable to connect device")) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                                Toast.LENGTH_SHORT).show();
                        //}
                        break;
                }
        }
    };


    // when item from Spinner is selected
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        int n = Integer.parseInt(spinner.getSelectedItem().toString());
        Log.e("onItemSelected: ",""+n);
        nClients = n;

    }
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.WRITE_EXTERNAL_STORAGE");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    // When nothing from spinner is selected
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
        Toast.makeText(getApplicationContext(), "You shall state how many people jump!", Toast.LENGTH_SHORT).show();
    }


    // When OK button is pressed
    public void okButtonPressed(View view) {


        // here we create server with defined number of clients.


        // mClientChatService.start() will run the Accept Thread and create server sockets.
        setupChat();
        mServerChatService.start();
        mServerChatService.setIsServer();
        Toast.makeText(getApplicationContext(), "waiting for connection", Toast.LENGTH_SHORT).show();

        while (mServerChatService.getConnectedThreads().size() < nClients) {

        }
        Intent videoIntent = new Intent(this, VideoHighFPSActivity.class);
        startActivity(videoIntent);

    }
}
