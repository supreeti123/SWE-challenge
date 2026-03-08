package com.todoservice.controller;

import com.todoservice.dto.CreateTodoRequest;
import com.todoservice.dto.TodoItemResponse;
import com.todoservice.dto.UpdateDescriptionRequest;
import com.todoservice.entity.TodoItem;
import com.todoservice.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

/**
 * REST controller exposing todo management endpoints.
 */
@RestController
@Validated
@RequestMapping("/api/todos")
@Tag(name = "Todos", description = "Operations for managing todo items")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping
    @Operation(
            summary = "Add a new todo item",
            responses = {
                @ApiResponse(responseCode = "201", description = "Todo item created"),
                @ApiResponse(responseCode = "400", description = "Invalid request payload"),
                @ApiResponse(responseCode = "429", description = "Write rate limit exceeded")
            })
    public ResponseEntity<TodoItemResponse> addItem(@Valid @RequestBody CreateTodoRequest request) {
        TodoItem item = todoService.addItem(request.getDescription(), request.getDueAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(TodoItemResponse.fromEntity(item));
    }

    @PatchMapping("/{id}/description")
    @Operation(
            summary = "Change todo description",
            responses = {
                @ApiResponse(responseCode = "200", description = "Todo item updated"),
                @ApiResponse(responseCode = "404", description = "Todo item not found"),
                @ApiResponse(responseCode = "422", description = "Past-due items cannot be modified"),
                @ApiResponse(responseCode = "429", description = "Write rate limit exceeded")
            })
    public ResponseEntity<TodoItemResponse> changeDescription(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateDescriptionRequest request) {
        TodoItem item = todoService.changeDescription(id, request.getDescription());
        return ResponseEntity.ok(TodoItemResponse.fromEntity(item));
    }

    @PatchMapping("/{id}/done")
    @Operation(
            summary = "Mark todo as done",
            responses = {
                @ApiResponse(responseCode = "200", description = "Todo item marked as done"),
                @ApiResponse(responseCode = "404", description = "Todo item not found"),
                @ApiResponse(responseCode = "422", description = "Past-due items cannot be modified"),
                @ApiResponse(responseCode = "429", description = "Write rate limit exceeded")
            })
    public ResponseEntity<TodoItemResponse> markDone(@PathVariable @Positive Long id) {
        TodoItem item = todoService.markDone(id);
        return ResponseEntity.ok(TodoItemResponse.fromEntity(item));
    }

    @PatchMapping("/{id}/not-done")
    @Operation(
            summary = "Mark todo as not done",
            responses = {
                @ApiResponse(responseCode = "200", description = "Todo item marked as not done"),
                @ApiResponse(responseCode = "404", description = "Todo item not found"),
                @ApiResponse(responseCode = "422", description = "Past-due items cannot be modified"),
                @ApiResponse(responseCode = "429", description = "Write rate limit exceeded")
            })
    public ResponseEntity<TodoItemResponse> markNotDone(@PathVariable @Positive Long id) {
        TodoItem item = todoService.markNotDone(id);
        return ResponseEntity.ok(TodoItemResponse.fromEntity(item));
    }

    @GetMapping
    @Operation(
            summary = "List todo items with pagination",
            description = "Returns not-done items by default. Set includeAll=true to return all statuses.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Paged todo items returned"),
                @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
            })
    public ResponseEntity<Page<TodoItemResponse>> getAllItems(
            @RequestParam(defaultValue = "false")
            @Parameter(description = "Include all statuses instead of only not done")
            boolean includeAll,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<TodoItemResponse> response = todoService.getAllItems(includeAll, pageable)
                .map(TodoItemResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get todo item by id",
            responses = {
                @ApiResponse(responseCode = "200", description = "Todo item returned"),
                @ApiResponse(responseCode = "404", description = "Todo item not found")
            })
    public ResponseEntity<TodoItemResponse> getItemById(@PathVariable @Positive Long id) {
        TodoItem item = todoService.getItemById(id);
        return ResponseEntity.ok(TodoItemResponse.fromEntity(item));
    }
}
