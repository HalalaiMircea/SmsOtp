package com.example.smsotp;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.smsotp.entity.Command;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

public class WebService extends Service {
    private static final String TAG = "SMSOTP_WebService";
    private static boolean isRunning = false;
    private WebServer webServer;

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        webServer = new WebServer(this, AppDatabase.getInstance(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!webServer.wasStarted()) {
            isRunning = true;
            createNotification();
            webServer.start();
            Log.d(TAG, "Service started!");
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
        final String CHANNEL_ID = "ForegroundServiceChannel";

        // Required for Oreo (API 26) and greater
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.service_notifi_title))
                .setContentText(getText(R.string.service_notifi_msg))
                .setSmallIcon(R.drawable.ic_baseline_web_24)
                .setContentIntent(pi)
                .build();

        startForeground(1, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class WebServer extends NanoHTTPD {
        private static final String MIME_JSON = "application/json";
        public static int port = 8080;
        private Context context;
        private AppDatabase database;

        public WebServer(Context context, AppDatabase database, int port) {
            super(port);
            WebServer.port = port;
            this.context = context;
            this.database = database;
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
                case "/index.html":
                    return handleIndexRequest(session);
                case "/api/sms":
                    return handleSmsRequest(session.getParameters(), method);
                default:
                    return handleResourceRequest(session);
            }

        }

        private Response handleResourceRequest(IHTTPSession session) {
            try {
                String uri = session.getUri().substring(1);
                InputStream inputStream = context.getAssets().open(uri);
                String resType = uri.substring(uri.lastIndexOf('.') + 1);

                return newChunkedResponse(Response.Status.OK, mimeTypes().get(resType), inputStream);
            } catch (IOException e) {
                return handleErrorRequest(Response.Status.NOT_FOUND, session, e);
            }
        }

        private Response handleIndexRequest(IHTTPSession session) {
            try {
                InputStream inputStream = context.getAssets().open("index.html");
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String html = result.toString(StandardCharsets.UTF_8.name())
                        .replace("${ip}", session.getRemoteHostName());

                return newFixedLengthResponse(html);
            } catch (IOException e) {
                return handleErrorRequest(Response.Status.INTERNAL_ERROR, session, e);
            }
        }

        private Response handleErrorRequest(Response.Status status, IHTTPSession session, Exception ex) {
            try {
                InputStream inputStream = context.getAssets().open("error.html");
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                String stackTraceString = sw.toString();

                String html = result.toString(StandardCharsets.UTF_8.name())
                        .replace("${status}", status.getDescription().replaceFirst(" ", " - "))
                        .replace("${path}", session.getUri())
                        .replace("${ex_stacktrace}", stackTraceString);

                Log.i(TAG, stackTraceString);
                return newFixedLengthResponse(status, MIME_HTML, html);
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
            if (!params.containsKey(Keys.USERNAME) || !params.containsKey(Keys.PASSWORD))
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT,
                        "Missing username or password parameters!");

            // If returned password string is null, username doesn't exist
            String password = database.userDao().getPasswordByUsername(params.get(Keys.USERNAME).get(0));
            if (password == null || !password.equals(params.get(Keys.PASSWORD).get(0)))
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT,
                        "Incorrect username and/or password!");

            Response response;
            if (checkParamValidity(params.get(Keys.PHONES)) && checkParamValidity(params.get(Keys.MESSAGE))) {
                try {
                    JSONObject jsonParams = sendManySms(params.get(Keys.PHONES),
                            params.get(Keys.MESSAGE).get(0));
                    int userId = database.userDao().getIdByUsername(params.get(Keys.USERNAME).get(0));
                    int commId = (int) database.commandDao().insert(new Command(userId, jsonParams.toString(),
                            new Date()));

                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("commandId", commId)
                            .put("userId", userId)
                            .put(Keys.MESSAGE, jsonParams.getString(Keys.MESSAGE))
                            .put(Keys.RESULTS, jsonParams.get(Keys.RESULTS));
                    response = newFixedLengthResponse(Response.Status.OK, MIME_JSON, jsonResponse.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                            "Could not convert params to JSON\n" + e.getMessage());
                }
            } else
                response = newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                        "Request query missing phones and/or message parameters!\nOr one or more phone " +
                                "strings are empty/null");

            return response;
        }

        private boolean checkParamValidity(List<String> paramValue) {
            // If request query key for this value is missing
            if (paramValue == null) return false;

            // If any element is invalid
            for (String value : paramValue) {
                if (value == null || value.trim().isEmpty())
                    return false;
            }

            return !paramValue.isEmpty();
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
            json.put(Keys.MESSAGE, msg);
            for (int i = 0; i < phones.size(); i++) {
                JSONObject phoneStatusPair = new JSONObject();
                phoneStatusPair.put("phone", phones.get(i))
                        .put("status", resultTypes[i]);
                json.accumulate(Keys.RESULTS, phoneStatusPair);
            }

            return json;
        }

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
                for (SmsResultType type : SmsResultType.values()) {
                    if (type.getCode() == code)
                        return type;
                }
                return null;
            }

            public int getCode() {
                return code;
            }
        }

        private static class Keys {

            static final String PHONES = "phones";
            static final String MESSAGE = "message";
            static final String USERNAME = "username";
            static final String PASSWORD = "password";
            static final String RESULTS = "results";
        }
    }
}
