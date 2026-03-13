package com.tekad.TimingLeague.API.models;

public class ErrorResponse {
    private boolean success = false;
    private String error;
    private int code;
    private long timestamp;

    public ErrorResponse(String error, int code) {
        this.error = error;
        this.code = code;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public int getCode() { return code; }
    public long getTimestamp() { return timestamp; }
}