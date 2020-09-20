package com.example.smsotp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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
    }
}
