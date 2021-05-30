package com.example.smsotp.server.handlers;

import com.example.smsotp.SmsOtpApplication;
import com.example.smsotp.server.ServerUtils;
import com.example.smsotp.server.WebServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.*;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import static fi.iki.elonen.NanoHTTPD.*;

public class DefaultHandler extends ServerUtils.HtmlHandler {
    private static final String TAG = "Web_ErrorHandler";

    @Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {

        try {
            String fileName = "reactapp" + session.getUri();
            InputStream iStream = SmsOtpApplication.getAppContext().getAssets().open(fileName);
            String resType = fileName.substring(fileName.lastIndexOf('.') + 1);

            return newChunkedResponse(Response.Status.OK, mimeTypes().get(resType), iStream);
        } catch (IOException e) {
            // If caught, we show 404 Not Found
            try {
                ServerUtils.HttpError error = new ServerUtils.HttpError(Response.Status.NOT_FOUND, session.getUri(),
                        e, "Resoruce not found!");
                Template template = WebServer.freemarkerCfg.getTemplate("error");
                StringWriter out = new StringWriter();
                template.process(error.getMapModel(), out);
                return newFixedLengthResponse(error.status, MIME_HTML, out.toString());
            } catch (IOException | TemplateException ioException) {
                ioException.printStackTrace();
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
            }
        }
    }
}
