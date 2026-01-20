package com.classifiedsplatform.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors,
        String path,
        LocalDateTime timestamp,
        String requestId
) {
    public record FieldError(
            String field,
            String message
    ) {
    }

    public static ErrorResponse of(int status, String error, String message, String path, String requestId) {
        return new ErrorResponse(status, error, message, null, path, LocalDateTime.now(), requestId);
    }

    public static ErrorResponse withFieldErrors(
            int status,
            String error,
            String message,
            List<FieldError> fieldErrors,
            String path,
            String requestId
    ) {
        return new ErrorResponse(status, error, message, fieldErrors, path, LocalDateTime.now(), requestId);
    }
}