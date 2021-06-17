package com.example.smsotp.server.handlers;

import android.content.Context;
import android.util.Log;

import com.example.smsotp.server.RoutedWebServer.UriResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

import static fi.iki.elonen.NanoHTTPD.MIME_HTML;
import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newChunkedResponse;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class IndexHandler extends HtmlHandler {
    private static final String TAG = "Web_IndexHandler";

    public IndexHandler(UriResource uriResource, Map<String, String> pathParams, IHTTPSession session) {
        super(uriResource, pathParams, session);
    }

    @Override
    public Response doGet() {
        Context context = uriResource.initParameter(Context.class);
        try {
            String uri = "reactapp/index.html";
            InputStream inputStream = context.getAssets().open(uri);

            return newChunkedResponse(Response.Status.OK, MIME_HTML, inputStream);
        } catch (IOException e) {
            Log.e(TAG, "doGet: ", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }
}
