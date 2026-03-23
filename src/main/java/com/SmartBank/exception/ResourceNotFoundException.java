package com.SmartBank.exception;

public class ResourceNotFoundException extends WebException{

    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}
