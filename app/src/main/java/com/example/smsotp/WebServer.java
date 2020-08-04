package com.example.smsotp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    static final String TAG = "WEB_SERVER";
    private Context context;

    public WebServer() {
        super(8080);
        mimeTypes().put("json", "application/json");
    }

    public void setContext(Context context) {
        this.context = context;
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

        //TODO Authenticate users with database

        Response response;
        if (method != Method.POST)
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT,
                    "Only POST is allowed!");

        if (params.containsKey("phone") && params.containsKey("message")) {
            response = newFixedLengthResponse(Response.Status.OK, MIME_TYPES.get("JSON"),
                    sendSms(params.get("phone"), params.get("message")) + "\n" + params);
        } else
            response = newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                    "Request query missing phone or message parameters!\n" + params.toString());

        return response;
    }

    /**
     * Sends intent-based SMS, meaning when the android system broadcasts the sentIntent
     * and deliveryIntent our app detects these broadcasts and does something for each case
     *
     * @param phoneNo Recipient's phone number
     * @param msg     Message to be sent
     */
    private String sendSms(String phoneNo, String msg) {
        /* We add the current thread's id to the action,
         so other receivers on different threads don't pick up our broadcast */
        final String SENT = this.getClass().getPackage().getName() + ".SMS_SENT"
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

        String returnString = null;
        // It may throw exception if our app doesn't have SEND_SMS permission
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, sentPI, null);
            // We check every 200ms if onReceive was executed
            // Yes, that's what I came up with...
            while (resultCode[0] == null) {
                Thread.sleep(200);
            }
            switch (resultCode[0]) {
                case Activity.RESULT_OK:
                    returnString = "SMS sent";
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    returnString = "Generic failure";
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    returnString = "No service";
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    returnString = "Null PDU";
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    returnString = "Radio off";
                    break;
                case 0:
                    returnString = "NULL Result Code!";
                    break;
                default:
                    returnString = "IDK MAN, default switch";
            }
            Log.d("sent SMS Recv", "Code: " + resultCode[0] + "\nMsg: " + returnString);
        } catch (Exception ex) {
            // If an exception happened, we assign the return string the message
            ex.printStackTrace();
            returnString = ex.getMessage();
        }

        context.unregisterReceiver(sentReceiver);   // We clean up
        return returnString;
    }
}