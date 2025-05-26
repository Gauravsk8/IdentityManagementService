package com.example.IdentityManagementService.exceptions;

import lombok.Getter;

@Getter
public class TimesheetException extends RuntimeException {
    private final String errorCode;

    public TimesheetException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TimesheetException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

