package com.example.smsotp.server.dto;

import java.util.Date;
import java.util.List;

public class CommandDto {
    private final int id, userId;
    private final String message;
    private final List<SmsDto.Result> results;
    private final Date executedDate;

    public CommandDto(int id, int userId, String message, List<SmsDto.Result> results, Date executedDate) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.results = results;
        this.executedDate = executedDate;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public List<SmsDto.Result> getResults() {
        return results;
    }

    public Date getExecutedDate() {
        return executedDate;
    }
}
