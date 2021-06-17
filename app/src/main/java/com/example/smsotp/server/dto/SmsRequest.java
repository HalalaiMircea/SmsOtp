package com.example.smsotp.server.dto;

import java.util.List;

public class SmsRequest {
    private final String username, password;
    private final List<String> phones;
    private final String message;

    public SmsRequest(String username, String password, List<String> phones, String message)
            throws IllegalArgumentException {
        this.username = username;
        this.password = password;
        this.phones = phones;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getPhones() {
        return phones;
    }

    public String getMessage() {
        return message;
    }
}
