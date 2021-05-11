package com.example.smsotp.server;

import android.util.Patterns;

import org.commonjava.mimeparse.MIMEParse;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

import static com.example.smsotp.server.WebServer.gson;
import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ServerUtils {
    public static void validatePhones(List<String> phones) throws IllegalArgumentException {
        validateParams(phones);
        for (String phone : phones) {
            if (!Patterns.PHONE.matcher(phone).matches())
                throw new IllegalArgumentException("One or multiple phone numbers are invalid");
        }
    }

    @SafeVarargs
    public static void validateParams(List<String>... paramValues) throws IllegalArgumentException {
        // If request query key for this value is missing
        for (List<String> paramValue : paramValues) {
            if (paramValue == null || paramValue.isEmpty())
                throw new IllegalArgumentException("Request param is either null or empty");

            // If any element is invalid
            for (String value : paramValue) {
                if (value == null || value.trim().isEmpty())
                    throw new IllegalArgumentException("A parameter is blank");
            }
        }
    }

    /**
     * Object to reduce bloat in important handlers
     */
    public static class HttpError {
        public Response.Status status;
        public String uri;
        public String exStacktrace;
        public String description;

        public HttpError(Response.Status status, String uri, Exception exception, String description) {
            this.status = status;
            this.uri = uri;
            this.description = description;
            if (exception != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exception.printStackTrace(pw);
                exStacktrace = sw.toString();
            }
        }

        public Map<String, Object> getMapModel() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", status.getDescription().replaceFirst(" ", " - "));
            map.put("uri", uri);
            map.put("description", description != null ? description : status.getDescription());
            map.put("exStacktrace", exStacktrace);
            return map;
        }
    }

    /**
     * Base handler for simple html endpoints, like index or error
     */
    public abstract static class HtmlHandler implements RouterNanoHTTPD.UriResponder {

        @Override
        public abstract Response get(UriResource uriResource, Map<String, String> urlParams,
                                     IHTTPSession session);

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

    public abstract static class RestHandler implements RouterNanoHTTPD.UriResponder {
        protected static final String MIME_JSON = "application/json";
        protected static final String[] supportedMimeTypes = {"application/xml", "text/xml", MIME_JSON};
        protected String acceptedMimeType = MIME_JSON;

        @Override
        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return defaultResponse(session);
        }

        @Override
        public Response put(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return defaultResponse(session);
        }

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return defaultResponse(session);
        }

        @Override
        public Response delete(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return defaultResponse(session);
        }

        @Override
        public Response other(String method, UriResource uriResource, Map<String, String> urlParams,
                              IHTTPSession session) {
            return defaultResponse(session);
        }

        protected Object postGson(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return defaultResponse(session);
        }

        protected Response defaultResponse(IHTTPSession session) {
            final String acceptHeader = session.getHeaders().get("accept");
            if (acceptHeader != null) {
                acceptedMimeType = MIMEParse.bestMatch(Arrays.asList(supportedMimeTypes), acceptHeader);
            }
            HttpError error = new HttpError(Response.Status.METHOD_NOT_ALLOWED,
                    session.getUri(), null, "This Method is not supported!");
            return handleHttpError(error);
        }

        protected Response handleHttpError(Response.Status status, String uri, Exception exception, String description) {
            return handleHttpError(new HttpError(status, uri, exception, description));
        }

        protected Response handleHttpError(HttpError error) {
            switch (acceptedMimeType) {
                case MIME_JSON:
                    return newFixedLengthResponse(error.status, acceptedMimeType, gson.toJson(error));
                case "text/xml":
                case "application/xml":
                    try {
                        return newFixedLengthResponse(error.status, acceptedMimeType,
                                XML.toString(new JSONObject(gson.toJson(error)), "error"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                default:
                    // In case we don't support the type requested by client
                    return newFixedLengthResponse(Response.Status.NOT_ACCEPTABLE, MIME_PLAINTEXT, null);
            }
        }
    }
}
