package com.example.smsotp.server.handlers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
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

        return Ok(commandsDto);
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
                return Unauthorized(session.getUri(), null, "Incorrect username and/or password!");
        } catch (IllegalArgumentException e) {
            return Unauthorized(session.getUri(), e, "Missing username or password parameters!");
        }

        try {
            final String msg = ServerUtils.validateParam(params.get(Key.MESSAGE)).get(0);
            final List<String> phones = ServerUtils.validatePhones(params.get(Key.PHONES));

            int userId = database.userDao().getIdByUsername(usernameParam);
            List<SmsDto.Result> reportResults = sendManySms(phones, msg);
            Command command = new Command(userId, msg, gson.toJson(reportResults), new Date());
            int commId = (int) database.commandDao().insert(command);
            SmsDto reportDto = new SmsDto(commId, userId, msg, reportResults);

            return Ok(reportDto);
        } catch (IllegalArgumentException e) {
            return BadRequest(session.getUri(), e, "Missing or invalid message or phones parameters!");
        }
    }

    /**
     * Sends intent-based SMS, meaning, when the android system broadcasts back the results,
     * our app detects them and returns an array of these results
     *
     * @param phones List of phone numbers as Strings
     * @param msg    Text message to send
     * @return a list of {@link SmsDto.Result} to insert into DB and return as response body
     */
    private List<SmsDto.Result> sendManySms(List<String> phones, String msg) {
        final Thread myThread = Thread.currentThread();
        final String baseAction = getClass().getName() + myThread.getId() + "#";
        final PendingIntent[] sentPIs = new PendingIntent[phones.size()];
        IntentFilter filter = new IntentFilter();
        // We build our pendingIntents for the smsManager using a per-thread, per-phone unique action name
        for (int i = 0; i < phones.size(); i++) {
            String action = baseAction + i;
            sentPIs[i] = PendingIntent.getBroadcast(context, 0, new Intent(action), 0);
            filter.addAction(action);
        }
        SentSmsReceiver receiver = new SentSmsReceiver(myThread, phones.size());
        HandlerThread handlerThread = new HandlerThread("Handler#" + myThread.getId());
        handlerThread.start();

        // Register it on the new thread, so we can sleep current thread and avoid doing a busy wait
        context.registerReceiver(receiver, filter, null, new Handler(handlerThread.getLooper()));
        for (int i = 0; i < phones.size(); i++)
            WebService.getSmsManager().sendTextMessage(phones.get(i), null, msg, sentPIs[i], null);

        // Wait until we have all results, or interrupt if it's done sooner
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.d(TAG, "sendManySms: Interrupted " + myThread);
        }
        Log.d(TAG, "SENT_SMS Result: " + receiver.resultTypes);
        context.unregisterReceiver(receiver);   // Prevent memory leak

        List<SmsDto.Result> results = new ArrayList<>(phones.size());
        for (int i = 0; i < phones.size(); i++) {
            SmsResultType status = receiver.resultTypes.get(i);
            if (status == null) status = SmsResultType.ERROR_TIMEOUT;
            results.add(new SmsDto.Result(phones.get(i), status));
        }
        return results;
    }

    private static class SentSmsReceiver extends BroadcastReceiver {
        private final Thread baseThread;
        List<SmsResultType> resultTypes;

        public SentSmsReceiver(Thread baseThread, int listSize) {
            super();
            this.baseThread = baseThread;
            this.resultTypes = Arrays.asList(new SmsResultType[listSize]);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = Objects.requireNonNull(intent.getAction());
            int phoneIdx = Integer.parseInt(action.substring(action.lastIndexOf('#') + 1));
            resultTypes.set(phoneIdx, SmsResultType.lookup(getResultCode()));

            // If there are no nulls left, wake up the base thread
            if (!resultTypes.contains(null)) baseThread.interrupt();
        }
    }

    private static class Key {
        static final String PHONES = "phones";
        static final String MESSAGE = "message";
        static final String USERNAME = "username";
        static final String PASSWORD = "password";
    }
}
