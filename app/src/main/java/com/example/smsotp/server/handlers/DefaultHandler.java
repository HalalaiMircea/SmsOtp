package com.example.smsotp.server.handlers;

import android.content.Context;

import com.example.smsotp.server.ServerUtils;
import com.example.smsotp.server.RoutedWebServer.UriResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.*;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import static fi.iki.elonen.NanoHTTPD.*;

public class DefaultHandler extends ServerUtils.HtmlHandler {
    private static final String TAG = "Web_DefaultHandler";
    private Context mContext;

    @Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        mContext = uriResource.initParameter(Context.class);
        try {
            String fileName = "reactapp" + session.getUri();
            InputStream iStream = mContext.getAssets().open(fileName);
            String resType = fileName.substring(fileName.lastIndexOf('.') + 1);

            return newChunkedResponse(Status.OK, mimeTypes().get(resType), iStream);
        } catch (IOException e) {
            // If caught, we show 404 Not Found
            return notFoundResponse(session.getUri());
        }
    }

    private Response notFoundResponse(String uri) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                mContext.getAssets().open("error.html"), StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            line = sb.toString().replace("${uri}", uri);

            return newFixedLengthResponse(Status.NOT_FOUND, MIME_HTML, line);
        } catch (IOException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, e.getMessage());
        }
    }
}
