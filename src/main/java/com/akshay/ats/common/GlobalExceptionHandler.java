package com.akshay.ats.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidStageTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidTransition(InvalidStageTransitionException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "INVALID_STAGE_TRANSITION", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateApplicationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_APPLICATION", ex.getMessage(), req);
    }

    @ExceptionHandler({AccessDeniedException.class, AccessDeniedCustomException.class})
    public ResponseEntity<ApiError> handleAccessDenied(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "This operation violates a data constraint (e.g. duplicate entry).", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", java.time.LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "VALIDATION_FAILED");
        body.put("fieldErrors", fieldErrors);
        body.put("message", String.join("; ", fieldErrors.values()));
        body.put("path", req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(org.springframework.web.server.ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(status, status.name(), ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(org.springframework.web.multipart.MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "The file is too large. The limit is 5 MB.", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), error, message, req.getRequestURI()));
    }
}
