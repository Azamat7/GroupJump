package com.example.user.groupjump;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;



public class PingTestActivity extends AppCompatActivity {

    private Button sendPing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping_test);
        sendPing = (Button) findViewById(R.id.sendPingButton);

        sendPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Sending Ping to a Server!";
                byte[] send = message.getBytes();
                ClientConnectionActivity.mClientChatService.write(send);

            }
        });

    }


}
