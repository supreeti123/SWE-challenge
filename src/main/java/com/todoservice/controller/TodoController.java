package com.todoservice.controller;

import com.todoservice.dto.CreateTodoRequest;
import com.todoservice.dto.TodoItemResponse;
import com.todoservice.dto.UpdateDescriptionRequest;
import com.todoservice.entity.TodoItem;
import com.todoservice.service.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping
    public ResponseEntity<TodoItemResponse> addItem(@Valid @RequestBody CreateTodoRequest request) {
        TodoItem item = todoService.addItem(request.getDescription(), request.getDueAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(TodoItemResponse.fromEntity(item));
    }

    @PatchMapping("/{id}/description")
    public ResponseEntity<TodoItemResponse> changeDescription(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateDescriptionRequest request) {
        TodoItem item = todoService.changeDescription(id, request.getDescription());
        return ResponseEntity.ok(TodoItemResponse.fromEntity(item));
    }

    @PatchMapping("/{id}/done")
    public ResponseEntity<TodoItemResponse> markDone(@PathVariable @Positive Long id) {
        TodoItem item = todoService.markDone(id);
        return ResponseEntity.ok(TodoItemResponse.fromEntity(item));
    }

    @PatchMapping("/{id}/not-done")
    public ResponseEntity<TodoItemResponse> markNotDone(@PathVariable @Positive Long id) {
        TodoItem item = todoService.markNotDone(id);
        return ResponseEntity.ok(TodoItemResponse.fromEntity(item));
    }

    @GetMapping
    public ResponseEntity<List<TodoItemResponse>> getAllItems(
            @RequestParam(defaultValue = "false") boolean includeAll) {
        List<TodoItem> items = todoService.getAllItems(includeAll);
        List<TodoItemResponse> response = items.stream()
                .map(TodoItemResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoItemResponse> getItemById(@PathVariable @Positive Long id) {
        TodoItem item = todoService.getItemById(id);
        return ResponseEntity.ok(TodoItemResponse.fromEntity(item));
    }
}
