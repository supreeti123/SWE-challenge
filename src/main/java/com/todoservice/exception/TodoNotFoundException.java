package com.todoservice.exception;

/**
 * Exception thrown when a todo item cannot be found by id.
 */
public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(Long id) {
        super("Todo item not found with id: " + id);
    }
}
