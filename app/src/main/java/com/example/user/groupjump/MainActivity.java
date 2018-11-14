package com.example.user.groupjump;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    public static final String deviceUniqueID = UUID.randomUUID().toString();

    public static final int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private Button videoButton;
    private Button dataButton;
    private Button ipButton;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    public static Context context;

    private int mFramesNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        videoButton = (Button) findViewById(R.id.videoButton);
        dataButton = (Button) findViewById(R.id.dataButton);
        //ipButton = (Button) findViewById(R.id.ipButton);

        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onVideoButton();
            }
        });
        dataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDataButton();
            }
        });
//        ipButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onIpButton();
//            }
//        });

        File path = getApplicationContext().getExternalFilesDir(null);
        File ipAddress = new File(path, "ipAddress.txt");

        if (!ipAddress.exists()) {
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(ipAddress, true), 1024);
                out.write("");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();

        setupSharedPreferences();
    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("frames_number")){
            mFramesNumber = Integer.valueOf(sharedPreferences.getString("frames_number","240"));
            //Log.e("mFramesNumber: ",Integer.toString(mFramesNumber));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void onVideoButton() {

        Intent serverConnectionIntent = new Intent(this, ServerConnectionActivity.class);
        startActivity(serverConnectionIntent);

    }

    private void onDataButton() {

        Intent clientConnectionIntent = new Intent(this, ClientConnectionActivity.class);
        startActivity(clientConnectionIntent);
    }

//    private void onIpButton() {
//    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.settings_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings){
            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
