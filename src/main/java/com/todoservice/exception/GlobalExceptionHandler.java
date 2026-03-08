package com.todoservice.exception;

import com.todoservice.web.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized REST exception mapper for consistent API error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Standardized API error response body.
     */
    public record ErrorResponse(
            String code,
            String message,
            String path,
            Instant timestamp,
            String requestId) {
    }

    @ExceptionHandler(TodoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TodoNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ApiErrorCode.TODO_NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(PastDueModificationException.class)
    public ResponseEntity<ErrorResponse> handlePastDue(PastDueModificationException ex, HttpServletRequest request) {
        return error(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCode.PAST_DUE_MODIFICATION_FORBIDDEN,
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, ex.getMessage(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                ApiErrorCode.CONFLICT,
                "Concurrent update detected. Please retry the request.",
                request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(buildError(ApiErrorCode.RATE_LIMIT_EXCEEDED, ex.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_ERROR,
                "Unexpected error occurred.",
                request);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(buildError(code, message, request));
    }

    private ErrorResponse buildError(ApiErrorCode code, String message, HttpServletRequest request) {
        return new ErrorResponse(
                code.name(),
                message,
                request.getRequestURI(),
                Instant.now(),
                MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY));
    }
}
