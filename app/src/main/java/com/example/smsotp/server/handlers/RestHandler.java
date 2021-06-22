package com.example.smsotp.server.handlers;

import androidx.annotation.Nullable;

import com.example.smsotp.server.RoutedWebServer;
import com.example.smsotp.server.RoutedWebServer.UriResource;
import com.example.smsotp.server.ServerUtils;
import com.google.gson.Gson;

import org.commonjava.mimeparse.MIMEParse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public abstract class RestHandler extends RoutedWebServer.UriResponder {
    public static final Gson gson = new Gson();
    protected static final String MIME_JSON = "application/json";
    protected static final String[] supportedMimeTypes = {"application/xml", "text/xml", MIME_JSON};
    protected final Map<String, String> mBodyFiles = new HashMap<>();
    protected String acceptedMimeType;

    public RestHandler(UriResource uriResource, Map<String, String> pathParams, IHTTPSession session) {
        super(uriResource, pathParams, session);
    }

    @Nullable
    public final Response checkErrors() {
        // First check if Accept header is good
        final String acceptHeader = session.getHeaders().get("accept");
        if (acceptHeader == null) {
            return newFixedLengthResponse(Status.NOT_ACCEPTABLE, MIME_PLAINTEXT,
                    "Missing \"Accept\" HTTP header");
        }
        acceptedMimeType = MIMEParse.bestMatch(Arrays.asList(supportedMimeTypes), acceptHeader);
        if (acceptedMimeType.isEmpty())
            return newFixedLengthResponse(Status.NOT_ACCEPTABLE, MIME_PLAINTEXT,
                    "Supported Accept header values: " + Arrays.toString(supportedMimeTypes));

        // Then check NanoHTTPD boilerplate code for body
        NanoHTTPD.Method method = session.getMethod();
        if (NanoHTTPD.Method.PUT == method || NanoHTTPD.Method.POST == method) {
            try {
                session.parseBody(mBodyFiles);
            } catch (IOException ex) {
                return InternalError(ex);
            } catch (NanoHTTPD.ResponseException re) {
                return newErrorResponse(re.getStatus(), session.getUri(), re, re.getMessage());
            }
        }
        return null;
    }

    @Override
    public Response doGet() {
        return MethodNotAllowed(session.getMethod());
    }

    @Override
    public Response doPut() {
        return MethodNotAllowed(session.getMethod());
    }

    @Override
    public Response doPost() {
        return MethodNotAllowed(session.getMethod());
    }

    @Override
    public Response doDelete() {
        return MethodNotAllowed(session.getMethod());
    }

    @Override
    public Response doOther(NanoHTTPD.Method method) {
        return MethodNotAllowed(method);
    }

    protected Response Ok(Object object) {
        return newGsonResponse(Status.OK, object);
    }

    protected Response BadRequest(Exception exception, String description) {
        return newErrorResponse(Status.BAD_REQUEST, session.getUri(), exception, description);
    }

    protected Response Unauthorized() {
        return newErrorResponse(Status.UNAUTHORIZED, session.getUri(), null,
                "Incorrect username and/or password!");
    }

    protected Response InternalError(Exception exception) {
        return newErrorResponse(Status.INTERNAL_ERROR, session.getUri(), exception,
                "SERVER INTERNAL ERROR");
    }

    protected Response MethodNotAllowed(NanoHTTPD.Method method) {
        return newErrorResponse(Status.METHOD_NOT_ALLOWED,
                session.getUri(), null, method + " HTTP Method is not supported!");
    }

    private Response newErrorResponse(Status status, String uri, Exception exception, String description) {
        final ServerUtils.HttpError error = new ServerUtils.HttpError(status, uri, exception, description);
        return newGsonResponse(error.status, error);
    }

    private Response newGsonResponse(Response.IStatus status, Object obj) {
        switch (acceptedMimeType) {
            case MIME_JSON:
                return newFixedLengthResponse(status, acceptedMimeType, gson.toJson(obj));
            case "text/xml":
            case "application/xml":
                // This is necessary for XML support... Yes, arrays in XML suck ass
                try {
                    JSONObject json;
                    String tagName;
                    if (obj instanceof List) {
                        json = new JSONObject()
                                .put("element", new JSONArray(gson.toJson(obj)));
                        tagName = "array";
                    } else {
                        json = new JSONObject(gson.toJson(obj));
                        tagName = "root";
                    }
                    return newFixedLengthResponse(status, acceptedMimeType, XML.toString(json, tagName));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            default:
                // We already checked earlier, but Java complains...
                return newFixedLengthResponse(Status.NOT_ACCEPTABLE, MIME_PLAINTEXT, null);
        }
    }
}
