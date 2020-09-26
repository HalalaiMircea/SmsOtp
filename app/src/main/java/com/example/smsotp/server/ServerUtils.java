package com.example.smsotp.server;

import android.util.Patterns;

import java.util.List;

public class ServerUtils {
    static void validatePhones(List<String> phones) throws IllegalArgumentException {
        validateParams(phones);
        for (String phone : phones) {
            if (!Patterns.PHONE.matcher(phone).matches())
                throw new IllegalArgumentException("One or multiple phone numbers are invalid");
        }
    }

    @SafeVarargs
    static void validateParams(List<String>... paramValues) throws IllegalArgumentException {
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
}
