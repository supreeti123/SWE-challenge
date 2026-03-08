package com.todoservice.dto;

import com.todoservice.entity.TodoItem;
import com.todoservice.entity.TodoItem.TodoStatus;
import java.time.Instant;

/**
 * Response DTO representing todo item details returned by the API.
 */
public record TodoItemResponse(
        Long id,
        String description,
        String status,
        Instant createdAt,
        Instant dueAt,
        Instant doneAt) {

    public static TodoItemResponse fromEntity(TodoItem item) {
        return new TodoItemResponse(
                item.getId(),
                item.getDescription(),
                mapStatus(item.getStatus()),
                item.getCreatedAt(),
                item.getDueAt(),
                item.getDoneAt());
    }

    private static String mapStatus(TodoStatus status) {
        return switch (status) {
            case NOT_DONE -> "not done";
            case DONE -> "done";
            case PAST_DUE -> "past due";
        };
    }
}
