package com.example.smsotp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    private static final String TAG = "SMSOTP_WebServer";
    public static int port = 8080;
    private Context context;
    private AppDatabase database;

    public WebServer(Context context, AppDatabase database, int port) {
        super(port);
        WebServer.port = port;
        this.context = context;
        this.database = database;
        mimeTypes().put("json", "application/json");
    }

    public WebServer(Context context, AppDatabase database) {
        this(context, database, 8080);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        Method method = session.getMethod();
        if (Method.PUT == method || Method.POST == method) {
            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (ResponseException re) {
                return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }

        Map<String, String> params = session.getParms();

        if (method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,
                    "Only POST requests are allowed!");
        }

        // We check if the request misses any credential
        if (!params.containsKey("username") || !params.containsKey("password"))
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT,
                    "Missing username or password parameters!");

        // If returned password string is null, username doesn't exist
        String password = database.userDao().getPasswordByUsername(params.get("username"));
        if (password == null || !password.equals(params.get("password")))
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT,
                    "Incorrect username and/or password!");

        Response response;
        if (params.containsKey("phone") && params.containsKey("message")) {
            response = newFixedLengthResponse(Response.Status.OK, mimeTypes().get("json"),
                    sendSms(params));
        } else
            response = newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                    "Request query missing phone and/or message parameters!");

        return response;
    }

    /**
     * Sends intent-based SMS, meaning when the android system broadcasts the sentIntent
     * and deliveryIntent our app detects these broadcasts and does something for each case
     *
     * @param params - request parameters
     * @return string made from JSONObject with request parameters and status
     */
    private String sendSms(Map<String, String> params) {
        /* We add the current thread's id to the action,
         so other receivers on different threads don't pick up our broadcast */
        final String SENT = Objects.requireNonNull(getClass().getPackage()).getName() + ".SMS_SENT"
                + Thread.currentThread().getId();

        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);

        final Integer[] resultCode = new Integer[1];
        BroadcastReceiver sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                resultCode[0] = getResultCode();
            }
        };
        context.registerReceiver(sentReceiver, new IntentFilter(SENT));

        String resultStatus;
        // It may throw exception if our app doesn't have SEND_SMS permission
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(params.get("phone"), null, params.get("message"), sentPI, null);
            // We check every 200ms if onReceive was executed
            // Yes, that's what I came up with...
            while (resultCode[0] == null) {
                Thread.sleep(200);
            }
            switch (resultCode[0]) {
                case Activity.RESULT_OK:
                    resultStatus = "SMS sent";
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    resultStatus = "Generic failure";
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    resultStatus = "No service";
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    resultStatus = "Null PDU";
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    resultStatus = "Radio off";
                    break;
                case 0:
                    resultStatus = "NULL Result Code!";
                    break;
                default:
                    resultStatus = "IDK MAN, default switch";
            }
            Log.d(TAG, "SMS_SENT Result Code: " + resultCode[0] + "\nMsg: " + resultStatus);
        } catch (Exception ex) {// If an exception happened, we assign the message
            ex.printStackTrace();
            resultStatus = ex.getMessage();
        }

        context.unregisterReceiver(sentReceiver);   // We clean up

        params.put("status", resultStatus);
        return new JSONObject(params).toString();
    }
}