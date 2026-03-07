package com.todoservice.exception;

/**
 * Exception thrown when a mutation is attempted on a past-due item.
 */
public class PastDueModificationException extends RuntimeException {

    public PastDueModificationException(Long id) {
        super("Cannot modify todo item " + id + ": item is past due");
    }
}
