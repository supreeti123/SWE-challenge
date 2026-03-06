package com.todoservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class CreateTodoRequest {

    @NotBlank(message = "Description must not be blank")
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
