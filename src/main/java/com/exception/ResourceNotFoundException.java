package com.exception;

public class ResourceNotFoundException extends WebException{

    public ResourceNotFoundException(int statusCode, String message) {
        super(404, message);
    }
}
