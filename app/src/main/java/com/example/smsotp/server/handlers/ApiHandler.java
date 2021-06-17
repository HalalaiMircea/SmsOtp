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
import com.example.smsotp.server.RoutedWebServer.UriResource;
import com.example.smsotp.server.dto.CommandDto;
import com.example.smsotp.server.dto.SmsDto;
import com.example.smsotp.server.dto.SmsRequest;
import com.example.smsotp.server.dto.SmsResultType;
import com.example.smsotp.sql.Command;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

import static com.example.smsotp.server.RoutedWebServer.database;
import static com.example.smsotp.server.ServerUtils.validateParam;
import static com.example.smsotp.server.ServerUtils.validatePhones;

public class ApiHandler extends RestHandler {
    private static final String TAG = "Web_ApiHandler";
    private final Context mContext;

    public ApiHandler(UriResource uriResource, Map<String, String> pathParams, IHTTPSession session) {
        super(uriResource, pathParams, session);
        mContext = uriResource.initParameter(Context.class);
    }

    @Override
    public Response doGet() {
        final Type resultListType = new TypeToken<List<SmsDto.Result>>() {
        }.getType();
        List<CommandDto> commandsDto = database.commandDao().getAll().stream()
                .map(cmd -> new CommandDto(cmd.id, cmd.userId, cmd.message,
                        gson.fromJson(cmd.phoneResults, resultListType), cmd.executedDate))
                .sorted((c1, c2) -> c2.getExecutedDate().compareTo(c1.getExecutedDate()))
                .collect(Collectors.toList());

        return Ok(commandsDto);
    }

    @Override
    public Response doPost() {
        SmsRequest reqBody;
        // We first validate the request, then we authorize
        try {
            if (MIME_JSON.equals(session.getHeaders().get("content-type"))) {
                reqBody = gson.fromJson(mBodyFiles.get("postData"), SmsRequest.class);
                if (StringUtils.isBlank(reqBody.getUsername()) ||
                        StringUtils.isBlank(reqBody.getPassword()) ||
                        StringUtils.isBlank(reqBody.getMessage())) {
                    throw new IllegalArgumentException("Null or blank values!");
                }
                validatePhones(reqBody.getPhones());
            } else {
                Map<String, List<String>> params = session.getParameters();
                reqBody = new SmsRequest(
                        validateParam(params.get(Key.USERNAME)).get(0),
                        validateParam(params.get(Key.PASSWORD)).get(0),
                        validatePhones(params.get(Key.PHONES)),
                        validateParam(params.get(Key.MESSAGE)).get(0));
            }
        } catch (JsonSyntaxException | IllegalArgumentException ex) {
            return BadRequest(ex, ex.getMessage());
        }

        // If the returned password string is null, username doesn't exist
        String password = database.userDao().getPasswordByUsername(reqBody.getUsername());
        if (!Objects.equals(password, reqBody.getPassword()))
            return Unauthorized();

        int userId = database.userDao().getIdByUsername(reqBody.getUsername());
        List<SmsDto.Result> reportResults = sendManySms(reqBody.getPhones(), reqBody.getMessage());
        int commId = (int) database.commandDao().insert(
                new Command(userId, reqBody.getMessage(), gson.toJson(reportResults), new Date())
        );
        SmsDto reportDto = new SmsDto(commId, userId, reqBody.getMessage(), reportResults);

        return Ok(reportDto);
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
            sentPIs[i] = PendingIntent.getBroadcast(mContext, 0, new Intent(action), 0);
            filter.addAction(action);
        }
        SentSmsReceiver receiver = new SentSmsReceiver(myThread, phones.size());
        HandlerThread handlerThread = new HandlerThread("Handler#" + myThread.getId());
        handlerThread.start();

        // Register it on the new thread, so we can sleep current thread and avoid doing a busy wait
        mContext.registerReceiver(receiver, filter, null, new Handler(handlerThread.getLooper()));
        for (int i = 0; i < phones.size(); i++)
            WebService.smsManager.sendTextMessage(phones.get(i), null, msg, sentPIs[i], null);

        // Wait until we have all results, or interrupt if it's done sooner
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.d(TAG, "sendManySms: Interrupted " + myThread);
        }
        Log.d(TAG, "SENT_SMS Result: " + receiver.resultTypes);
        mContext.unregisterReceiver(receiver);   // Prevent memory leak

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
