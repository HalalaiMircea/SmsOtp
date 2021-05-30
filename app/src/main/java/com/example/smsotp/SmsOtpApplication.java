package com.example.smsotp;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class SmsOtpApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static Context getAppContext() {
        return SmsOtpApplication.context;
    }

    public void onCreate() {
        super.onCreate();
        SmsOtpApplication.context = getApplicationContext();
    }
}
