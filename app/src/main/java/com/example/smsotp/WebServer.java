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
import java.util.Arrays;
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

    /**
     * Sends intent-based SMS, meaning when the android system broadcasts the sentIntent our app detects
     * these broadcasts and does something for each case
     *
     * @param phones List of String phone numbers
     * @param msg    Text message to send
     * @return string made from JSONObject with request parameters and status
     */
    private String sendManySms(List<String> phones, String msg) throws JSONException {
        String packageName = Objects.requireNonNull(getClass().getPackage()).getName()
                + ".SMS_SENT" + Thread.currentThread().getId();
        final String[] SENT_IDS = new String[phones.size()];
        for (int i = 0; i < phones.size(); i++) SENT_IDS[i] = packageName + i;

        PendingIntent[] sentPIs = new PendingIntent[phones.size()];
        for (int i = 0; i < phones.size(); i++)
            sentPIs[i] = PendingIntent.getBroadcast(context, 0, new Intent(SENT_IDS[i]), 0);

        final SmsResultType[] resultTypes = new SmsResultType[phones.size()];
        Arrays.fill(resultTypes, SmsResultType.ERROR_NONE);
        BroadcastReceiver sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for (int i = 0; i < phones.size(); i++) {
                    if (Objects.requireNonNull(intent.getAction()).equals(SENT_IDS[i]))
                        resultTypes[i] = SmsResultType.lookup(getResultCode());
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        for (String action : SENT_IDS) intentFilter.addAction(action);
        context.registerReceiver(sentReceiver, intentFilter);

        // It may throw exception if our app doesn't have SEND_SMS permission
        try {
            SmsManager smsManager = SmsManager.getDefault();
            for (int i = 0; i < phones.size(); i++)
                smsManager.sendTextMessage(phones.get(i), null, msg, sentPIs[i], null);

            // Wait until we have all results
            for (int i = 0; i < phones.size(); i++) {
                while (resultTypes[i] == SmsResultType.ERROR_NONE)
                    Thread.sleep(100);
            }

            StringBuilder stringBuilder = new StringBuilder("SMS_SENT Results: ");
            for (int i = 0; i < phones.size(); i++)
                stringBuilder.append(resultTypes[i].getCode()).append(" ").append(resultTypes[i]).append(
                        "\n");
            Log.d(TAG, stringBuilder.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        context.unregisterReceiver(sentReceiver);   // Prevent memory leak

        JSONObject json = new JSONObject();
        json.put("message", msg);
        for (int i = 0; i < phones.size(); i++) {
            JSONObject phoneStatusPair = new JSONObject();
            phoneStatusPair.put("phone", phones.get(i));
            phoneStatusPair.put("status", resultTypes[i]);
            json.accumulate("results", phoneStatusPair);
        }

        return json.toString();
    }

    private enum SmsResultType {
        OK(Activity.RESULT_OK),
        ERROR_GENERIC_FAILURE(SmsManager.RESULT_ERROR_GENERIC_FAILURE),
        ERROR_NO_SERVICE(SmsManager.RESULT_ERROR_NO_SERVICE),
        ERROR_NULL_PDU(SmsManager.RESULT_ERROR_NULL_PDU),
        ERROR_RADIO_OFF(SmsManager.RESULT_ERROR_RADIO_OFF),
        ERROR_NONE(0);

        private final int code;

        SmsResultType(int code) {
            this.code = code;
        }

        public static SmsResultType lookup(int code) {
            for (SmsResultType type : values()) {
                if (type.getCode() == code)
                    return type;
            }
            return null;
        }

        public int getCode() {
            return code;
        }
    }
}