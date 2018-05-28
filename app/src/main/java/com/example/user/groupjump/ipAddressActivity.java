package com.example.user.groupjump;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ipAddressActivity extends AppCompatActivity {

    private EditText ipAddressText;
    private Button sendIpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_address);

        ipAddressText = (EditText) findViewById(R.id.ipAddressText);
        sendIpButton = (Button) findViewById(R.id.sendIpButton);

        sendIpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onIpButton();
            }
        });
    }

    private void onIpButton() {
        Toast.makeText(this, "IP address is rewritten", Toast.LENGTH_SHORT).show();
        String ipAddress = ipAddressText.getText().toString();
        File path = getApplicationContext().getExternalFilesDir(null);
        File ipAddressFile = new File(path, "ipAddress.txt");
        ipAddressFile.delete();
        File ipAddressFileNew = new File(path, "ipAddress.txt");
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(ipAddressFileNew, true), 1024);
            out.write(ipAddress);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ipAddressText.setText("");
    }
}
