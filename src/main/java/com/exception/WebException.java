package com.exception;

public class WebException extends RuntimeException {
    private final int statusCode;

    public WebException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
