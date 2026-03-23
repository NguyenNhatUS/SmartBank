package com.SmartBank.exception;

public class AccountNotActiveException extends WebException {

    public AccountNotActiveException(String message) {
        super(404, message);
    }
}
