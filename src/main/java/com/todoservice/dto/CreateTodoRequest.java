package com.todoservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request payload for creating a new todo item.
 */
public record CreateTodoRequest(
        @NotBlank(message = "Description must not be blank")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,
        Instant dueAt) {
}
