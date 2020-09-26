package com.example.smsotp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import com.example.smsotp.server.WebServer;

import java.io.IOException;

public class WebService extends Service {
    private static final String TAG = "SMSOTP_WebService";
    public static MutableLiveData<Boolean> isRunning = new MutableLiveData<>();
    private WebServer webServer;

    @Override
    public void onCreate() {
        isRunning.setValue(true);
        startForeground(1, createNotification());
        // We instantiate web server on background thread so we don't block UI thread
        new Thread(() -> {
            webServer = new WebServer(this, 8080);
            try {
                webServer.start();
                Log.i(TAG, "Web Service started!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        isRunning.setValue(false);
        webServer.stop();
        Log.i(TAG, "Web Service stopped!");
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
    public IBinder onBind(Intent intent) {
        return null;
    }
}
