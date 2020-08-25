package com.example.smsotp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.smsotp.entity.Command;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    private static final String TAG = "SMSOTP_WebServer";
    private static final String MIME_JSON = "application/json";
    public static int port = 8080;
    private Context context;
    private AppDatabase database;

    public WebServer(Context context, AppDatabase database, int port) {
        super(port);
        WebServer.port = port;
        this.context = context;
        this.database = database;
//        addMappings();
    }

    public WebServer(Context context, AppDatabase database) {
        this(context, database, 8080);
    }

    @Override
    public void start() {
        try {
            super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            Log.d(TAG, "Server running! Point your browsers to http://localhost:8080/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*@Override
    public void addMappings() {
        super.addMappings();
    }*/

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

        switch (session.getUri()) {
            case "/":
                return handleIndexRequest(session);
            case "/api/sms":
                return handleSmsRequest(session.getParameters(), method);
            default:
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                        "Error 404: Not Found");
        }

    }

    private Response handleIndexRequest(IHTTPSession session) {
        BufferedReader reader;
        try {
            StringBuilder answer = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open("index.html")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("${ip}"))
                    line = line.replace("${ip}", session.getRemoteHostName());
                answer.append(line);
            }
            reader.close();
            return newFixedLengthResponse(answer.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "SERVER INTERNAL ERROR: IOException: " + e.getMessage());
        }
    }

    /**
     * This method checks the correctness of the request to the /api/sms route and returns a response for
     * each case
     */
    @SuppressWarnings("ConstantConditions")
    private Response handleSmsRequest(Map<String, List<String>> params, Method method) {
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
        if (password == null || !password.equals(params.get("password").get(0)))
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT,
                    "Incorrect username and/or password!");

        Response response;
        if (params.containsKey("phones") && params.containsKey("message")) {
            try {
                JSONObject jsonParams = sendManySms(params.get("phones"), params.get("message").get(0));
                int userId = database.userDao().getIdByUsername(params.get("username").get(0));
                int commId = (int) database.commandDao().insert(new Command(userId, jsonParams.toString(),
                        new Date()));

                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("commandId", commId)
                        .put("userId", userId)
                        .put("message", jsonParams.getString("message"))
                        .put("results", jsonParams.get("results"));
                response = newFixedLengthResponse(Response.Status.OK, MIME_JSON, jsonResponse.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "Could not convert params to JSON\n" + e.getMessage());
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
     * @return JSON parameters to insert into DB and append to response JSON
     * @throws JSONException if JSON parsing failed
     */
    private JSONObject sendManySms(List<String> phones, String msg) throws JSONException {
        String packageName = Objects.requireNonNull(getClass().getPackage()).getName()
                + ".SMS_SENT" + Thread.currentThread().getId() + "#";
        final String[] SENT_IDS = new String[phones.size()];
        for (int i = 0; i < phones.size(); i++) SENT_IDS[i] = packageName + i;

        PendingIntent[] sentPIs = new PendingIntent[phones.size()];
        for (int i = 0; i < phones.size(); i++)
            sentPIs[i] = PendingIntent.getBroadcast(context, 0, new Intent(SENT_IDS[i]), 0);

        final SmsResultType[] resultTypes = new SmsResultType[phones.size()];
        BroadcastReceiver sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = Objects.requireNonNull(intent.getAction());
                int phoneIdx = Integer.parseInt(action.substring(action.lastIndexOf('#') + 1));
                resultTypes[phoneIdx] = SmsResultType.lookup(getResultCode());
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
                while (resultTypes[i] == null)
                    Thread.sleep(100);
            }

            StringBuilder stringBuilder = new StringBuilder("SMS_SENT Results: ");
            for (int i = 0; i < phones.size(); i++)
                stringBuilder.append(resultTypes[i].getCode()).append(" ").append(resultTypes[i]).append('\n');

            Log.d(TAG, stringBuilder.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        context.unregisterReceiver(sentReceiver);   // Prevent memory leak

        JSONObject json = new JSONObject();
        json.put("message", msg);
        for (int i = 0; i < phones.size(); i++) {
            JSONObject phoneStatusPair = new JSONObject();
            phoneStatusPair.put("phone", phones.get(i))
                    .put("status", resultTypes[i]);
            json.accumulate("results", phoneStatusPair);
        }

        return json;
    }

    @SuppressWarnings("unused")
    private enum SmsResultType {
        OK(Activity.RESULT_OK),
        ERROR_GENERIC_FAILURE(SmsManager.RESULT_ERROR_GENERIC_FAILURE),
        ERROR_NO_SERVICE(SmsManager.RESULT_ERROR_NO_SERVICE),
        ERROR_NULL_PDU(SmsManager.RESULT_ERROR_NULL_PDU),
        ERROR_RADIO_OFF(SmsManager.RESULT_ERROR_RADIO_OFF);

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
