package com.example.smsotp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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

        Map<String, List<String>> params = session.getParameters();

        if (method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,
                    "Only POST requests are allowed!");
        }

        // We check if the request misses any credential
        if (!params.containsKey("username") || !params.containsKey("password"))
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT,
                    "Missing username or password parameters!");

        // If returned password string is null, username doesn't exist
        String password = database.userDao().getPasswordByUsername(Objects.requireNonNull(params.get(
                "username")).get(0));
        //noinspection ConstantConditions
        if (password == null || !password.equals(params.get("password").get(0)))
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT,
                    "Incorrect username and/or password!");

        Response response;
        if (params.containsKey("phones") && params.containsKey("message")) {
            try {
                //noinspection ConstantConditions
                response = newFixedLengthResponse(Response.Status.OK, mimeTypes().get("json"),
                        sendManySms(params.get("phones"), params.get("message").get(0)));
            } catch (JSONException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "Could not convert params to JSON");
            }
        } else
            response = newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                    "Request query missing phones and/or message parameters!");

        return response;
    }

    private String sendManySms(List<String> phones, String msg) throws JSONException {
        String packageName = Objects.requireNonNull(getClass().getPackage()).getName()
                + ".SMS_SENT" + Thread.currentThread().getId();
        final String[] SENT_IDS = new String[phones.size()];
        for (int i = 0; i < phones.size(); i++) {
            SENT_IDS[i] = packageName + phones.get(i);
        }

        PendingIntent[] sentPIs = new PendingIntent[phones.size()];
        for (int i = 0; i < phones.size(); i++) {
            sentPIs[i] = PendingIntent.getBroadcast(context, 0, new Intent(SENT_IDS[i]), 0);
        }

        final Integer[] resultCodes = new Integer[phones.size()];
        BroadcastReceiver sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for (int i = 0; i < phones.size(); i++) {
                    if (Objects.requireNonNull(intent.getAction()).equals(SENT_IDS[i])) {
                        resultCodes[i] = getResultCode();
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        for (String action : SENT_IDS) intentFilter.addAction(action);
        context.registerReceiver(sentReceiver, intentFilter);

        String[] resultStatuses = new String[phones.size()];
        // It may throw exception if our app doesn't have SEND_SMS permission
        try {
            SmsManager smsManager = SmsManager.getDefault();
            for (int i = 0; i < phones.size(); i++) {
                smsManager.sendTextMessage(phones.get(i), null, msg, sentPIs[i], null);
            }
            // We check every 100ms if onReceive was executed
            // Yes, that's what I came up with...
            for (int i = 0; i < phones.size(); i++) {
                while (resultCodes[i] == null) {
                    Thread.sleep(100);
                }
            }
            for (int i = 0; i < phones.size(); i++) {
                switch (resultCodes[i]) {
                    case Activity.RESULT_OK:
                        resultStatuses[i] = "SMS sent";
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        resultStatuses[i] = "Generic failure";
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        resultStatuses[i] = "No service";
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        resultStatuses[i] = "Null PDU";
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        resultStatuses[i] = "Radio off";
                        break;
                    case 0:
                        resultStatuses[i] = "NULL Result Code!";
                        break;
                    default:
                        resultStatuses[i] = "IDK MAN, default switch";
                }
            }
            StringBuilder stringBuilder = new StringBuilder("SMS_SENT Results: ");
            for (int i = 0; i < phones.size(); i++) {
                stringBuilder.append(resultCodes[i]).append(" ").append(resultStatuses[i]).append("\n");
            }
            Log.d(TAG, stringBuilder.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        context.unregisterReceiver(sentReceiver);

        JSONObject json = new JSONObject();
        json.put("message", msg);
        for (int i = 0; i < phones.size(); i++) {
            JSONObject phoneStatusPair = new JSONObject();
            phoneStatusPair.put("phone", phones.get(i));
            phoneStatusPair.put("status", resultStatuses[i]);
            json.accumulate("results", phoneStatusPair);
        }

        return json.toString();
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
        String packageName = Objects.requireNonNull(getClass().getPackage()).getName();
        final String SENT = packageName + ".SMS_SENT" + Thread.currentThread().getId();

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
        params.remove("password");
        return new JSONObject(params).toString();
    }
}