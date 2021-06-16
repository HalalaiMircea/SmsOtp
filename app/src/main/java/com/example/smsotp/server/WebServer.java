package com.example.smsotp.server;

import android.content.Context;

import com.example.smsotp.server.handlers.ApiHandler;
import com.example.smsotp.server.handlers.DefaultHandler;
import com.example.smsotp.server.handlers.IndexHandler;
import com.example.smsotp.sql.AppDatabase;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebServer extends WebServerRouter {
    private static final String TAG = "SMSOTP_NanoHTTPD";
    public static Gson gson = new Gson();
    public static AppDatabase database;

    static {
        mimeTypes().put("json", "application/json");
        mimeTypes().put("ftl", "text/html");
    }

    private final Context context;

    public WebServer(Context context, int port) {
        super(port);
        this.context = context;
        WebServer.database = AppDatabase.getInstance(context);

        addMappings();
    }

    @Override
    public void addMappings() {
        setNotFoundHandler(DefaultHandler.class, context);
        addRoute("/", IndexHandler.class, context);
        addRoute("/api/sms", ApiHandler.class, context);
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
        return super.serve(session);
    }
}
