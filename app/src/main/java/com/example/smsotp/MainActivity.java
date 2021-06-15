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
import android.webkit.WebView;
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
        // Preload the system WebView.
        // WTF I only STUMBLED BY ACCIDENT on this solution to the inflation taking too long
        WebView wv = new WebView(this);

        loadDefaultPreferencesOnFirstBoot();

        if (BuildConfig.DEBUG) {
            final String msg;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                msg = "Server started automatically in debug build variant!";
                startService(new Intent(this, WebService.class));
            } else msg = "Please grant SEND_SMS permission to start server automatically";
            Log.d(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void loadDefaultPreferencesOnFirstBoot() {
        final SharedPreferences setDefaultPrefs = this.getSharedPreferences(
                KEY_HAS_SET_DEFAULT_VALUES, Context.MODE_PRIVATE);
        if (!setDefaultPrefs.getBoolean(KEY_HAS_SET_DEFAULT_VALUES, false)) {
            SharedPreferences defaultAppSp = PreferenceManager.getDefaultSharedPreferences(this);
            // Load the defaults from xml INSIDE this if block because the function modifies the
            // KEY_HAS_SET_DEFAULT_VALUES file to true
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

            // We programmatically put the SIM id in our app's default SharedPrefs file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                final String defaultSubID = String.valueOf(SmsManager.getDefaultSmsSubscriptionId());
                defaultAppSp.edit()
                        .putString(SettingsFragment.KEY_PREF_SIM, defaultSubID)
                        .apply();
            }
            Log.i(TAG, "Loaded default prefs " + defaultAppSp.getAll());
        }
    }
}
