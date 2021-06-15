package com.example.smsotp.server;

import android.util.Patterns;

import androidx.annotation.NonNull;

import org.commonjava.mimeparse.MIMEParse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
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
    @NonNull
    public static List<String> validatePhones(List<String> phones) throws IllegalArgumentException {
        phones = validateParam(phones);
        for (String phone : phones) {
            if (!Patterns.PHONE.matcher(phone).matches())
                throw new IllegalArgumentException("One or multiple phone numbers are invalid");
        }
        return phones;
    }

    @NonNull
    public static List<String> validateParam(List<String> param) throws IllegalArgumentException {
        if (param == null) throw new IllegalArgumentException("A parameter is missing");
        // If any element is invalid
        for (String value : param) {
            if (value == null || value.trim().isEmpty())
                throw new IllegalArgumentException("A parameter is blank");
        }
        return param;
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

        /*public Map<String, Object> getMapModel() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", status.getDescription().replaceFirst(" ", " - "));
            map.put("uri", uri);
            map.put("description", description != null ? description : status.getDescription());
            map.put("exStacktrace", exStacktrace);
            return map;
        }*/
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

        protected Response Ok(Object object) {
            return newGsonResponse(Response.Status.OK, object);
        }

        protected Response BadRequest(String uri, Exception exception, String description) {
            return newErrorResponse(Response.Status.BAD_REQUEST, uri, exception, description);
        }

        protected Response Unauthorized(String uri, Exception exception, String description) {
            return newErrorResponse(Response.Status.UNAUTHORIZED, uri, exception, description);
        }

        protected Response InternalError(String uri, Exception exception, String description) {
            return newErrorResponse(Response.Status.INTERNAL_ERROR, uri, exception, description);
        }

        private Response newErrorResponse(Response.Status status, String uri, Exception exception, String description) {
            final HttpError error = new HttpError(status, uri, exception, description);
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
                    // In case we don't support the mimetype requested by client
                    return newFixedLengthResponse(Response.Status.NOT_ACCEPTABLE, MIME_PLAINTEXT, null);
            }
        }

        private Response defaultResponse(IHTTPSession session) {
            final String acceptHeader = session.getHeaders().get("accept");
            if (acceptHeader != null) {
                acceptedMimeType = MIMEParse.bestMatch(Arrays.asList(supportedMimeTypes), acceptHeader);
            }
            return newErrorResponse(Response.Status.METHOD_NOT_ALLOWED,
                    session.getUri(), null, "This HTTP Method is not supported!");
        }
    }
}
