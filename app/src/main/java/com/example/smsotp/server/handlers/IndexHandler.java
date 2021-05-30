package com.example.smsotp.server.handlers;

import com.example.smsotp.SmsOtpApplication;
import com.example.smsotp.server.ServerUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

import static fi.iki.elonen.NanoHTTPD.MIME_HTML;
import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newChunkedResponse;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class IndexHandler extends ServerUtils.HtmlHandler {
    private static final String TAG = "Web_IndexHandler";

    @Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        try {
            String uri = "reactapp/index.html";
            InputStream inputStream = SmsOtpApplication.getAppContext().getAssets().open(uri);

            return newChunkedResponse(Response.Status.OK, MIME_HTML, inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }
}
