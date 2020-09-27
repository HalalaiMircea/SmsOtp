package com.example.smsotp.server;

import android.util.Patterns;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

import static com.example.smsotp.server.WebServer.gson;
import static fi.iki.elonen.NanoHTTPD.mimeTypes;
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
     * POJO to reduce bloat in important handlers
     */
    public static class HttpError {
        public NanoHTTPD.Response.Status status;
        public String uri;
        public String exStacktrace;
        public String description;

        public HttpError(NanoHTTPD.Response.Status status, String uri, Exception exception,
                         String description) {
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
        public abstract NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource,
                                               Map<String, String> urlParams, NanoHTTPD.IHTTPSession session);

        @Override
        public NanoHTTPD.Response put(RouterNanoHTTPD.UriResource uriResource,
                                      Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        @Override
        public NanoHTTPD.Response post(RouterNanoHTTPD.UriResource uriResource,
                                       Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        @Override
        public NanoHTTPD.Response delete(RouterNanoHTTPD.UriResource uriResource,
                                         Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        @Override
        public NanoHTTPD.Response other(String method, RouterNanoHTTPD.UriResource uriResource, Map<String,
                String> urlParams, NanoHTTPD.IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }
    }

    public abstract static class RestHandler implements RouterNanoHTTPD.UriResponder {
        protected String errorDescription;

        @Override
        public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource,
                                      Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return defaultResponse(session);
        }

        @Override
        public NanoHTTPD.Response put(RouterNanoHTTPD.UriResource uriResource,
                                      Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return defaultResponse(session);
        }

        @Override
        public NanoHTTPD.Response post(RouterNanoHTTPD.UriResource uriResource,
                                       Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return defaultResponse(session);
        }

        @Override
        public NanoHTTPD.Response delete(RouterNanoHTTPD.UriResource uriResource,
                                         Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return defaultResponse(session);
        }

        @Override
        public NanoHTTPD.Response other(String method, RouterNanoHTTPD.UriResource uriResource, Map<String,
                String> urlParams, NanoHTTPD.IHTTPSession session) {
            return defaultResponse(session);
        }

        protected NanoHTTPD.Response defaultResponse(NanoHTTPD.IHTTPSession session) {
            HttpError error = new HttpError(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                    session.getUri(), null, errorDescription);
            return newFixedLengthResponse(error.status, mimeTypes().get("json"), gson.toJson(error));
        }

        //protected abstract String errorDescription();
    }

}
