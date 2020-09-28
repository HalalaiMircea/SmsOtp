package com.example.smsotp.server.handlers;

import com.example.smsotp.server.ServerUtils;
import com.example.smsotp.server.WebServer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import static fi.iki.elonen.NanoHTTPD.mimeTypes;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class IndexHandler extends ServerUtils.HtmlHandler {
    private static final String TAG = "Web_IndexHandler";

    @Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        try {
            Template template = WebServer.freemarkerCfg.getTemplate("index");
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("ip", session.getRemoteHostName());

            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            return newFixedLengthResponse(out.toString());
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, mimeTypes().get("txt"),
                    e.getMessage());
        }
    }
}