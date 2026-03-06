package com.todoservice.exception;

public class PastDueModificationException extends RuntimeException {

    public PastDueModificationException(Long id) {
        super("Cannot modify todo item " + id + ": item is past due");
    }
}
