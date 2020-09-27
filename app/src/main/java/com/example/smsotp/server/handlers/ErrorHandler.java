package com.example.smsotp.server.handlers;

import com.example.smsotp.server.ServerUtils;
import com.example.smsotp.server.WebServer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import static fi.iki.elonen.NanoHTTPD.MIME_HTML;
import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ErrorHandler implements RouterNanoHTTPD.UriResponder {
    private static final String TAG = "Web_ErrorHandler";

    @Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        ServerUtils.HttpError error = new ServerUtils.HttpError(Response.Status.NOT_FOUND, session.getUri(),
                null, "Resoruce not found!");
        try {
            Template template = WebServer.freemarkerCfg.getTemplate("error");
            StringWriter out = new StringWriter();
            template.process(error.getMapModel(), out);
            return newFixedLengthResponse(error.status, MIME_HTML, out.toString());
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }

    @Override
    public Response put(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

    @Override
    public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

    @Override
    public Response delete(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

    @Override
    public Response other(String method, UriResource uriResource, Map<String, String> urlParams,
                          IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }
}
