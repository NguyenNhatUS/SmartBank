package com.SmartBank.entity.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ========== COMMON ==========
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR(1000, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(1001, "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1002, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1003, "Forbidden", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1004, "Resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(1005, "Method not allowed", HttpStatus.METHOD_NOT_ALLOWED),

    // ========== AUTH ==========
    USERNAME_ALREADY_EXISTS(1100, "Username already exists", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1101, "Email already exists", HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_EXISTS(1102, "Phone number already exists", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_OR_PASSWORD(1103, "Invalid username or password", HttpStatus.UNAUTHORIZED),
    USER_NOT_ACTIVE(1104, "User is not active", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1105, "Invalid token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1106, "Token expired", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND(1107, "Refresh token not found", HttpStatus.NOT_FOUND),

    // ========== CUSTOMER ==========
    CUSTOMER_NOT_FOUND(1200, "Customer not found", HttpStatus.NOT_FOUND),
    CUSTOMER_ALREADY_EXISTS(1201, "Customer already exists", HttpStatus.BAD_REQUEST),
    CUSTOMER_INVALID_DATA(1202, "Invalid customer data", HttpStatus.BAD_REQUEST),

    // ========== EMPLOYEE ==========
    EMPLOYEE_NOT_FOUND(1300, "Employee not found", HttpStatus.NOT_FOUND),
    EMPLOYEE_ALREADY_EXISTS(1301, "Employee already exists", HttpStatus.BAD_REQUEST),

    // ========== ACCOUNT ==========
    ACCOUNT_NOT_FOUND(1400, "Account not found", HttpStatus.NOT_FOUND),
    ACCOUNT_ALREADY_EXISTS(1401, "Account already exists", HttpStatus.BAD_REQUEST),
    ACCOUNT_INSUFFICIENT_BALANCE(1402, "Insufficient balance", HttpStatus.BAD_REQUEST),
    ACCOUNT_LOCKED(1403, "Account is locked", HttpStatus.FORBIDDEN),
    ACCOUNT_INACTIVE(1404, "Account is inactive", HttpStatus.BAD_REQUEST),

    // ========== TRANSACTION ==========
    TRANSACTION_NOT_FOUND(1500, "Transaction not found", HttpStatus.NOT_FOUND),
    INVALID_TRANSACTION_AMOUNT(1501, "Invalid transaction amount", HttpStatus.BAD_REQUEST),
    TRANSACTION_FAILED(1502, "Transaction failed", HttpStatus.INTERNAL_SERVER_ERROR),
    SAME_ACCOUNT_TRANSFER(1503, "Cannot transfer to the same account", HttpStatus.BAD_REQUEST),

    // ========== VALIDATION ==========
    INVALID_KEY(1600, "Invalid key", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT(1601, "Invalid format", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD(1602, "Missing required field", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
