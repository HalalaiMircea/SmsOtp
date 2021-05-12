package com.example.smsotp.server.handlers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.example.smsotp.WebService;
import com.example.smsotp.model.Command;
import com.example.smsotp.server.ServerUtils;
import com.example.smsotp.server.dto.SmsDto;
import com.example.smsotp.server.dto.SmsResultType;

import java.util.*;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

import static com.example.smsotp.server.WebServer.database;
import static com.example.smsotp.server.WebServer.gson;

public class ApiHandler extends ServerUtils.RestHandler {
    private static final String TAG = "Web_ApiHandler";
    private Context context;

    @Override
    public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        Response response = super.post(uriResource, urlParams, session);
        // In case we don't support the mimetype requested by client, forward the response
        if (response.getStatus() == Response.Status.NOT_ACCEPTABLE)
            return response;

        Map<String, List<String>> params = session.getParameters();
        context = uriResource.initParameter(Context.class);
        Log.d(TAG, "Accepted MIME type: " + acceptedMimeType);

        List<String> usernameParam = params.get(Key.USERNAME);
        List<String> passwordParam = params.get(Key.PASSWORD);
        try {
            // We check if the request misses any credential
            ServerUtils.validateParams(usernameParam, passwordParam);
            // If returned password string is null, username doesn't exist
            String password = database.userDao().getPasswordByUsername(usernameParam.get(0));
            if (password == null || !password.equals(passwordParam.get(0)))
                return newErrorResponse(Response.Status.UNAUTHORIZED,
                        session.getUri(), null, "Incorrect username and/or password!");
        } catch (IllegalArgumentException e) {
            return newErrorResponse(Response.Status.UNAUTHORIZED, session.getUri(),
                    e, "Missing username or password parameters!");
        }

        try {
            ServerUtils.validateParams(params.get(Key.MESSAGE));
            ServerUtils.validatePhones(params.get(Key.PHONES));

            final int userId = database.userDao().getIdByUsername(usernameParam.get(0));
            final String msg = params.get(Key.MESSAGE).get(0);
            List<SmsDto.Result> reportResults = sendManySms(params.get(Key.PHONES), msg);
            Command command = new Command(userId, msg, gson.toJson(reportResults), new Date());
            final int commId = (int) database.commandDao().insert(command);
            SmsDto reportDto = new SmsDto(commId, userId, msg, reportResults);

            return newGsonResponse(Response.Status.OK, reportDto);
        } catch (IllegalArgumentException e) {
            return newErrorResponse(Response.Status.BAD_REQUEST,
                    session.getUri(), e, "Missing or invalid message and/or phones parameters!");
        } catch (Exception e) {
            e.printStackTrace();
            return newErrorResponse(Response.Status.INTERNAL_ERROR, session.getUri(), e, null);
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
    private List<SmsDto.Result> sendManySms(List<String> phones, String msg) throws Exception {
        final String baseAction = getClass().getName() + Thread.currentThread().getId() + "#";
        final PendingIntent[] sentPIs = new PendingIntent[phones.size()];
        IntentFilter filter = new IntentFilter();
        // We build our pendingIntents for the smsManager using a per-thread, per-phone unique action name
        for (int i = 0; i < phones.size(); i++) {
            String action = baseAction + i;
            sentPIs[i] = PendingIntent.getBroadcast(context, 0, new Intent(action), 0);
            filter.addAction(action);
        }
        SentSmsReceiver receiver = new SentSmsReceiver(phones.size());
        context.registerReceiver(receiver, filter);

        for (int i = 0; i < phones.size(); i++)
            WebService.getSmsManager().sendTextMessage(phones.get(i), null, msg, sentPIs[i], null);
        // Wait until we have all results
        while (receiver.resultTypes.contains(null)) {
            Thread.sleep(100);
        }

        Log.d(TAG, "SENT_SMS Result: " + receiver.resultTypes);
        context.unregisterReceiver(receiver);   // Prevent memory leak

        List<SmsDto.Result> results = new ArrayList<>(phones.size());
        for (int i = 0; i < phones.size(); i++) {
            results.add(new SmsDto.Result(phones.get(i), receiver.resultTypes.get(i)));
        }
        return results;
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

    private static class Key {
        static final String PHONES = "phones";
        static final String MESSAGE = "message";
        static final String USERNAME = "username";
        static final String PASSWORD = "password";
        static final String RESULTS = "results";
    }
}
