package com.example.smsotp.server.handlers;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.smsotp.model.Command;
import com.example.smsotp.server.ServerUtils;

import org.json.JSONObject;
import org.json.XML;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

import static com.example.smsotp.server.WebServer.database;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ApiHandler extends ServerUtils.RestHandler {
    private static final String TAG = "Web_ApiHandler";
    private Context context;

    @SuppressWarnings("ConstantConditions")
    @Override
    public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        super.post(uriResource, urlParams, session);
        Map<String, List<String>> params = session.getParameters();
        context = uriResource.initParameter(Context.class);

        List<String> usernameParam = params.get(Keys.USERNAME);
        List<String> passwordParam = params.get(Keys.PASSWORD);
        try {
            // We check if the request misses any credential
            ServerUtils.validateParams(usernameParam, passwordParam);
            // If returned password string is null, username doesn't exist
            String password = database.userDao().getPasswordByUsername(usernameParam.get(0));
            if (password == null || !password.equals(passwordParam.get(0)))
                return handleErrorRest(new ServerUtils.HttpError(Response.Status.UNAUTHORIZED,
                        session.getUri(), null, "Incorrect username and/or password!"));
        } catch (IllegalArgumentException e) {
            return handleErrorRest(new ServerUtils.HttpError(Response.Status.UNAUTHORIZED, session.getUri(),
                    null, "Missing username or password parameters!"));
        }

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

            return newFixedLengthResponse(Response.Status.OK, dataFormat,
                    dataFormat.equals(MIME_JSON) ? jsonResponse.toString() :
                            XML.toString(jsonResponse, "root"));
        } catch (IllegalArgumentException e) {
            return handleErrorRest(new ServerUtils.HttpError(Response.Status.BAD_REQUEST,
                    session.getUri(), null, "Missing or invalid message and/or phones parameters!"));
        } catch (Exception e) {
            e.printStackTrace();
            return handleErrorRest(new ServerUtils.HttpError(Response.Status.INTERNAL_ERROR,
                    session.getUri(), e, null));
        }
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

    private static class Keys {
        static final String PHONES = "phones";
        static final String MESSAGE = "message";
        static final String USERNAME = "username";
        static final String PASSWORD = "password";
        static final String RESULTS = "results";
    }
}