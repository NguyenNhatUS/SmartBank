package com.SmartBank.exception;

public class InsufficientFundsException extends WebException {

    public InsufficientFundsException(String message) {
        super(400, message);
    }
}
