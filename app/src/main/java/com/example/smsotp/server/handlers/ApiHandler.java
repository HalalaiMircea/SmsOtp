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
import com.example.smsotp.server.dto.CommandDto;
import com.example.smsotp.server.dto.SmsDto;
import com.example.smsotp.server.dto.SmsResultType;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

import static com.example.smsotp.server.WebServer.database;
import static com.example.smsotp.server.WebServer.gson;

public class ApiHandler extends ServerUtils.RestHandler {
    private static final String TAG = "Web_ApiHandler";
    private Context context;

    @Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        Response response = super.get(uriResource, urlParams, session);
        if (response.getStatus() == Response.Status.NOT_ACCEPTABLE) return response;

        final Type resultListType = new TypeToken<List<SmsDto.Result>>() {
        }.getType();
        List<CommandDto> commandsDto = database.commandDao().getAll().stream()
                .map(cmd -> new CommandDto(cmd.id, cmd.userId, cmd.message,
                        gson.fromJson(cmd.phoneResults, resultListType), cmd.executedDate))
                .collect(Collectors.toList());

        return ok(commandsDto);
    }

    @Override
    public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        Response response = super.post(uriResource, urlParams, session);
        // In case we don't support the mimetype requested by client, forward the response
        if (response.getStatus() == Response.Status.NOT_ACCEPTABLE) return response;

        Map<String, List<String>> params = session.getParameters();
        context = uriResource.initParameter(Context.class);

        final String usernameParam, passwordParam;
        try {
            // We check if the request misses any credential
            usernameParam = ServerUtils.validateParam(params.get(Key.USERNAME)).get(0);
            passwordParam = ServerUtils.validateParam(params.get(Key.PASSWORD)).get(0);
            // If the returned password string is null, username doesn't exist
            String password = database.userDao().getPasswordByUsername(usernameParam);
            if (!Objects.equals(password, passwordParam))
                return unauthorized(session.getUri(), null, "Incorrect username and/or password!");
        } catch (IllegalArgumentException e) {
            return unauthorized(session.getUri(), e, "Missing username or password parameters!");
        }

        try {
            final String msg = ServerUtils.validateParam(params.get(Key.MESSAGE)).get(0);
            final List<String> phones = ServerUtils.validatePhones(params.get(Key.PHONES));

            int userId = database.userDao().getIdByUsername(usernameParam);
            List<SmsDto.Result> reportResults = sendManySms(phones, msg);
            Command command = new Command(userId, msg, gson.toJson(reportResults), new Date());
            int commId = (int) database.commandDao().insert(command);
            SmsDto reportDto = new SmsDto(commId, userId, msg, reportResults);

            return ok(reportDto);
        } catch (IllegalArgumentException e) {
            return badRequest(session.getUri(), e, "Missing or invalid message or phones parameters!");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return internalError(session.getUri(), e, null);
        }
    }

    /**
     * Sends intent-based SMS, meaning when the android system broadcasts the sentIntent our app detects
     * these broadcasts and does something for each case
     *
     * @param phones List of String phone numbers
     * @param msg    Text message to send
     * @return JSON parameters to insert into DB and append to response JSON
     * @throws InterruptedException if JSON parsing failed or thread is interrupted
     */
    private List<SmsDto.Result> sendManySms(List<String> phones, String msg) throws InterruptedException {
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
    }
}
