package com.todoservice.dto;

import com.todoservice.entity.TodoItem;
import com.todoservice.entity.TodoItem.TodoStatus;
import java.time.LocalDateTime;

public class TodoItemResponse {

    private Long id;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime dueAt;
    private LocalDateTime doneAt;

    public static TodoItemResponse fromEntity(TodoItem item) {
        TodoItemResponse response = new TodoItemResponse();
        response.id = item.getId();
        response.description = item.getDescription();
        response.status = mapStatus(item.getStatus());
        response.createdAt = item.getCreatedAt();
        response.dueAt = item.getDueAt();
        response.doneAt = item.getDoneAt();
        return response;
    }

    private static String mapStatus(TodoStatus status) {
        return switch (status) {
            case NOT_DONE -> "not done";
            case DONE -> "done";
            case PAST_DUE -> "past due";
        };
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public LocalDateTime getDoneAt() {
        return doneAt;
    }
}
