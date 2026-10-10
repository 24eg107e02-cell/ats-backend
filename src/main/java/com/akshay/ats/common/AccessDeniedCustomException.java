package com.akshay.ats.common;

public class AccessDeniedCustomException extends RuntimeException {
    public AccessDeniedCustomException(String message) {
        super(message);
    }
}
