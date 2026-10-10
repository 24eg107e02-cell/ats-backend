package com.akshay.ats.common;

import java.time.LocalDateTime;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path);
    }
}
