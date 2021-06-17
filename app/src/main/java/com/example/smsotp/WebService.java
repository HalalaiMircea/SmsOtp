package com.example.smsotp;

import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.example.smsotp.server.WebServer;

import java.io.IOException;

import static com.example.smsotp.ui.SettingsFragment.KEY_PREF_PORT;
import static com.example.smsotp.ui.SettingsFragment.KEY_PREF_SIM;

public class WebService extends Service {
    private static final String TAG = "SMSOTP_WebService";
    public static MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    public static SmsManager smsManager;
    private WebServer webServer;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate() {
        startForeground(1, createNotification());

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Acquire the right SmsManager for the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            int subId = Integer.parseInt(sharedPrefs.getString(KEY_PREF_SIM, "0"));
            smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
        } else smsManager = SmsManager.getDefault();

        // Start the server
        int port = Integer.parseInt(sharedPrefs.getString(KEY_PREF_PORT, "8080"));
        webServer = new WebServer(this, port);
        try {
            webServer.start();
            isRunning.setValue(true);
            Log.i(TAG, "Web Service started!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Notification createNotification() {
        final String CHANNEL_ID = "ForegroundServiceChannel";

        // Required for Oreo (API 26) and greater
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.service_notifi_title))
                .setContentText(getText(R.string.service_notifi_msg))
                .setSmallIcon(R.drawable.ic_baseline_web_24)
                .setContentIntent(pi)
                .build();
    }

    @Override
    public void onDestroy() {
        isRunning.setValue(false);
        webServer.stop();
        Log.i(TAG, "Web Service stopped!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
