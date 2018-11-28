package com.example.user.groupjump;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class ImagePreviewActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        imageView = (ImageView) findViewById(R.id.imagePreview);

        imageView.setImageBitmap(VideoHighFPSActivity.action);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

}
