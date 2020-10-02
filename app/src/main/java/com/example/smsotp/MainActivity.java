package com.example.smsotp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.example.smsotp.databinding.ActivityMainBinding;
import com.example.smsotp.ui.SettingsFragment;

import static androidx.preference.PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES;

/**
 * Effectively, the application's main entry point
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SMSOTP_MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityMainBinding.inflate(getLayoutInflater()).getRoot());

        loadDefaultSharedPrefsOnFirstBoot();

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

    private void loadDefaultSharedPrefsOnFirstBoot() {
        final SharedPreferences isDefaultSetSP = this.getSharedPreferences(
                KEY_HAS_SET_DEFAULT_VALUES, Context.MODE_PRIVATE);
        // We programmatically set the SIM id in our app's default SharedPrefs file
        if (!isDefaultSetSP.getBoolean(KEY_HAS_SET_DEFAULT_VALUES, false)) {
            SharedPreferences defaultAppSp = PreferenceManager.getDefaultSharedPreferences(this);
            final String defaultSubID;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                defaultSubID = String.valueOf(SmsManager.getDefaultSmsSubscriptionId());
            } else defaultSubID = "0";
            defaultAppSp.edit()
                    .putString(SettingsFragment.KEY_PREF_SIM, defaultSubID)
                    .apply();
            // We set the rest of defaults from preferences.xml
            // Be careful not to override our programmatic default from above!
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
            Log.i(TAG, "Loaded default prefs " + defaultAppSp.getAll());
        }
    }
}
