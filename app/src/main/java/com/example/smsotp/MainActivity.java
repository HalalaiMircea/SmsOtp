package com.example.smsotp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static android.Manifest.permission.SEND_SMS;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "SMSOTP_MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        // Required for Marshmallow (API 23) and greater
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(SEND_SMS) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{SEND_SMS}, 10);
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Server started automatically in debug build variant!");
            startService(new Intent(this, SmsOtpService.class));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 10) {// If request is cancelled, the result arrays are empty.
            for (int i = 0; i < permissions.length; i++) {
                // If user denies permission, we kill the app including the service
                if (permissions[i].equals(SEND_SMS) && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Log.d(TAG, "Permission " + permissions[i] + " denied by user!");
                    Process.killProcess(Process.myPid());
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy was called!");
        if (isFinishing()) Log.d(TAG, "Activity closed!");
    }
}