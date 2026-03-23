package com.SmartBank.exception;

public class DuplicateResourceException extends WebException {

    public DuplicateResourceException(String message) {
        super(409, message);
    }
}
