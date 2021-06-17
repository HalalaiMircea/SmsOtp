package com.example.smsotp.server;

import android.util.Log;
import android.util.Patterns;

import androidx.annotation.NonNull;

import java.util.List;

import fi.iki.elonen.NanoHTTPD.Response.Status;

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
                exStacktrace = Log.getStackTraceString(exception);
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
}
