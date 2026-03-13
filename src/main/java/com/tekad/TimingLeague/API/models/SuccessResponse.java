package com.tekad.TimingLeague.API.models;

public class SuccessResponse {
    private boolean success = true;
    private String message;
    private long timestamp;

    public SuccessResponse(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}