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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import fi.iki.elonen.NanoHTTPD;
import freemarker.cache.ByteArrayTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

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
        private Configuration templatingCfg;

        public WebServer(Context context, AppDatabase database, int port) {
            super(port);
            WebServer.port = port;
            this.context = context;
            this.database = database;

            templatingCfg = new Configuration(new Version(2, 3, 30));
            templatingCfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
            try {
                String[] list = Objects.requireNonNull(this.context.getAssets().list(""));
                List<String> htmlFiles = Arrays.stream(list)
                        .filter(s -> s.substring(s.lastIndexOf('.') + 1).equals("ftl"))
                        .collect(Collectors.toList());
                loadTemplates(htmlFiles);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                case "/index.ftl":
                    return handleIndexRequest(session);
                case "/api/sms":
                    return handleSmsRequest(session);
                default:
                    return handleResourceRequest(session);
            }
        }

        private void loadTemplates(List<String> files) throws IOException {
            ByteArrayTemplateLoader templateLoader = new ByteArrayTemplateLoader();
            for (String file : files) {
                InputStream inputStream = context.getAssets().open(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, length);
                }
                templateLoader.putTemplate(file, baos.toByteArray());
            }

            templatingCfg.setTemplateLoader(templateLoader);
        }

        private Response handleResourceRequest(IHTTPSession session) {
            try {
                String uri = session.getUri().substring(1);
                InputStream inputStream = context.getAssets().open(uri);
                String resType = uri.substring(uri.lastIndexOf('.') + 1);

                return newChunkedResponse(Response.Status.OK, mimeTypes().get(resType), inputStream);
            } catch (IOException e) {
                return handleError(Response.Status.NOT_FOUND, session.getUri(), null, e);
            }
        }

        private Response handleIndexRequest(IHTTPSession session) {
            try {
                Template template = templatingCfg.getTemplate("index.ftl");
                Map<String, Object> templateData = new HashMap<>();
                templateData.put("ip", session.getRemoteHostName());

                Writer out = new StringWriter();
                template.process(templateData, out);
                out.flush();
                return newFixedLengthResponse(out.toString());
            } catch (IOException | TemplateException e) {
                return handleError(Response.Status.NOT_FOUND, session.getUri(), null, e);
            }
        }

        private Response handleError(Response.Status status, String uri, String desc, Exception ex) {
            String stackString = null;
            if (ex != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                stackString = sw.toString();
            }
            try {
                Template template = templatingCfg.getTemplate("error.ftl");
                Map<String, Object> dataModel = new HashMap<>();
                dataModel.put("status", status.getDescription().replaceFirst(" ", " - "));
                dataModel.put("path", uri);
                dataModel.put("description", desc != null ? desc : status.getDescription());
                dataModel.put("ex_stacktrace", stackString);

                StringWriter out = new StringWriter();
                template.process(dataModel, out);
                return newFixedLengthResponse(out.toString());
            } catch (IOException | TemplateException e) {
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
        private Response handleSmsRequest(IHTTPSession session) {
            Map<String, List<String>> params = session.getParameters();

            if (session.getMethod() != Method.POST) {
                return handleError(Response.Status.METHOD_NOT_ALLOWED, session.getUri(),
                        "Only POST requests are allowed!", null);
            }
            // We check if the request misses any credential
            if (!isParamNonBlank(params.get(Keys.USERNAME)) || !isParamNonBlank(params.get(Keys.PASSWORD))) {
                return handleError(Response.Status.UNAUTHORIZED, session.getUri(),
                        "Missing username or password parameters!", null);
            }

            // If returned password string is null, username doesn't exist
            String password = database.userDao().getPasswordByUsername(params.get(Keys.USERNAME).get(0));
            if (password == null || !password.equals(params.get(Keys.PASSWORD).get(0)))
                return handleError(Response.Status.UNAUTHORIZED, session.getUri(),
                        "Incorrect username and/or password!", null);

            Response response;
            if (isParamNonBlank(params.get(Keys.PHONES)) && isParamNonBlank(params.get(Keys.MESSAGE))) {
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
                    response = handleError(Response.Status.INTERNAL_ERROR, session.getUri(),
                            "Could not convert params to JSON\n" + e.getMessage(), e);
                }
            } else
                response = handleError(Response.Status.BAD_REQUEST, session.getUri(),
                        "Request query missing phones and/or message parameters, or their values are blank!",
                        null);

            return response;
        }

        private boolean isParamNonBlank(List<String> paramValue) {
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
