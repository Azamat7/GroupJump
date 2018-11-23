package com.example.user.groupjump;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.example.user.groupjump.MainActivity.deviceUniqueID;

public class ClientConnectionActivity extends Activity implements AdapterView.OnItemClickListener {


    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;
    private static final int N_CLIENTS = 1;


    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private TextView mTitle;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private Button btnDiscoverDevices;
    private Button btnEnableDisable_Discoverable;
    private ListView lvNewDevices;
    private Button mConnectLast;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    public static BluetoothChatService mClientChatService = null;

    private DeviceListAdapter mDeviceListAdapter;

    private ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    // paired device with which we want to create a connection
    // mBTdevice is the device to which we are bonded.
    BluetoothDevice mBTDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        setContentView(R.layout.activity_client_connection);

        // setup custom title
        mTitle = (TextView) findViewById(R.id.m_title);

        btnDiscoverDevices = (Button) findViewById(R.id.btnDiscoverDevices);

        //btnEnableDisable_Discoverable = (Button) findViewById(R.id.btnDiscoverable_on_off);
//        btnEnableDisable_Discoverable.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                ensureDiscoverable();
//            }
//        });
        mConnectLast = (Button) findViewById(R.id.connect_last);
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String defaultString = "default";
        final String name = sharedPref.getString(getString(R.string.lastDeviceName),defaultString);
        final String address = sharedPref.getString(getString(R.string.lastDeviceAddress),defaultString);
        if (name.equals("default")){
            mConnectLast.setVisibility(View.INVISIBLE);
        }else {
            mConnectLast.setText("Connect to " + name);
            mConnectLast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

                    //create the bond.
                    //NOTE: Requires API 17+? I think this is JellyBean
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        device.createBond();

                        // connect to the bonded mBTDevice
                        mClientChatService.connect(device);
                    }
                    long timeInitial = System.currentTimeMillis();
                    boolean connected = true;
                    while (mClientChatService.getConnectedThreads().size() < N_CLIENTS) {
                        long timeNow = System.currentTimeMillis();
                        if ((timeNow - timeInitial) > 5000) {
                            connected = false;
                            break;
                        }
                    }
                    if (connected) {
                        Intent dataIntent = new Intent(getApplicationContext(), DataActivity.class);
                        startActivity(dataIntent);
                    } else {
                        Toast.makeText(getApplicationContext(), "Couldn't connect to " + name + ". Please try again.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        lvNewDevices.setOnItemClickListener(ClientConnectionActivity.this);


        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Log.e("BTadapter:","got default bt adapter");

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mClientChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mClientChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mClientChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
//                mClientChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
//        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
//        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
//        mSendButton = (Button) findViewById(R.id.button_send);
//        mSendButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                // Send a message using content of the edit text widget
//                TextView view = (TextView) findViewById(R.id.edit_text_out);
//                String message = view.getText().toString();
//                sendMessage(message);
//            }
//        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mClientChatService = new BluetoothChatService(this, mHandler, N_CLIENTS);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
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
        if (mClientChatService != null) mClientChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

//    private void ensureDiscoverable() {
//        if(D) Log.d(TAG, "ensure discoverable");
//        if (mBluetoothAdapter.getScanMode() !=
//                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            startActivity(discoverableIntent);
//        }
//    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mClientChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }


        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write


            message = deviceUniqueID + ":" + message;


            byte[] send = message.getBytes();
            mClientChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendMessage(message);
                    }
                    if(D) Log.i(TAG, "END onEditorAction");
                    return true;
                }
            };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            mTitle.setText(R.string.title_connected_to + mConnectedDeviceName);

                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    if (readMessage.length() > 0) {
                        mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
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


    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                if (device.getName()==null){
                    return;
                }
                mBTDevices.add(device);
                Log.e("BTdevices: ","device: "+device.getName());
                Log.d(TAG, "mBR3 onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "onItemClick: " + deviceName);
            mBTDevices.get(i).createBond();

            Log.d(TAG, "onItemClick: bonded");
            mBTDevice = mBTDevices.get(i);

            // connect to the bonded mBTDevice
            mClientChatService.connect(mBTDevice);
        }
        long timeInitial = System.currentTimeMillis();
        boolean connected = true;
        while (mClientChatService.getConnectedThreads().size()  < N_CLIENTS) {
            long timeNow = System.currentTimeMillis();
            if ((timeNow-timeInitial)>5000){
                connected = false;
                break;
            }
        }
        if (connected) {
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.lastDeviceAddress),deviceAddress);
            editor.putString(getString(R.string.lastDeviceName),deviceName);
            editor.commit();
            Intent dataIntent = new Intent(this, DataActivity.class);
            startActivity(dataIntent);
        }else{
            Toast.makeText(getApplicationContext(), "Couldn't connect to " + deviceName + ". Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            Log.e("BtnDiscover", "registerReceiver() is called");
        }
        if(!mBluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            Log.e("BtnDiscover", "registerReceiver() is called");
        }
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
}