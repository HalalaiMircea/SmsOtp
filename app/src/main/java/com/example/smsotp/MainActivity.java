package com.example.smsotp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smsotp.databinding.ActivityMainBinding;

/**
 * Effectively, the application's main entry point
 */
public class MainActivity extends AppCompatActivity {
    static final String TAG = "SMSOTP_MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityMainBinding.inflate(getLayoutInflater()).getRoot());

        if (BuildConfig.DEBUG)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Server started automatically in debug build variant!");
                startService(new Intent(this, WebService.class));
            } else {
                final String msg = "Please grant SEND_SMS permission to start server automatically";
                Log.d(TAG, msg);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
    }
}
