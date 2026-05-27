package com.SmartBank.exception;


public class AppException extends RuntimeException {

    private final ErrorCode ErrorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.ErrorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return ErrorCode;
    }


}
