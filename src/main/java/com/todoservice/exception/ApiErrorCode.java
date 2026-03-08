package com.todoservice.exception;

/**
 * Stable error codes returned by the API for client-side handling.
 */
public enum ApiErrorCode {
    TODO_NOT_FOUND,
    PAST_DUE_MODIFICATION_FORBIDDEN,
    VALIDATION_FAILED,
    CONFLICT,
    RATE_LIMIT_EXCEEDED,
    INTERNAL_ERROR
}
