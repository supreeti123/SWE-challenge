package com.todoservice.exception;

public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(Long id) {
        super("Todo item not found with id: " + id);
    }
}
