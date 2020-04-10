package com.example.test_ocr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    final String TAG = getClass().getSimpleName();
    Button startBtn;
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBtn = findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission( getApplicationContext(),
                        Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED )
                {
                    Log.d(TAG,"카메라 권한 설정 요청");
                    ActivityCompat.requestPermissions( MainActivity.this, new String[]
                            { Manifest.permission.CAMERA }, 0 );


                } else {

                    Log.d(TAG,"카메라 권한 설정 완료");
                    Intent intent = new Intent(getApplicationContext(),CameraView.class);
                    startActivity(intent);
                }

            }
        });


        // Example of a call to a native method
//        TextView tv = findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
