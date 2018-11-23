package com.example.user.groupjump;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

public class ImagePreviewActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        imageView = (ImageView) findViewById(R.id.imagePreview);

        imageView.setImageBitmap(VideoHighFPSActivity.action);

//        Bundle bundle = getIntent().getExtras();
//        String savedImage = bundle.getString("savedImage");
//
//        String photoPath = Environment.getExternalStorageDirectory()+"/saved_images/" + savedImage;
//
//        File file = new File(photoPath);
//
//        if (file.exists()) {
//            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
//
//            imageView.setImageBitmap(myBitmap);
//        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        View decorView = getWindow().getDecorView();
//        if(hasFocus) {
//            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
//        }
//    }
}
