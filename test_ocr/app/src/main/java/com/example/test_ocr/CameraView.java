package com.example.test_ocr;


import android.app.Activity;
import android.os.Bundle;
import android.view.TextureView;

import androidx.appcompat.app.AppCompatActivity;

public class CameraView extends AppCompatActivity{
    private TextureView cameraTextureView;
    private Preview preview;

    Activity CameraView = this;

    private static final String TAG = "CAMERAVIEW";
    static final int REQUEST_CAMERA = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraTextureView = (TextureView) findViewById(R.id.cameraTextureView);
        preview = new Preview(this,cameraTextureView);
    }

    @Override
    protected void onResume(){
        super.onResume();
        preview.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        preview.onPause();
    }

}




