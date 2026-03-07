package com.todoservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Request payload for creating a new todo item.
 */
public class CreateTodoRequest {

    @NotBlank(message = "Description must not be blank")
    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private LocalDateTime dueAt;

    public CreateTodoRequest() {
    }

    public CreateTodoRequest(String description, LocalDateTime dueAt) {
        this.description = description;
        this.dueAt = dueAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }
}
