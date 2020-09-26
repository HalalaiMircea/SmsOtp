package com.example.smsotp.server;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.model.Command;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.json.XML;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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

public class WebServer extends NanoHTTPD {
    private static final String TAG = "SMSOTP_NanoHTTPD";
    public static int port = 8080;

    static {
        mimeTypes().put("json", "application/json");
        mimeTypes().put("ftl", "text/html");
    }

    private Gson gson;
    private Context context;
    private AppDatabase database;
    private Configuration freemarkerCfg;

    public WebServer(Context context, int port) {
        super(port);
        WebServer.port = port;
        this.context = context;
        this.database = AppDatabase.getInstance(context);

        gson = new Gson();
        freemarkerCfg = new Configuration(new Version(2, 3, 30));
        freemarkerCfg.setDefaultEncoding("UTF-8");
        try {
            String parentPath = "";
            String[] list = Objects.requireNonNull(this.context.getAssets().list(parentPath));
            List<String> htmlFiles = Arrays.stream(list)
                    .filter(s -> s.substring(s.lastIndexOf('.') + 1).equals("ftl"))
                    .map(s -> parentPath + s)
                    .collect(Collectors.toList());
            loadTemplates(htmlFiles);
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
            } catch (IOException ex) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "SERVER INTERNAL ERROR: " + ex.getMessage());
            } catch (ResponseException re) {
                return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }
        switch (session.getUri()) {
            case "/":
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
            // We strip the name of the extension
            String templateName = file.substring(0, file.lastIndexOf('.'));
            templateLoader.putTemplate(templateName, baos.toByteArray());
        }
        freemarkerCfg.setTemplateLoader(templateLoader);
    }

    private Response handleIndexRequest(IHTTPSession session) {
        try {
            Template template = freemarkerCfg.getTemplate("index");
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("ip", session.getRemoteHostName());

            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            return newFixedLengthResponse(out.toString());
        } catch (IOException | TemplateException e) {
            return handleErrorHtml(new HttpError(Response.Status.NOT_FOUND, session.getUri(), e, null));
        }
    }

    private Response handleResourceRequest(IHTTPSession session) {
        try {
            String uri = session.getUri().substring(1);
            InputStream inputStream = context.getAssets().open(uri);
            String mimeType = getMimeTypeForFile(uri);

            return newChunkedResponse(Response.Status.OK, mimeType, inputStream);
        } catch (IOException e) {
            return handleErrorHtml(new HttpError(Response.Status.NOT_FOUND, session.getUri(), e,
                    "Requested resource isn't available"));
        }
    }

    private Response handleErrorHtml(HttpError error) {
        try {
            Template template = freemarkerCfg.getTemplate("error");
            StringWriter out = new StringWriter();
            template.process(error.getMapModel(), out);
            return newFixedLengthResponse(error.status, MIME_HTML, out.toString());
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                    "SERVER INTERNAL ERROR: " + e.getMessage());
        }
    }

    private Response handleErrorRest(HttpError error) {
        return newFixedLengthResponse(error.status, mimeTypes().get("json"), gson.toJson(error));
    }

    /**
     * This method checks the correctness of the request to the /api/sms route and returns a response for
     * each case
     */
    @SuppressWarnings("ConstantConditions")
    private Response handleSmsRequest(IHTTPSession session) {
        Map<String, List<String>> params = session.getParameters();

        if (session.getMethod() != Method.POST) {
            return handleErrorRest(new HttpError(Response.Status.METHOD_NOT_ALLOWED, session.getUri(),
                    null, "Only POST requests are allowed!"));
        }
        List<String> usernameParam = params.get(Keys.USERNAME);
        List<String> passwordParam = params.get(Keys.PASSWORD);
        try {
            // We check if the request misses any credential
            ServerUtils.validateParams(usernameParam, passwordParam);
            // If returned password string is null, username doesn't exist
            String password = database.userDao().getPasswordByUsername(usernameParam.get(0));
            if (password == null || !password.equals(passwordParam.get(0)))
                return handleErrorRest(new HttpError(Response.Status.UNAUTHORIZED, session.getUri(), null,
                        "Incorrect username and/or password!"));
        } catch (IllegalArgumentException e) {
            return handleErrorRest(new HttpError(Response.Status.UNAUTHORIZED, session.getUri(), null,
                    "Missing username or password parameters!"));
        }

        Response response;
        try {
            ServerUtils.validateParams(params.get(Keys.MESSAGE));
            ServerUtils.validatePhones(params.get(Keys.PHONES));

            JSONObject jsonParams = sendManySms(params.get(Keys.PHONES), params.get(Keys.MESSAGE).get(0));
            int userId = database.userDao().getIdByUsername(usernameParam.get(0));
            final Command command = new Command(userId, jsonParams.toString(), new Date());
            int commId = (int) database.commandDao().insert(command);

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("commandId", commId)
                    .put("userId", userId)
                    .put(Keys.MESSAGE, jsonParams.getString(Keys.MESSAGE))
                    .put(Keys.RESULTS, jsonParams.get(Keys.RESULTS));

            final String acceptHeader = session.getHeaders().get("accept");
            boolean useXML = false;
            if (acceptHeader != null) {
                useXML = acceptHeader.equals(mimeTypes().get("xml")) ||
                        acceptHeader.equals("application/xml");
            }

            response = newFixedLengthResponse(Response.Status.OK,
                    mimeTypes().get(useXML ? "xml" : "json"),
                    useXML ? XML.toString(jsonResponse, "root") : jsonResponse.toString());
        } catch (IllegalArgumentException e) {
            response = handleErrorRest(new HttpError(Response.Status.BAD_REQUEST, session.getUri(), null,
                    "Missing or invalid message and/or phones parameters!"));
        } catch (Exception e) {
            e.printStackTrace();
            response = handleErrorRest(new HttpError(Response.Status.INTERNAL_ERROR, session.getUri(),
                    e, null));
        }
        return response;
    }

    /**
     * Sends intent-based SMS, meaning when the android system broadcasts the sentIntent our app detects
     * these broadcasts and does something for each case
     *
     * @param phones List of String phone numbers
     * @param msg    Text message to send
     * @return JSON parameters to insert into DB and append to response JSON
     * @throws Exception if JSON parsing failed or thread is interrupted
     */
    private JSONObject sendManySms(List<String> phones, String msg) throws Exception {
        String packageName = Objects.requireNonNull(getClass().getPackage()).getName()
                + ".SMS_SENT" + Thread.currentThread().getId() + "#";
        final String[] SENT_IDS = new String[phones.size()];
        for (int i = 0; i < phones.size(); i++) SENT_IDS[i] = packageName + i;

        PendingIntent[] sentPIs = new PendingIntent[phones.size()];
        for (int i = 0; i < phones.size(); i++)
            sentPIs[i] = PendingIntent.getBroadcast(context, 0, new Intent(SENT_IDS[i]), 0);

        IntentFilter intentFilter = new IntentFilter();
        for (String action : SENT_IDS) intentFilter.addAction(action);
        SentSmsReceiver receiver = new SentSmsReceiver(phones.size());
        context.registerReceiver(receiver, intentFilter);

        SmsManager smsManager = SmsManager.getDefault();
        for (int i = 0; i < phones.size(); i++)
            smsManager.sendTextMessage(phones.get(i), null, msg, sentPIs[i], null);
        // Wait until we have all results
        while (receiver.resultTypes.contains(null)) {
            Thread.sleep(100);
        }

        Log.d(TAG, "SENT_SMS Result: " + receiver.resultTypes);
        context.unregisterReceiver(receiver);   // Prevent memory leak

        JSONObject json = new JSONObject();
        json.put(Keys.MESSAGE, msg);
        for (int i = 0; i < phones.size(); i++) {
            JSONObject phoneStatusPair = new JSONObject();
            phoneStatusPair.put("phone", phones.get(i))
                    .put("status", receiver.resultTypes.get(i));
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

    private static class HttpError {
        Response.Status status;
        String uri;
        String exStacktrace;
        String description;

        public HttpError(Response.Status status, String uri, Exception exception, String description) {
            this.status = status;
            this.uri = uri;
            this.description = description;
            if (exception != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exception.printStackTrace(pw);
                exStacktrace = sw.toString();
            }
        }

        public Map<String, Object> getMapModel() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", status.getDescription().replaceFirst(" ", " - "));
            map.put("uri", uri);
            map.put("description", description != null ? description : status.getDescription());
            map.put("exStacktrace", exStacktrace);
            return map;
        }
    }

    private static class Keys {
        static final String PHONES = "phones";
        static final String MESSAGE = "message";
        static final String USERNAME = "username";
        static final String PASSWORD = "password";
        static final String RESULTS = "results";
    }

    private static class SentSmsReceiver extends BroadcastReceiver {
        List<SmsResultType> resultTypes;

        public SentSmsReceiver(int listSize) {
            super();
            resultTypes = Arrays.asList(new SmsResultType[listSize]);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = Objects.requireNonNull(intent.getAction());
            int phoneIdx = Integer.parseInt(action.substring(action.lastIndexOf('#') + 1));
            resultTypes.set(phoneIdx, SmsResultType.lookup(getResultCode()));
        }
    }
}
