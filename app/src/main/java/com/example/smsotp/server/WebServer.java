package com.example.smsotp.server;

import android.content.Context;
import android.util.Log;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.server.handlers.ApiHandler;
import com.example.smsotp.server.handlers.ErrorHandler;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import fi.iki.elonen.router.RouterNanoHTTPD;
import freemarker.cache.ByteArrayTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Version;

public class WebServer extends RouterNanoHTTPD {
    private static final String TAG = "SMSOTP_NanoHTTPD";
    public static Configuration freemarkerCfg;
    public static Gson gson;
    public static AppDatabase database;

    static {
        mimeTypes().put("json", "application/json");
        mimeTypes().put("ftl", "text/html");
    }

    private Context context;

    public WebServer(Context context, int port) {
        super(port);
        WebServer.database = AppDatabase.getInstance(context);
        this.context = context;

        Log.i(TAG, "Configuration time: " + configure());
        addMappings();
    }

    @Override
    public void addMappings() {
        setNotFoundHandler(ErrorHandler.class);
        addRoute("/", com.example.smsotp.server.handlers.IndexHandler.class);
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

    private long configure() {
        long t1 = System.currentTimeMillis();
        freemarkerCfg = new Configuration(new Version(2, 3, 30));
        freemarkerCfg.setDefaultEncoding("UTF-8");
        gson = new Gson();
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
        return System.currentTimeMillis() - t1;
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
}
