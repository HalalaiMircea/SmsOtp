package com.example.smsotp;

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

        // Required for Marshmallow (API 23) and greater
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(SEND_SMS) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{SEND_SMS}, 10);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy was called!");
        if (isFinishing()) Log.d(TAG, "Activity closed!");
    }
}