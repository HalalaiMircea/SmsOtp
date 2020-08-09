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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class SmsOtpService extends Service {
    private static final String TAG = "SMSOTP_Service";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private boolean isRunning;
    private WebServer webServer;

    @Override
    public void onCreate() {
        isRunning = false;
        webServer = new WebServer(this, AppDatabase.getInstance(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            Log.d(TAG, "Service started!");
            createNotification();
            try {
                webServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                Log.d(TAG, "Server running! Point your browsers to http://localhost:8080/");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            Log.w(TAG, "onStartCommand called more than once in the same service session!");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        webServer.stop();
        Log.d(TAG, "Service stopped!");
    }

    private void createNotification() {
        // Required for Oreo (API 26) and greater
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(getText(R.string.service_notifi_title))
                        .setContentText(getText(R.string.service_notifi_msg))
//                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pi)
                        .build();

        startForeground(1, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
