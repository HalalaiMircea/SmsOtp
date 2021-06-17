package com.example.smsotp.server;

import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.smsotp.server.RoutedWebServer.UriResource;
import com.google.gson.Gson;

import org.commonjava.mimeparse.MIMEParse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
        public Status status;
        public String uri;
        public String exStacktrace;
        public String description;

        public HttpError(Status status, String uri, Exception exception, String description) {
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
    public abstract static class HtmlHandler implements RoutedWebServer.UriResponder {

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

    public abstract static class RestHandler implements RoutedWebServer.UriResponder {
        protected static final String MIME_JSON = "application/json";
        protected static final String[] supportedMimeTypes = {"application/xml", "text/xml", MIME_JSON};
        protected static final Gson gson = new Gson();
        protected String acceptedMimeType;
        protected Map<String, String> bodyFiles;

        @Override
        public final Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            final Response response = checkErrors(session);
            if (response != null) return response;
            return GET(uriResource, urlParams, session);
        }

        @Override
        public final Response put(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            final Response response = checkErrors(session);
            if (response != null) return response;
            return PUT(uriResource, urlParams, session);
        }

        @Override
        public final Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            final Response response = checkErrors(session);
            if (response != null) return response;
            return POST(uriResource, urlParams, session);
        }

        @Override
        public final Response delete(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            final Response response = checkErrors(session);
            if (response != null) return response;
            return DELETE(uriResource, urlParams, session);
        }

        @Override
        public final Response other(String method, UriResource uriResource, Map<String, String> urlParams,
                                    IHTTPSession session) {
            final Response response = checkErrors(session);
            if (response != null) return response;
            return OTHER(method, uriResource, urlParams, session);
        }

        public Response GET(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return newErrorResponse(Status.METHOD_NOT_ALLOWED,
                    session.getUri(), null, "This HTTP Method is not supported!");
        }


        public Response PUT(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return newErrorResponse(Status.METHOD_NOT_ALLOWED,
                    session.getUri(), null, "This HTTP Method is not supported!");
        }


        public Response POST(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return newErrorResponse(Status.METHOD_NOT_ALLOWED,
                    session.getUri(), null, "This HTTP Method is not supported!");
        }


        public Response DELETE(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return newErrorResponse(Status.METHOD_NOT_ALLOWED,
                    session.getUri(), null, "This HTTP Method is not supported!");
        }


        public Response OTHER(String method, UriResource uriResource, Map<String, String> urlParams,
                              IHTTPSession session) {
            return newErrorResponse(Status.METHOD_NOT_ALLOWED,
                    session.getUri(), null, "This HTTP Method is not supported!");
        }

        protected Response Ok(Object object) {
            return newGsonResponse(Status.OK, object);
        }

        protected Response BadRequest(String uri, Exception exception, String description) {
            return newErrorResponse(Status.BAD_REQUEST, uri, exception, description);
        }

        protected Response Unauthorized(String uri, Exception exception, String description) {
            return newErrorResponse(Status.UNAUTHORIZED, uri, exception, description);
        }

        protected Response InternalError(String uri, Exception exception, String description) {
            return newErrorResponse(Status.INTERNAL_ERROR, uri, exception, description);
        }

        private Response newErrorResponse(Status status, String uri, Exception exception, String description) {
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
                    // We already checked earlier, but Java complains...
                    return newFixedLengthResponse(Status.NOT_ACCEPTABLE, MIME_PLAINTEXT, null);
            }
        }

        @Nullable
        private Response checkErrors(IHTTPSession session) {
            // First check if Accept header is good
            final String acceptHeader = session.getHeaders().get("accept");
            if (acceptHeader == null) {
                return newFixedLengthResponse(Status.NOT_ACCEPTABLE, MIME_PLAINTEXT,
                        "Missing \"Accept\" HTTP header");
            }
            acceptedMimeType = MIMEParse.bestMatch(Arrays.asList(supportedMimeTypes), acceptHeader);
            if (acceptedMimeType.isEmpty())
                return newFixedLengthResponse(Status.NOT_ACCEPTABLE, MIME_PLAINTEXT,
                        "Only accepts " + Arrays.toString(supportedMimeTypes));

            // The check NanoHTTPD boilerplate code for body
            bodyFiles = new HashMap<>();
            NanoHTTPD.Method method = session.getMethod();
            if (NanoHTTPD.Method.PUT == method || NanoHTTPD.Method.POST == method) {
                try {
                    session.parseBody(bodyFiles);
                } catch (IOException ex) {
                    return InternalError(session.getUri(), ex, "SERVER INTERNAL ERROR");
                } catch (NanoHTTPD.ResponseException re) {
                    return newErrorResponse(re.getStatus(), session.getUri(), re, re.getMessage());
                }
            }
            return null;
        }
    }
}
