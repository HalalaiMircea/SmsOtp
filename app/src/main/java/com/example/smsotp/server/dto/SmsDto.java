package com.example.smsotp.server.dto;

import java.util.List;

public class SmsDto {
    private final int commandId;
    private final int userId;
    private final String message;
    private final List<Result> results;

    public SmsDto(int commandId, int userId, String message, List<Result> results) {
        this.commandId = commandId;
        this.userId = userId;
        this.message = message;
        this.results = results;
    }

    public int getCommandId() {
        return commandId;
    }

    public int getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public List<Result> getResults() {
        return results;
    }

    public static class Result {
        private final String phone;
        private final SmsResultType status;

        public Result(String phone, SmsResultType status) {
            this.phone = phone;
            this.status = status;
        }

        public String getPhone() {
            return phone;
        }

        public SmsResultType getStatus() {
            return status;
        }
    }

}
